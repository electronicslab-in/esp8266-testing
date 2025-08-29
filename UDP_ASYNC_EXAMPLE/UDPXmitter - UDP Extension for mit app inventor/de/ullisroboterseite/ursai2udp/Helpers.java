package de.ullisroboterseite.ursai2udp;

import java.util.*;
import java.net.*;

public class Helpers {
    static final String LOG_TAG = UDPBase.LOG_TAG;

    // #region Ermittlung der Host-Adresse ========================
    private static UrsAsyncFunction<InetAddress> getLocalHostTask = new UrsAsyncFunction<InetAddress>() {
        @Override
        protected InetAddress execute() {
            DatagramSocket socket = null;
            InetAddress localHost = null;
            try {
                localHost = InetAddress.getByName("0.0.0.0");
                socket = new DatagramSocket();
                // IP-Adresse und Port sind eigentlich beliebig. Es darf nur keine
                // Spezial-Adresse sein.
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                localHost = socket.getLocalAddress();
                socket.close();
                socket = null;
            } catch (Exception e) {
                // nichts zu tun
            }
            return localHost;
        }
    };

    public static String getLocalHostString() {
        InetAddress localHost = getLocalHostTask.doExecute();
        if (localHost.isAnyLocalAddress())
            return "";
        else
            return localHost.getHostAddress();

    }

    public static InetAddress getLocalHost() {
        return getLocalHostTask.doExecute();
    }
    // #endregion

    // #regionErmittlung aller NICs ===============================
    static UrsAsyncFunction<List<InetAddress>> getNICs = new UrsAsyncFunction<List<InetAddress>>() {
        @Override
        protected List<InetAddress> execute() {
            ArrayList<InetAddress> ipList = new ArrayList<InetAddress>();
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface iface = interfaces.nextElement();
                    if (iface.isLoopback() || !iface.isUp()) // filters out 127.0.0.1 and inactive interfaces
                        continue;

                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof Inet4Address) {
                            ipList.add(addr);
                        }
                    }
                }
            } catch (SocketException e) {
            }
            return ipList;
        }
    };

    public static List<InetAddress> getNICList() {
        return getNICs.doExecute();
    }

    public static List<String> getNICNames() {
        List<InetAddress> li = getNICs.doExecute();
        List<String> ls = new ArrayList<String>();
        for (InetAddress ip : li) {
            ls.add(ip.getHostAddress());
        }
        return ls;
    }
    // #endregion

    // Prüft, ob die IP des Pakets mit einer IP eines Network-Interfaces übereinstimmt.
    public static boolean isLocalNicIP(InetAddress packetIP) {
        for (InetAddress ip : getNICList())
            if (ip.equals(packetIP))
                return true;
        return false;
    }


    // #region getIpFromHostName ==================================
    // Ermittelt die IP-Adresse aus einem Host-Namen.
    // Muss in separatem Task ausgeführt werden ()
    public static UrsAsyncFunctionEx<String, InetAddress> getIpFromHostName = new UrsAsyncFunctionEx<String, InetAddress>() {
        @Override
        protected InetAddress execute(String... p) {
            try {
                return InetAddress.getByName((String) p[0]);
            } catch (Exception ex) {
                return null;
            }
        }
    };
    // #endregion

    // #region Helper Methods =====================================
    // Konvertiert einen Komma/Semikolon-separierten String mit Zahlenwerten
    // in ein Byte-Array.
    // Wirft NumberFormatException, wenn der String nicht konvertiert werden konnte.
    public static byte[] StringToBytes(String inp) throws NumberFormatException {
        inp = inp.replace(',', ';');
        String[] bs = inp.split(";");
        byte[] bytes = new byte[bs.length];

        for (int i = 0; i < bs.length; i++) {
            int b = Integer.decode(bs[i].trim());
            if (b > 255 || b < 0)
                throw new NumberFormatException();
            bytes[i] = (byte) b;
        }
        return bytes;
    }
    // #endregion
}