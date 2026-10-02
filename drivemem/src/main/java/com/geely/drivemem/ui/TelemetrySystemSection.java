package com.geely.drivemem.ui;

import android.app.Activity;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.geely.drivemem.R;
import com.geely.drivemem.util.Style;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/** System information and maintenance panel of the Config screen. Shows the
 * application version and APK SHA-256, plus maintenance operations (cleanup).
 *
 * Extracted from TelemetryActivity.buildSystem() so that 2800+ line file
 * stops growing with every new Config section -- one View subclass per
 * section, same pattern ChargeStatsView/DailyStatsView already use. */
public final class TelemetrySystemSection extends LinearLayout {
    private final Activity activity;

    public TelemetrySystemSection(Activity activity) {
        super(activity);
        this.activity = activity;
        setOrientation(VERTICAL);

        addView(Style.header(activity, activity.getString(R.string.cfg_system_header)));

        int pad = Style.dp(activity, 14);

        // ---- App Info Card ----
        addView(Style.header(activity, activity.getString(R.string.cfg_app_info_header)));
        LinearLayout infoCard = new LinearLayout(activity);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setBackground(Style.card(Style.CARD, activity));
        infoCard.setPadding(pad, pad, pad, pad);

        String verName = "Unknown";
        long verCode = 0;
        try {
            android.content.pm.PackageInfo pi = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            verName = pi.versionName;
            verCode = pi.getLongVersionCode();
        } catch (Throwable ignored) {}

        TextView verView = new TextView(activity);
        verView.setText(activity.getString(R.string.cfg_version_label, verName, verCode));
        verView.setTextColor(Style.TEXT);
        verView.setTextSize(15);
        verView.setTypeface(null, android.graphics.Typeface.BOLD);
        infoCard.addView(verView);

        TextView devIdView = new TextView(activity);
        devIdView.setText(activity.getString(R.string.cfg_device_id,
            com.geely.drivemem.net.MqttReporter.getDevId()));
        devIdView.setTextColor(Style.TEXT_DIM);
        devIdView.setTextSize(13);
        devIdView.setPadding(0, Style.dp(activity, 4), 0, 0);
        infoCard.addView(devIdView);

        // For connecting over adb without guessing the car's address --
        // this head unit's IP moves around DHCP, and asking the driver to
        // dig through Android's own network settings mid-task isn't
        // reasonable when this screen already shows every other identifier.
        TextView ipView = new TextView(activity);
        ipView.setText(activity.getString(R.string.cfg_ip_label, wifiIpAddress(activity)));
        ipView.setTextColor(Style.TEXT_DIM);
        ipView.setTextSize(13);
        ipView.setTextIsSelectable(true);
        ipView.setPadding(0, Style.dp(activity, 2), 0, Style.dp(activity, 8));
        infoCard.addView(ipView);

        TextView shaLbl = Style.label(activity, activity.getString(R.string.cfg_sha_label) + ":");
        shaLbl.setTextSize(14);
        shaLbl.setTextColor(Style.TEXT_DIM);
        infoCard.addView(shaLbl);

        final TextView shaView = new TextView(activity);
        shaView.setText("...");
        shaView.setTextColor(Style.TEXT);
        shaView.setTextSize(12);
        shaView.setTypeface(android.graphics.Typeface.MONOSPACE);
        shaView.setPadding(0, Style.dp(activity, 4), 0, 0);
        shaView.setTextIsSelectable(true);
        infoCard.addView(shaView);

        new Thread(() -> {
            String sha = sha256(new File(activity.getPackageCodePath()));
            activity.runOnUiThread(() -> shaView.setText(sha != null ? sha : "N/A"));
        }).start();

        addView(infoCard);

        // ---- Maintenance Card ----
        addView(Style.header(activity, activity.getString(R.string.cfg_maintenance_header)));
        LinearLayout maintCard = new LinearLayout(activity);
        maintCard.setOrientation(LinearLayout.VERTICAL);
        maintCard.setBackground(Style.card(Style.CARD, activity));
        maintCard.setPadding(pad, pad, pad, pad);

        LinearLayout maintRow = new LinearLayout(activity);
        maintRow.setOrientation(LinearLayout.HORIZONTAL);
        maintRow.addView(button(activity, activity.getString(R.string.cfg_cleanup), 0xFF8A3A3A, () -> {
            activity.startActivity(new Intent(activity, CleanupActivity.class));
        }));
        maintCard.addView(maintRow);

        addView(maintCard);
    }

    private static String sha256(File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) > 0) digest.update(buffer, 0, count);
            char[] hex = "0123456789abcdef".toCharArray();
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest.digest()) {
                result.append(hex[(value >>> 4) & 0x0f]);
                result.append(hex[value & 0x0f]);
            }
            return result.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String wifiIpAddress(Activity activity) {
        try {
            android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager)
                activity.getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
            if (wm == null) return "N/A";
            int ip = wm.getConnectionInfo().getIpAddress();
            return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." +
                   ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
        } catch (Throwable e) {
            return "N/A";
        }
    }

    private TextView button(Activity activity, String label, int color, Runnable onClick) {
        TextView t = new TextView(activity);
        t.setText(label);
        t.setTextColor(Style.onFill(color));
        t.setTextSize(15);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setGravity(android.view.Gravity.CENTER);
        t.setBackground(Style.card(color, activity));
        int ph = Style.dp(activity, 18);
        int pv = Style.dp(activity, 12);
        t.setPadding(ph, pv, ph, pv);
        t.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = Style.dp(activity, 4);
        lp.setMargins(m, Style.dp(activity, 8), m, 0);
        t.setLayoutParams(lp);
        return t;
    }

}
