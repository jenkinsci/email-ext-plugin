package hudson.plugins.emailext.plugins;

import hudson.plugins.emailext.SizedDataSource;
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

public class ZipDataSource implements SizedDataSource {

    private static final String MIME_TYPE = "application/zip";
    private static final String FILE_EXTENSION = ".zip";
    private static final int BUFFER_SIZE = 1024;

    private final String name;
    private byte[] contents;

    public ZipDataSource(File f) throws IOException {
        this(f.getName(), new FileInputStream(f));
    }

    public ZipDataSource(String name, InputStream in) throws IOException {
        this.name = name + FILE_EXTENSION;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        zos.putNextEntry(new ZipEntry(name));

        int size;
        byte[] buffer = new byte[BUFFER_SIZE];
        while ((size = in.read(buffer, 0, buffer.length)) > 0) {
            zos.write(buffer, 0, size);
        }
        zos.closeEntry();
        zos.close();
        in.close();
        contents = baos.toByteArray();
    }

    public String getContentType() {
        return MIME_TYPE;
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(contents);
    }

    public String getName() {
        return name;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new ZipException("This zip file " + name + " is not modifiable");
    }

    @Override
    public long getSize() {
        return contents.length;
    }
}
