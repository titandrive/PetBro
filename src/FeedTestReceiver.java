package local.petlibrocompanion;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.SystemClock;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.TimeZone;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class FeedTestReceiver extends BroadcastReceiver {
    private static final String KEY = "feed-test";
    private static final int BASE64 = Base64.NO_WRAP;

    static String save(Context context, String token, String feeder) throws Exception {
        String id = UUID.randomUUID().toString();
        boolean saved = encryptToken(prefs(context).edit(), token)
                .putString("feeder", feeder)
                .putString("id", id)
                .putLong("expires", SystemClock.elapsedRealtime() + 180000)
                .putString("outcome", "Test armed. Waiting for the alarm.")
                .commit();
        if (!saved) {
            throw new IllegalStateException("Could not save test");
        }
        return id;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences state = prefs(context);
        String id = intent.getStringExtra("testId");
        if (id == null || !id.equals(state.getString("id", null))) {
            return;
        }
        String encrypted = state.getString("token", "");
        String iv = state.getString("iv", "");
        String feeder = state.getString("feeder", "");
        boolean expired = SystemClock.elapsedRealtime() > state.getLong("expires", 0);
        String initial = expired ? "The test alarm arrived too late. No request was sent."
                : "Test started. If the outcome stays unknown, check the bowl before trying again.";
        if (!state.edit().clear().putString("outcome", initial).commit() || expired) {
            return;
        }

        boolean locked = context.getSystemService(KeyguardManager.class).isDeviceLocked();
        boolean interactive = context.getSystemService(PowerManager.class).isInteractive();
        String delivery = " At alarm delivery, the phone was " + (locked ? "locked" : "unlocked")
                + " and the screen was " + (interactive ? "interactive." : "non-interactive.");
        PendingResult pending = goAsync();
        new Thread(() -> {
            String outcome;
            try {
                String token = decryptToken(encrypted, iv);
                JSONObject payload = new JSONObject().put("deviceSn", feeder).put("grainNum", 1)
                        .put("requestId", id.replace("-", ""));
                MainActivity.request("/device/device/manualFeeding", payload, token, TimeZone.getDefault().getID());
                outcome = "Petlibro accepted the test request. Check the bowl or feeding log for one portion.";
            } catch (Exception e) {
                outcome = "The test could not confirm feeding. Check the bowl or feeding log before trying again.";
            }
            try {
                if (!state.contains("id")) {
                    state.edit().putString("outcome", outcome + delivery).commit();
                }
            } finally {
                pending.finish();
            }
        }, "petlibro-feed-test").start();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
    }

    static SharedPreferences.Editor encryptToken(SharedPreferences.Editor editor, String token) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
        return editor.putString("token", Base64.encodeToString(encrypted, BASE64))
                .putString("iv", Base64.encodeToString(cipher.getIV(), BASE64));
    }

    static String decryptToken(String encrypted, String iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(iv, BASE64)));
        return new String(cipher.doFinal(Base64.decode(encrypted, BASE64)), StandardCharsets.UTF_8);
    }

    private static synchronized SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey existing = (SecretKey) store.getKey(KEY, null);
        if (existing != null) {
            return existing;
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }
}
