package hudson.plugins.emailext;

import hudson.util.FormValidation;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

/**
 * Simple Matcher that checks a FormValidations message.
 */
public class FormValidationMessageMatcher extends TypeSafeMatcher<FormValidation> {

    private final Matcher<String> messageMatcher;

    private FormValidationMessageMatcher(Matcher<String> messageMatcher) {
        this.messageMatcher = messageMatcher;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("message ");
        messageMatcher.describeTo(description);
    }

    @Override
    protected void describeMismatchSafely(FormValidation item, Description mismatchDescription) {
        mismatchDescription.appendText("message ");
        messageMatcher.describeMismatch(item.renderHtml(), mismatchDescription);
    }

    @Override
    protected boolean matchesSafely(FormValidation item) {
        return messageMatcher.matches(item.renderHtml());
    }

    /**
     * Creates a matcher of {@link FormValidation} that matches when the examined validations message is exactly the given String.
     * This should only be used where there is a single validation otherwise if the format produced by {@link FormValidation#aggregate(java.util.Collection)}changes then this will break.
     * @param message the exact message expected.
     */
    public static FormValidationMessageMatcher hasMessage(String message) {
        return new FormValidationMessageMatcher(Matchers.is(message));
    }

    /**
     * Creates a matcher of {@link FormValidation} that matches when the examined validations message is matches the given {@code matcher}.
     * @param matcher the matcher to be used to check the message.
     */
    public static FormValidationMessageMatcher hasMessage(Matcher<String> matcher) {
        return new FormValidationMessageMatcher(matcher);
    }
}
