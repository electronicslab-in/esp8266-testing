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

// Diese Klasse ist Basis-Klasse für Xmitter.
// Sie implementiert die Properties und Events.

@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET,android.permission.ACCESS_NETWORK_STATE")
public class UDPXmitterBase extends UDPBase {

    public UDPXmitterBase(ComponentContainer container) {
        super(container);
    } // ctor

    public String remoteHost = ""; // Übergebener Hostname
    public InetAddress remoteIP = null; // Ermittelte Adresse; null, wenn nicht ermittlebar
    public int remotePort = 0;
    public int localPort = 0;

    @SimpleProperty(description = "Name or IP address of remote host.")
    public String RemoteHost() {
        return remoteHost;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Name or IP address of remote host.")
    public void RemoteHost(String value) {
        remoteHost = value;
        remoteIP = Helpers.getIpFromHostName.doExecute(remoteHost);
    }

    @SimpleProperty(description = "Name or IP address of remote host.")
    public String RemoteIP() {
        if (remoteIP != null)
            return remoteIP.getHostAddress();
        else
            return "";
    }

    @SimpleProperty(description = "Remote port number.")
    public int RemotePort() {
        return remotePort;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Remote port number.")
    public void RemotePort(int value) {
        remotePort = value;
    }

    @SimpleProperty(description = "Default local port number.")
    public int LocalPort() {
        return localPort;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Default local port number.")
    public void LocalPort(int value) {
        localPort = value;
    }

    // Ereignis
    @SimpleEvent(description = "Raised after transfer of a UDP packet.")
    public void AfterXmit(boolean Success, int ErrorCode) {
        EventDispatcher.dispatchEvent(this, "AfterXmit", Success, ErrorCode);
        if(!Success){
            XmitFailure(ErrorCode);
        }
    }

    @SimpleEvent(description = "Raised if transfer fails.")
    public void XmitFailure(int ErrorCode) {
        EventDispatcher.dispatchEvent(this, "XmitFailure", ErrorCode);
    }
}