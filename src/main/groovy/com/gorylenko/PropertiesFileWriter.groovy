package com.gorylenko

class PropertiesFileWriter {

    boolean write(Map<String, String> properties, File file, boolean force) {
        if (!force && hasSameContent(file, properties)) {
            // Skipping writing [${file}] as it is up-to-date.
            return false
        } else {
            // Writing to [${file}]...
            writeToPropertiesFile(properties, file)
            return true
        }
    }

    private static class SortedProperties extends Properties {
        private static final long serialVersionUID = 1L

        @Override
        synchronized Enumeration<Object> keys() {
            Vector<String> v = new Vector<String>(keySet())
            Collections.sort(v)
            return new Vector<Object>(v).elements()
        }

        @Override
        void store(OutputStream out, String comments) throws IOException {
            store(new BufferedWriter(new OutputStreamWriter(out, "8859_1")), comments)
        }

        @Override
        void store(Writer writer, String comments) throws IOException {
            // write to our writer first, so that we can remove comments
            def baos = new ByteArrayOutputStream()
            def pw = new PrintWriter(baos)
            super.store(pw, comments)

            // remove comments, and join as multi-line string again
            def value = new ByteArrayInputStream(baos.toByteArray()).readLines()
                                                                    .findAll { !it.startsWith("#") }
                                                                    .join(String.format("%n"))

            // write to the actual writer
            writer.append(value)
            writer.flush()
        }
    }

    private void writeToPropertiesFile(Map<String, String> properties, File propsFile) {
        if (!propsFile.parentFile.exists()) {
            propsFile.parentFile.mkdirs()
        }
        if (propsFile.exists()) {
            propsFile.delete()
        }
        propsFile.createNewFile()
        propsFile.withOutputStream {
            def props = new SortedProperties()
            props.putAll(properties)
            props.store(it, null)
        }
    }

    private boolean hasSameContent(File propsFile, Map<String, String> properties) {
        boolean sameContent = false
        if (propsFile.exists()) {
            def props = new Properties()
            propsFile.withInputStream {
                props.load it
            }
            if (props.equals(properties)) {
                sameContent = true
            }
        }
        return sameContent
    }
}
