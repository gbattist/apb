

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


package apb.testrunner.output;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//
// User: emilio
// Date: Nov 11, 2008
// Time: 6:06:24 PM

//
public abstract class DefaultTestReport
    implements TestReport,
               Serializable
{
    //~ Instance fields ......................................................................................

    boolean suiteOpen;

    // Counters for suites
    private int suitesFailed;
    private int suitesRun;

    // Counters for the current suite

    private int suiteTestFailures;
    private int suiteTestSkipped;
    private int suiteTestsRun;

    // Cumulative Counters
    private int  totalFailures;
    private int  totalSkipped;
    private int  totalSuites;
    private int  totalTestsRun;
    private long startTime;
    private long suiteStartTime;

    // current Test & Suite name
    @Nullable private String currentSuite;
    @Nullable private String currentTest;

    //~ Constructors .........................................................................................

    public DefaultTestReport()
    {
        suiteTestsRun = 0;
        totalTestsRun = 0;
        suiteTestFailures = 0;
        totalFailures = 0;
        totalSkipped = 0;
        suiteTestSkipped = 0;
        suitesFailed = 0;
        suitesRun = 0;
    }

    //~ Methods ..............................................................................................

    @Nullable public final String getCurrentSuite()
    {
        return currentSuite;
    }

    @Nullable public final String getCurrentTest()
    {
        return currentTest;
    }

    public int getSuiteTestFailures()
    {
        return suiteTestFailures;
    }

    public int getSuiteTestsRun()
    {
        return suiteTestsRun;
    }

    public int getSuiteTestSkipped()
    {
        return suiteTestSkipped;
    }

    public long getSuiteTimeEllapsed()
    {
        return System.currentTimeMillis() - suiteStartTime;
    }

    public long getTimeEllapsed()
    {
        return System.currentTimeMillis() - startTime;
    }

    public int getSuitesFailed()
    {
        return suitesFailed;
    }

    public int getSuitesRun()
    {
        return suitesRun;
    }

    public int getTotalSuites()
    {
        return totalSuites;
    }

    public int getTotalFailures()
    {
        return totalFailures;
    }

    public int getTotalSkipped()
    {
        return totalSkipped;
    }

    public int getTotalTestsRun()
    {
        return totalTestsRun;
    }

    public void startRun(int n)
    {
        totalSuites = n;
        startTime = System.currentTimeMillis();
    }

    public void stopRun() {}

    public void startSuite(@NotNull String suiteName)
    {
        suiteOpen = true;
        currentSuite = suiteName;
        suiteTestsRun = 0;
        suiteTestFailures = 0;
        suiteStartTime = System.currentTimeMillis();
    }

    public void endSuite()
    {
        if (suiteOpen) {
            suitesRun++;

            if (suiteTestFailures > 0) {
                suitesFailed++;
            }

            suiteOpen = false;
        }
    }

    public void startTest(@NotNull String testName)
    {
        currentTest = testName;
    }

    public void endTest()
    {
        suiteTestsRun++;
        totalTestsRun++;
    }

    public void failure(@NotNull Throwable t)
    {
        suiteTestFailures++;
        totalFailures++;
    }

    public void skip()
    {
        suiteTestSkipped++;
        totalSkipped++;
    }

    //~ Static fields/initializers ...........................................................................

    private static final long serialVersionUID = 2748763729187869689L;
}
