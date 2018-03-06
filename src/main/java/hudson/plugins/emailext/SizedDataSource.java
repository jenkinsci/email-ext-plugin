package hudson.plugins.emailext;

import javax.activation.DataSource;

public interface SizedDataSource extends DataSource {
    long getSize();
}
