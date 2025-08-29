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

import de.ullisroboterseite.ursai2bytearray.UrsAI2ByteArray;

// Diese Klasse ist Basis-Klasse für Listener.
// Sie implementiert die Properties und Events.

@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET,android.permission.ACCESS_NETWORK_STATE")
public class UDPListenerBase extends UDPBase {
    final UDPListenerBase thisInstance = this;
    public boolean dropOwnBroadcast = true; // Datagramme von der eigenen IP sollen unterdrückt werden

    UDPListenerThread rcvTask = null;

    Object receivingByteArrayComponent = null;

    public UDPListenerBase(ComponentContainer container) {
        super(container);
    } // ctor{

    @SimpleProperty(description = "true: (Broadcast) Messages sent to yourself are dropped.")
    public boolean DropSentToYourself() {
        return dropOwnBroadcast;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "true: (Broadcast) sent to yourself are dropped.")
    public void DropSentToYourself(boolean value) {
        dropOwnBroadcast = value;
        if (rcvTask != null)
            rcvTask.dropOwnBroadcast = value;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COMPONENT, defaultValue = "")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "ByteArray component to receive data.")
    public void ReceivingByteArray(Component ByteArray) {
        if (!(ByteArray instanceof UrsAI2ByteArray)) {
            receivingByteArrayComponent = null;
            form.ErrorOccurred(this, "ReceivingByteArray", 17333, "Wrong type for parameter ByteArray");
            return;
        }

        receivingByteArrayComponent = (UrsAI2ByteArray) ByteArray;
    }

    @SimpleFunction(description = "Removes the receiving ByteArray component.")
    public void RemoveReceivingByteArray() {
        receivingByteArrayComponent = null;
    }

    @SimpleProperty(description = "true: UDP server is running.")
    public boolean IsRunning() {
        return rcvTask != null;
    }

    void dataReceived(String stringResult, String RemoteIP, int RemotePort, DatagramPacket packet) {
        handler.post(new Dispatcher(stringResult, RemoteIP, RemotePort, packet));
    }

    class Dispatcher implements Runnable {
        final String stringResult;
        final String remoteIP;
        final int remotePort;
        final DatagramPacket packet;

        Dispatcher(String stringResult, String remoteIP, int remotePort, DatagramPacket packet) {
            this.stringResult = stringResult;
            this.remoteIP = remoteIP;
            this.remotePort = remotePort;
            this.packet = packet;
        }

        public void run() {
            if (receivingByteArrayComponent != null) {
                UrsAI2ByteArray ba = (UrsAI2ByteArray) receivingByteArrayComponent;
                ba.Clear();
                byte[] bytes = packet.getData();
                for (int i = 0; i < packet.getLength(); i++)
                    ba.bytes.add(bytes[i]);
            }
            EventDispatcher.dispatchEvent(thisInstance, "DataReceived", stringResult, remoteIP, remotePort);
        }
    }

    @SimpleEvent(description = "A datagram has been received.")
    public void DataReceived(String Data, String RemoteIP, int RemotePort) {
        // EventDispatcher.dispatchEvent(this, "DataReceived", Data, RemoteIP, RemotePort);
        // Nur Definition des Events
    }

    @SimpleEvent(description = "The UDP listening server has stopped with failure.")
    public void ListenerFailure(final int ErrorCode) {
        // Damit die Methode evtl. zu Ende ausgeführt werden kann, bevor das Ereignis
        // ausglöst wird.
        final UDPListenerBase thisExtension = this;
        handler.post(new Runnable() {
            public void run() {
                EventDispatcher.dispatchEvent(thisExtension, "ListenerFailure", ErrorCode);
            } // run
        }); // post
    }
}