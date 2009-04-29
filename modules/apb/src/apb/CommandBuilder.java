

// Copyright 2008-2009 Emilio Lopez-Gabeiras
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License
//


package apb;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.TreeMap;

import apb.metadata.BuildTarget;
import apb.metadata.DefaultTarget;
import apb.metadata.ProjectElement;

import apb.utils.NameUtils;

import org.jetbrains.annotations.NotNull;
//
// User: emilio
// Date: Mar 17, 2009
// Time: 4:47:11 PM

//
public class CommandBuilder
{
    //~ Instance fields ......................................................................................

    private SortedMap<String, Command> commands;

    //~ Constructors .........................................................................................

    public CommandBuilder(ProjectElement element)
    {
        this(element.getClass());
    }

    private CommandBuilder(Class<? extends ProjectElement> c)
    {
        commands = new TreeMap<String, Command>();
        commands.putAll(extensionCommands);
        commands.putAll(buildCommands(c));
        commands.put(HELP_COMMAND.getName(), HELP_COMMAND);
    }

    //~ Methods ..............................................................................................

    public Collection<Command> listCommands()
    {
        return commands.values();
    }

    public SortedMap<String, Command> commands()
    {
        return commands;
    }

    private static boolean hasRightSignature(Method method)
    {
        Class<?>[] pars = method.getParameterTypes();
        return pars.length == 1 && pars[0].equals(Environment.class) &&
               java.lang.reflect.Modifier.isPublic(method.getModifiers()) &&
               method.getReturnType().equals(Void.TYPE);
    }

    private static Map<String, InstanceCommand> buildCommands(Class<? extends ProjectElement> clazz)
    {
        Map<String, InstanceCommand> commands = new HashMap<String, InstanceCommand>();

        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            addCommandsForClass(c.getMethods(), commands);
            addDefaultCommand(c, commands);
        }

        // Load direct dependencies
        for (InstanceCommand command : commands.values()) {
            command.solveDirectDependencies(commands);
        }

        return commands;
    }

    private static void addCommandsForClass(final Method[] methods, Map<String, InstanceCommand> commands)
    {
        for (Method method : methods) {
            BuildTarget a = method.getAnnotation(BuildTarget.class);

            if (a != null && hasRightSignature(method)) {
                InstanceCommand cmd = new InstanceCommand(method, a);
                commands.put(cmd.getName(), cmd);
            }
        }
    }

    /**
     * Add the default command to the map if it is not yet present
     * @param c The class to look commands for
     * @param commands
     */
    private static void addDefaultCommand(Class<?> c, Map<String, InstanceCommand> commands)
    {
        if (!commands.containsKey(Command.DEFAULT_COMMAND)) {
            DefaultTarget a = c.getAnnotation(DefaultTarget.class);

            if (a != null) {
                InstanceCommand def = commands.get(a.value());

                if (def == null) {
                    throw new IllegalStateException("Cannot assign default command " + a.value());
                }

                commands.put(Command.DEFAULT_COMMAND, def);
            }
        }
    }

    //~ Static fields/initializers ...........................................................................

    private static final Command HELP_COMMAND = new HelpCommand();

    private static Map<String, Command> extensionCommands;

    static {
        extensionCommands = new HashMap<String, Command>();

        for (Command cmd : ServiceLoader.load(Command.class)) {
            extensionCommands.put(cmd.getQName(), cmd);
        }
    }

    //~ Inner Classes ........................................................................................

    private static class HelpCommand
        extends Command
    {
        public HelpCommand()
        {
            super("help", "List the available commands with a brief description", false);
        }

        public void invoke(ProjectElement projectElement, Environment env)
        {
            System.err.println("Commands for '" + projectElement.getName() + "' : ");

            final Collection<Command> cmds = env.getHelper(projectElement).listCommands().values();

            for (Command cmd : cmds) {
                if (!cmd.hasNameSpace()) {
                    printSpaces();
                    printCommand(cmd);
                }
            }

            String lastNameSpace = null;

            for (Command cmd : cmds) {
                if (cmd.hasNameSpace()) {
                    final String ns = cmd.getNameSpace();

                    if (!ns.equals(lastNameSpace)) {
                        printSpaces();
                        System.err.println(ns + ":");
                        lastNameSpace = ns;
                    }

                    printSpaces();
                    printCommand(cmd);
                }
            }

            System.exit(0);
        }

        private static void printSpaces()
        {
            System.err.print("    ");
        }

        private static void printCommand(Command cmd)
        {
            System.err.printf(cmd.hasNameSpace() ? "  %-18s: %s\n" : "%-20s: %s\n", cmd.getName(), cmd.getDescription());
        }
    }

    private static class InstanceCommand
        extends Command
    {
        private List<Command> directDependencies;

        private final Method method;

        protected InstanceCommand(@NotNull String name, @NotNull Method method, @NotNull String description, boolean recursive)
        {
            super(name, description, recursive);
            this.method = method;
            directDependencies = new ArrayList<Command>();
        }

        InstanceCommand(Method method, BuildTarget annotation)
        {
            this(annotation.name().isEmpty() ? NameUtils.idFromMember(method) : annotation.name(), method,
                 annotation.description(), annotation.recursive());
        }

        @NotNull @Override public List<Command> getDirectDependencies()
        {
            return directDependencies;
        }

        public void invoke(ProjectElement element, Environment env)
        {
            try {
                if (Modifier.isStatic(method.getModifiers())) {
                    method.invoke(null, element, env);
                }
                else {
                    method.invoke(element, env);
                }
            }
            catch (IllegalAccessException e) {
                env.handle(e);
            }
            catch (InvocationTargetException e) {
                final Throwable t = e.getCause();
                env.handle(t);
            }
        }

        //    private boolean checkTarget(Environment env) {
        //        boolean result = true;
        //        final BuildTarget t = method.getAnnotation(BuildTarget.class);
        //        if (!t.target().isEmpty() && !t.source().isEmpty()) {
        //            File source = env.fileFromBase(t.source());
        //            File target = env.fileFromBase(t.target());
        //            result = source.lastModified() >= target.lastModified();
        //        }
        //        return result;
        //
        //    }

        void solveDirectDependencies(Map<String, InstanceCommand> commandsMap)
        {
            final BuildTarget target = method.getAnnotation(BuildTarget.class);

            for (String dep : target.depends()) {
                Command cmd = commandsMap.get(dep);

                if (cmd != null) {
                    directDependencies.add(cmd);
                }
            }

            if (!target.before().isEmpty()) {
                InstanceCommand cmd = commandsMap.get(target.before());

                if (cmd != null) {
                    cmd.directDependencies.add(this);
                }
            }
        }
    }
}
