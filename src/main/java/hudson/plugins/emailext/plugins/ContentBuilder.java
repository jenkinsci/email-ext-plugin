package hudson.plugins.emailext.plugins;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailExtException;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.Util;
import hudson.tasks.Mailer;
import hudson.tasks.Publisher;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link Publisher} that sends notification e-mail.
 *
 * @author kyle.sweeney@valtech.com
 *
 */
public class ContentBuilder {
	
	private static final Logger LOGGER = Logger.getLogger(Mailer.class.getName());

	private static final String DEFAULT_BODY = "\\$DEFAULT_CONTENT|\\$\\{DEFAULT_CONTENT\\}";
	private static final String DEFAULT_SUBJECT = "\\$DEFAULT_SUBJECT|\\$\\{DEFAULT_SUBJECT\\}";
	
	private static final String PROJECT_DEFAULT_BODY = "\\$PROJECT_DEFAULT_CONTENT|\\$\\{PROJECT_DEFAULT_CONTENT\\}";
	private static final String PROJECT_DEFAULT_SUBJECT = "\\$PROJECT_DEFAULT_SUBJECT|\\$\\{PROJECT_DEFAULT_SUBJECT\\}";
	
	private static final Map<String,EmailContent> EMAIL_CONTENT_TYPE_MAP = new LinkedHashMap<String,EmailContent>();
	
	public static void addEmailContentType(EmailContent contentType) throws EmailExtException {
		if (EMAIL_CONTENT_TYPE_MAP.containsKey(contentType.getToken())) {
			throw new EmailExtException("An email content type with token name " +
					contentType.getToken() + " was already added.");
		}
		
		EMAIL_CONTENT_TYPE_MAP.put(contentType.getToken(), contentType);
	}
	
	public static void removeEmailContentType(EmailContent contentType) {
		if(EMAIL_CONTENT_TYPE_MAP.containsKey(contentType.getToken())) {
			EMAIL_CONTENT_TYPE_MAP.remove(contentType);
		}
	}
	
	public static EmailContent getEmailContentType(String token) {
		return EMAIL_CONTENT_TYPE_MAP.get(token);
	}
	
	public static Collection<EmailContent> getEmailContentTypes() {
		return EMAIL_CONTENT_TYPE_MAP.values();
	}
	
	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	String transformText(String origText, ExtendedEmailPublisher publisher, EmailType type, B build) {
		String newText = origText.replaceAll(PROJECT_DEFAULT_BODY, Matcher.quoteReplacement(publisher.defaultContent))
		 						 .replaceAll(PROJECT_DEFAULT_SUBJECT, Matcher.quoteReplacement(publisher.defaultSubject))
								 .replaceAll(DEFAULT_BODY, Matcher.quoteReplacement(ExtendedEmailPublisher.DESCRIPTOR.getDefaultBody()))
								 .replaceAll(DEFAULT_SUBJECT, Matcher.quoteReplacement(ExtendedEmailPublisher.DESCRIPTOR.getDefaultSubject()));
						
		newText = replaceTokensWithContent(newText, publisher, type, build);
		return newText;
	}
	
	private static <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	String replaceTokensWithContent(String origText, ExtendedEmailPublisher publisher, EmailType type, AbstractBuild<P, B> build) {
		StringBuffer sb = new StringBuffer();
		Tokenizer tokenizer = new Tokenizer(origText);

		while (tokenizer.find()) {
			String tokenName = tokenizer.getTokenName();
			Map<String, Object> args = tokenizer.getArgs();
			EmailContent content = EMAIL_CONTENT_TYPE_MAP.get(tokenName);
			String replacement;
			if (content != null) {
				try {
					replacement = content.getContent(build, publisher, type, args);
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE,
							"Exception thrown while replacing " + tokenizer.group(),
							e);
					replacement = "[[ Exception while replacing " + tokenName + ".  Please report this as a bug. ]]";
				}
				if (content.hasNestedContent()) {
					replacement = replaceTokensWithContent(replacement, publisher, type, build);
				}
			} else {
				replacement = tokenizer.group();
			}
			tokenizer.appendReplacement(sb, replacement);
		}
		tokenizer.appendTail(sb);
		
		return sb.toString();
	}

	static class Tokenizer {
		private static final String tokenNameRegex = "[a-zA-Z0-9_]+";
		
		private static final String numberRegex = "-?[0-9]+(\\.[0-9]*)?";
		private static final String boolRegex = "(true)|(false)";
		// Sequence of (1) not \ " CR LF and (2) \ followed by non line terminator
		private static final String stringRegex = "\"([^\\\\\"\\r\\n]|(\\\\.))*\"";
		private static final String valueRegex = "(" + numberRegex + ")|(" + boolRegex + ")|(" + stringRegex + ")";
		
		private static final String spaceRegex = "[ \\t]*";
		private static final String argRegex = "(" + tokenNameRegex + ")" + spaceRegex + "=" + spaceRegex + "(" + valueRegex + ")";
		private static final String argsRegex = "((" + spaceRegex + "," + spaceRegex + argRegex + ")*)";
		
		private static final String delimitedTokenRegex = "\\{" + spaceRegex + "(" + tokenNameRegex + ")" + argsRegex + spaceRegex + "\\}";
		private static final String tokenRegex = "\\$((" + tokenNameRegex + ")|(" + delimitedTokenRegex + "))";
		
		private static final Pattern argPattern = Pattern.compile(argRegex);
		private static final Pattern tokenPattern = Pattern.compile(tokenRegex);
		
		private final Matcher tokenMatcher;
		private String tokenName = null;
		private Map<String, Object> args = null;
		
		Tokenizer(String origText) {
			tokenMatcher = tokenPattern.matcher(origText);
		}
		
		String getTokenName() {
			return tokenName;
		}
		
		Map<String, Object> getArgs() {
			return args;
		}
		
		String group() {
			return tokenMatcher.group();
		}
		
		boolean find() {
			if (tokenMatcher.find()) {
				tokenName = tokenMatcher.group(2);
				if (tokenName == null) {
					tokenName = tokenMatcher.group(4);
				}
				args = new HashMap<String, Object>();
				if (tokenMatcher.group(5) != null) {
					parseArgs(tokenMatcher.group(5), args);
				}
				return true;
			} else {
				return false;
			}
		}
		
		static void parseArgs(String argsString, Map<String, Object> args) {
			Matcher argMatcher = argPattern.matcher(argsString);
			while (argMatcher.find()) {
				Object arg;
				if (argMatcher.group(3) != null) {
					// number
					if (argMatcher.group(4) != null) {
						arg = Float.valueOf(argMatcher.group(3));
					} else {
						arg = Integer.valueOf(argMatcher.group(3));
					}
				} else if (argMatcher.group(5) != null) {
					// boolean
					if (argMatcher.group(6) != null) {
						arg = Boolean.TRUE;
					} else {
						arg = Boolean.FALSE;
					}
				} else { // if (argMatcher.group(8) != null) {
					// string
					arg = Util.unescapeString(argMatcher.group(8));
				}
				args.put(argMatcher.group(1), arg);
			}
		}
		
		void appendReplacement(StringBuffer sb, String replacement) {
			tokenMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		
		void appendTail(StringBuffer sb) {
			tokenMatcher.appendTail(sb);
		}
		
	}

}
