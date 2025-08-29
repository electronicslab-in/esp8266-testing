package de.ullisroboterseite.ursai2udp;

/**
 * Klasse zum Ausführen asynchroner Funktionen
 */
public abstract class UrsAsyncFunction<ReturnType> {
    private ReturnType result;

    private MyThread myThread;
    private volatile boolean threadIsRunning = false;
    private volatile boolean threadWasAborted = false;
    private volatile Exception ex = null;

    /**
     * Die Arbeitsmethode
     *
     * @return Der zurück zu liefernde Wert.
     */
    protected abstract ReturnType execute();

    /**
     * Führt die Methode \ref execute aus.
     *
     * @return
     */
    public ReturnType doExecute() {
        threadWasAborted = false;
        ex = null;
        myThread = new MyThread();
        threadIsRunning = true;
        myThread.start();
        while (threadIsRunning)
            ;
        if (threadWasAborted)
            throw new RuntimeException("Fehler beim Ausführen der Funktion. Siehe getCause()", ex);
        return result;
    }

    /**
     * Gibt an, ob der Thread auf Grund eines Fehlers in \ref execute
     * abgebrochen wurde.
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
     * @return Die in \ref execute aufgetretene Exception oder null, falls
     *         <br>
     *         - der Thread noch nie gestartet wurde<br>
     *         - der Thread noch aktiv ist<br>
     *         - der Thread fehlerfrei beendet wurde.
     */
    public synchronized Exception getExeption() {
        return ex;
    }

    /**
     * Der Thread in dem /ref execute ausgeführt wird.
     */
    private class MyThread extends Thread {
        @Override
        public void run() {
            try {
                result = execute();
            } catch (Exception e) {
                threadWasAborted = true;
                ex = e;
            } finally {
                threadIsRunning = false;
            }
        }
    }
}
