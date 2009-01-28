package hudson.plugins.emailext;

/**
 * Utility class for internal use.
 *
 * @author jjamison
 *
 */
public class Util {

	// Prevent instantiation.
	private Util() {}
	
	public static String unescapeString(String escapedString) {
		StringBuffer sb = new StringBuffer();
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

	public static char unescapeChar(char escapedChar) {
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
	
	public static interface PrintfSpec {
		
		boolean printSpec(StringBuffer buf, char formatChar);
	
	}
	
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
