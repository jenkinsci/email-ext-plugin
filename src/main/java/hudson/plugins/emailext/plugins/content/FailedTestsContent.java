package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;

import java.util.List;

/**
 * An EmailContent for failing tests. Only shows tests that have failed.
 * 
 * @author markltbaker
 */
public class FailedTestsContent implements EmailContent {
    
    public String getToken() {
        return "FAILED_TESTS";
    }

    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(AbstractBuild<P, B> build, EmailType emailType) {
        
        StringBuffer buffer = new StringBuffer();
        AbstractTestResultAction<?> testResult = build.getTestResultAction();
		
        if (null == testResult) {
        	return "No tests ran.";
        }
        
		int failCount = testResult.getFailCount();
		
		if (failCount == 0){
			buffer.append("All tests passed");
		}
		else {
			buffer.append(failCount);
			buffer.append(" tests failed.");
			buffer.append('\n');
			
			List<CaseResult> failedTests = testResult.getFailedTests();
			for (CaseResult failedTest: failedTests) {
				outputTest(buffer, failedTest);
			}
		}
        
        return buffer.toString();
    }

	private void outputTest(StringBuffer buffer, CaseResult failedTest) {
		buffer.append(failedTest.getStatus().toString());
		buffer.append(":  ");
		buffer.append(failedTest.getClassName());
		buffer.append("\n\n");
		buffer.append("Error Message:\n");
		buffer.append(failedTest.getErrorDetails());
		buffer.append("\n\nStack Trace:\n");
		buffer.append(failedTest.getErrorStackTrace());
		buffer.append("\n\n");
	}

    public boolean hasNestedContent() {
        return false;
    }

    public String getHelpText() {
        return "Displays failing unit test information, if any tests have failed.";
    }
}
