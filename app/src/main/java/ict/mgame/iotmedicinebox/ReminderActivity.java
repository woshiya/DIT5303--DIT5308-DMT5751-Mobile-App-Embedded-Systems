package ict.mgame.iotmedicinebox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class ReminderActivity extends Activity {

    private MediaPlayer mediaPlayer;
    private DatabaseHelper databaseHelper;
    private long medicationId;
    private int boxNumber;
    private Handler handler;
    private Runnable stopAlarmRunnable;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 确保在锁屏状态下也能显示
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        setContentView(R.layout.activity_reminder);

        databaseHelper = new DatabaseHelper(this);
        handler = new Handler();

        // 初始化振动器
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // 获取传递的药物信息
        Intent intent = getIntent();
        if (intent != null) {
            medicationId = intent.getLongExtra("medication_id", -1);
            boxNumber = intent.getIntExtra("box_number", -1);
            String time = intent.getStringExtra("reminder_time");
            String medicineName = intent.getStringExtra("medicine_name");
            String dosage = intent.getStringExtra("dosage");
            String instructions = intent.getStringExtra("instructions");

            // 更新UI显示
            updateReminderUI(time, medicineName, dosage, instructions);
        }

        // 启动闹钟和振动
        startAlarm();
        startVibration();

        // 设置按钮点击事件
        setupButtonListeners();
    }

    private void updateReminderUI(String time, String medicineName, String dosage, String instructions) {
        TextView tvTime = findViewById(R.id.tvReminderTime);
        TextView tvMedName = findViewById(R.id.tvMedName);
        TextView tvDosage = findViewById(R.id.tvDosage);
        TextView tvInstructions = findViewById(R.id.tvInstructions);

        if (tvTime != null && time != null) {
            tvTime.setText("Time: " + time);
        }
        if (tvMedName != null && medicineName != null) {
            tvMedName.setText(medicineName);
        }
        if (tvDosage != null && dosage != null) {
            tvDosage.setText(dosage);
        }
        if (tvInstructions != null && instructions != null) {
            tvInstructions.setText(instructions);
        }
    }

    private void startAlarm() {
        try {
            // 使用系统默认的通知铃声
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                if (alarmSound == null) {
                    alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }
            }

            mediaPlayer = MediaPlayer.create(this, alarmSound);
            mediaPlayer.setLooping(true); // 循环播放
            mediaPlayer.start();

            // 自动停止闹钟的计时器（5分钟后自动停止）
            stopAlarmRunnable = new Runnable() {
                @Override
                public void run() {
                    stopAlarm();
                    stopVibration();
                }
            };
            handler.postDelayed(stopAlarmRunnable, 5 * 60 * 1000); // 5分钟
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            // 振动模式：等待0ms，振动1000ms，暂停1000ms
            long[] pattern = {0, 1000, 1000};
            vibrator.vibrate(pattern, 0);
        }
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (stopAlarmRunnable != null) {
            handler.removeCallbacks(stopAlarmRunnable);
        }
    }

    private void stopVibration() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void setupButtonListeners() {
        // 已服用按钮
        LinearLayout btnTaken = findViewById(R.id.btnTaken);
        if (btnTaken != null) {
            btnTaken.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logMedicationStatus("Taken");
                    stopAlarm();
                    stopVibration();

                    // 如果已连接蓝牙，发送开盒指令
                    if (boxNumber > 0) {
                        sendOpenBoxCommand(boxNumber);
                    }

                    finish();
                }
            });
        }

        // 跳过按钮
        LinearLayout btnSkip = findViewById(R.id.btnSkip);
        if (btnSkip != null) {
            btnSkip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logMedicationStatus("Missed");
                    stopAlarm();
                    stopVibration();
                    finish();
                }
            });
        }

        // 稍后提醒按钮
        LinearLayout btnSnooze = findViewById(R.id.btnSnooze);
        if (btnSnooze != null) {
            btnSnooze.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logMedicationStatus("Snoozed");
                    stopAlarm();
                    stopVibration();

                    // 设置5分钟后再次提醒
                    scheduleSnooze();

                    finish();
                }
            });
        }
    }

    private void logMedicationStatus(String status) {
        if (medicationId != -1) {
            Medication medication = databaseHelper.getMedicationById(medicationId);
            if (medication != null) {
                // 记录到日志
                databaseHelper.logMedication(
                        medication.getMedicineName(),
                        "1 tablet", // 默认剂量，可以根据需要修改
                        medication.getBoxNumber(),
                        status
                );
            }
        }
    }

    private void sendOpenBoxCommand(int boxNumber) {
        // 这里可以调用蓝牙服务发送开盒指令
        // 例如: bluetoothService.sendCommand("Box" + boxNumber + "_OPEN");
        // 注意：需要先确保蓝牙已连接
    }

    private void scheduleSnooze() {
        // 5分钟后再次提醒
        Intent snoozeIntent = new Intent(this, ReminderActivity.class);
        if (getIntent() != null) {
            snoozeIntent.putExtras(getIntent().getExtras());
        }

        // 添加必要的标志
        snoozeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // 使用AlarmManager设置5分钟后的闹钟
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            long snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000; // 5分钟后

            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                    this,
                    (int) (medicationId * 1000), // 使用不同的requestCode
                    snoozeIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                android.app.AlarmManager.AlarmClockInfo alarmClockInfo =
                        new android.app.AlarmManager.AlarmClockInfo(
                                snoozeTime,
                                pendingIntent
                        );
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        pendingIntent
                );
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        android.app.AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        android.app.AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        pendingIntent
                );
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 停止振动
        if (vibrator != null) {
            vibrator.cancel();
        }

        stopAlarm();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
