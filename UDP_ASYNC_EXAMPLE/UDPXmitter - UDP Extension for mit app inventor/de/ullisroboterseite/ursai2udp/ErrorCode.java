package de.ullisroboterseite.ursai2udp;

public enum ErrorCode {
    NoError(0, ""), //
    UnknownRemoteHost(1, "Unknown remote host"), // Xmit
    InvalidLocalPort(2, "Invalid local port"), // Xmit, Port in Benutzung?
    InvalidRemotePort(3, "Invalid remote port"), // Xmit
    XmitError(4, "Xmit failed"), // Xmit
    BinaryConversionFailed(5, "Binary conversion failed"), // Xmit
    ListenerThreadAborted(6, "Listener thread aborted"), // Listener
    ListenerAlreadyRunning(7, "Listener already running"), // Listener
    InvalidDataType(8, "Invalid data type."), // Xmit
    ;

    public final String errorText;
    public final int errorCode;

    ErrorCode(int code, String text) {
        this.errorCode = code;
        errorText = text;
    }

    @Override
    public String toString() {
        return errorText;
    }
}