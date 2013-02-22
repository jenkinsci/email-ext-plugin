package hudson.plugins.emailext.plugins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import javax.activation.DataSource;

public class ZipDataSource implements DataSource {

	private final static String MIME_TYPE = "application/zip";
	private final static String FILE_EXTENSION = ".zip";
	private final static int BUFFER_SIZE = 1024;

	private final String name;
	private byte[] contents;

	public ZipDataSource(File f) throws IOException {
		name = f.getName() + FILE_EXTENSION;

		InputStream fin = new FileInputStream(f);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(baos);
		zos.putNextEntry(new ZipEntry(f.getName()));

		int size;
		byte[] buffer = new byte[BUFFER_SIZE];
		while ((size = fin.read(buffer, 0, buffer.length)) > 0) {
			zos.write(buffer, 0, size);
		}
		zos.closeEntry();
		zos.close();
		fin.close();

		contents = baos.toByteArray();
	}

	public String getContentType() {
		return MIME_TYPE;
	}

	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(contents);
	}

	public String getName() {
		return name;
	}

	public OutputStream getOutputStream() throws IOException {
		throw new ZipException("This zip file " + name + " is not modifiable");
	}
}
