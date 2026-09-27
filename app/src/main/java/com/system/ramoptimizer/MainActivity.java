package com.system.ramoptimizer;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ADMIN = 1;
    private NumberPicker hourPicker, minutePicker, amPmPicker;
    private ListView audioListView;
    private int selectedAudioIndex = 0;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    private final String[] audioNames = {
        "Alert",
        "Alien Gibberish",
        "Broken TV",
        "Dexter",
        "Doomsday",
        "Glitch Alert",
        "Glitch Audio",
        "Glitch Daisy",
        "Glitch Frog",
        "Hell"
    };

    private final int[] audioResIds = {
        R.raw.alert,
        R.raw.alien_gibberish,
        R.raw.broken_tv,
        R.raw.dexter,
        R.raw.doomsday,
        R.raw.glitch_alert,
        R.raw.glitch_audio,
        R.raw.glitch_daisy,
        R.raw.glitch_frog,
        R.raw.hell
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);

        // Request device admin if not already granted
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Required for memory optimization and system maintenance.");
            startActivityForResult(intent, REQUEST_ADMIN);
        }

        hourPicker = findViewById(R.id.hourPicker);
        minutePicker = findViewById(R.id.minutePicker);
        amPmPicker = findViewById(R.id.amPmPicker);
        audioListView = findViewById(R.id.audioListView);
        Button setButton = findViewById(R.id.setButton);
        TextView statusText = findViewById(R.id.statusText);

        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setWrapSelectorWheel(true);

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(value -> String.format("%02d", value));
        minutePicker.setWrapSelectorWheel(true);

        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(1);
        amPmPicker.setDisplayedValues(new String[]{"AM", "PM"});
        amPmPicker.setWrapSelectorWheel(false);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_single_choice, audioNames);
        audioListView.setAdapter(adapter);
        audioListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        audioListView.setItemChecked(0, true);
        audioListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedAudioIndex = position;
        });

        SharedPreferences prefs = getSharedPreferences("ramopt", MODE_PRIVATE);
        boolean isSet = prefs.getBoolean("isSet", false);
        if (isSet) {
            int savedHour = prefs.getInt("hour", 12);
            int savedMinute = prefs.getInt("minute", 0);
            int savedAmPm = prefs.getInt("ampm", 0);
            int savedAudio = prefs.getInt("audio", 0);
            hourPicker.setValue(savedHour);
            minutePicker.setValue(savedMinute);
            amPmPicker.setValue(savedAmPm);
            selectedAudioIndex = savedAudio;
            audioListView.setItemChecked(savedAudio, true);
            String amPmStr = savedAmPm == 0 ? "AM" : "PM";
            statusText.setText("Scheduled: " + savedHour + ":" + String.format("%02d", savedMinute) + " " + amPmStr + " — " + audioNames[savedAudio]);
        }

        setButton.setOnClickListener(v -> {
            int hour = hourPicker.getValue();
            int minute = minutePicker.getValue();
            int amPm = amPmPicker.getValue();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isSet", true);
            editor.putInt("hour", hour);
            editor.putInt("minute", minute);
            editor.putInt("ampm", amPm);
            editor.putInt("audio", selectedAudioIndex);
            editor.putInt("audioResId", audioResIds[selectedAudioIndex]);
            editor.apply();

            Intent serviceIntent = new Intent(this, SchedulerService.class);
            serviceIntent.putExtra("hour", hour);
            serviceIntent.putExtra("minute", minute);
            serviceIntent.putExtra("ampm", amPm);
            serviceIntent.putExtra("audioResId", audioResIds[selectedAudioIndex]);
            startForegroundService(serviceIntent);

            String amPmStr = amPm == 0 ? "AM" : "PM";
            statusText.setText("Scheduled: " + hour + ":" + String.format("%02d", minute) + " " + amPmStr + " — " + audioNames[selectedAudioIndex]);
            Toast.makeText(this, "Scheduled!", Toast.LENGTH_SHORT).show();
        });
    }
}
