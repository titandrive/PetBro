package local.petlibrocompanion;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Settings;
import android.os.Bundle;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

public final class MainActivity extends Activity {
    private EditText email;
    private EditText password;
    private TextView result;
    private Button check;
    private Button test;
    private Button forget;
    private TextView testResult;
    private SharedPreferences savedSession;
    private String token;
    private String feeder;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(padding, padding, padding, padding);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(form);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("Check your Petlibro feeder");
        title.setTextSize(24);
        form.addView(title);

        TextView instructions = new TextView(this);
        instructions.setText("Sign in with a US Petlibro account that has access to the feeder. Use a separate account with shared access to keep your main app session signed in.");
        form.addView(instructions);

        email = new EditText(this);
        email.setHint("Petlibro email");
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        email.setSaveEnabled(false);
        form.addView(email);

        password = new EditText(this);
        password.setHint("Petlibro password");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setSaveEnabled(false);
        form.addView(password);

        check = new Button(this);
        check.setText("Sign in and list devices");
        check.setOnClickListener(view -> checkAccount());
        form.addView(check);

        savedSession = getSharedPreferences("petlibro-session", MODE_PRIVATE);
        forget = new Button(this);
        forget.setText("Forget saved sign-in");
        forget.setOnClickListener(view -> forgetSession());
        form.addView(forget);

        result = new TextView(this);
        result.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
        form.addView(result);

        test = new Button(this);
        test.setText("Test one portion in 10 seconds");
        test.setEnabled(false);
        test.setOnClickListener(view -> armTest());
        form.addView(test);

