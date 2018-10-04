package com.gorylenko.writer

import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*


public class NormalizeEOLOutputStreamTest {
    ByteArrayOutputStream b
    NormalizeEOLOutputStream s
    OutputStreamWriter w

    @Before
    public void setUp() throws Exception {
        b = new ByteArrayOutputStream()
        s = new NormalizeEOLOutputStream(b)
        w = new OutputStreamWriter(s, "8859_1")
    }

    @Test
    public void testNormarlizeCRLF() {
        try {
            w.write("line1\r\nline2\r\n\r\n")
        } finally {
            w.close()
        }
        assertEquals("line1\nline2\n\n", new String(b.toByteArray(), "8859_1"))
    }

    @Test
    public void testNormarlizeCR() {
        try {
            w.write("line1\rline2\r\r")
        } finally {
            w.close()
        }
        assertEquals("line1\nline2\n\n", new String(b.toByteArray(), "8859_1"))
    }

    @Test
    public void testNormarlizeLF() {
        try {
            w.write("line1\nline2\n\n")
        } finally {
            w.close()
        }
        assertEquals("line1\nline2\n\n", new String(b.toByteArray(), "8859_1"))
    }
}
