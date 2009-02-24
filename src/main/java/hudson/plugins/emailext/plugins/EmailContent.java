package hudson.plugins.emailext.plugins;

import java.util.List;
import java.util.Map;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;

public interface EmailContent {
	
	/**
	 * This is the token that will be replaced by the content when the email is sent.
	 * If the email has a string like "$REPLACE_ME", then the implementation of this 
	 * method should return "REPLACE_ME".
	 */
	public String getToken();
	
	/**
	 * These are the arguments accepted when generating the content for this token.
	 */
	public List<String> getArguments();
	
	/**
	 * This is a string that will be rendered in the help section of the plugin.
	 * It describes what the content does and what it puts in the email.
	 */
	public String getHelpText();
	
	/**
	 * This method returns the generated content that should replace the token.
	 * @param publisher TODO
	 * @param args the arguments for generating the content
	 */
	public <P extends AbstractProject<P,B>, B extends AbstractBuild<P,B>>
	String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher,
			EmailType emailType, Map<String, ?> args);

	/**
	 * Specifies whether or not the content returned by this object can have nested
	 * tokens in it that need to be resolved before sending the email.
	 */
	public boolean hasNestedContent();
	
	public abstract class Args {
		
		private Args() {}
		
		public static String get(Map<String, ?> args, String name, String defaultValue)  {
			Object arg = args.get(name);
			if (arg instanceof String) {
				return (String)arg;
			} else {
				return defaultValue;
			}
		}
		
		public static int get(Map<String, ?> args, String name, int defaultValue)  {
			Object arg = args.get(name);
			if (arg instanceof Integer) {
				return ((Integer)arg).intValue();
			} else {
				return defaultValue;
			}
		}
		
		public static float get(Map<String, ?> args, String name, float defaultValue)  {
			Object arg = args.get(name);
			if (arg instanceof Float) {
				return ((Float)arg).floatValue();
			} else {
				return defaultValue;
			}
		}
		
		public static boolean get(Map<String, ?> args, String name, boolean defaultValue)  {
			Object arg = args.get(name);
			if (arg instanceof Boolean) {
				return ((Boolean)arg).booleanValue();
			} else {
				return defaultValue;
			}
		}
		
	}

}
