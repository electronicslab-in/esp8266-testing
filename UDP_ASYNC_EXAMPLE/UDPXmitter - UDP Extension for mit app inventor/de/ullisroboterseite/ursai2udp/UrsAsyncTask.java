package de.ullisroboterseite.ursai2udp;

/**
 * Wrapper für die Thread-Klasse.
 */
public abstract class UrsAsyncTask {
    private MyThread myThread;
    private volatile boolean threadIsRunning = false;
    private volatile boolean threadWasAborted = false;
    private volatile boolean stopRequest = false;
    private volatile Exception ex = null;

    /**
     * Die Arbeitsmethode
     */
    public abstract void execute();

    /**
     * Startet den Thread erneut.
     *
     * Startet den Thread erneut. Der Thread darf nicht aktiv sein.
     *
     * @return true, wenn ein neuer Thread gestartet wurde; false, falls bereits
     *         aktiviert.
     */
    protected synchronized boolean doStart() {
        if (threadIsRunning)
            return false;
        threadIsRunning = true;
        threadWasAborted = false;
        ex = null;
        myThread = new MyThread();
        myThread.start();
        return true;
    }

    /**
     * Sorgt dafür, dass \ref keepRunning false liefert.
     *
     * Die Methode \ref doInBackground muss daraufhin abbrechen. Es wird
     * <strong>nicht<strong> auf die Beendigung der Methode gewartet.
     *
     * @return false, falls der Thread nicht aktiv war; ansonsten true.
     */
    public synchronized boolean stopAsync() {
        if (!threadIsRunning)
            return false;
        stopRequest = true;
        return true;
    }

    /**
     * Sorgt dafür, dass \ref keepRunning false liefert und wartet auf die
     * Beendigung des Threads.
     *
     * Die Methode \ref doInBackground muss daraufhin abbrechen. Tut sie dies nicht,
     * wird die Methode nicht verlassen (Endlosschleife)!
     *
     * @return false, falls der Thread nicht aktiv war; ansonsten true.
     */
    public boolean stop() {
        if (!threadIsRunning)
            return false;
        stopRequest = true;
        while (threadIsRunning)
            Thread.yield();
        return true;
    }

    /**
     * \brief Gibt an, ob der Thread aktiv ist.
     *
     * @return true, wenn der Thread aktiv ist; ansonsten false.
     */
    public synchronized boolean isRunning() {
        return threadIsRunning;
    }

    /**
     * Gibt an, ob der Thread auf Grund eines Fehlers in \ref execute abgebrochen
     * wurde.
     *
     * @return true, falls \ref execute eine Exception geworfen hat. false,
     *         wenn der Thread normal beendet wurde.
     */
    public synchronized boolean wasAborted() {
        return threadWasAborted;
    }

    /**
     * Liefert die in \ref execute aufgetretene Exception.
     *
     * @return Die in \ref execute aufgetretene Exception oder null, falls <br>
     *         - der Thread noch nie gestartet wurde<br>
     *         - der Thread noch aktiv ist<br>
     *         - der Thread fehlerfrei beendet wurde.
     */
    public synchronized Exception getExeption() {
        return ex;
    }

    /**
     * Gibt an, ob \ref execute weiter ausgeführt weden soll (zur Benutzung in \ref
     * execute).
     *
     * @return true, wenn \ref execute weiter ausgeführt weden soll; ansonsten
     *         false.
     */
    protected synchronized boolean keepRunning() {
        return this.stopRequest == false;
    }

    /**
     * Pausiert den Thread (zur Benutzung in \ref execute).
     *
     * Kapselt den in Thread.sleep() notwendigen try/catch-Block.
     *
     * @param ms
     *
     */
    protected void sleep(long ms) {
        try {
            MyThread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    class MyThread extends Thread {
        @Override
        public void run() {
            try {
                execute();
            } catch (Exception e) {
                threadWasAborted = true;
                ex = e;
            } finally {
                threadIsRunning = false;
            }
        }
    }
}