        testResult = new TextView(this);
        testResult.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
        form.addView(testResult);
        forget.setEnabled(savedSession.contains("token"));
        if (savedSession.contains("token")) {
            checkAccount(null, null);
        }
    }

    private void checkAccount() {
        String address = email.getText().toString().trim();
        String secret = password.getText().toString();
        if (address.isEmpty() || secret.isEmpty()) {
            result.setText("Enter your Petlibro email and password.");
            return;
        }

        password.setText("");
        checkAccount(address, secret);
    }

    private void checkAccount(String address, String secret) {
        check.setEnabled(false);
        forget.setEnabled(false);
        test.setEnabled(false);
        token = null;
        feeder = null;
        result.setText(secret == null ? "Restoring Petlibro sign-in…" : "Checking Petlibro…");
        new Thread(() -> {
            String message;
            String session = null;
            String serial = null;
            try {
                if (secret == null) {
                    try {
                        session = FeedTestReceiver.decryptToken(savedSession.getString("token", ""),
                                savedSession.getString("iv", ""));
                    } catch (Exception e) {
                        savedSession.edit().clear().apply();
                        throw new IOException("Could not restore saved sign-in. Please sign in again.");
                    }
                } else {
                    session = login(address, secret);
                }
                JSONArray devices = (JSONArray) request("/device/device/list", new JSONObject(),
                        session, TimeZone.getDefault().getID());
                message = describeDevices(devices);
                if (secret != null) {
                    try {
                        if (!FeedTestReceiver.encryptToken(savedSession.edit(), session).commit()) {
                            throw new IOException("Session storage failed.");
                        }
                    } catch (Exception e) {
                        message += "\n\nThis sign-in could not be saved.";
                    }
                }
                for (int i = 0; i < devices.length(); i++) {
                    JSONObject device = devices.getJSONObject(i);
                    if ("PLAF301".equals(device.optString("productIdentifier"))) {
                        if (serial != null) {
                            serial = null;
                            break;
                        }
                        serial = device.optString("deviceSn", null);
                    }
                }
            } catch (SessionExpired e) {
                if (secret == null) {
                    savedSession.edit().clear().apply();
                }
                message = "Petlibro ended this session. Please sign in again.";
            } catch (IOException e) {
                message = e.getMessage();
            } catch (Exception e) {
                message = "Petlibro returned an unexpected response.";
            }
            String outcome = message;
            String checkedToken = session;
            String checkedFeeder = serial;
            runOnUiThread(() -> {
                if (!isDestroyed()) {
                    result.setText(outcome);
                    check.setEnabled(true);
                    forget.setEnabled(savedSession.contains("token") || checkedToken != null);
                    token = checkedToken;
                    feeder = checkedFeeder;
                    test.setEnabled(token != null && feeder != null && !feeder.isEmpty());
                }
            });
        }, "petlibro-check").start();
    }

    private void forgetSession() {
        savedSession.edit().clear().apply();
        token = null;
        feeder = null;
        email.setText("");
        password.setText("");
        test.setEnabled(false);
        forget.setEnabled(false);
        result.setText("Saved sign-in cleared.");
    }

    private void armTest() {
        AlarmManager alarms = getSystemService(AlarmManager.class);
        if (!alarms.canScheduleExactAlarms()) {
            result.setText("Allow Alarms & reminders, then return and tap the test button again.");
            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle("Dispense one portion?")
                .setMessage("Stay by the feeder and lock your phone after arming. The test runs once in 10 seconds.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Arm test", (dialog, button) -> scheduleTest(alarms))
                .show();
    }

    private void scheduleTest(AlarmManager alarms) {
        try {
            String testId = FeedTestReceiver.save(this, token, feeder);
            Intent intent = new Intent(this, FeedTestReceiver.class).putExtra("testId", testId);
            PendingIntent pending = PendingIntent.getBroadcast(this, 1, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 10000, pending);
            test.setEnabled(false);
            testResult.setText("Armed. Lock your phone now and watch the feeder in 10 seconds.");
        } catch (Exception e) {
            testResult.setText("Could not arm the test. No feed was scheduled.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String outcome = getSharedPreferences("feed-test", MODE_PRIVATE).getString("outcome", null);
        if (outcome != null) {
            testResult.setText(outcome);
        }
    }

    private static String login(String email, String password) throws Exception {
        String zone = TimeZone.getDefault().getID();
        byte[] digest = MessageDigest.getInstance("MD5").digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();
        for (byte value : digest) {
            hash.append(String.format(Locale.ROOT, "%02x", value & 255));
        }

        JSONObject login = new JSONObject()
                .put("appId", 1)
                .put("appSn", "c35772530d1041699c87fe62348507a8")
                .put("country", "US")
                .put("email", email)
                .put("password", hash.toString())
                .put("phoneBrand", "")
                .put("phoneSystemVersion", "")
                .put("timezone", zone)
                .put("thirdId", JSONObject.NULL)
                .put("type", JSONObject.NULL);

        JSONObject account = (JSONObject) request("/member/auth/login", login, null, zone);
        String token = account.getString("token");
        if (token.isEmpty()) {
            throw new IOException("Petlibro sign-in returned an empty session.");
        }
        return token;
    }

    static Object request(String path, JSONObject payload, String token, String zone) throws Exception {
        URL url = new URL("https://api.us.petlibro.com" + path);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(12000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("source", "ANDROID");
            connection.setRequestProperty("language", "EN");
            connection.setRequestProperty("timezone", zone);
            connection.setRequestProperty("version", "1.3.45");
            if (token != null) {
                connection.setRequestProperty("token", token);
            }
            byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);

            JSONObject response;
            try {
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body);
                }
                int status = connection.getResponseCode();
                if (status != 200) {
                    throw new IOException("Petlibro returned HTTP " + status + ".");
                }
                try (InputStream input = connection.getInputStream()) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (bytes.size() + count > 1048576) {
                            throw new IOException("Petlibro response is too large.");
                        }
                        bytes.write(buffer, 0, count);
                    }
                    response = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                throw new IOException("Petlibro request failed: " + e.getClass().getSimpleName() + ".", e);
            }
            int code = response.getInt("code");
            if (token != null && code == 1009) {
                throw new SessionExpired();
            }
            if (code != 0) {
                String operation = token == null ? "Sign-in" : "Device lookup";
                throw new IOException(operation + " rejected by Petlibro (code " + code + ").");
            }
            return response.get("data");
        } finally {
            connection.disconnect();
        }
    }

    private static final class SessionExpired extends IOException {}

    private static String describeDevices(JSONArray devices) throws Exception {
        if (devices.length() == 0) {
            return "Signed in. Petlibro returned no devices for this account.";
        }
        StringBuilder summary = new StringBuilder("Devices on this account");
        for (int i = 0; i < devices.length(); i++) {
            JSONObject device = devices.getJSONObject(i);
            String model = device.optString("productIdentifier", "");
            String name = device.optString("name", device.optString("deviceName", "Petlibro device"));
            summary.append("\n\n").append(name);
            if (!model.isEmpty()) {
                summary.append(" · ").append(model);
            }
        }
        return summary.toString();
    }
}
