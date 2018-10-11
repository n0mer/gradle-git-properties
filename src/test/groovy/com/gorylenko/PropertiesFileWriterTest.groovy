package com.gorylenko

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotEquals
import static org.junit.Assert.assertTrue

class PropertiesFileWriterTest {

    PropertiesFileWriter writer

    @Before
    public void setUp() throws Exception {
        writer = new PropertiesFileWriter()
    }

    @Test
    public void testWriteNormalProperties() {

        File file = File.createTempFile("temp",".tmp")
        writer.write([greeting: 'Hello'], file, true)

        Properties props = loadProperties(file)
        assertEquals(props, [greeting: 'Hello'])
    }

    @Test
    public void testWritingSameContent() {

        // Set up a Properties file with greeting=Hello
        File file = File.createTempFile("temp",".tmp")
        Properties props = new Properties()
        props.setProperty('greeting', 'Hello')
        file.withOutputStream {
            props.store(it, null)
        }

        // Write to same file with same content (force=false)
        writer.write([greeting: 'Hello'], file, false)

        // Write to same file with same content (force=true)
        writer.write([greeting: 'Hello'], file, true)
    }


    @Test
    public void testWritingDifferentContent() {

        // write a Properties file with greeting=Hello
        File file = File.createTempFile("temp",".tmp")
        Properties props = new Properties()
        props.setProperty('greeting', 'Hello')
        file.withOutputStream {
            props.store(it, null)
        }

        // Try to write to same file with different content
        writer.write([greeting: 'Hello2'], file, false)

        // Make sure content is updated
        assertEquals(loadProperties(file), [greeting: 'Hello2'])
    }

    private Properties loadProperties(File file) {
        def props = new Properties()
        file.withInputStream {
            props.load it
        }
        return props
    }


    @Test
    public void testWriteNormalPropertiesResultMustBeSorted() {

        File file = File.createTempFile("temp",".tmp")
        writer.write([greeting_3: 'Hello', greeting_1: 'Hello', greeting_2: 'Hello', greeting_4: 'Hello'], file, true)

        String result = file.text
        int index1 = result.indexOf("greeting_1")
        int index2 = result.indexOf("greeting_2")
        int index3 = result.indexOf("greeting_3")
        int index4 = result.indexOf("greeting_4")

        assertTrue(index1 < index2)
        assertTrue(index2 < index3)
        assertTrue(index3 < index4)
    }

    @Test
    public void shouldNotContainComments() {
        // given:
        def map = [greeting_3: 'Hello', greeting_1: 'Hello', greeting_2: 'Hello']
        File file = File.createTempFile("temp", ".tmp")

        // when:
        writer.write(map, file, true)
        def lines = file.text.readLines()
        def nonEmptyLines = lines.findAll { it.length() > 0 }
        def hasComments = lines.findAll({ it.startsWith("#") })
                               .isEmpty()

        // then:
        assertTrue("git.properties should not contain comments", hasComments)
        assertEquals(map.size(), lines.size())
        assertEquals(map.size(), nonEmptyLines.size())
    }
}
