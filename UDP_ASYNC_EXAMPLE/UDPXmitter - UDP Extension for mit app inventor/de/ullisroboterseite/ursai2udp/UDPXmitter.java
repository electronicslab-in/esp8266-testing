package de.ullisroboterseite.ursai2udp;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.*;

import java.net.*;
import android.os.Handler;
import android.util.Log;

import de.ullisroboterseite.ursai2bytearray.UrsAI2ByteArray;

class XmitDatagramSocket {// Rückgabewert bei der Ermittlung des Sende-Sockets.
    boolean isListenerSocket; // Gibt an, ob dieser Socket (Port) in einem Listener Verwand wird.
    DatagramSocket socket; // Der Socket.

    XmitDatagramSocket(DatagramSocket _socket, boolean _isListenerSocket) {
        socket = _socket;
        isListenerSocket = _isListenerSocket;
    }
}

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
public class UDPXmitter extends UDPXmitterBase {

    public UDPXmitter(ComponentContainer container) {
        super(container);
    } // ctor

    // Sendet ein Datagramm an das im Designer eingestellte Ziel.
    // Liefert einen Return-Code zur synchronen Auswertung.
    @SimpleFunction(description = "Send a datagram")
    public int Xmit(String Message) {
        byte[] bytes = null; // Daten
        ErrorCode rtc = ErrorCode.NoError;

        if (binaryMode) {
            try {
                bytes = Helpers.StringToBytes(Message);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Xmit, cannot convert to Byte Array: " + Message);
                rtc = ErrorCode.BinaryConversionFailed;
            }
        } else {
            bytes = Message.getBytes();
        }

        if (rtc == ErrorCode.NoError) {
            if (remoteIP == null) {
                Log.e(LOG_TAG, "Xmit, no Remote IP for: " + remoteHost);
                rtc = ErrorCode.UnknownRemoteHost;
            } else {
                rtc = doXmit(remoteIP, remotePort, localPort, bytes);
            }
        }

        setErrorInfo(rtc); // An dieser Stelle, damit der StackTrace die richtige Methode/Action liefert.
        return rtc.ordinal();
    }

    // Sendet ein Datagramm an das angegebene Ziel.
    // Liefert einen Return-Code zur synchronen Auswertung.
    @SimpleFunction(description = "Send a datagram")
    public int XmitTo(String RemoteHost, int RemotePort, String Message) {
        byte[] bytes = null; // Daten
        ErrorCode rtc = ErrorCode.NoError;

        if (binaryMode) {
            try {
                bytes = Helpers.StringToBytes(Message);
            } catch (Exception e) {
                Log.e(LOG_TAG, "XmitTo, cannot convert to Byte Array: " + Message);
                rtc = ErrorCode.BinaryConversionFailed;
            }
        } else {
            bytes = Message.getBytes();
        }

        if (rtc == ErrorCode.NoError) {
            // Remote-IP-Adresse ermitteln
            InetAddress toAddr = Helpers.getIpFromHostName.doExecute(RemoteHost);
            if (toAddr == null) {
                Log.e(LOG_TAG, "XmitTo, invalid RemoteHostName: " + RemoteHost);
                rtc = ErrorCode.UnknownRemoteHost;
            } else {
                rtc = doXmit(toAddr, RemotePort, localPort, bytes);
            }
        }

        setErrorInfo(rtc); // An dieser Stelle, damit der StackTrace die richtige Methode/Action liefert.
        return rtc.ordinal();
    }

    // Sendet ein Datagramm an das im Designer eingestellte Ziel.
    // Ergebnis über Event AfterXmit.
    @SimpleFunction(description = "Send a datagram")
    public void XmitAsync(String Message) {
        byte[] bytes = null; // Daten
        ErrorCode rtc = ErrorCode.NoError;

        if (binaryMode) {
            try {
                bytes = Helpers.StringToBytes(Message);
            } catch (Exception e) {
                Log.e(LOG_TAG, "XmitAsync, cannot convert to Byte Array: " + Message);
                rtc = ErrorCode.BinaryConversionFailed;
            }
        } else {
            bytes = Message.getBytes();
        }

        if (rtc == ErrorCode.NoError) {
            if (remoteIP == null) {
                Log.e(LOG_TAG, "XmitAsync, no Remote IP for: " + remoteHost);
                rtc = ErrorCode.UnknownRemoteHost;
            } else {
                rtc = doXmit(remoteIP, remotePort, localPort, bytes);
            }
        }

        setErrorInfo(rtc); // An dieser Stelle, damit der StackTrace die richtige Methode/Action liefert.
        AfterXmit(rtc == ErrorCode.NoError, rtc.ordinal());
        return;
    }

