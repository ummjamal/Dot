package com.alshuibi.grocery.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/** Lightweight audible/haptic feedback for successful POS actions. */
public final class FeedbackManager {
    private FeedbackManager() { }

    public static void success(Context context) {
        if (context == null) return;
        try {
            ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55);
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 90);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(tone::release, 160);
        } catch (Exception ignored) { }
        vibrate(context, 45);
    }

    public static void error(Context context) {
        if (context == null) return;
        try {
            ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45);
            tone.startTone(ToneGenerator.TONE_PROP_NACK, 120);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(tone::release, 190);
        } catch (Exception ignored) { }
        vibrate(context, 85);
    }

    @SuppressWarnings("deprecation")
    private static void vibrate(Context context, long durationMs) {
        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = manager == null ? null : manager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator == null || !vibrator.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(durationMs);
            }
        } catch (Exception ignored) { }
    }
}
