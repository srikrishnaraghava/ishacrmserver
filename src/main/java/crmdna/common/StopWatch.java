package crmdna.common;

public class StopWatch {
    private long startNS;

    private StopWatch() {
        startNS = System.nanoTime();
    }

    public static StopWatch createStarted() {
        StopWatch sw = new StopWatch();
        return sw;
    }

    public long msElapsed() {
        long endNS = System.nanoTime();

        int ms = Math.round((endNS - startNS) / 1000000);

        return ms;
    }

    public long nsElapsed() {
        long endNS = System.nanoTime();

        return endNS - startNS;
    }
}
