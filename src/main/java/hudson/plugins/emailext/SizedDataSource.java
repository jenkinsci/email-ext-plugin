package hudson.plugins.emailext;

import jakarta.activation.DataSource;

public interface SizedDataSource extends DataSource {
    long getSize();
}
