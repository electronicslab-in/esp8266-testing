package de.ullisroboterseite.ursai2udp;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.*;

import java.net.*;
import android.os.Handler;
import android.util.Log;

public class UDPListenerThread extends Thread {
    static final String LOG_TAG = UDPBase.LOG_TAG;

    UDPListener parent = null;
    public DatagramSocket listenSocket = null;
    public boolean isRunning = false;

    public volatile boolean stopRequest = false;
    public volatile boolean dropOwnBroadcast = false;
    public volatile String AbortedCause = "";

    UDPListenerThread(UDPListener parent, DatagramSocket listenSocket, boolean dropOwnBroadcast) {
        this.parent = parent;
        this.listenSocket = listenSocket;
        this.dropOwnBroadcast = dropOwnBroadcast;
        try {
            listenSocket.setSoTimeout(100); // Reaktionszeit auf StopRequest
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error setting SoTimeout");
        }

    }

    public void Begin() {
        isRunning = true;
        start();
    } // Begin

    public void StopThread() {
        stopRequest = true;
        while (isRunning) { // einfach nur warten
            Thread.yield();
        }
    }

    @Override
    public void run() {
        ErrorCode errCode = ErrorCode.NoError;
        byte[] buf = new byte[4096];

        Log.d(LOG_TAG, "Listener Thread start");
        Log.d(LOG_TAG, "Listener port: " + listenSocket.getLocalPort());
        Log.d(LOG_TAG, "Listener IP: " + listenSocket.getLocalAddress().toString());
        Log.d(LOG_TAG, "Listener LS: " + listenSocket.getLocalSocketAddress().toString());
        while (!stopRequest) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                listenSocket.receive(packet);
                Log.d(LOG_TAG, "Packet received");
                if (dropOwnBroadcast) {
                    InetSocketAddress adr = (InetSocketAddress) packet.getSocketAddress();
                    if (Helpers.isLocalNicIP(adr.getAddress())) {
                        continue;
                    }
                }
                parent.PacketReceived(packet); // nach oben melden
            } catch (SocketTimeoutException e) {
                // nichts zu tun
            } catch (Exception e) {
                errCode = ErrorCode.ListenerThreadAborted;
                AbortedCause = e.toString();
                Log.d(LOG_TAG, "Listener Thread aborted: " + e.toString());
                if (e.getCause() != null)
                    Log.d(LOG_TAG, "cause: " + e.getCause().toString());
                break;
            }
        } // end while

        // Tread beendet, aufräumen
        Log.d(LOG_TAG, "Listener Thread ends");
        listenSocket.close();
        listenSocket = null;

        isRunning = false;
        parent.ListenerThreadStopped(errCode);
    } // run

}