    // Sendet ein Datagramm an das angegebene Ziel.
    // Ergebnis über Event AfterXmit.
    @SimpleFunction(description = "Send a datagram")
    public void XmitToAsync(String RemoteHost, int RemotePort, String Message) {
        byte[] bytes = null; // Daten
        ErrorCode rtc = ErrorCode.NoError;

        if (binaryMode) {
            try {
                bytes = Helpers.StringToBytes(Message);
            } catch (Exception e) {
                Log.e(LOG_TAG, "XmitToAsync, cannot convert to Byte Array: " + Message);
                rtc = ErrorCode.BinaryConversionFailed;
            }
        } else {
            bytes = Message.getBytes();
        }

        if (rtc == ErrorCode.NoError) {
            // Remote-IP-Adresse ermitteln
            InetAddress toAddr = Helpers.getIpFromHostName.doExecute(RemoteHost);
            if (toAddr == null) {
                Log.e(LOG_TAG, "XmitToAsync, invalid RemoteHostName: " + RemoteHost);
                rtc = ErrorCode.UnknownRemoteHost;
            } else {
                rtc = doXmit(toAddr, RemotePort, localPort, bytes);
            }
        }

        setErrorInfo(rtc); // An dieser Stelle, damit der StackTrace die richtige Methode/Action liefert.
        AfterXmit(rtc == ErrorCode.NoError, rtc.ordinal());
        return;
    }

    // Sendet ein Datagramm an das im Designer eingestellte Ziel.
    // Liefert einen Return-Code zur synchronen Auswertung.
    @SimpleFunction(description = "Send a datagram")
    public int XmitByteArray(Component ByteArray) {
        ErrorCode rtc = ErrorCode.NoError;

        if (!(ByteArray instanceof UrsAI2ByteArray)) {
            Log.e(LOG_TAG, "XmitByteArray, invalid Data Type");
            rtc = ErrorCode.InvalidDataType;
        } else {
            if (remoteIP == null) {
                Log.e(LOG_TAG, "XmitByteArray, no Remote IP for: " + remoteHost);
                rtc = ErrorCode.UnknownRemoteHost;
            } else {
                rtc = doXmit(remoteIP, remotePort, localPort, ((UrsAI2ByteArray) ByteArray).toByteArray());
            }
        }

        setErrorInfo(rtc); // An dieser Stelle, damit der StackTrace die richtige Methode/Action liefert.
        return rtc.ordinal();
    }

    // Sendet ein Datagramm an das angegebene Ziel.
    // Liefert einen Return-Code zur synchronen Auswertung.
    @SimpleFunction(description = "Send a datagram")
    public int XmitByteArrayTo(String RemoteHost, int RemotePort, Component ByteArray) {
        ErrorCode rtc = ErrorCode.NoError;

        if (!(ByteArray instanceof UrsAI2ByteArray)) {
            Log.e(LOG_TAG, "XmitByteArrayTo, invalid Data Type");
            rtc = ErrorCode.InvalidDataType;
        } else {
            // Remote-IP-Adresse ermitteln
            InetAddress toAddr = Helpers.getIpFromHostName.doExecute(RemoteHost);
            if (toAddr == null) {
                Log.e(LOG_TAG, "XmitByteArrayTo, invalid RemoteHostName: " + RemoteHost);
                rtc = ErrorCode.UnknownRemoteHost;
            } else {
                rtc = doXmit(toAddr, RemotePort, localPort, ((UrsAI2ByteArray) ByteArray).toByteArray());
            }
        }

        setErrorInfo(rtc); // An dieser Stelle, damit der StackTrace die richtige Methode/Action liefert.
        return rtc.ordinal();
    }

