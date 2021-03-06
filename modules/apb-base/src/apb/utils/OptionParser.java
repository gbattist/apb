

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


package apb.utils;

import java.util.ArrayList;
import java.util.List;

import apb.Messages;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import static apb.Apb.exit;

import static apb.utils.StringUtils.nChars;

/**
 * This class parses command line arguments using two alternatives:
 * one-letter options which must be preceded by '-' and
 * long options which must be preceded by '--'.  Multiple boolean
 * one-letter options can be combined (preceding them with a single
 * '-').
 */
public abstract class OptionParser
{
    //~ Instance fields ......................................................................................

    protected final List<String> arguments;

    @NotNull protected final List<Option> options;
    private final boolean                 exitOnStopParsing = true;

    private final String appName;

    //~ Constructors .........................................................................................

    protected OptionParser(final List<String> args, String name)
    {
        arguments = args;
        appName = name;
        options = new ArrayList<Option>();
        options.add(new HelpOption(this));
        options.add(new VersionOption(this));
    }

    //~ Methods ..............................................................................................

    public abstract void printVersion();

    @NotNull public List<Option> getOptions()
    {
        return options;
    }

    public List<String> getArguments()
    {
        return arguments;
    }

    public String getArgShortDescription()
    {
        return "";
    }

    public String[] getArgFullDescription()
    {
        return null;
    }

    public final Option<String> addOption(final char letter, @NotNull final String name,
                                          @NotNull String description, String valueDescription)
    {
        return addOption(String.class, letter, name, description, valueDescription);
    }

    public final Option<String> addOption(@NotNull final String name, @NotNull String description,
                                          @NotNull String valueDescription)
    {
        return addOption(String.class, '\0', name, description, valueDescription);
    }

    public void printHelp()
    {
        System.err.println(getAppName() + " " + Messages.OPT_IN_BRACKETS + " " + getArgShortDescription());
        System.err.println();

        System.err.println(Messages.WHERE);

        final String[] full = getArgFullDescription();

        if (full != null) {
            for (String line : full) {
                System.err.print(SPACES);
                System.err.println(line);
            }

            System.err.println();
        }

        System.err.println(Messages.OPTIONS);

        int len = 0;

        for (Option opt : options) {
            len = Math.max(len, opt.getHelp().length());
        }

        for (Option opt : options) {
            String ops = opt.getHelp();
            System.err.println(ops + nChars(len - ops.length(), ' ') + ": " + opt.getDescription());
        }
    }

    public String getAppName()
    {
        return appName;
    }

    public final Option<Boolean> addBooleanOption(final char letter, @NotNull final String name,
                                                  @NotNull String description)
    {
        return addOption(Boolean.class, letter, name, description, "");
    }

    public final Option<Integer> addIntegerOption(final char letter, @NotNull final String name,
                                                  @NotNull String description, String valueDescription)
    {
        return addOption(Integer.class, letter, name, description, valueDescription);
    }

    public List<String> parse()
    {
        int i;

        for (i = 0; i < arguments.size(); ++i) {
            String arg = arguments.get(i);

            if (!arg.startsWith("-")) {
                break;
            }

            if ("--".equals(arg) || "-".equals(arg)) {
                i++;
                break;
            }

            if (arg.charAt(1) == '-') {
                Option opt = findOption(arg);
                i = execute(opt, i);
            }
            else {
                for (int j = 1; j < arg.length(); j++) {
                    char   optChar = arg.charAt(j);
                    Option opt = findOption(optChar);

                    // Check if the value is embedded (e.g -Daaaa)
                    if (!opt.isBoolean() && j < arg.length() - 1) {
                        opt.execute(arg.substring(j + 1));
                        break;
                    }

                    i = execute(opt, i);
                }
            }
        }

        return new ArrayList<String>(arguments.subList(i, arguments.size()));
    }

    Option findOption(char option)
    {
        for (Option o : options) {
            if (o.getLetter() == option) {
                return o;
            }
        }

        printError(Messages.INVOPT("-" + option));
        stopParsing();
        throw new RuntimeException();
    }

    Option findOption(String option)
    {
        final boolean negation = option.startsWith(NEG_PREFIX);

        option = option.substring(negation ? NEG_PREFIX.length() : 2);

        for (Option o : options) {
            if (o.getName().equals(option)) {
                return negation ? o.negate() : o;
            }
        }

        printError(Messages.INVOPT("'--" + option + "'"));
        stopParsing();
        throw new RuntimeException();
    }

    private int execute(Option opt, int i)
    {
        if (opt.isBoolean()) {
            opt.execute("true");
        }
        else {
            if (i == arguments.size() - 1) {
                printError(Messages.EXPECTEDARG(opt.getLetter(), opt.getName()));
                stopParsing();
            }

            opt.execute(arguments.get(++i));
        }

        return i;
    }

