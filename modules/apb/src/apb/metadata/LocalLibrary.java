
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

package apb.metadata;

import java.io.File;
import java.util.Collection;

import apb.Environment;
import static apb.utils.CollectionUtils.optionalSingleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A class representing a Library that will be stored in the file system.
 */
public class LocalLibrary
    extends Library
{
    //~ Instance fields ......................................................................................

    @NotNull public final String   path;
    @Nullable public final String runtimePath;
    private boolean optional;
    @Nullable private String  sourcesPath;

    //~ Constructors .........................................................................................

    protected LocalLibrary(@NotNull String path)
    {
        this(path, false);
    }
    protected LocalLibrary(@NotNull String path, String runtimePath)
    {
        this(path, runtimePath, false);
    }

    protected LocalLibrary(@NotNull String path, String runtimePath, boolean optional)
    {
        super("", path, "");
        this.path = path;
        this.optional = optional;
        this.runtimePath = runtimePath;
        NameRegistry.intern(this);
    }

    protected LocalLibrary(@NotNull String path, boolean optional) {
        this(path, null, optional);
    }

    //~ Methods ..............................................................................................


    @NotNull public Collection<File> getFiles(@NotNull final Environment env)
    {
        return optionalSingleton(fileFromBase(env, path));
    }

    @Nullable public File getSourcesFile(@NotNull final Environment env)
    {
        return sourcesPath == null ? null : fileFromBase(env, sourcesPath);
    }

    public void setSources(@NotNull String sources)
    {
        sourcesPath = sources;
    }

    @Nullable
    private File fileFromBase(@NotNull Environment env, @NotNull String p)
    {
        final File result;

        final File lib = env.fileFromBase(p);

        if (lib.exists()) {
            result = lib.getAbsoluteFile();
        }
        else {
            result = null;

            if (!optional) {
                env.handle("Libray not found: " + lib);
            }
        }

        return result;
    }
}
