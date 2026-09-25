package com.khammeni.game;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** اكتشاف المستضيفين القريبين عن طريق بث UDP */
public final class Discovery {

    private Discovery() { }

    public static List<String> scan(int millis, Context ctx) {
        Set<String> found = new HashSet<>();
        DatagramSocket socket = null;
        WifiManager.MulticastLock lock = null;

        try {
            WifiManager wifi = (WifiManager) ctx.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                lock = wifi.createMulticastLock("khammeni-discovery");
                lock.setReferenceCounted(false);
                lock.acquire();
            }

            socket = new DatagramSocket(45999);
            socket.setReuseAddress(true);
            socket.setSoTimeout(400);

            byte[] buf = new byte[64];
            long deadline = System.currentTimeMillis() + millis;
            while (System.currentTimeMillis() < deadline) {
                try {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);
                    String data = new String(p.getData(), 0, p.getLength(), "UTF-8");
                    if (data.startsWith("KHM")) {
                        InetAddress a = p.getAddress();
                        found.add(a.getHostAddress());
                    }
                } catch (java.net.SocketTimeoutException ignored) { }
                catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
        finally {
            if (socket != null) socket.close();
            if (lock != null && lock.isHeld()) lock.release();
        }
        return new ArrayList<>(found);
    }
}