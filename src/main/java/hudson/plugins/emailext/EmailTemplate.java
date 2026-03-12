package hudson.plugins.emailext;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import java.io.Serializable;
import java.util.regex.Pattern;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Represents an email template that can be provisioned via Configuration as
 * Code.
 *
 * <p>
 * Templates declared in the CasC YAML will be written to
 * {@code $JENKINS_HOME/email-templates/} so they are available for use
 * in the Email Extension plugin's content tokens (SCRIPT, TEMPLATE,
 * JELLY_SCRIPT).
 *
 * <p>
 * Security: template names are strictly validated to prevent path traversal
 * and other filesystem attacks. Only simple filenames with allowed extensions
 * are accepted.
 */
public class EmailTemplate extends AbstractDescribableImpl<EmailTemplate> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Allowed filename pattern: alphanumeric, hyphens, underscores, dots.
     * Must end with an allowed extension (.groovy, .jelly, or .template).
     */
    private static final Pattern SAFE_NAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9._-]*\\.(groovy|jelly|template)$");

    private final String name;
    private final String content;

    @DataBoundConstructor
    public EmailTemplate(@NonNull String name, @NonNull String content) {
        if (name == null || content == null) {
            throw new IllegalArgumentException("Template name and content must not be null");
        }

        String trimmedName = name.trim();

        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Template name must not be empty");
        }

        // Reject path traversal and directory separators
        if (trimmedName.contains("/") || trimmedName.contains("\\") || trimmedName.contains("..")) {
            throw new IllegalArgumentException(
                    "Template name must not contain path separators or relative path components: " + trimmedName);
        }

        // Reject null bytes
        if (trimmedName.contains("\0")) {
            throw new IllegalArgumentException("Template name must not contain null bytes");
        }

        // Enforce safe filename pattern and allowed extensions
        if (!SAFE_NAME_PATTERN.matcher(trimmedName).matches()) {
            throw new IllegalArgumentException(
                    "Template name must be a simple filename ending with .groovy, .jelly, or .template: "
                            + trimmedName);
        }

        this.name = trimmedName;
        this.content = content;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getContent() {
        return content;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<EmailTemplate> {
        @NonNull
        @Override
        public String getDisplayName() {
            return "Email Template";
        }
    }
}
