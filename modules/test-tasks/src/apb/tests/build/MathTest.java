

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


package apb.tests.build;

import apb.DefinitionException;

import apb.tests.testutils.FileAssert;
//
// User: emilio
// Date: Sep 3, 2009
// Time: 5:38:33 PM

//
public class MathTest
    extends ApbTestCase
{
    //~ Methods ..............................................................................................

    public void testMath()
        throws DefinitionException
    {
        build("Math", "package");
        checkOutput(COMPILING_1_FILE,  //
                    BUILDING + mathJar.getPath());

        FileAssert.assertExists(mathJar);
        FileAssert.assertExists(fractionClass);

        build("Math", "package");
        checkOutput();

        build("Math", "clean");
        checkOutput(DELETING_DIRECTORY + mathClasses,  //
                    DELETING_FILE + mathJar.getPath());

        FileAssert.assertDoesNotExist(mathJar);
        FileAssert.assertDoesNotExist(fractionClass);
    }

    public void testPlay()
        throws DefinitionException
    {
        build("PlayWithMath", "package");
        checkOutput(COMPILING_1_FILE,  //
                    BUILDING + mathJar.getPath(),  //
                    COMPILING_1_FILE,  //
                    BUILDING + playJar.getPath(),  //
                    BUILDING + playSrcJar.getPath());

        FileAssert.assertExists(mathJar);
        FileAssert.assertExists(playJar);
        FileAssert.assertExists(playSrcJar);
        FileAssert.assertExists(playClass);
    }
}
