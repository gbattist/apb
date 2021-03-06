

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


package apb.tests.javas;

import org.jetbrains.annotations.NotNull;

/**
 * A simple main to test the java task
 */
//
// User: emilio
// Date: Sep 7, 2009
// Time: 3:13:04 PM

//
@SuppressWarnings("ConstantConditions")
public class SumArgs
{
    //~ Methods ..............................................................................................

    public static void main(String[] args)
    {
        int result = 0;

        for (String arg : args) {
            result += toInt("null".equals(arg) ? null : arg);
        }

        System.out.println(result);
        System.exit(result == 0 ? 0 : result > 0 ? 1 : 2);
    }

    private static int toInt(@NotNull String s)
    {
        return s == null ? 0 : Integer.parseInt(s);
    }
}
