package hudson.plugins.emailext;

/**
 * Represents the modes for attaching build logs to certain operations or actions.
 * This enumeration defines three possible modes:
 * - NONE: Do not attach the build log.
 * - ATTACH: Attach the build log without any compression.
 * - COMPRESS_AND_ATTACH: Compress the build log before attaching it.
 */
public enum AttachBuildLogMode {
    NONE(Messages.attachBuildLog_doNotAttach(), 0),
    ATTACH(Messages.attachBuildLog_attach(), 1),
    COMPRESS_AND_ATTACH(Messages.attachBuildLog_compressAndAttach(), 2);

    private final String description;
    private final int legacyValue;

    AttachBuildLogMode(String description, int legacyValue) {
        this.description = description;
        this.legacyValue = legacyValue;
    }

    public String getDescription() {
        return description;
    }

    public int toLegacyValue() {
        return legacyValue;
    }

    public boolean isAttaching() {
        return switch (this) {
            case ATTACH, COMPRESS_AND_ATTACH -> true;
            default -> false;
        };
    }

    public boolean isCompressing() {
        return this == COMPRESS_AND_ATTACH;
    }

    public static AttachBuildLogMode fromLegacyValue(int value) {
        return switch (value) {
            case 1 -> ATTACH;
            case 2 -> COMPRESS_AND_ATTACH;
            default -> NONE;
        };
    }

    public static AttachBuildLogMode fromLegacyBool(boolean attachLog, boolean compressLog) {
        AttachBuildLogMode result = AttachBuildLogMode.NONE;
        if (attachLog && compressLog) {
            result = AttachBuildLogMode.COMPRESS_AND_ATTACH;
        } else if (attachLog) {
            result = AttachBuildLogMode.ATTACH;
        }
        return result;
    }
}
