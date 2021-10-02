package hudson.plugins.emailext.plugins;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;
import org.junit.Test;

public class ZipDataSourceTest {

    private static final int BUFFER_SIZE = 1024;

    @Test
    public void testGetName() throws IOException {
        String name = "myFile";

        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ZipDataSource dataSource = new ZipDataSource(name, in);

        assertEquals(name + ".zip", dataSource.getName());
    }

    @Test
    public void testGetContentType() throws IOException {
        String name = "myFile";

        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ZipDataSource dataSource = new ZipDataSource(name, in);

        assertEquals("application/zip", dataSource.getContentType());
    }

    @Test
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
        assertArrayEquals(sample, baos.toByteArray());
    }

    @Test
    public void testGetOutputStream() throws IOException {
        String name = "myFile";

        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ZipDataSource dataSource = new ZipDataSource(name, in);

        assertThrows(
                "It is not possible to get an OutputStream from the ZipDataSource",
                IOException.class,
                dataSource::getOutputStream);
    }
}
