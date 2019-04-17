package com.truex.googlereferenceapp.player;

class SeekPosition {
    private long milliseconds;

    private SeekPosition(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    long getMilliseconds() {
        return milliseconds;
    }

    double getSeconds() {
        return getSeconds(milliseconds);
    }

    void subtractMilliseconds(long milliseconds) {
        this.milliseconds -= milliseconds;
    }

    void subtractSeconds(double seconds) {
        this.milliseconds -= getMilliseconds(seconds);
    }

    void addMilliseconds(long milliseconds) {
        this.milliseconds += milliseconds;
    }

    void addSeconds(double seconds) {
        this.milliseconds += getMilliseconds(seconds);
    }

    static SeekPosition fromMilliseconds(long milliseconds) {
        return new SeekPosition(milliseconds);
    }

    static SeekPosition fromSeconds(double seconds) {
        long milliseconds = getMilliseconds(seconds);
        return new SeekPosition(milliseconds);
    }

    private static long getMilliseconds(double seconds) {
        return Math.round(Math.floor(seconds * 1000.0));
    }

    private static double getSeconds(long milliseconds) {
        return milliseconds / 1000.0;
    }
}
