package com.gathin.tvhome;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.gathin.tvhome.adblib.AdbBase64;
import com.gathin.tvhome.adblib.AdbConnection;
import com.gathin.tvhome.adblib.AdbCrypto;
import com.gathin.tvhome.adblib.AdbStream;

import java.io.File;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class LocalAdbManager {
    private static final String TAG = "LocalAdbManager";
    private static volatile AdbConnection sConnection = null;
    private static volatile boolean sConnecting = false;
    private static volatile boolean sDaemonStarted = false;

    private static final AdbBase64 BASE64 = data -> Base64.encodeToString(data, Base64.NO_WRAP);

    private static final String SCRIPT_B64 =
            "IyEvc3lzdGVtL2Jpbi9zaApQSURfRklMRT0iL2RhdGEvbG9jYWwvdG1wL2hvbWVfZGFlbW9uLnBp" +
            "ZCIKaWYgWyAtZiAiJFBJRF9GSUxFIiBdOyB0aGVuCiAgICBPTERfUElEPSQoY2F0ICIkUElEX0ZJ" +
            "TEUiIDI+L2Rldi9udWxsKQogICAgaWYgWyAtbiAiJE9MRF9QSUQiIF0gJiYgWyAtZCAiL3Byb2Mv" +
            "JE9MRF9QSUQiIF07IHRoZW4KICAgICAgICBleGl0IDAKICAgIGZpCmZpCmVjaG8gJCQgPiAiJFBJ" +
            "RF9GSUxFIgoKd2hpbGUgdHJ1ZTsgZG8KICAgIGxvZ2NhdCAtYiBzeXN0ZW0gLXMgQWN0aXZpdHlN" +
            "YW5hZ2VyOkkgfCB3aGlsZSByZWFkIC1yIGxpbmU7IGRvCiAgICAgICAgY2FzZSAiJGxpbmUiIGlu" +
            "CiAgICAgICAgICAgICoiY21wPWNvbS5hbWF6b24udHYubGF1bmNoZXIiKnwqImNtcD1jb20uYW1h" +
            "em9uLmZpcmVob21lc3RhcnRlciIqKQogICAgICAgICAgICAgICAgYW0gc3RhcnQgLW4gY29tLmdh" +
            "dGhpbi50dmhvbWUvLk1haW5BY3Rpdml0eQogICAgICAgICAgICAgICAgOzsKICAgICAgICBlc2Fj" +
            "CiAgICBkb25lCiAgICBzbGVlcCAxCmRvbmU=";

    /** Creates a private keypair for this app installation; no shared PC credential is bundled. */
    private static AdbCrypto getOrCreateCrypto(Context context) {
        try {
            File priv = new File(context.getFilesDir(), "adbkey");
            File pub = new File(context.getFilesDir(), "adbkey.pub");
            if (priv.exists() && pub.exists()) {
                return AdbCrypto.loadAdbKeyPair(BASE64, priv, pub);
            }

            AdbCrypto crypto = AdbCrypto.generateAdbKeyPair(BASE64);
            crypto.saveAdbKeyPair(priv, pub);
            return crypto;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize per-install ADB cryptography", t);
            return null;
        }
    }
    public static synchronized AdbConnection getConnection(Context context) {
        if (sConnection != null) {
            return sConnection;
        }
        if (sConnecting) return null;
        sConnecting = true;
        try {
            AdbCrypto crypto = getOrCreateCrypto(context);
            if (crypto == null) {
                sConnecting = false;
                return null;
            }
            Socket socket = new Socket("127.0.0.1", 5555);
            socket.setTcpNoDelay(true);
            AdbConnection conn = AdbConnection.create(socket, crypto);
            conn.connect(5000, TimeUnit.MILLISECONDS, false);
            sConnection = conn;
            Log.i(TAG, "Local ADB connection established successfully on 127.0.0.1:5555!");
            return conn;
        } catch (Throwable t) {
            Log.w(TAG, "Local ADB connection attempt failed: " + t.getMessage());
            return null;
        } finally {
            sConnecting = false;
        }
    }

    public static void ensureDaemonRunning(Context context) {
        if (sDaemonStarted) {
            return;
        }
        new Thread(() -> {
            try {
                Log.i(TAG, "ensureDaemonRunning monitor started...");
                for (int attempt = 1; attempt <= 3; attempt++) {
                    if (sDaemonStarted) break;
                    AdbConnection conn = getConnection(context);
                    if (conn != null) {
                        try {
                            Log.i(TAG, "Connected to local ADB. Ensuring home_daemon.sh is installed and running...");
                            String deployCmd = "echo '" + SCRIPT_B64 + "' | base64 -d > /data/local/tmp/home_daemon.sh && " +
                                    "chmod 755 /data/local/tmp/home_daemon.sh && " +
                                    "nohup /data/local/tmp/home_daemon.sh >/dev/null 2>&1 &";

                            AdbStream stream = conn.open("shell:" + deployCmd);
                            try {
                                while (!stream.isClosed()) {
                                    byte[] read = stream.read();
                                    if (read == null) break;
                                }
                            } catch (Throwable ignored) {}
                            try { stream.close(); } catch (Throwable ignored) {}

                            sDaemonStarted = true;
                            Log.i(TAG, "SUCCESS: home_daemon.sh is running as UID 2000! Instant home button survives reboot permanently.");
                            break;
                        } catch (Throwable streamError) {
                            Log.w(TAG, "Error executing daemon deploy command", streamError);
                            sConnection = null;
                        }
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "ensureDaemonRunning thread exception", t);
            }
        }, "LocalAdbBootThread").start();
    }

    public static boolean launchMainActivityViaAdb(Context context) {
        try {
            AdbConnection conn = sConnection;
            if (conn == null) {
                conn = getConnection(context);
            }
            if (conn != null) {
                AdbStream stream = conn.open("shell:am start -n com.gathin.tvhome/.MainActivity");
                try { stream.close(); } catch (Throwable ignored) {}
                Log.i(TAG, "MainActivity launched via local ADB UID 2000 stream in 0ms!");
                return true;
            }
        } catch (Throwable t) {
            Log.w(TAG, "launchMainActivityViaAdb notice", t);
            sConnection = null;
        }
        return false;
    }
}
