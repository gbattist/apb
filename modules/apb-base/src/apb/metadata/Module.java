

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


package apb.metadata;

import java.util.ArrayList;
import java.util.List;

import apb.ModuleHelper;
import apb.ProjectBuilder;

import apb.tasks.FileSet;

import org.jetbrains.annotations.NotNull;

import static apb.tasks.CoreTasks.copy;

/**
 * This class defines a Module for the building system
 * Every Module definition must inherit (directly or indirectly) from this class
 * This class have handy defaults for several attributes that assume the following layout:
 * <pre>
 *
 *  basedir/
 *         +--- project-definitions--+
 *                                   +-- Module1.java
 *                                   +-- Module2.java
 *                                   .
 *                                   .
 *                                   +-- ModuleN.java
 *
 *        +--- module1/
 *                    +-- src    --   <-- Sources here
 *                    .
 *        +--- module2/
 *        .
 *        .
 *        +--- moduleN/
 *        .
 *        +-- output/
 *                  .
 *                  +--- lib/
 *                          +-- module1.jar
 *                          +-- module2.jar
 *                          .
 *                          .
 *                          +-- moduleN.jar
 *
 *                  +-- module1/
 *                             +-- classes --   <-- Compiled classes and resources here
 *                             +-- gsrc    --   <-- Generated Sources here
 *                             +-- javadoc --   <-- javadoc here
 *                  .
 *                  +--- module2/
 *                  .
 * </pre>
 *
 */
@DefaultTarget("package")
public class Module
    extends ProjectElement
    implements Dependency
{
    //~ Instance fields ......................................................................................

    /**
     * Info for the compiler
     */
    @BuildProperty public final CompileInfo compiler = new CompileInfo();

    /**
     * All the info for Javadoc
     */
    @BuildProperty public JavadocInfo javadoc = new JavadocInfo();

    /**
     * All the info for the package (Jar,War, etc)
     */
    @BuildProperty public final PackageInfo pkg = new PackageInfo();

    /**
     * All the info for processing resources
     */
    @BuildProperty public ResourcesInfo resources = new ResourcesInfo();

    /**
     * The directory where the generated source files for this module are placed
     */
    @BuildProperty public String generatedSource = "$output-base/gsrc";

    /**
     * The "group" (Usually organization name) for this element.
     */
    @BuildProperty public String group = "";

    /**
     * The directory for the output classes
     */
    @BuildProperty public String output = "$output-base/classes";

    /**
     * A handy property that marks the base for all generated files
     * You can modify this property or every property related to output directories
     * (i.e: generatedSource, output, coverage-info.output, javadoc-info.output..)
     */
    @BuildProperty(order = 1)
    public String outputBase = "output/$moduledir";

    /**
     * The directory where the source files for this module are placed
     */
    @BuildProperty public String source = "$moduledir/src";

    /**
     * The module version
     */
    @BuildProperty public String version = "";

    /**
     * The list of modules & libraries this module depends from
     */
    protected final DependencyList dependencies = new DependencyList();

    /*
     * The list of test modules
     */
    final List<TestModule> tests = new ArrayList<TestModule>();

    //~ Methods ..............................................................................................

    public DependencyList dependencies()
    {
        return dependencies;
    }

    public List<TestModule> tests()
    {
        return tests;
    }

    @NotNull @Override public ModuleHelper getHelper()
    {
        return (ModuleHelper) super.getHelper();
    }

    @BuildTarget(description = "Deletes all output directories (compiled code and packages).")
    public void clean()
    {
        getHelper().clean();
        ProjectBuilder.forward("clean", tests());
    }

    @BuildTarget(description = "Copy (eventually filtering) resources to the output directory.")
    public void resources()
    {
        final FileSet fileSet =
            FileSet.fromDir(resources.dir)  //
                   .including(resources.includes())  //
                   .excluding(resources.excludes());

        copy(fileSet).to(resources.output).execute();
        // todo add filtered
    }

    @BuildTarget(
                 depends = "resources",
                 description = "Compile classes and place them in the output directory."
                )
    public void compile()
    {
        getHelper().compile();
    }

    @BuildTarget(
                 depends = "compile",
                 description = "Compile test classes.",
                 recursive = false
                )
    public void compileTests()
    {
        ProjectBuilder.forward("compile", tests());
    }

    @BuildTarget(
                 depends = { "compile-tests", "package" },
                 description = "Test the module (generating reports and  coverage info).",
                 recursive = false
                )
    public void runTests()
    {
        ProjectBuilder.forward("run", tests());
    }

    @BuildTarget(
                 depends = "compile-tests",
                 description = "Run test with the annotation @Test(group=\"minimal\")",
                 recursive = false
                )
    public void runMinimalTests()
    {
        ProjectBuilder.forward("run-minimal", tests());
    }

    @BuildTarget(
                 depends = "compile",
                 name = "package",
                 description = "Creates a jar file with the module classes and resources."
                )
    public void packageit()
    {
        getHelper().createPackage();
    }

    @BuildTarget(
                 description = "Generates the Java Documentation (Javadoc) for the module.",
                 recursive = false
                )
    public void javadoc()
    {
        getHelper().generateJavadoc();
    }

    @NotNull public Module asModule()
    {
        return this;
    }

    public boolean isModule()
    {
        return true;
    }

    public boolean isLibrary()
    {
        return false;
    }

    @NotNull public Library asLibrary()
    {
        throw new UnsupportedOperationException();
    }

    public boolean mustInclude(boolean compile)
    {
        return true;
    }

    /**
     * Method used to set dependencies when defining a module
     * @param dependencyList The list of dependencies to be set
     */
    protected final void dependencies(Dependencies... dependencyList)
    {
        dependencies.addAll(dependencyList);
    }

    /**
     * Method used to associate tests when defining a module
     * @param testList The list of tests to be set
     */
    protected final void tests(TestModule... testList)
    {
        for (TestModule test : testList) {
            test.setModuleToTest(this);
            tests.add(NameRegistry.intern(test));
        }
    }

    protected LocalLibrary localLibrary(String path)
    {
        return new LocalLibrary(path, false);
    }

    protected Dependencies compile(Dependency... dep)
    {
        return DecoratedDependency.asCompileOnly(dep);
    }

    protected Dependencies runtime(Dependency... dep)
    {
        return DecoratedDependency.asRuntimeOnly(dep);
    }

    protected LocalLibrary optionalLibrary(String path)
    {
        return new LocalLibrary(path, true);
    }
}
