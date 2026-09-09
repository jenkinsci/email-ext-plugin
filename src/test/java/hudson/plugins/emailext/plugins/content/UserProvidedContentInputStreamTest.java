package hudson.plugins.emailext.plugins.content;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the class {@link UserProvidedContentInputStream}.
 *
 * @author Akash Manna
 */
class UserProvidedContentInputStreamTest {

    /** Simple byte content used by most tests. */
    private static final byte[] DATA = {0x01, 0x02, 0x03, 0x04, 0x05};

    private ByteArrayInputStream realDelegate;
    private UserProvidedContentInputStream stream;

    @BeforeEach
    void setUp() {
        realDelegate = new ByteArrayInputStream(DATA);
        stream = new UserProvidedContentInputStream(realDelegate);
    }

    @AfterEach
    void tearDown() throws IOException {
        stream.close();
    }

    /**
     * {@link UserProvidedContentInputStream#read()} should delegate to the
     * underlying stream and return each byte in turn.
     */
    @Test
    void testReadSingleByte() throws IOException {
        assertEquals(0x01, stream.read());
        assertEquals(0x02, stream.read());
        assertEquals(0x03, stream.read());
    }

    /**
     * {@link UserProvidedContentInputStream#read()} should return -1 when the
     * delegate is exhausted.
     */
    @Test
    void testReadSingleByteReturnsMinusOneAtEof() throws IOException {
        for (int i = 0; i < DATA.length; i++) {
            stream.read();
        }
        assertEquals(-1, stream.read());
    }

    /**
     * {@link UserProvidedContentInputStream#read(byte[])} should fill the
     * buffer from the delegate and return the number of bytes read.
     */
    @Test
    void testReadByteArray() throws IOException {
        byte[] buf = new byte[DATA.length];
        int n = stream.read(buf);
        assertEquals(DATA.length, n);
        assertArrayEquals(DATA, buf);
    }

    /**
     * {@link UserProvidedContentInputStream#read(byte[])} should return -1
     * when called on an exhausted stream.
     */
    @Test
    void testReadByteArrayReturnsMinusOneAtEof() throws IOException {
        byte[] buf = new byte[DATA.length];
        assertNotEquals(-1, stream.read(buf));
        assertEquals(-1, stream.read(buf));
    }

    /**
     * {@link UserProvidedContentInputStream#read(byte[], int, int)} should read
     * into the correct slice of the buffer.
     */
    @Test
    void testReadByteArrayWithOffsetAndLength() throws IOException {
        byte[] buf = new byte[10];
        int n = stream.read(buf, 2, 3);
        assertEquals(3, n);
        assertEquals(0x01, buf[2]);
        assertEquals(0x02, buf[3]);
        assertEquals(0x03, buf[4]);
        assertEquals(0, buf[0]);
        assertEquals(0, buf[1]);
        assertEquals(0, buf[5]);
    }

    /**
     * {@link UserProvidedContentInputStream#read(byte[], int, int)} should return
     * -1 at EOF.
     */
    @Test
    void testReadByteArrayWithOffsetAndLengthReturnsMinusOneAtEof() throws IOException {
        byte[] buf = new byte[DATA.length];
        assertNotEquals(-1, stream.read(buf, 0, DATA.length));
        assertEquals(-1, stream.read(buf, 0, 1));
    }

    /**
     * {@link UserProvidedContentInputStream#skip(long)} should skip the requested
     * number of bytes via the delegate.
     */
    @Test
    void testSkip() throws IOException {
        long skipped = stream.skip(3);
        assertEquals(3, skipped);
        assertEquals(0x04, stream.read());
    }

    /**
     * Skipping past the end of the stream should skip only as many bytes as
     * remain.
     */
    @Test
    void testSkipPastEnd() throws IOException {
        long skipped = stream.skip(100);
        assertEquals(DATA.length, skipped);
        assertEquals(-1, stream.read());
    }

    /**
     * {@link UserProvidedContentInputStream#available()} should reflect the
     * delegate's available byte count.
     */
    @Test
    void testAvailable() throws IOException {
        assertEquals(DATA.length, stream.available());
        stream.read();
        assertEquals(DATA.length - 1, stream.available());
    }

