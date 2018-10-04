package com.gorylenko.writer

/**
 * Convert all line endings to LF characters
 *
 */
class NormalizeEOLOutputStream extends FilterOutputStream {

    private int eolCount = 0
    private int prevChar = 0

    private static final int CR = '\r'
    private static final int LF = '\n'

    public NormalizeEOLOutputStream(OutputStream out) {
        super(out)
    }

    @Override
    public void write(int b) throws IOException {
        if (b == CR || b == LF) {
            if (prevChar != CR && prevChar != LF) {
                eolCount ++ // first line ending (CR or LF), count 1
                prevChar = b
            } else if (prevChar == b) {
                eolCount ++ // next line ending char (like LF LF or CR CR), count 1
                prevChar = b
            } else {
                // second char of a pair (CR LF), ignore and reset prevChar to 0
                prevChar = 0
            }
        } else {
            writeEOLs()
            prevChar = b
            super.write(b)
        }
    }

    private void writeEOLs() throws IOException {
        // write all buffered line endings
        while (eolCount > 0) {
            out.write(LF)
            eolCount --
        }
    }

    @Override
    public void flush() throws IOException {
        writeEOLs()
        prevChar = 0
        super.flush()
    }
}
