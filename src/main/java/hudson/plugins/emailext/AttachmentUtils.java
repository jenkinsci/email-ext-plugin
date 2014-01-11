package hudson.plugins.emailext;

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import hudson.plugins.emailext.plugins.ContentBuilder;
import hudson.plugins.emailext.plugins.ZipDataSource;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;

/**
 * @author acearl
 *
 */
public class AttachmentUtils implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String attachmentsPattern;

    public AttachmentUtils(String attachmentsPattern) {
        this.attachmentsPattern = attachmentsPattern;
    }

    /**
     * Provides a datasource wrapped around the FilePath class to allow access
     * to remote files (on slaves).
     *
     * @author acearl
     *
     */
    private static class FilePathDataSource implements DataSource {

        private final FilePath file;

        public FilePathDataSource(FilePath file) {
            this.file = file;
        }

        public InputStream getInputStream() throws IOException {
            return file.read();
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Unsupported");
        }

        public String getContentType() {
            return MimetypesFileTypeMap.getDefaultFileTypeMap()
                    .getContentType(file.getName());
        }

        public String getName() {
            return file.getName();
        }
    }

    private List<MimeBodyPart> getAttachments(final ExtendedEmailPublisherContext context)
            throws MessagingException, InterruptedException, IOException {
        List<MimeBodyPart> attachments = null;
        FilePath ws = context.getBuild().getWorkspace();
        long totalAttachmentSize = 0;
        long maxAttachmentSize = context.getPublisher().getDescriptor().getMaxAttachmentSize();
        if (ws == null) {
            context.getListener().error("Error: No workspace found!");
        } else if (!StringUtils.isBlank(attachmentsPattern)) {
            attachments = new ArrayList<MimeBodyPart>();

            FilePath[] files = ws.list(new ContentBuilder().transformText(attachmentsPattern, context, null));

            for (FilePath file : files) {
                if (maxAttachmentSize > 0
                        && (totalAttachmentSize + file.length()) >= maxAttachmentSize) {
                    context.getListener().getLogger().println("Skipping `" + file.getName()
                            + "' (" + file.length()
                            + " bytes) - too large for maximum attachments size");
                    continue;
                }

                MimeBodyPart attachmentPart = new MimeBodyPart();
                FilePathDataSource fileDataSource = new FilePathDataSource(file);

                try {
                    attachmentPart.setDataHandler(new DataHandler(fileDataSource));
                    attachmentPart.setFileName(file.getName());
                    attachmentPart.setContentID(String.format("<%s>", file.getName()));
                    attachments.add(attachmentPart);
                    totalAttachmentSize += file.length();
                } catch (MessagingException e) {
                    context.getListener().getLogger().println("Error adding `"
                            + file.getName() + "' as attachment - "
                            + e.getMessage());
                }
            }
        }
        return attachments;
    }

    public void attach(Multipart multipart, ExtendedEmailPublisher publisher, AbstractBuild<?, ?> build, BuildListener listener) {
        final ExtendedEmailPublisherContext context = new ExtendedEmailPublisherContext(publisher, build, listener);
        attach(multipart, context);
        
    }
    
    public void attach(Multipart multipart, ExtendedEmailPublisherContext context) {
        try {
            
            List<MimeBodyPart> attachments = getAttachments(context);
            if (attachments != null) {
                for (MimeBodyPart attachment : attachments) {
                    multipart.addBodyPart(attachment);
                }
            }
        } catch (IOException e) {
            context.getListener().error("Error accessing files to attach: " + e.getMessage());
        } catch (MessagingException e) {
            context.getListener().error("Error attaching items to message: " + e.getMessage());
        } catch (InterruptedException e) {
            context.getListener().error("Interrupted in processing attachments: " + e.getMessage());
        }    
    }
    
    public static void attachBuildLog(ExtendedEmailPublisherContext context, Multipart multipart, boolean compress) {
        try {
            File logFile = context.getBuild().getLogFile();
            long maxAttachmentSize = context.getPublisher().getDescriptor().getMaxAttachmentSize();

            if (maxAttachmentSize > 0 && logFile.length() >= maxAttachmentSize) {
                context.getListener().getLogger().println("Skipping build log attachment - "
                        + " too large for maximum attachments size");
                return;
            }

            DataSource fileSource;
            MimeBodyPart attachment = new MimeBodyPart();
            if (compress) {
                context.getListener().getLogger().println("Request made to compress build log");
                fileSource = new ZipDataSource(logFile);
                attachment.setFileName("build.zip");
            } else {
                fileSource = new FileDataSource(logFile);
                attachment.setFileName("build.log");
            }
            attachment.setDataHandler(new DataHandler(fileSource));
            multipart.addBodyPart(attachment);
        } catch (MessagingException e) {
            context.getListener().error("Error attaching build log to message: " + e.getMessage());
        } catch (IOException e) {
            context.getListener().error("Error attaching build log to message: " + e.getMessage());
        }
    }

    /**
     * Attaches the build log to the multipart item.
     */
    public static void attachBuildLog(ExtendedEmailPublisher publisher, Multipart multipart, AbstractBuild<?, ?> build, BuildListener listener, boolean compress) {
        final ExtendedEmailPublisherContext context = new ExtendedEmailPublisherContext(publisher, build, listener);
        attachBuildLog(context, multipart, compress);
    }
}