    /**
     * {@link UserProvidedContentInputStream#close()} should delegate to the
     * wrapped stream without throwing.
     */
    @Test
    void testClose() throws IOException {
        InputStream mockDelegate = mock(InputStream.class);
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(mockDelegate)) {
            // close() delegation is verified below; the try-with-resources
            // performs the single close when the block exits
        }
        verify(mockDelegate).close();
    }

    /**
     * {@link UserProvidedContentInputStream#markSupported()} should reflect
     * the delegate value. {@link ByteArrayInputStream} supports mark.
     */
    @Test
    void testMarkSupported() {
        assertTrue(stream.markSupported());
    }

    /**
     * {@link UserProvidedContentInputStream#markSupported()} should return false
     * when the delegate does not support mark.
     */
    @Test
    void testMarkNotSupported() throws IOException {
        InputStream mockDelegate = mock(InputStream.class);
        when(mockDelegate.markSupported()).thenReturn(false);
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(mockDelegate)) {
            assertFalse(s.markSupported());
        }
    }

    /**
     * {@link UserProvidedContentInputStream#mark(int)} and
     * {@link UserProvidedContentInputStream#reset()} should allow re-reading
     * from the marked position.
     */
    @Test
    void testMarkAndReset() throws IOException {
        assertEquals(0x01, stream.read());
        stream.mark(DATA.length);
        assertEquals(0x02, stream.read());
        assertEquals(0x03, stream.read());
        stream.reset();
        assertEquals(0x02, stream.read());
    }

    /**
     * Verifies that {@link UserProvidedContentInputStream#mark(int)} delegates
     * to the underlying stream.
     */
    @Test
    void testMarkDelegatesToDelegate() throws IOException {
        InputStream mockDelegate = mock(InputStream.class);
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(mockDelegate)) {
            s.mark(42);
        }
        verify(mockDelegate).mark(42);
    }

    /**
     * Verifies that {@link UserProvidedContentInputStream#reset()} delegates
     * to the underlying stream.
     */
    @Test
    void testResetDelegatesToDelegate() throws IOException {
        InputStream mockDelegate = mock(InputStream.class);
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(mockDelegate)) {
            s.reset();
        }
        verify(mockDelegate).reset();
    }

    /**
     * {@link UserProvidedContentInputStream#hashCode()} must delegate to the
     * underlying stream's hashCode.
     */
    @Test
    void testHashCode() throws IOException {
        InputStream delegate = new ByteArrayInputStream(DATA) {
            @Override
            public int hashCode() {
                return 42;
            }
        };
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(delegate)) {
            assertEquals(42, s.hashCode());
        }
    }

    /**
     * {@link UserProvidedContentInputStream#equals(Object)} must delegate to
     * the underlying stream.
     */
    @Test
    void testEqualsWhenDelegateReturnsTrue() throws IOException {
        final Object sentinel = new Object();
        InputStream delegate = new ByteArrayInputStream(DATA) {
            @Override
            public boolean equals(Object obj) {
                return obj == sentinel;
            }
        };
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(delegate)) {
            assertEquals(s, sentinel);
            assertNotEquals(s, new Object());
        }
    }

    /**
     * {@link UserProvidedContentInputStream#equals(Object)} must delegate to
     * the underlying stream and return false when the delegate does.
     */
    @Test
    void testEqualsWhenDelegateReturnsFalse() throws IOException {
        InputStream delegate = new ByteArrayInputStream(DATA) {
            @Override
            public boolean equals(Object obj) {
                return false;
            }
        };
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(delegate)) {
            assertNotEquals(s, new Object());
        }
    }

    /**
     * {@link UserProvidedContentInputStream#toString()} should prepend
     * {@code "UserProvidedContentInputStream: "} to the delegate's toString.
     */
    @Test
    void testToString() throws IOException {
        InputStream delegate = new ByteArrayInputStream(DATA) {
            @Override
            public String toString() {
                return "mockStream";
            }
        };
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(delegate)) {
            assertEquals("UserProvidedContentInputStream: mockStream", s.toString());
        }
    }

    /**
     * {@link UserProvidedContentInputStream#read()} should propagate
     * {@link IOException} thrown by the delegate.
     */
    @Test
    void testReadSingleBytePropagatesIOException() throws IOException {
        InputStream mockDelegate = mock(InputStream.class);
        when(mockDelegate.read()).thenThrow(new IOException("read error"));
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(mockDelegate)) {
            assertThrows(IOException.class, s::read);
        }
    }

    /**
     * {@link UserProvidedContentInputStream#read(byte[])} should propagate
     * {@link IOException} thrown by the delegate.
     */
    @Test
    void testReadByteArrayPropagatesIOException() throws IOException {
        InputStream mockDelegate = mock(InputStream.class);
        when(mockDelegate.read(any(byte[].class))).thenThrow(new IOException("read array error"));
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(mockDelegate)) {
            assertThrows(IOException.class, () -> s.read(new byte[4]));
        }
    }

    /**
     * {@link UserProvidedContentInputStream#read(byte[], int, int)} should
     * propagate {@link IOException} thrown by the delegate.
     */
    @Test
    void testReadByteArrayWithOffsetPropagatesIOException() throws IOException {
        InputStream mockDelegate = mock(InputStream.class);
        when(mockDelegate.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("read offset error"));
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(mockDelegate)) {
            assertThrows(IOException.class, () -> s.read(new byte[4], 0, 4));
        }
    }

    /**
     * {@link UserProvidedContentInputStream#skip(long)} should propagate
     * {@link IOException} thrown by the delegate.
     */
    @Test
    void testSkipPropagatesIOException() throws IOException {
        InputStream mockDelegate = mock(InputStream.class);
        when(mockDelegate.skip(anyLong())).thenThrow(new IOException("skip error"));
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(mockDelegate)) {
            assertThrows(IOException.class, () -> s.skip(1));
        }
    }

    /**
     * {@link UserProvidedContentInputStream#available()} should propagate
     * {@link IOException} thrown by the delegate.
     */
    @Test
    void testAvailablePropagatesIOException() throws IOException {
        InputStream mockDelegate = mock(InputStream.class);
        when(mockDelegate.available()).thenThrow(new IOException("available error"));
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(mockDelegate)) {
            assertThrows(IOException.class, s::available);
        }
    }

    /**
     * {@link UserProvidedContentInputStream#reset()} should propagate
     * {@link IOException} thrown by the delegate.
     */
    @Test
    void testResetPropagatesIOException() throws IOException {
        InputStream mockDelegate = mock(InputStream.class);
        doThrow(new IOException("reset error")).when(mockDelegate).reset();
        try (UserProvidedContentInputStream s = new UserProvidedContentInputStream(mockDelegate)) {
            assertThrows(IOException.class, s::reset);
        }
    }
}
