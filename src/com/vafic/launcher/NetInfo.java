package com.vafic.launcher;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

/** Snapshot of the active network: type, signal level and IPv4 address. */
public final class NetInfo {

    public static final int WIFI = 0;
    public static final int ETHERNET = 1;
    public static final int OFFLINE = 2;

    public static class State {
        public final int mode;
        public final String label;
        public final String ssid;
        public final int level; // 0..3
        public final int rssi;
        public final String ip;

        State(int mode, String label, String ssid, int level, int rssi, String ip) {
            this.mode = mode;
            this.label = label;
            this.ssid = ssid;
            this.level = level;
            this.rssi = rssi;
            this.ip = ip;
        }

        public int percent() {
            return Math.round(level * 100f / 3f);
        }
    }

    private NetInfo() {
    }

    public static State read(Context c) {
        int mode = OFFLINE;
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo();
            if (ni != null && ni.isConnected()) {
                mode = ni.getType() == ConnectivityManager.TYPE_WIFI ? WIFI : ETHERNET;
            }
        } catch (Exception ignored) {
        }

        String ssid = null;
        int level = 0;
        int rssi = 0;

        if (mode == WIFI) {
            try {
                WifiManager wm = (WifiManager) c.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
                WifiInfo wi = wm == null ? null : wm.getConnectionInfo();
                if (wi != null) {
                    rssi = wi.getRssi();
                    if (rssi > -120 && rssi < 0) {
                        level = WifiManager.calculateSignalLevel(rssi, 4);
                    } else {
                        level = 3;
                    }
                    String s = wi.getSSID();
                    if (s != null) {
                        s = s.replace("\"", "").trim();
                        if (s.length() > 0 && !s.equals("<unknown ssid>") && !s.equals("0x")) {
                            ssid = s;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        } else if (mode == ETHERNET) {
            level = 3;
        }

        String label = mode == WIFI ? "Wi-Fi" : mode == ETHERNET ? "Ethernet" : "Offline";
        return new State(mode, label, ssid, level, rssi, ipv4());
    }

    private static String ipv4() {
        try {
            List<NetworkInterface> ifaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : ifaces) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && !a.isLoopbackAddress() && !a.isLinkLocalAddress()) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
