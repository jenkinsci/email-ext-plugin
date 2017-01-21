package hudson.plugins.emailext;

import org.jvnet.localizer.Localizable;

/**
 * Controls when the e-mail gets sent in case of the matrix project.
 */
public enum MatrixTriggerMode {
    ONLY_PARENT(Messages._MatrixTriggerMode_OnlyParent(),true,false),
    ONLY_CONFIGURATIONS(Messages._MatrixTriggerMode_OnlyConfigurations(),false,true),
    BOTH(Messages._MatrixTriggerMode_Both(),true,true); // traditional default behaviour
    
    private final Localizable description;

    public final boolean forParent;
    public final boolean forChild;

    MatrixTriggerMode(Localizable description, boolean forParent, boolean forChild) {
        this.description = description;
        this.forParent = forParent;
        this.forChild = forChild;
    }
 
    public String getDescription() {
        return description.toString();
    }
}
