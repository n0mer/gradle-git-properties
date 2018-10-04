package com.gorylenko.writer

import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*


public class SkipPropertiesCommentsOutputStreamTest {
    ByteArrayOutputStream b
    SkipPropertiesCommentsOutputStream s
    OutputStreamWriter w

    @Before
    public void setUp() throws Exception {
        b = new ByteArrayOutputStream()
        s = new SkipPropertiesCommentsOutputStream(b)
        w = new OutputStreamWriter(s, "8859_1")
    }

    @Test
    public void test0Comment() {
        try {
            w.write("line1\nline2\n\n")
        } finally {
            w.close()
        }
        assertEquals("line1\nline2\n\n", new String(b.toByteArray(), "8859_1"))
    }
    @Test
    public void test0CommentWithSpecialChar() {
        try {
            w.write("line1#withspecialchars!\n\n")
        } finally {
            w.close()
        }
        assertEquals("line1#withspecialchars!\n\n", new String(b.toByteArray(), "8859_1"))
    }
    @Test
    public void test1Comment1() {
        try {
            w.write("#line1\nline2\n\n")
        } finally {
            w.close()
        }
        assertEquals("line2\n\n", new String(b.toByteArray(), "8859_1"))
    }
    @Test
    public void test1Comment2() {
        try {
            w.write("!line1\nline2\n\n")
        } finally {
            w.close()
        }
        assertEquals("line2\n\n", new String(b.toByteArray(), "8859_1"))
    }
    @Test
    public void test2Comments1() {
        try {
            w.write("##line1\n#line2\nline3\n\n")
        } finally {
            w.close()
        }
        assertEquals("line3\n\n", new String(b.toByteArray(), "8859_1"))
    }
    @Test
    public void test2Comments2() {
        try {
            w.write("!!line1\n!line2\nline3\n\n")
        } finally {
            w.close()
        }
        assertEquals("line3\n\n", new String(b.toByteArray(), "8859_1"))
    }

}
