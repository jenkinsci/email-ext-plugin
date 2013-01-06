/**
 * 
 */
package hudson.plugins.emailext;

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import hudson.plugins.emailext.plugins.ContentBuilder;

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
	
	private String attachmentsPattern;
	
	public AttachmentUtils(String attachmentsPattern) {
		this.attachmentsPattern = attachmentsPattern;
	}
	
	/**
     * Provides a datasource wrapped around the FilePath class to
     * allow access to remote files (on slaves).
     * @author acearl
     *
     */
    private static class FilePathDataSource implements DataSource {
    	private FilePath file;

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
    
    private List<MimeBodyPart> getAttachments(final ExtendedEmailPublisher publisher, final AbstractBuild<?, ?> build, final BuildListener listener)
    		throws MessagingException, InterruptedException, IOException {
    	List<MimeBodyPart> attachments = null;
    	FilePath ws = build.getWorkspace();
    	long totalAttachmentSize = 0;
		long maxAttachmentSize = 
				ExtendedEmailPublisher.DESCRIPTOR.getMaxAttachmentSize();
    	if(ws == null) {
    		listener.error("Error: No workspace found!");
    	} else if(!StringUtils.isBlank(attachmentsPattern)) {
    		attachments = new ArrayList<MimeBodyPart>();

    		FilePath[] files = ws.list(new ContentBuilder().transformText(attachmentsPattern, publisher, null, build, listener));
    	
	    	for(FilePath file : files) {
		    	if(maxAttachmentSize > 0 && 
		    			(totalAttachmentSize + file.length()) >= maxAttachmentSize) {
		    		listener.getLogger().println("Skipping `" + file.getName() 
		    				+ "' ("+ file.length() + 
		    				" bytes) - too large for maximum attachments size");
		    		continue;
		    	}           
	    	
		    	MimeBodyPart attachmentPart = new MimeBodyPart();
		    	FilePathDataSource fileDataSource = new FilePathDataSource(file);
			
				try {
					attachmentPart.setDataHandler(new DataHandler(fileDataSource));
					attachmentPart.setFileName(file.getName());
					attachments.add(attachmentPart);
					totalAttachmentSize += file.length();
				} catch(MessagingException e) {
					listener.getLogger().println("Error adding `" + 
							file.getName() + "' as attachment - " + 
							e.getMessage());
				}
	    	}
    	}    	
    	return attachments;
    }
    
    public void attach(Multipart multipart, ExtendedEmailPublisher publisher, AbstractBuild<?,?> build, BuildListener listener) {  
    	try {
	    	List<MimeBodyPart> attachments = getAttachments(publisher, build, listener);
	        if(attachments != null) {
                for(MimeBodyPart attachment : attachments) {
		        	multipart.addBodyPart(attachment);
		        }
	        }
    	} catch (IOException e) {
    		listener.error("Error accessing files to attach: " + e.getMessage());
    	} catch (MessagingException e) {
    		listener.error("Error attaching items to message: " + e.getMessage());
    	} catch (InterruptedException e) {
			listener.error("Interrupted in processing attachments: " + e.getMessage());
		}
    }

    /**
     * Attaches the build log to the multipart item.
     */
    public static void attachBuildLog(Multipart multipart, AbstractBuild<?, ?> build, BuildListener listener) {
        try {
            File logFile = build.getLogFile();
            long maxAttachmentSize =
                    ExtendedEmailPublisher.DESCRIPTOR.getMaxAttachmentSize();

            if(maxAttachmentSize > 0 && logFile.length() >= maxAttachmentSize) {
                listener.getLogger().println("Skipping build log attachment - " 
                        + " too large for maximum attachments size");
                return;
            }

            FileDataSource fileSource = new FileDataSource(logFile);
            MimeBodyPart attachment = new MimeBodyPart();
            attachment.setDataHandler(new DataHandler(fileSource));
            attachment.setFileName("build.log");
            multipart.addBodyPart(attachment);
        } catch (MessagingException e) {
            listener.error("Error attaching build log to message: " + e.getMessage());
        }
    }
}
