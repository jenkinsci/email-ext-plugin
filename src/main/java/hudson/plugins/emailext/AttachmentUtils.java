package hudson.plugins.emailext;

import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.plugins.emailext.plugins.ContentBuilder;
import hudson.plugins.emailext.plugins.ZipDataSource;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileTypeMap;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeUtility;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.variant.OptionalExtension;

/**
 * @author acearl
 *
 */
public class AttachmentUtils implements Serializable {

    @Serial
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

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream stream = null;
            try {
                stream = file.read();
            } catch (InterruptedException e) {
                stream = null;
            }
            return stream;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Unsupported");
        }

        @Override
        public String getContentType() {
            return FileTypeMap.getDefaultFileTypeMap().getContentType(file.getName());
        }

        @Override
        public String getName() {
            return file.getName();
        }
    }

    private static class LogFileDataSource implements SizedDataSource {

        private static final String DATA_SOURCE_NAME = "build.log";

        private final Run<?, ?> run;

        public LogFileDataSource(Run<?, ?> run) {
            this.run = run;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream res;
            long logFileLength = run.getLogText().length();
            long pos = 0;
            ByteArrayOutputStream bao = new ByteArrayOutputStream();

            while (pos < logFileLength) {
                pos = run.getLogText().writeLogTo(pos, bao);
            }

            res = new ByteArrayInputStream(bao.toByteArray());
            return res;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Unsupported");
        }

        @Override
        public String getContentType() {
            return "text/plain";
        }

        @Override
        public String getName() {
            return DATA_SOURCE_NAME;
        }

        @Override
        public long getSize() {
            return run.getLogText().length();
        }
    }

    private List<MimeBodyPart> getAttachments(final ExtendedEmailPublisherContext context)
            throws MessagingException, InterruptedException, IOException {
        List<MimeBodyPart> attachments = null;
        FilePath ws = context.getWorkspace();
        long totalAttachmentSize = 0;
        long maxAttachmentSize = context.getPublisher().getDescriptor().getMaxAttachmentSize();
        if (!StringUtils.isBlank(attachmentsPattern)) {
            if (ws == null) {
                context.getListener().error("Error: No workspace found!");
            } else {
                attachments = new ArrayList<>();

                FilePath[] files = ws.list(ContentBuilder.transformText(attachmentsPattern, context, null));

                for (FilePath file : files) {
                    if (maxAttachmentSize > 0 && totalAttachmentSize + file.length() >= maxAttachmentSize) {
                        context.getListener()
                                .getLogger()
                                .println("Skipping `" + file.getName()
                                        + "' (" + file.length()
                                        + " bytes) - too large for maximum attachments size");
                        continue;
                    }

                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    FilePathDataSource fileDataSource = new FilePathDataSource(file);

                    try {
                        attachmentPart.setDataHandler(new DataHandler(fileDataSource));
                        attachmentPart.setFileName(MimeUtility.encodeText(file.getName()));
                        attachmentPart.setContentID("<%s>".formatted(file.getName()));
                        attachments.add(attachmentPart);
                        totalAttachmentSize += file.length();
                    } catch (MessagingException e) {
                        context.getListener()
                                .getLogger()
                                .println("Error adding `" + file.getName() + "' as attachment - " + e.getMessage());
                    }
                }
            }
        }
        return attachments;
    }

    @Deprecated
    public void attach(
            Multipart multipart, ExtendedEmailPublisher publisher, AbstractBuild<?, ?> build, BuildListener listener) {
        final ExtendedEmailPublisherContext context =
                new ExtendedEmailPublisherContext(publisher, build, null, listener);
        attach(multipart, context);
    }

    public void attach(
            Multipart multipart,
            ExtendedEmailPublisher publisher,
            AbstractBuild<?, ?> build,
            Launcher launcher,
            BuildListener listener) {
        final ExtendedEmailPublisherContext context =
                new ExtendedEmailPublisherContext(publisher, build, launcher, listener);
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

    private static void attachSingleLog(
            ExtendedEmailPublisherContext context, Run<?, ?> run, Multipart multipart, boolean compress) {
        try {
            long maxAttachmentSize = context.getPublisher().getDescriptor().getMaxAttachmentSize();

            SizedDataSource fileSource;
            MimeBodyPart attachment = new MimeBodyPart();
            if (compress) {
                context.getListener().getLogger().println("Request made to compress build log");
            }

            fileSource = new LogFileDataSource(run);
            if (compress) {
                fileSource = new ZipDataSource(fileSource.getName(), fileSource.getInputStream());
            }

            if (maxAttachmentSize > 0 && fileSource.getSize() >= maxAttachmentSize) {
                context.getListener()
                        .getLogger()
                        .println("Skipping build log attachment - " + " too large for maximum attachments size");
                return;
            }

            var suffix = "";
            for (var ma : ExtensionList.lookup(MatrixAssist.class)) {
                var _suffix = ma.suffix(run);
                if (_suffix != null) {
                    suffix = _suffix;
                    break;
                }
            }
            attachment.setFileName("build" + suffix + "." + (compress ? "zip" : "log"));
            attachment.setDataHandler(new DataHandler(fileSource));
            multipart.addBodyPart(attachment);
        } catch (MessagingException | IOException e) {
            context.getListener().error("Error attaching build log to message: " + e.getMessage());
        }
    }

    public static void attachBuildLog(ExtendedEmailPublisherContext context, Multipart multipart, boolean compress) {
        var main = context.getRun();
        var all = List.of(main);
        for (var ma : ExtensionList.lookup(MatrixAssist.class)) {
            var _all = ma.getExactRuns(main);
            if (_all != null) {
                all = _all;
                break;
            }
        }
        for (var run : all) {
            attachSingleLog(context, run, multipart, compress);
        }
    }

    public interface MatrixAssist {
        List<? extends Run<?, ?>> getExactRuns(Run<?, ?> run);

        String suffix(Run<?, ?> run);
    }

    @OptionalExtension(requirePlugins = "matrix-project")
    public static final class MatrixAssistImpl implements MatrixAssist {
        @Override
        public List<? extends Run<?, ?>> getExactRuns(Run<?, ?> run) {
            return run instanceof MatrixBuild mb ? mb.getExactRuns() : null;
        }

        @Override
        public String suffix(Run<?, ?> run) {
            return run instanceof MatrixRun mr
                    ? "-" + mr.getParent().getCombination().toString('-', '-')
                    : null;
        }
    }

    @Deprecated
    public static void attachBuildLog(
            ExtendedEmailPublisher publisher,
            Multipart multipart,
            AbstractBuild<?, ?> build,
            BuildListener listener,
            boolean compress) {
        final ExtendedEmailPublisherContext context =
                new ExtendedEmailPublisherContext(publisher, build, null, listener);
        attachBuildLog(context, multipart, compress);
    }

    public static void attachBuildLog(
            ExtendedEmailPublisher publisher,
            Multipart multipart,
            AbstractBuild<?, ?> build,
            Launcher launcher,
            BuildListener listener,
            boolean compress) {
        final ExtendedEmailPublisherContext context =
                new ExtendedEmailPublisherContext(publisher, build, launcher, listener);
        attachBuildLog(context, multipart, compress);
    }
}
