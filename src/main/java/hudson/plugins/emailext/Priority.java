package hudson.plugins.emailext;

public enum Priority {
    DEFAULT("(Default)", ""),
    HIGH("High", "1"),
    NORMAL("Normal", "3"),
    LOW("Low", "5");

    private final String displayName;
    private final String xPriorityValue;

    Priority(String displayName, String xPriorityValue) {
        this.displayName = displayName;
        this.xPriorityValue = xPriorityValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getXPriorityValue() {
        return xPriorityValue;
    }
}
