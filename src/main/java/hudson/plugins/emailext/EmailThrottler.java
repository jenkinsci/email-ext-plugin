package hudson.plugins.emailext;

public class EmailThrottler {
    public static final int THROTTLING_LIMIT = 100; // Set your limit
    private static EmailThrottler instance;
    private int emailCount;
    private long lastResetTime;
    private static final long THROTTLING_PERIOD = 60 * 60 * 1000; // Set your period (in milliseconds)

    public EmailThrottler() {
        this.emailCount = 0;
        this.lastResetTime = System.currentTimeMillis();
    }

    public static synchronized EmailThrottler getInstance() {
        if (instance == null) {
            instance = new EmailThrottler();
        }
        return instance;
    }

    public synchronized boolean isThrottlingLimitExceeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResetTime > THROTTLING_PERIOD) {
            resetEmailCount();
        }
        return emailCount >= THROTTLING_LIMIT;
    }

    public synchronized void incrementEmailCount() {
        emailCount++;
    }

    public synchronized void resetEmailCount() {
        emailCount = 0;
        lastResetTime = System.currentTimeMillis();
    }
}