    private void printError(String msg)
    {
        System.err.println(msg);
    }

    private <T> Option<T> addOption(@NotNull final Class<T> type, final char letter,
                                    @NotNull final String name, @NotNull String description,
                                    @NotNull String valueDescription)
    {
        final Option<T> opt = new Option<T>(this, type, letter, name, description, valueDescription);
        options.add(opt);
        return opt;
    }

    private void stopParsing()
    {
        if (exitOnStopParsing) {
            exit(1);
        }

        throw new RuntimeException();
    }

    //~ Static fields/initializers ...........................................................................

    @NonNls private static final String SPACES = "    ";

    @NonNls private static final String NEG_PREFIX = "--no-";

    //~ Inner Classes ........................................................................................

    /**
    * User: emilio
    * Date: Aug 29, 2005
    * Time: 12:37:38 PM
    */
    public static class Option<T>
    {
        final OptionParser     optionParser;
        private boolean        canRepeat;
        private final char     letter;
        private final Class<T> type;
        private final List<T>  validValues;

        private final List<T> values;
        private final String  description;
        private final String  name;
        private final String  valueDescription;

        private T value;

        Option(OptionParser optionParser, final Class<T> type, final char letter, @NotNull final String name,
               @NotNull String description, @NotNull String valueDescription)
        {
            this.optionParser = optionParser;
            this.letter = letter;
            this.name = name.startsWith("--") ? name.substring(2) : name;
            this.type = type;
            this.description = description;
            validValues = new ArrayList<T>();
            values = new ArrayList<T>();
            this.valueDescription = valueDescription;
            setValue("");
        }

        public List<T> getValues()
        {
            return values;
        }

        public char getLetter()
        {
            return letter;
        }

        public String getName()
        {
            return name;
        }

        public final T getValue()
        {
            return value;
        }

        public String getDescription()
        {
            String result = description;

            if (!validValues.isEmpty()) {
                StringBuilder str = new StringBuilder();

                for (T validValue : validValues) {
                    str.append(str.length() == 0 ? '[' : '|');
                    str.append(String.valueOf(validValue));
                }

                str.append(']');
                result += " " + str.toString();
            }

            return result;
        }

        public void addValidValue(T val)
        {
            validValues.add(val);
        }

        public String toString()
        {
            return name;
        }

        public String getValueDescription()
        {
            return valueDescription;
        }

        public void setCanRepeat(boolean canRepeat)
        {
            this.canRepeat = canRepeat;
        }

        public void setValue(String str)
        {
            value =
                type.cast(type == Integer.class ? str.isEmpty() ? 0 : Integer.valueOf(str)
                                                : type == Boolean.class ? Boolean.valueOf(str) : str);
        }

        public Option<T> negate()
        {
            if (!isBoolean()) {
                optionParser.printError(Messages.NONBOOLNEG(letter, name));
                optionParser.stopParsing();
            }

            final Option<T> result =
                new Option<T>(optionParser, type, letter, name, description, valueDescription);
            result.value = type.cast(!(Boolean) value);
            return result;
        }

        public List<T> getValidValues()
        {
            return validValues;
        }

        void execute(final String str)
        {
            setValue(str);

            if (canRepeat) {
                values.add(value);
            }

            if (!validValues.isEmpty() && !validValues.contains(value)) {
                optionParser.printError(Messages.INVARG(str, name));
                optionParser.stopParsing();
            }
        }

        boolean isBoolean()
        {
            return type == Boolean.class;
        }

        private String getHelp()
        {
            StringBuilder result = new StringBuilder(SPACES);

            if (letter == 0) {
                result.append("   ");
            }
            else {
                result.append('-').append(letter).append(',');
            }

            result.append(" --");
            result.append(getName());

            if (getValueDescription() != null) {
                result.append(' ').append(getValueDescription());
            }

            return result.toString();
        }

        @NonNls static final String HELP = "help";
        @NonNls static final String VERSION = "version";
    }

    private static class HelpOption
        extends Option<Boolean>
    {
        public HelpOption(OptionParser optionParser)
        {
            super(optionParser, Boolean.class, 'h', Option.HELP, Messages.HELP, "");
        }

        public void execute(String v)
        {
            optionParser.printHelp();
            exit(0);
        }
    }

    private static class VersionOption
        extends Option<Boolean>
    {
        public VersionOption(OptionParser optionParser)
        {
            super(optionParser, Boolean.class, '\0', Option.VERSION, Messages.VERSION, "");
        }

        public void execute(String v)
        {
            optionParser.printVersion();
            exit(0);
        }
    }
}
