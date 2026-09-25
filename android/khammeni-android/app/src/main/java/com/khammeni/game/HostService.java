package com.khammeni.game;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import com.khammeni.game.server.GameServer;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** الخدمة الأمامية اللي بتشغّل سيرفر اللعبة جوه الموبايل */
public class HostService extends Service {

    public static volatile GameServer server;
    public static final AtomicBoolean running = new AtomicBoolean(false);

    private Thread beaconThread;

    public static boolean isRunning() {
        return running.get();
    }

    public static List<String> getIpsText() {
        return GameServer.getIps();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        Notification n = new Notification.Builder(this, "khammeni_server")
                .setContentTitle("🃏 كرت فكة بيستضيف دلوقتي")
                .setContentText("صحابك يوصلك تلقائيًا على نفس الشبكة")
                .setSmallIcon(android.R.drawable.ic_menu_share)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1, n);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (server == null) {
            new Thread(() -> {
                try {
                    GameServer gs = new GameServer(3000, path -> getAssets().open("www/" + path));
                    gs.start();
                    server = gs;
                    running.set(true);
                    startBeacon();
                } catch (Exception e) {
                    running.set(false);
                }
            }, "khammeni-server").start();
        }
        return START_NOT_STICKY;
    }

    private void startBeacon() {
        beaconThread = new Thread(() -> {
            byte[] payload = "KHM1".getBytes();
            while (running.get()) {
                try (java.net.DatagramSocket s = new java.net.DatagramSocket()) {
                    s.setBroadcast(true);
                    s.send(new java.net.DatagramPacket(payload, payload.length,
                            java.net.InetAddress.getByName("255.255.255.255"), 45999));
                } catch (Exception ignored) { }
                try { Thread.sleep(2000); } catch (InterruptedException e) { break; }
            }
        }, "khammeni-beacon");
        beaconThread.setDaemon(true);
        beaconThread.start();
    }

    @Override
    public void onDestroy() {
        running.set(false);
        GameServer srv = server;
        server = null;
        if (srv != null) {
            new Thread(srv::stop, "khammeni-stop").start();
        }
        if (beaconThread != null) beaconThread.interrupt();
        super.onDestroy();
    }

    private void createChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(
                "khammeni_server", "سيرفر كرت فكة", NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(ch);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}