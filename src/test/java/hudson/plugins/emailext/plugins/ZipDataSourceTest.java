package hudson.plugins.emailext.plugins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.junit.Assert;

import junit.framework.TestCase;

public class ZipDataSourceTest extends TestCase {
	private final static int BUFFER_SIZE = 1024;

    public void testGetName() throws IOException{
        String name = "myFile";

        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ZipDataSource dataSource = new ZipDataSource(name, in);

        assertEquals(name + ".zip", dataSource.getName());
    }

    public void testGetContentType() throws IOException {
        String name = "myFile";

        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ZipDataSource dataSource = new ZipDataSource(name, in);

        assertEquals("application/zip", dataSource.getContentType());
    }

    public void testGetInputStream() throws IOException {
        byte[] sample = "Hello World lllllllllllots of repeated charactersssssssssssss Hello World again".getBytes();
        ZipDataSource dataSource = new ZipDataSource("name", new ByteArrayInputStream(sample));
        InputStream in = dataSource.getInputStream();

        ZipInputStream zin = new ZipInputStream(in);
        // Need this to move the ZipInputStream to the start of the "file"
        zin.getNextEntry();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int size;
        byte[] buffer = new byte[BUFFER_SIZE];
        while ((size = zin.read(buffer, 0, buffer.length)) > 0) {
            baos.write(buffer, 0, size);
        }
        Assert.assertArrayEquals(sample, baos.toByteArray());

    }

    public void testGetOutputStream() throws IOException {
        String name = "myFile";

        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ZipDataSource dataSource = new ZipDataSource(name, in);

        try {
            dataSource.getOutputStream();
        } catch (IOException e) {
            return;
        }
        Assert.fail("It is not possible to get an OutputStream from the ZipDataSource, an exception should have been thrown");
    }
}
