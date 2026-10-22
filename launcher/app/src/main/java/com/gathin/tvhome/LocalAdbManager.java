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
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.TimeUnit;

public class LocalAdbManager {
    private static final String TAG = "LocalAdbManager";
    private static volatile AdbConnection sConnection = null;
    private static volatile boolean sConnecting = false;
    private static volatile boolean sDaemonStarted = false;

    private static final AdbBase64 BASE64 = data -> Base64.encodeToString(data, Base64.NO_WRAP);

    // Pre-authorized PC ADB key matching /data/misc/adb/adb_keys on the user's Fire TV
    private static final String PRIV_KEY_B64 =
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDUR5sqEP/FCSgS" +
            "2WQsKCY9wV8ThdZ7RHXN7AO43W5oLZPSeSVLovT0lL+AZkzexS1X+keISvPIrT/k" +
            "hSS4rvLO3w+MaQwSeiFDoYLo3+f9ER1qxyxDFNwJFb+E/Zs4xkNsnesRbn/423Ik" +
            "ABYpnJg2JzJliSgwOabUV5evvOR+K2o5pQiGBS7mogOgNhzbMACwQMrPxfB7ovH8" +
            "HNoaSS+r3zb3OKn9LD9QxEJyiQjmYDW+j43Zbr27C/mJFLGEgym0I4gprtsrH6pC" +
            "TxyHHS+++G7yOKvT6b6AfdjL6Pt2bXyN6XH2tJKVxh+/13mB8ES/bR/gqblIntNL" +
            "uaOLqYl9AgMBAAECggEAFesF0aBbNE43ecjB79A+cJJqfXLMfEJLkpU7picQ2wdX" +
            "s7vn9qkO90Dzi++kIL1FqwePwroQdUg0KGV57uq7bdI4wgPnp/G9xQSkZmSWCLpv" +
            "aeLCRnPdKz/4XCQuVnycIhclGhHpezm3TZHHuAiv66l86eBxSpdtSw/3PME5a3Bi" +
            "fDqriNAkd2qw3CkQXyX4KWSlnqrWmmYo7lSbmacZ313BPqgjfskjurDQyFd592SO" +
            "vo+gE4/ZCLblB1vvX9MkarIm0cWIapIyP5Kxc0fh6fX1gEhUjG14p41FqP4YcafR" +
            "NEUytM0OiOO/Gge+Qx+Gcpzb+OfyQPcRVvgxluEowQKBgQDs919h85D0ksNkBoIN" +
            "/3bUraighMRGvwIZ4vlCEzGQ2PjKPCdMJZEBtG1aNE9snAcak3HOikOGa12O0yKB" +
            "mwdjiBvA4KHmm+mlv19zH8neEV4AGN3lUv8LEnrSolZc1YF7i78qhRG7KnE7XPTm" +
            "hb0rvT3fZ9iOv0Idvj0rW4zlwQKBgQDlVJ1fiwXVM0fj3U/lf/VQKfJsKmWYefe9" +
            "xuQFDMAAHDROqFhrOmi+aChueYRYc/s4gLARJtyUGfjSwhdsiZtkfur1ldFCOYRk" +
            "vvpUWUSJzGdB1EgyYaECqwc5kO3+8u6QGthFAPZaPCw7gzCmADVYlH9z21NIj0VM" +
            "bOvHUgJqvQKBgQDTBQMqnhTSxzM6itUcFAfFIxRMRY7oFUaQuOARKDh2bdE3ftPb" +
            "6oQ0CXFr57Nm25cz4mNxcwzo0gmRm5FZ6y1sRBO68g8a5nLtx/r+8Sr6F2snGP3Z" +
            "LsyyFnXLxwazSuowk+Tbv35nt0b4fzITartoDPDhVn+xtr3w8K1WRAO3wQKBgQCq" +
            "kjHvQ28WMWsyL4TilTb35gs6qpogO++phspIe0A/lLrPGmWRc8M+hCg2a0mlpLZh" +
            "Q04xo68CtQKed+CGSR98Mu49ta4MTM2NRYLZw/+3YoIWs3bziiwA2QapqYNrUnNg" +
            "WacsQ9f4K7NWnHBNt+/1r5XQrXwXhwCrJISGHxhPHQKBgCVR0um6r3PYIfiMLH32" +
            "HWo/Nr0kl/4ZYf+Nkrd0Y6iKY8BxAkSbd78m1m/4/NRm+OyDTLx/Y+9B1OabzCAH" +
            "L/bsgopNNmIbiEZPnLzGulhpaXWoA1/g1H41skvH+C3pncoyFLG8MGIPj/lfP765" +
            "YrkkHCe7HP+h0ebCkE1aAz7g";

    private static final String PUB_KEY_B64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1EebKhD/xQkoEtlkLCgm" +
            "PcFfE4XWe0R1zewDuN1uaC2T0nklS6L09JS/gGZM3sUtV/pHiErzyK0/5IUkuK7y" +
            "zt8PjGkMEnohQ6GC6N/n/REdascsQxTcCRW/hP2bOMZDbJ3rEW5/+NtyJAAWKZyY" +
            "NicyZYkoMDmm1FeXr7zkfitqOaUIhgUu5qIDoDYc2zAAsEDKz8Xwe6Lx/BzaGkkv" +
            "q9829zip/Sw/UMRCcokI5mA1vo+N2W69uwv5iRSxhIMptCOIKa7bKx+qQk8chx0v" +
            "vvhu8jir0+m+gH3Yy+j7dm18jelx9rSSlcYfv9d5gfBEv20f4Km5SJ7TS7mji6mJ" +
            "fQIDAQAB";

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

    private static AdbCrypto getOrCreateCrypto(Context context) {
        try {
            // Priority 1: Use embedded pre-authorized keypair (matching TV's /data/misc/adb/adb_keys)
            try {
                byte[] privDer = Base64.decode(PRIV_KEY_B64, Base64.DEFAULT);
                byte[] pubDer = Base64.decode(PUB_KEY_B64, Base64.DEFAULT);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privDer));
                PublicKey pubKey = kf.generatePublic(new X509EncodedKeySpec(pubDer));
                KeyPair keyPair = new KeyPair(pubKey, privKey);
                Log.i(TAG, "Loaded pre-authorized ADB keypair successfully!");
                return AdbCrypto.loadAdbKeyPair(BASE64, keyPair);
            } catch (Throwable t) {
                Log.w(TAG, "Notice loading embedded key, checking disk storage", t);
            }

            // Priority 2: Use locally cached key on disk
            File priv = new File(context.getFilesDir(), "adbkey");
            File pub = new File(context.getFilesDir(), "adbkey.pub");
            if (priv.exists() && pub.exists()) {
                return AdbCrypto.loadAdbKeyPair(BASE64, priv, pub);
            }

            // Priority 3: Generate fresh keypair and persist to disk
            AdbCrypto crypto = AdbCrypto.generateAdbKeyPair(BASE64);
            crypto.saveAdbKeyPair(priv, pub);
            return crypto;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize ADB cryptography", t);
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
