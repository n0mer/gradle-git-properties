package com.gorylenko.writer

import groovy.transform.CompileStatic

/**
 * Remove all lines start with # and end with LF (\n)
 */
@CompileStatic
class SkipPropertiesCommentsOutputStream extends FilterOutputStream {

    private static int START_COMMENT_CHAR1 = '#' as char
    private static int START_COMMENT_CHAR2 = '!' as char
    private static int LINE_END_CHAR = '\n' as char

    private boolean commentFound = false
    private int lastChar = LINE_END_CHAR

    public SkipPropertiesCommentsOutputStream(OutputStream out) {
        super(out)
    }

    @Override
    public void write(int b) throws IOException {
        if (!commentFound && (b == START_COMMENT_CHAR1 || b == START_COMMENT_CHAR2) && lastChar == LINE_END_CHAR) {
            commentFound = true
        } else if (commentFound && b == LINE_END_CHAR) {
            commentFound = false
        } else if (!commentFound) {
            super.write(b)
        }
        lastChar = b
    }
}
