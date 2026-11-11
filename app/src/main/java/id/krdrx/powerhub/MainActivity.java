package id.krdrx.powerhub;

import android.app.AlertDialog;
import android.os.Bundle;
import android.content.res.Configuration;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButton;
import rikka.shizuku.Shizuku;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 4102;
    private TextView statusText;
    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (requestCode, grantResult) -> refreshStatus();

    private static class PowerAction {
        final String title, description, command;
        PowerAction(String title, String description, String command) {
            this.title = title; this.description = description; this.command = command;
        }
    }

    private final PowerAction[] actions = new PowerAction[] {
        new PowerAction("Reboot", "Restart perangkat", "reboot"),
        new PowerAction("Reboot to recovery", "Restart ke mode recovery", "reboot recovery"),
        new PowerAction("Reboot to bootloader", "Restart ke mode fastboot / bootloader", "reboot bootloader"),
        new PowerAction("Power off", "Matikan perangkat", "poweroff"),
        new PowerAction("Turn the screen off", "Matikan layar dan kunci perangkat", "input keyevent 26")
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        Shizuku.addRequestPermissionResultListener(permissionListener);
        findViewById(R.id.shizukuButton).setOnClickListener(v -> requestShizuku());
        findViewById(R.id.themeButton).setOnClickListener(v -> toggleTheme());

        bindAction(R.id.actionReboot, actions[0]);
        bindAction(R.id.actionRecovery, actions[1]);
        bindAction(R.id.actionBootloader, actions[2]);
        bindAction(R.id.actionPowerOff, actions[3]);
        bindAction(R.id.actionScreenOff, actions[4]);
        refreshStatus();
    }

    private void applySavedTheme() {
        int mode = getPreferences(MODE_PRIVATE).getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private void toggleTheme() {
        int current = AppCompatDelegate.getDefaultNightMode();
        int next = current == AppCompatDelegate.MODE_NIGHT_YES
                ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
        getPreferences(MODE_PRIVATE).edit().putInt("theme", next).apply();
        AppCompatDelegate.setDefaultNightMode(next);
    }

    private void bindAction(int id, PowerAction action) {
        View row = findViewById(id);
        ((TextView) row.findViewById(R.id.title)).setText(action.title);
        ((TextView) row.findViewById(R.id.description)).setText(action.description);
        MaterialButton run = row.findViewById(R.id.runButton);
        run.setText("Jalankan");
        run.setOnClickListener(v -> confirmAction(action));
    }

    private void confirmAction(PowerAction action) {
        new AlertDialog.Builder(this)
            .setTitle(action.title)
            .setMessage(action.title.equals("Turn the screen off")
                ? "Layar akan dimatikan. Jika kunci layar aktif, perangkat juga akan terkunci."
                : "Jalankan perintah " + action.title + "?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Lanjutkan", (d, w) -> runPrivileged(action.command))
            .show();
    }

    private void requestShizuku() {
        if (!Shizuku.pingBinder()) {
            new AlertDialog.Builder(this)
                .setTitle("Shizuku belum aktif")
                .setMessage("Jalankan Shizuku terlebih dahulu, lalu kembali ke PowerHub.")
                .setPositiveButton("Mengerti", null).show();
            refreshStatus();
            return;
        }
        if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            refreshStatus();
        } else {
            Shizuku.requestPermission(REQUEST_CODE);
        }
    }

    private boolean hasPermission() {
        return Shizuku.pingBinder()
            && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void refreshStatus() {
        if (statusText == null) return;
        if (!Shizuku.pingBinder()) statusText.setText("Shizuku tidak aktif");
        else if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED)
            statusText.setText("Shizuku aktif · Izin diberikan");
        else statusText.setText("Shizuku aktif · Izin belum diberikan");
    }

    private void runPrivileged(String command) {
        if (!hasPermission()) {
            requestShizuku();
            return;
        }
        try {
            Process process = Shizuku.newProcess(new String[]{"sh", "-c", command}, null, null);
            new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    runOnUiThread(() -> {
                        if (exitCode != 0) showError("Perintah gagal (kode " + exitCode + "). Firmware mungkin membatasi operasi ini.");
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } catch (Exception e) {
            showError("Tidak dapat menjalankan perintah: " + e.getMessage());
        }
    }

    private void showError(String message) {
        new AlertDialog.Builder(this).setTitle("PowerHub").setMessage(message)
            .setPositiveButton("OK", null).show();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        super.onDestroy();
    }
}
