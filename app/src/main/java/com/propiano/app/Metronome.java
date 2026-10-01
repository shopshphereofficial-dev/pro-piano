package com.propiano.app;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;

public class Metronome {
    public interface OnBeatListener {
        void onBeat(int beatIndex);
    }

    private boolean isRunning = false;
    private int bpm = 100;
    private Thread tickerThread;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private OnBeatListener listener;
    private byte[] tickSample;
    private byte[] tockSample;
    private AudioTrack clickTrack;

    public Metronome() {
        generateClickBuffers();
    }

    public void setOnBeatListener(OnBeatListener listener) {
        this.listener = listener;
    }

    public void setBpm(int bpm) {
        this.bpm = Math.max(30, Math.min(260, bpm));
    }

    public int getBpm() {
        return bpm;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;

        int sampleRate = 22050;
        int bufSize = Math.max(
                AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_8BIT),
                1024
        );
        clickTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        clickTrack.play();

        tickerThread = new Thread(() -> {
            int beat = 0;
            while (isRunning) {
                long intervalMs = 60000 / bpm;
                long start = System.currentTimeMillis();

                final int currentBeat = beat;
                mainHandler.post(() -> {
                    if (listener != null) listener.onBeat(currentBeat);
                });

                // Play click sound
                byte[] click = (beat == 0) ? tickSample : tockSample;
                if (clickTrack != null && isRunning) {
                    clickTrack.write(click, 0, click.length);
                }

                beat = (beat + 1) % 4;

                long elapsed = System.currentTimeMillis() - start;
                long sleep = intervalMs - elapsed;
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }, "MetronomeTicker");
        tickerThread.start();
    }

    public void stop() {
        isRunning = false;
        if (tickerThread != null) {
            tickerThread.interrupt();
            tickerThread = null;
        }
        if (clickTrack != null) {
            try {
                clickTrack.stop();
                clickTrack.release();
            } catch (Exception ignored) {}
            clickTrack = null;
        }
    }

    private void generateClickBuffers() {
        int len = 400; // ~18ms
        tickSample = new byte[len];
        tockSample = new byte[len];
        for (int i = 0; i < len; i++) {
            double decay = (double) (len - i) / len;
            // 1500Hz high wood block for beat 1
            tickSample[i] = (byte) (128 + 120 * decay * Math.sin(2.0 * Math.PI * 1500 * i / 22050.0));
            // 800Hz low wood block for other beats
            tockSample[i] = (byte) (128 + 90 * decay * Math.sin(2.0 * Math.PI * 800 * i / 22050.0));
        }
    }
}
