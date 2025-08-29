package de.ullisroboterseite.ursai2udp;

// Autor: https://UllisRoboterSeite.de

// Doku:  https://UllisRoboterSeite.de/android-AI2-UDP.html
// Created: 2020-12-12
//
// Version 4.0 (2020-07-15)
// -------------------------
// - Komplett neu überarbeitet
//
// Version 4.1 (2020-07-15)
// -------------------------
// - Bei DropOwnBradcast wurde die falsche Adresse abgefragt.
//
// Version 4.2 (2021-03-30)
// -------------------------
// - Socket.setReuseAddress ermöglicht die Benutzung des gleichen Sockets
//   beim Senden und Empfangen
// - Beseitigt auch den Abbruch des Listeners im Companion, weil dort
//   kein onDestroy ausgelöst und der Listener so nicht automatisch geschlossen wird
//
// Version 4.3 (2021-04-09)
// -------------------------
// - Senden und Empfang von ByteArrays
//
//
// Version 4.4 (2022-03-30)
// -------------------------
// - Multicast-Funktion ergänzt
//
//
// Version 4.5 (2025-04-19)
// -------------------------
// - mit neuer Version des Byte-Array kompiliert
// - keine funktionalen Änderungen



import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.*;

import java.net.*;
import android.os.Handler;
import android.util.Log;

import de.ullisroboterseite.ursai2bytearray.UrsAI2ByteArray;

@DesignerComponent(version = 4, //
        versionName = UDPListener.VersionName, //
        dateBuilt = UDPListener.dateBuilt, //
        description = "AI2 extension block for UDP communication.", //
        category = com.google.appinventor.components.common.ComponentCategory.EXTENSION, //
        nonVisible = true, //
        helpUrl = "http://UllisRoboterSeite.de/android-AI2-UDP.html", //
        iconName = "aiwebres/icon.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET,android.permission.ACCESS_NETWORK_STATE")
public class UDPListener extends UDPListenerBase implements OnDestroyListener {
    static final String VersionName = "4.5.0";
    static final String dateBuilt = "2025-04-19";

    InetAddress group = null; // Multicast group IP
    DatagramSocket ListenSocket = null;
    int localPort = 0;

    public UDPListener(ComponentContainer container) {
        super(container);
        form.registerForOnDestroy(this);
    }

    // Erstellt den Listener-Thread und startet ihn
    @SimpleFunction(description = "Start the UDP listening server.")
    public void Start(int LocalPort) {
        Log.d(LOG_TAG, "UDPListener Start");

        // der Start erfolgt synchron, die Methode kehrt erst dann zurück,
        // wenn der Thread gestartet wurde.
        DatagramSocket ListenSocket = null;

        if (rcvTask != null) {
            ListenerFailure(ErrorCode.ListenerAlreadyRunning.ordinal());
            return;
        }

        group = null;
        localPort = LocalPort;

        setLock(); // Warten, bis andere Prozesse beendet sind.

        try {
            ListenSocket = new DatagramSocket((SocketAddress) null);
            ListenSocket.setReuseAddress(true);
            ListenSocket.bind(new InetSocketAddress(LocalPort));
        } catch (Exception ex) {
            resetLock();
            ListenerFailure(ErrorCode.InvalidLocalPort.ordinal());
            Log.e(LOG_TAG, "Start InvalidLocalPort: " + ex.toString());
            return;
        }

        listenerSocketsInUse.add(ListenSocket);

        rcvTask = new UDPListenerThread(this, ListenSocket, dropOwnBroadcast);
        rcvTask.Begin();

        resetLock();
        return;
    }

    // Erstellt den Listener-Thread und startet ihn
    @SimpleFunction(description = "Start the UDP listening server to listen to a multicast group.")
    public void StartMulticast(String GroupIP, int LocalPort) {
        Log.d(LOG_TAG, "UDPListener Multicast Start");

        // der Start erfolgt synchron, die Methode kehrt erst dann zurück,
        // wenn der Thread gestartet wurde.

        if (rcvTask != null) {
            ListenerFailure(ErrorCode.InvalidLocalPort.ordinal());
            return;
        }

        setLock(); // Warten, bis andere Prozesse beendet sind.

        try {
            localPort = LocalPort;
            group = InetAddress.getByName(GroupIP);
            MulticastSocket s = new MulticastSocket(LocalPort);
            s.joinGroup(new InetSocketAddress(group, LocalPort), null);
            ListenSocket = s;
            ListenSocket.setReuseAddress(true);
        } catch (Exception ex) {
            resetLock();
            ListenerFailure(ErrorCode.InvalidLocalPort.ordinal());
            Log.e(LOG_TAG, "Start InvalidLocalPort: " + ex.toString());
            return;
        }

        listenerSocketsInUse.add(ListenSocket);

        rcvTask = new UDPListenerThread(this, ListenSocket, dropOwnBroadcast);
        rcvTask.Begin();

        resetLock();
        return;
    }

    @SimpleFunction(description = "Stops the UDP listening server.")
    public void Stop() {
        // Der Stopp erfolgt synchron, die Methode kehrt erst dann zurück,
        // wenn der Thread beendet wurde.
        // Aufräumarbeiten, werden bemim Thread-Ende in 'ListenerThreadStopped'
        // ausgeführt.
        setLock(); // Warten, bis andere Prozesse beendet sind.
        if (rcvTask != null) {
            rcvTask.StopThread();
        }
        if (group != null)
            try {
                ((MulticastSocket) ListenSocket).leaveGroup(new InetSocketAddress(group, localPort), null);
            } catch (Exception e) {
                //TODO: handle exception
            }

        group = null;

        resetLock();
    } // StopListening

    public void PacketReceived(DatagramPacket packet) {
        final String remoteIP = packet.getAddress().getHostAddress();
        final int remotePort = packet.getPort();

        String stringResult;

        if (binaryMode) {
            byte[] d = packet.getData();
            stringResult = "";
            for (int i = 0; i < d.length; i++) {
                int temp = d[i];
                if (temp < 0)
                    temp += 256;
                stringResult += ";" + temp;
            }
            if (stringResult.length() > 0)
                stringResult = stringResult.substring(1);
        } else {
            stringResult = new String(packet.getData(), 0, packet.getLength());
        }
        Log.d(LOG_TAG, "Paket received: (" + packet.getLength() + ")" + stringResult);
        dataReceived(stringResult, remoteIP, remotePort, packet);

    } // PacketReceived

    public void ListenerThreadStopped(final ErrorCode errorCode) {
        // Wird bei Thread-Ende aufgerufen
        listenerSocketsInUse.remove(rcvTask.listenSocket);
        rcvTask = null;

        if (errorCode != ErrorCode.NoError)
            ListenerFailure(errorCode.ordinal());

        Log.d(LOG_TAG, "ListenerThreadStopped");
    } // ListenerThreadStopped

    @Override
    public void onDestroy() {
        if (rcvTask != null) {
            rcvTask.StopThread();
        }
    }
}