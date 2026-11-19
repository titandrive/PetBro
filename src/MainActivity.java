package local.petlibrocompanion;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
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
    private static final int MANTLE = 0xff1e2030;
    private static final int BASE = 0xff24273a;
    private static final int SURFACE = 0xff363a4f;
    private static final int TEXT = 0xffcad3f5;
    private static final int MUTED = 0xffa5adcb;
    private static final int MAUVE = 0xffc6a0f6;
    private static final int PEACH = 0xfff5a97f;

    private LinearLayout credentials;
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
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        LinearLayout content = column();
        content.setPadding(dp(16), dp(16), dp(16), dp(24));
        content.setFocusableInTouchMode(true);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(MANTLE);
        scroll.setFillViewport(true);
        scroll.addView(content);
        setContentView(scroll);

        TextView title = label("PetBro", 28, TEXT);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        title.setAccessibilityHeading(true);
        title.setPadding(dp(12), dp(4), dp(12), dp(8));
        content.addView(title);

        LinearLayout feeding = card(content, "Feeder test", "▶", PEACH);
        add(feeding, label("Dispense one portion after 10 seconds.", 14, MUTED), 12);
        test = action("Arm 10-second test", PEACH, MANTLE);
        test.setEnabled(false);
        test.setOnClickListener(view -> armTest());
        add(feeding, test, 16);

        testResult = status();
        add(feeding, testResult, 12);

        LinearLayout account = card(content, "Petlibro account", "●", MAUVE);
        result = status();
        add(account, result, 12);

        credentials = column();
        add(account, credentials, 0);
        add(credentials, label("Use a US Petlibro account with shared access to your feeder.", 14, MUTED), 12);
        email = field("Email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        add(credentials, email, 12);

        password = field("Password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        add(credentials, password, 10);

        check = action("Sign in", MAUVE, MANTLE);
        check.setOnClickListener(view -> checkAccount());
        add(credentials, check, 12);

        savedSession = getSharedPreferences("petlibro-session", MODE_PRIVATE);
        forget = action("Forget saved sign-in", SURFACE, TEXT);
        forget.setOnClickListener(view -> forgetSession());
        add(account, forget, 12);
        forget.setVisibility(View.GONE);
        if (savedSession.contains("token")) {
            credentials.setVisibility(View.GONE);
            checkAccount(null, null);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private void add(LinearLayout parent, View view, int marginTop) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(marginTop);
        parent.addView(view, params);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radius));
        return background;
    }

    private TextView label(String text, int size, int color) {
        TextView label = new TextView(this);
        label.setTypeface(Typeface.MONOSPACE);
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(color);
        return label;
    }

    private TextView status() {
        TextView status = label("", 14, MUTED);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        status.setVisibility(View.GONE);
        return status;
    }

    private void show(TextView status, String message) {
        status.setText(message);
        status.setVisibility(View.VISIBLE);
    }

    private LinearLayout card(LinearLayout parent, String title, String symbol, int accent) {
        LinearLayout card = column();
        int inset = dp(16);
        card.setPadding(inset, inset, inset, inset);
        card.setBackground(rounded(BASE, 24));
        add(parent, card, 16);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = label(symbol, 15, accent);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(rounded(SURFACE, 10));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        heading.addView(icon, dp(32), dp(32));
        TextView name = label(title, 20, TEXT);
        name.setAccessibilityHeading(true);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1);
        nameParams.setMarginStart(dp(12));
        heading.addView(name, nameParams);
        card.addView(heading);
        return card;
    }

    private EditText field(String hint, int inputType) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setInputType(inputType);
        field.setTypeface(Typeface.MONOSPACE);
        field.setTextSize(15);
        field.setTextColor(TEXT);
        field.setHintTextColor(MUTED);
        field.setSingleLine();
        field.setMinHeight(dp(52));
        field.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable background = rounded(SURFACE, 12);
        background.setStroke(dp(1), 0xff494d64);
        field.setBackground(background);
        field.setSaveEnabled(false);
        return field;
    }

    private Button action(String title, int fill, int textColor) {
        Button button = new Button(this);
        button.setText(title);
        button.setAllCaps(false);
        button.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        button.setMinHeight(dp(48));
        button.setPadding(dp(16), dp(8), dp(16), dp(8));
        int[][] states = {{-android.R.attr.state_enabled}, {}};
        button.setTextColor(new ColorStateList(states, new int[]{MUTED, textColor}));
        GradientDrawable shape = rounded(fill, 24);
        shape.setColor(new ColorStateList(states, new int[]{SURFACE, fill}));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ffffff), shape, null));
        button.setStateListAnimator(null);
        return button;
    }

    private void checkAccount() {
        String address = email.getText().toString().trim();
        String secret = password.getText().toString();
        if (address.isEmpty() || secret.isEmpty()) {
            show(result, "Enter your Petlibro email and password.");
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
        show(result, secret == null ? "Restoring Petlibro sign-in…" : "Checking Petlibro…");
        new Thread(() -> {
            String message;
            String session = null;
            String serial = null;
            boolean devicesLoaded = false;
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
                devicesLoaded = true;
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
            boolean verified = devicesLoaded;
            runOnUiThread(() -> {
                if (!isDestroyed()) {
                    show(result, outcome);
                    check.setEnabled(true);
                    boolean connected = checkedToken != null && verified;
                    boolean remembered = savedSession.contains("token");
                    credentials.setVisibility(connected ? View.GONE : View.VISIBLE);
                    forget.setVisibility(connected || remembered ? View.VISIBLE : View.GONE);
                    forget.setEnabled(true);
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
        forget.setVisibility(View.GONE);
        credentials.setVisibility(View.VISIBLE);
        show(result, "Saved sign-in cleared.");
    }

    private void armTest() {
        AlarmManager alarms = getSystemService(AlarmManager.class);
        if (!alarms.canScheduleExactAlarms()) {
            show(result, "Allow Alarms & reminders, then return and tap the test button again.");
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
            show(testResult, "Armed. Lock your phone now and watch the feeder in 10 seconds.");
        } catch (Exception e) {
            show(testResult, "Could not arm the test. No feed was scheduled.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String outcome = getSharedPreferences("feed-test", MODE_PRIVATE).getString("outcome", null);
        if (outcome != null) {
            show(testResult, outcome);
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
