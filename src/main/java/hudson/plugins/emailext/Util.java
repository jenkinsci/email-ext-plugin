package hudson.plugins.emailext;

/**
 * Utility class for internal use.
 *
 * @author jjamison
 *
 */
@Deprecated
public class Util {

    // Prevent instantiation.
    private Util() {
    }

    /**
     * Replaces all the printf-style escape sequences in a string
     * with the appropriate characters.
     *
     * @param escapedString the string containing escapes
     * @return the string with all the escape sequences replaced
     */
    public static String unescapeString(String escapedString) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < escapedString.length() - 1; ++i) {
            char c = escapedString.charAt(i);
            if (c == '\\') {
                ++i;
                sb.append(unescapeChar(escapedString.charAt(i)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static char unescapeChar(char escapedChar) {
        switch (escapedChar) {
            case 'b':
                return '\b';
            case 't':
                return '\t';
            case 'n':
                return '\n';
            case 'f':
                return '\f';
            case 'r':
                return '\r';
            default:
                return escapedChar;
        }
    }

    /**
     * An interface for use with Util.printf to specialize behavior.
     * Implementing printSpec allows % escape sequences to be handled
     * in an individual way.
     */
    public interface PrintfSpec {

        /**
         * Specializes the behavior of printf for % escape characters.
         * Given a character, appends the replacement of that escape character
         * to the given buffer, if the escape character can be handled.
         * Otherwise, does nothing.
         *
         * @param buf the buffer to append the result to
         * @param formatChar the escape character being replaced
         * @return true if the character was handled
         */
        boolean printSpec(StringBuffer buf, char formatChar);
    }

    /**
     * Formats a string and puts the result into a StringBuffer.
     * Allows for standard Java backslash escapes and a customized behavior
     * for % escapes in the form of a PrintfSpec.
     *
     * @param buf the buffer to append the result to
     * @param formatString the string to format
     * @param printfSpec the specialization for printf
     */
    public static void printf(StringBuffer buf, String formatString, PrintfSpec printfSpec) {
        for (int i = 0; i < formatString.length(); ++i) {
            char c = formatString.charAt(i);
            if ((c == '%') && (i + 1 < formatString.length())) {
                ++i;
                char code = formatString.charAt(i);
                if (code == '%') {
                    buf.append('%');
                } else {
                    boolean handled = printfSpec.printSpec(buf, code);
                    if (!handled) {
                        buf.append('%');
                        buf.append(code);
                    }
                }
            } else if ((c == '\\') && (i + 1 < formatString.length())) {
                ++i;
                buf.append(Util.unescapeChar(formatString.charAt(i)));
            } else {
                buf.append(c);
            }
        }
    }
}
