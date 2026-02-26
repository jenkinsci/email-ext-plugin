package hudson.plugins.emailext.plugins.content;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailToken;
import hudson.util.ClassLoaderSanityThreadFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * Token that reads a file from the workspace and includes its content.
 * Supports both relative and absolute paths with environment variable expansion.
 *
 * @author Akash Manna
 */
@Extension
@EmailToken
public class FileContent extends DataBoundTokenMacro {

    private static final Logger LOGGER = Logger.getLogger(FileContent.class.getName());

    public static final String MACRO_NAME = "FILE";

    @Parameter(required = true)
    public String path = "";

    @Parameter
    public String encoding = StandardCharsets.UTF_8.name();

    @Parameter
    public String fileNotFoundMessage = "";

    private static final ExecutorService threadPoolForRemoting =
            Executors.newCachedThreadPool(new ClassLoaderSanityThreadFactory(
                    Executors.defaultThreadFactory()));

    public FileContent() {}

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(build, build.getWorkspace(), listener, macroName);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        
        if (StringUtils.isEmpty(path)) {
            return "";
        }

        try {
            EnvVars envVars = run.getEnvironment(listener);
            
            String expandedPath = envVars.expand(path);
            
            String content = readFile(run, workspace, expandedPath, listener);
            return content;
            
        } catch (FileNotFoundException e) {
            String errorMsg = StringUtils.isEmpty(fileNotFoundMessage) 
                ? "File [" + path + "] was not found." 
                : fileNotFoundMessage;
            LOGGER.log(Level.WARNING, "File not found: " + path, e);
            return errorMsg;
        }
    }

    private String readFile(Run<?, ?> run, FilePath workspace, String expandedPath, TaskListener listener)
            throws IOException, InterruptedException, FileNotFoundException {
        
        if (workspace == null) {
            throw new FileNotFoundException("Workspace not available for file path: " + expandedPath);
        }

        FilePath fileToRead = null;
        
        File pathFile = new File(expandedPath);
        if (pathFile.isAbsolute()) {
            fileToRead = new FilePath(pathFile);
            
            if (!isChildOf(fileToRead, workspace)) {
                throw new FileNotFoundException(
                    "File [" + expandedPath + "] is outside the workspace and cannot be accessed for security reasons.");
            }
        } else {
            fileToRead = workspace.child(expandedPath);
        }

        if (!fileToRead.exists()) {
            throw new FileNotFoundException("File [" + expandedPath + "] does not exist.");
        }

        if (fileToRead.isDirectory()) {
            throw new FileNotFoundException("Path [" + expandedPath + "] is a directory, not a file.");
        }

        final FilePath finalFileToRead = fileToRead;
        final String finalEncoding = encoding;
        try {
            Future<String> future = threadPoolForRemoting.submit(() -> {
                try (InputStream is = finalFileToRead.read()) {
                    return IOUtils.toString(is, finalEncoding);
                }
            });
            return future.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new IOException("Timeout reading file: " + expandedPath, e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException("Error reading file: " + expandedPath, e);
        }
    }

    /**
     * Checks if a file is within the workspace (child of workspace).
     * This is important for security to prevent accessing files outside the workspace.
     */
    private boolean isChildOf(FilePath potentialChild, FilePath workspace) 
            throws IOException, InterruptedException {
        try {
            String childPath = potentialChild.getRemote();
            String workspacePath = workspace.getRemote();
            
            childPath = childPath.replace('\\', '/');
            workspacePath = workspacePath.replace('\\', '/');
            
            if (!workspacePath.endsWith("/")) {
                workspacePath += "/";
            }
            
            return childPath.startsWith(workspacePath);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking if file is child of workspace", e);
            return false;
        }
    }

    @Override
    public boolean hasNestedContent() {
        return true;
    }
}
