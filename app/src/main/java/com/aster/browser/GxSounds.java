package com.aster.browser;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** Tiny synthesized interface sounds (no audio assets needed). */
final class GxSounds {
    private static final int RATE = 44100;
    private AudioTrack click;
    private AudioTrack open;
    private AudioTrack close;

    GxSounds() {
        click = build(synth(1900, 1100, 0.028, 0.22, false));
        open = build(synth(520, 1450, 0.10, 0.20, true));
        close = build(synth(1300, 420, 0.09, 0.18, true));
    }

    void click() { play(click); }
    void open() { play(open); }
    void close() { play(close); }

    void release() {
        release(click);
        release(open);
        release(close);
        click = open = close = null;
    }

    private static short[] synth(double f0, double f1, double seconds, double volume, boolean shimmer) {
        int n = (int) (RATE * seconds);
        short[] pcm = new short[n];
        double phase = 0;
        double phase2 = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double f = f0 + (f1 - f0) * t;
            phase += 2 * Math.PI * f / RATE;
            phase2 += 2 * Math.PI * f * 1.5 / RATE;
            double env = Math.min(1.0, i / (RATE * 0.003)) * Math.pow(1 - t, 2.2);
            double s = Math.sin(phase) + (shimmer ? 0.35 * Math.sin(phase2) : 0);
            pcm[i] = (short) (s * env * volume * Short.MAX_VALUE * 0.75);
        }
        return pcm;
    }

    private static AudioTrack build(short[] pcm) {
        try {
            AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .setAudioFormat(new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.length * 2)
                .build();
            track.write(pcm, 0, pcm.length);
            return track;
        } catch (Throwable e) {
            return null;
        }
    }

    private static void play(AudioTrack track) {
        if (track == null) return;
        try {
            if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) track.stop();
            track.reloadStaticData();
            track.play();
        } catch (Throwable ignored) { }
    }

    private static void release(AudioTrack track) {
        if (track == null) return;
        try { track.release(); } catch (Throwable ignored) { }
    }
}
