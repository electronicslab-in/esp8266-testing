package de.ullisroboterseite.ursai2udp;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.*;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Handler;
import android.util.Log;

// Diese Klasse ist Basis-Klasse für Xmitter und Listener.
// Sie verwaltet die benutzten Listener-Sockets.

@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET,android.permission.ACCESS_NETWORK_STATE")
public class UDPBase extends AndroidNonvisibleComponent {
    static final String LOG_TAG = "UDP";
    final Handler handler = new Handler();

    // Zur Synchronisation der Zugriffe auf die Sockets zwischen verschiedenen Extensions
    public static ReentrantLock myLock = new ReentrantLock();

    public int lockCount = 0;

    public void setLock() {
        myLock.lock();
        lockCount++;
        Log.d(LOG_TAG, "+ Lock set: " + lockCount);
    }

    public void resetLock() {
        myLock.unlock();
        lockCount--;
        Log.d(LOG_TAG, "- Lock reset: " + lockCount);

    }

    public boolean binaryMode = false; // Die Nachrichten enthalten Code mit Byte-Daten

    // Liste der Listener-Sockets.
    // Zur Wiederverwendung des benutzten Local Port zum Senden.
    public static List<DatagramSocket> listenerSocketsInUse = new ArrayList<DatagramSocket>();

    public UDPBase(ComponentContainer container) {
        super(container.$form());
        Log.d(LOG_TAG, "-------------------");
    } // ctor

    @SimpleProperty(description = "Returns the IP address of the local host. An empty string if not connected to a network.")
    public String LocalHost() {
        return Helpers.getLocalHostString();
    }

    @SimpleProperty(description = "Returns the IPv4 addresses of all known network interfaces.")
    public YailList NICList() {
        return YailList.makeList(Helpers.getNICNames());
    }

    @SimpleProperty(description = "true: Binary data expected.")
    public boolean BinaryMode() {
        return binaryMode;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "true: Binary data expected.")
    public void BinaryMode(boolean value) {
        binaryMode = value;
    }

    // ----- LastError ------------------------------------------------
    public volatile String lastErrMsg = ""; // Text des letzten Fehlers
    public volatile int lastErrorCode = 0; // Fehlercode des letzten Fehlers
    public volatile String lastAction = ""; // Name der Methode, in der der Fehler aufgetreten ist.

    @SimpleProperty(description = "Returns a text message about the last error.")
    public String LastErrorMessage() {
        return lastErrMsg;
    }

    @SimpleProperty(description = "Returns the code of the last error.")
    public int LastErrorCode() {
        return lastErrorCode;
    }

    @SimpleProperty(description = "Returns the last Action the error code belongs to.")
    public String LastAction() {
        return lastAction;
    }

    // Speichert die Fehler-Informationen und löst das Ereignis ErrorOccurred aus.
    public void setErrorInfo(ErrorCode errorCode) {
        lastAction = new Throwable().getStackTrace()[1].getMethodName();
        lastErrorCode = errorCode.ordinal();
        lastErrMsg = errorCode.toString();
        if (errorCode != ErrorCode.NoError)
            Log.e(LOG_TAG, "ErrorOccurred Action: " + lastAction //
                    + "\nErrorCode: " + lastErrorCode + " ErrorMsg: " + lastErrMsg);
    }

    public void rsetErrorInfo(ErrorCode errorCode, String debugInfo) {
        lastAction = new Throwable().getStackTrace()[1].getMethodName();
        lastErrorCode = errorCode.ordinal();
        lastErrMsg = errorCode.toString();
        Log.e(LOG_TAG, "ErrorOccurred" + "\nAction: " + lastAction //
                + "\nErrorCode: " + lastErrorCode + "\nErrorMsg: " + lastErrMsg + "\nInfo: " + debugInfo);
    }

    // Löscht die Fehlerinformationen
    public void resetErrorInfo() {
        setErrorInfo(ErrorCode.NoError);
    }

}