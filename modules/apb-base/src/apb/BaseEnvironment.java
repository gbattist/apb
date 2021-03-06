

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

import java.io.File;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

import apb.utils.DebugOption;
import apb.utils.FileUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static apb.utils.CollectionUtils.copyProperties;

/**
 * This class represents an Environment that includes common services like:
 * - Properties
 * - Options
 * - Logging
 * - Error handling
 */

final class BaseEnvironment
    extends DefaultEnvironment
{
    //~ Instance fields ......................................................................................

    /**
     * Control what to show when logging
     */

    @NotNull protected final EnumSet<DebugOption> debugOptions;

    /**
     * The directory where the project definition files are stored
     */
    @Nullable File projectsHome;

    private boolean failOnError = true;
    private boolean forceBuild;

    private boolean nonRecursive;

    /**
     * Processing and messaging options
     */
    private boolean quiet;

    /**
     * Override properties are argument properties
     * They take preference over the properties defined in project elements
     */
    @NotNull private final Map<String, String> overrideProperties;

    /**
     * The current ProjectBuilder
     */
    private ProjectBuilder currentProjectBuilder;

    //~ Constructors .........................................................................................

    /**
     * Crate an Environment
     * @param override
     */
    BaseEnvironment(@NotNull Logger logger, Map<String, String> override)
    {
        super(logger);
        debugOptions = EnumSet.noneOf(DebugOption.class);

        // Base User properties
        copyProperties(properties, FileUtils.userProperties());

        // Properties defined in the environment & in the command line

        overrideProperties = new TreeMap<String, String>();
        loadSystemProperties(overrideProperties);
        overrideProperties.putAll(override);

        initOptions();
    }

    //~ Methods ..............................................................................................

    /**
     * Returns true if log level is quiet
     * @return true if log level is quiet
     */
    public boolean isQuiet()
    {
        return quiet;
    }

    public void setQuiet()
    {
        quiet = true;
        getLogger().setLevel(Logger.Level.WARNING);
    }

    /**
     * Returns true if the build must NOT proceed recursive to the module dependecies
     */
    public boolean isNonRecursive()
    {
        return nonRecursive;
    }

    public void setNonRecursive(boolean b)
    {
        nonRecursive = b;
    }

    public void setFailOnError(boolean b)
    {
        failOnError = b;
    }

    @Override public String getId()
    {
        return "base";
    }

    /**
     * Returns true if we want the build to proceed unconditionally without checking file timestamps
     * @return true if we want the build to proceed unconditionally without checking file timestamps
     */
    public boolean forceBuild()
    {
        return forceBuild;
    }

    public void setForceBuild(boolean b)
    {
        forceBuild = b;
    }

    /**
     * Returns true if log level is verbose
     * @return true if log level is verbose
     */
    public boolean isVerbose()
    {
        return !debugOptions.isEmpty();
    }

    /**
     * Returns true if must show the following option
     */
    public boolean mustShow(DebugOption option)
    {
        return debugOptions.contains(option);
    }

    public void setDebugOptions(@NotNull EnumSet<DebugOption> options)
    {
        debugOptions.addAll(options);

        if (!options.isEmpty()) {
            setVerbose();
        }
    }

    public boolean isFailOnError()
    {
        return failOnError;
    }

    @Nullable protected String overrideProperty(@NotNull String id)
    {
        return overrideProperties.get(id);
    }

    protected void setVerbose()
    {
        getLogger().setLevel(Logger.Level.VERBOSE);
    }

    void register(ProjectBuilder pb)
    {
        currentProjectBuilder = pb;
    }

    ProjectBuilder getCurrentProjectBuilder()
    {
        return currentProjectBuilder;
    }

    private static void loadSystemProperties(Map<String, String> overrideProperties)
    {
        for (String entry : apbEnvironmentVariables) {
            String ename = entry;
            String pname = entry;
            int    colon = entry.indexOf(':');

            if (colon != -1) {
                ename = entry.substring(0, colon);
                pname = entry.substring(colon + 1);
            }

            String value = System.getenv(ename);

            if (value != null) {
                overrideProperties.put(pname, value);
            }
        }

        copyProperties(overrideProperties, System.getProperties());
    }

    private void initOptions()
    {
        setDebugOptions(DebugOption.findAll(getProperty("debug", "")));
    }

    //~ Static fields/initializers ...........................................................................

    private static String[] apbEnvironmentVariables = { "APB_EXT_PATH:ext.path" };

    static final String GROUP_PROP_KEY = "group";
    static final String VERSION_PROP_KEY = "version";
    static final String PKG_PROP_KEY = "pkg";
    static final String PKG_DIR_KEY = PKG_PROP_KEY + ".dir";
}
