package hudson.plugins.emailext.plugins.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class FileContentTest {

    private FileContent fileContent;
    private TaskListener listener;
    private Run<?, ?> run;
    private FilePath workspace;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp(JenkinsRule j) throws Exception {
        fileContent = new FileContent();
        listener = StreamTaskListener.fromStdout();
        
        // Create a temporary workspace
        workspace = new FilePath(tempDir);
        
        // Mock the run and environment variables
        run = mock(Run.class);
        EnvVars envVars = new EnvVars();
        envVars.put("WORKSPACE", workspace.getRemote());
        envVars.put("BUILD_NUMBER", "42");
        envVars.put("JOB_NAME", "test-job");
        
        when(run.getEnvironment(listener)).thenReturn(envVars);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Cleanup is handled by @TempDir
    }

    @Test
    void testReadRelativeFile() throws Exception {
        // Create a test file in the workspace
        FilePath testFile = workspace.child("test.txt");
        testFile.write("Hello from test file!", StandardCharsets.UTF_8.name());

        // Test reading the file with relative path
        fileContent.path = "test.txt";
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertEquals("Hello from test file!", content);
    }

    @Test
    void testReadRelativeFileInSubdirectory() throws Exception {
        // Create a subdirectory and file
        FilePath subdir = workspace.child("reports");
        subdir.mkdirs();
        FilePath testFile = subdir.child("report.html");
        testFile.write("<html><body>Test Report</body></html>", StandardCharsets.UTF_8.name());

        // Test reading the file with relative path
        fileContent.path = "reports/report.html";
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertTrue(content.contains("Test Report"));
    }

    @Test
    void testReadFileWithWorkspaceEnvVar() throws Exception {
        // Create a test file
        FilePath testFile = workspace.child("build-info.txt");
        testFile.write("Build completed successfully", StandardCharsets.UTF_8.name());

        // Test reading the file using $WORKSPACE environment variable
        fileContent.path = "$WORKSPACE/build-info.txt";
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertEquals("Build completed successfully", content);
    }

    @Test
    void testReadFileWithMultipleEnvVars() throws Exception {
        // Create a test file
        FilePath testFile = workspace.child("build-42.log");
        testFile.write("Build log content", StandardCharsets.UTF_8.name());

        // Test reading file using environment variables in the name
        fileContent.path = "$WORKSPACE/build-$BUILD_NUMBER.log";
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertEquals("Build log content", content);
    }

    @Test
    void testReadAbsolutePathWithinWorkspace() throws Exception {
        // Create a test file
        FilePath testFile = workspace.child("absolute-test.txt");
        testFile.write("Absolute path content", StandardCharsets.UTF_8.name());

        // Test reading with absolute path
        fileContent.path = testFile.getRemote();
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertEquals("Absolute path content", content);
    }

    @Test
    void testFileNotFound() throws Exception {
        // Test reading a non-existent file
        fileContent.path = "nonexistent.txt";
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertTrue(content.contains("File [nonexistent.txt] was not found."));
    }

    @Test
    void testFileNotFoundWithCustomMessage() throws Exception {
        // Test reading a non-existent file with custom error message
        fileContent.path = "missing.txt";
        fileContent.fileNotFoundMessage = "Custom error: File not available";
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertEquals("Custom error: File not available", content);
    }

    @Test
    void testSecurityPreventAccessOutsideWorkspace() throws Exception {
        // Try to access a file outside the workspace using absolute path
        File outsideFile = new File(tempDir.getParentFile(), "outside.txt");
        outsideFile.createNewFile();
        outsideFile.deleteOnExit();
        
        try (var writer = new java.io.FileWriter(outsideFile)) {
            writer.write("Outside content");
        }

        fileContent.path = outsideFile.getAbsolutePath();
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertTrue(content.contains("outside the workspace") || content.contains("was not found"));
    }

    @Test
    void testSecurityPreventParentDirectoryEscape() throws Exception {
        // Create a test file in a subdirectory
        FilePath subdir = workspace.child("testsubdir");
        subdir.mkdirs();
        FilePath testFile = subdir.child("test.txt");
        testFile.write("Test content", StandardCharsets.UTF_8.name());

        // Try to escape using ../ - workspace.child() normalizes this
        // so it actually stays within workspace, but just to verify
        fileContent.path = "testsubdir/../test-outside.txt";
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        // Should fail because test-outside.txt doesn't exist
        assertTrue(content.contains("does not exist") || content.contains("was not found"));
    }

    @Test
    void testDirectoryInsteadOfFile() throws Exception {
        // Create a directory
        FilePath subdir = workspace.child("testdir");
        subdir.mkdirs();

        // Try to read a directory
        fileContent.path = "testdir";
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertTrue(content.contains("is a directory") || content.contains("was not found"));
    }

    @Test
    void testCustomEncoding() throws Exception {
        // Create a file with specific encoding
        FilePath testFile = workspace.child("encoded.txt");
        String testContent = "Special chars: café, naïve, 日本語";
        testFile.write(testContent, "UTF-8");

        // Read with UTF-8 encoding
        fileContent.path = "encoded.txt";
        fileContent.encoding = "UTF-8";
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertEquals(testContent, content);
    }

    @Test
    void testEmptyPath() throws Exception {
        // Test with empty path
        fileContent.path = "";
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertEquals("", content);
    }

    @Test
    void testNullWorkspace() throws Exception {
        // Test when workspace is null
        fileContent.path = "test.txt";
        String content = fileContent.evaluate(run, null, listener, FileContent.MACRO_NAME);
        
        assertTrue(content.contains("Workspace not available") || content.contains("was not found"));
    }

    @Test
    void testWindowsStylePathWithBackslashes() throws Exception {
        // Create a test file in subdirectory
        FilePath subdir = workspace.child("reports");
        subdir.mkdirs();
        FilePath testFile = subdir.child("summary.txt");
        testFile.write("Summary content", StandardCharsets.UTF_8.name());

        // Test with backslashes (Windows-style path)
        fileContent.path = "reports\\summary.txt";
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertEquals("Summary content", content);
    }

    @Test
    void testLargeFile() throws Exception {
        // Create a larger file
        FilePath testFile = workspace.child("large.txt");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Line ").append(i).append(": This is a test line with some content.\n");
        }
        testFile.write(sb.toString(), StandardCharsets.UTF_8.name());

        fileContent.path = "large.txt";
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertTrue(content.contains("Line 0:"));
        assertTrue(content.contains("Line 999:"));
    }

    @Test
    void testFileWithSpecialCharactersInName() throws Exception {
        // Create a file with special characters in its name
        FilePath testFile = workspace.child("test-file_v1.0 (copy).txt");
        testFile.write("Special name content", StandardCharsets.UTF_8.name());

        fileContent.path = "test-file_v1.0 (copy).txt";
        String content = fileContent.evaluate(run, workspace, listener, FileContent.MACRO_NAME);
        
        assertEquals("Special name content", content);
    }

    @Test
    void testAcceptsMacroName() {
        assertTrue(fileContent.acceptsMacroName("FILE"));
    }
}
