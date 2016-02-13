package junit.textui;

import com.crumby.impl.crumby.UnsupportedUrlFragment;
import java.io.PrintStream;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.runner.BaseTestRunner;
import junit.runner.Version;

public class TestRunner extends BaseTestRunner {
    public static final int EXCEPTION_EXIT = 2;
    public static final int FAILURE_EXIT = 1;
    public static final int SUCCESS_EXIT = 0;
    private ResultPrinter fPrinter;

    public TestRunner() {
        this(System.out);
    }

    public TestRunner(PrintStream writer) {
        this(new ResultPrinter(writer));
    }

    public TestRunner(ResultPrinter printer) {
        this.fPrinter = printer;
    }

    public static void run(Class<? extends TestCase> testClass) {
        run(new TestSuite((Class) testClass));
    }

    public static TestResult run(Test test) {
        return new TestRunner().doRun(test);
    }

    public static void runAndWait(Test suite) {
        new TestRunner().doRun(suite, true);
    }

    public void testFailed(int status, Test test, Throwable t) {
    }

    public void testStarted(String testName) {
    }

    public void testEnded(String testName) {
    }

    protected TestResult createTestResult() {
        return new TestResult();
    }

    public TestResult doRun(Test test) {
        return doRun(test, false);
    }

    public TestResult doRun(Test suite, boolean wait) {
        TestResult result = createTestResult();
        result.addListener(this.fPrinter);
        long startTime = System.currentTimeMillis();
        suite.run(result);
        this.fPrinter.print(result, System.currentTimeMillis() - startTime);
        pause(wait);
        return result;
    }

    protected void pause(boolean wait) {
        if (wait) {
            this.fPrinter.printWaitPrompt();
            try {
                System.in.read();
            } catch (Exception e) {
            }
        }
    }

    public static void main(String[] args) {
        try {
            if (!new TestRunner().start(args).wasSuccessful()) {
                System.exit(FAILURE_EXIT);
            }
            System.exit(0);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(EXCEPTION_EXIT);
        }
    }

    public TestResult start(String[] args) throws Exception {
        String testCase = UnsupportedUrlFragment.DISPLAY_NAME;
        String method = UnsupportedUrlFragment.DISPLAY_NAME;
        boolean wait = false;
        int i = 0;
        while (i < args.length) {
            if (args[i].equals("-wait")) {
                wait = true;
            } else if (args[i].equals("-c")) {
                i += FAILURE_EXIT;
                testCase = extractClassName(args[i]);
            } else if (args[i].equals("-m")) {
                i += FAILURE_EXIT;
                String arg = args[i];
                int lastIndex = arg.lastIndexOf(46);
                testCase = arg.substring(0, lastIndex);
                method = arg.substring(lastIndex + FAILURE_EXIT);
            } else if (args[i].equals("-v")) {
                System.err.println("JUnit " + Version.id() + " by Kent Beck and Erich Gamma");
            } else {
                testCase = args[i];
            }
            i += FAILURE_EXIT;
        }
        if (testCase.equals(UnsupportedUrlFragment.DISPLAY_NAME)) {
            throw new Exception("Usage: TestRunner [-wait] testCaseName, where name is the name of the TestCase class");
        }
        try {
            if (method.equals(UnsupportedUrlFragment.DISPLAY_NAME)) {
                return doRun(getTest(testCase), wait);
            }
            return runSingleMethod(testCase, method, wait);
        } catch (Exception e) {
            throw new Exception("Could not create and run test suite: " + e);
        }
    }

    protected TestResult runSingleMethod(String testCase, String method, boolean wait) throws Exception {
        return doRun(TestSuite.createTest(loadSuiteClass(testCase).asSubclass(TestCase.class), method), wait);
    }

    protected void runFailed(String message) {
        System.err.println(message);
        System.exit(FAILURE_EXIT);
    }

    public void setPrinter(ResultPrinter printer) {
        this.fPrinter = printer;
    }
}