    // Sendet ein Datagramm an das im Designer eingestellte Ziel.
    // Ergebnis über Event AfterXmit.
    @SimpleFunction(description = "Send a datagram")
    public void XmitByteArrayAsync(Component ByteArray) {
        ErrorCode rtc = ErrorCode.NoError;

        if (!(ByteArray instanceof UrsAI2ByteArray)) {
            Log.e(LOG_TAG, "XmitByteArrayAsync, invalid Data Type");
            rtc = ErrorCode.InvalidDataType;
        } else {
            if (remoteIP == null) {
                Log.e(LOG_TAG, "XmitByteArrayAsync, no Remote IP for: " + remoteHost);
                rtc = ErrorCode.UnknownRemoteHost;
            } else {
                rtc = doXmit(remoteIP, remotePort, localPort, ((UrsAI2ByteArray) ByteArray).toByteArray());
            }
        }

        setErrorInfo(rtc); // An dieser Stelle, damit der StackTrace die richtige Methode/Action liefert.
        AfterXmit(rtc == ErrorCode.NoError, rtc.ordinal());
        return;
    }

    // Sendet ein Datagramm an das angegebene Ziel.
    // Ergebnis über Event AfterXmit.
    @SimpleFunction(description = "Send a datagram")
    public void XmitByteArrayToAsync(String RemoteHost, int RemotePort, Component ByteArray) {
        ErrorCode rtc = ErrorCode.NoError;

        if (!(ByteArray instanceof UrsAI2ByteArray)) {
            Log.e(LOG_TAG, "XmitbyXmitByteArrayToAsyncteArrayToAsync, invalid Data Type");
            rtc = ErrorCode.InvalidDataType;
        } else {
            // Remote-IP-Adresse ermitteln
            InetAddress toAddr = Helpers.getIpFromHostName.doExecute(RemoteHost);
            if (toAddr == null) {
                Log.e(LOG_TAG, "XmitByteArrayToAsync, invalid RemoteHostName: " + RemoteHost);
                rtc = ErrorCode.UnknownRemoteHost;
            } else {
                rtc = doXmit(toAddr, RemotePort, localPort, ((UrsAI2ByteArray) ByteArray).toByteArray());
            }
        }

        setErrorInfo(rtc); // An dieser Stelle, damit der StackTrace die richtige Methode/Action liefert.
        AfterXmit(rtc == ErrorCode.NoError, rtc.ordinal());
        return;
    }

    // Versendet die Daten
    public ErrorCode doXmit(InetAddress toAddr, int remotePort, int localPort, byte[] data) {
        Log.d(LOG_TAG, "doXmit");

        setLock();
        try { // finally nimmt den Lock zurück
              // DatagramPacket bereitstellen
              // ------------------------------------------------
            DatagramPacket dp;
            try {
                dp = new DatagramPacket(data, data.length, toAddr, remotePort);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Cannot build Datagram: " + e.toString());
                // Das Datagramm konnte nicht erstellt werden.
                return ErrorCode.InvalidRemotePort;
            }

            // DatagramSocket bereitstellen
            // ------------------------------------------------
            DatagramSocket sendSocket = null;
            try {
                sendSocket = new DatagramSocket((SocketAddress) null);
                sendSocket.setReuseAddress(true);
                sendSocket.bind(new InetSocketAddress(localPort));
            } catch (Exception e) {
                return ErrorCode.InvalidLocalPort;
            }

            // Datagram versenden
            // ------------------------------------------------

            Exception rtc = XmitTask.doExecute(sendSocket, dp);

            sendSocket.close();

            if (rtc != null) {
                Log.e(LOG_TAG, "Cannot send datagram: " + rtc.toString());
                return ErrorCode.XmitError;
            }

            return ErrorCode.NoError;
        } finally {
            resetLock();
        }
    } // Xmit

    UrsAsyncFunctionEx<Object, Exception> XmitTask = new UrsAsyncFunctionEx<Object, Exception>() {
        @Override
        protected Exception execute(Object... p) {
            DatagramSocket so = (DatagramSocket) p[0]; // Socket für den Versand
            DatagramPacket dp = (DatagramPacket) p[1]; // zu versendendes Datagramm
            try {
                so.send(dp);
            } catch (Exception ex) {
                return ex;
            }
            return null;
        }
    };
}