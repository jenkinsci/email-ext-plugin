package hudson.plugins.emailext.plugins.content;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.FilePath;
import java.io.File;
import org.junit.jupiter.api.Test;

class AbstractEvalContentTest {

    @Test
    void isChildOf_returnsTrueForValidChild() throws Exception {
        File parentDir = new File(System.getProperty("java.io.tmpdir"), "parent");
        File childDir = new File(parentDir, "child");
        parentDir.mkdirs();
        childDir.mkdirs();

        FilePath parent = new FilePath(parentDir);
        FilePath child = new FilePath(childDir);

        assertTrue(AbstractEvalContent.isChildOf(child, parent));
    }

    @Test
    void isChildOf_returnsFalseWhenNotChild() throws Exception {
        File parentDir = new File(System.getProperty("java.io.tmpdir"), "parent");
        File otherDir = new File(System.getProperty("java.io.tmpdir"), "other");
        parentDir.mkdirs();
        otherDir.mkdirs();

        FilePath parent = new FilePath(parentDir);
        FilePath other = new FilePath(otherDir);

        assertFalse(AbstractEvalContent.isChildOf(other, parent));
    }

    @Test
    void isChildOf_returnsFalseWhenSameDirectory() throws Exception {
        File parentDir = new File(System.getProperty("java.io.tmpdir"), "parent");
        parentDir.mkdirs();

        FilePath parent = new FilePath(parentDir);

        assertFalse(AbstractEvalContent.isChildOf(parent, parent));
    }
}
