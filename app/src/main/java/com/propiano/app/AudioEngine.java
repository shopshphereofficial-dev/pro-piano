package com.propiano.app;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.util.concurrent.ConcurrentHashMap;

public class AudioEngine {
    public static final int SAMPLE_RATE = 44100;
    public static final int BUFFER_SIZE = 1024;

    public enum Instrument {
        GRAND_PIANO("Grand Piano"),
        ELECTRIC_PIANO("Electric Piano"),
        CHURCH_ORGAN("Church Organ"),
        SYNTH_PAD("Warm Synth"),
        MUSIC_BOX("Music Box");

        public final String title;
        Instrument(String title) { this.title = title; }
    }

    private static class Voice {
        int note;
        double frequency;
        Instrument instrument;
        boolean keyHeld = true;
        double age = 0; // in seconds
        double releaseAge = 0;
        boolean released = false;
        double phase = 0;
    }

    private AudioTrack audioTrack;
    private Thread audioThread;
    private volatile boolean isRunning = false;
    private final ConcurrentHashMap<Integer, Voice> activeVoices = new ConcurrentHashMap<>();
    private volatile Instrument currentInstrument = Instrument.GRAND_PIANO;
    private volatile boolean sustainEnabled = false;
    private volatile float masterVolume = 0.85f;

    public void start() {
        if (isRunning) return;
        int minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        int bufSize = Math.max(minBuf, BUFFER_SIZE * 4);

        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        audioTrack.play();
        isRunning = true;

        audioThread = new Thread(this::renderAudio, "AudioEngineRenderer");
        audioThread.setPriority(Thread.MAX_PRIORITY);
        audioThread.start();
    }

    public void stop() {
        isRunning = false;
        if (audioThread != null) {
            try {
                audioThread.join(500);
            } catch (InterruptedException ignored) {}
            audioThread = null;
        }
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception ignored) {}
            audioTrack = null;
        }
        activeVoices.clear();
    }

    public void noteOn(int midiNote) {
        Voice voice = new Voice();
        voice.note = midiNote;
        voice.frequency = 440.0 * Math.pow(2.0, (midiNote - 69) / 12.0);
        voice.instrument = currentInstrument;
        voice.keyHeld = true;
        voice.age = 0;
        voice.phase = 0;
        activeVoices.put(midiNote, voice);
    }

    public void noteOff(int midiNote) {
        Voice voice = activeVoices.get(midiNote);
        if (voice != null) {
            voice.keyHeld = false;
            if (!sustainEnabled) {
                voice.released = true;
            }
        }
    }

    public void setSustain(boolean enabled) {
        this.sustainEnabled = enabled;
        if (!enabled) {
            for (Voice v : activeVoices.values()) {
                if (!v.keyHeld) {
                    v.released = true;
                }
            }
        }
    }

    public boolean isSustainEnabled() {
        return sustainEnabled;
    }

    public void setInstrument(Instrument instrument) {
        this.currentInstrument = instrument;
    }

    public Instrument getInstrument() {
        return currentInstrument;
    }

    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    private void renderAudio() {
        short[] buffer = new short[BUFFER_SIZE];
        double dt = 1.0 / SAMPLE_RATE;

        while (isRunning) {
            if (activeVoices.isEmpty()) {
                java.util.Arrays.fill(buffer, (short) 0);
                audioTrack.write(buffer, 0, buffer.length);
                continue;
            }

            for (int i = 0; i < BUFFER_SIZE; i++) {
                double sample = 0;

                for (java.util.Map.Entry<Integer, Voice> entry : activeVoices.entrySet()) {
                    Voice v = entry.getValue();
                    double amp = computeEnvelope(v);

                    if (amp <= 0.0005) {
                        activeVoices.remove(entry.getKey());
                        continue;
                    }

                    double noteSample = synthesizeSample(v);
                    sample += noteSample * amp;

                    v.phase += 2.0 * Math.PI * v.frequency * dt;
                    if (v.phase > 2.0 * Math.PI) {
                        v.phase -= 2.0 * Math.PI;
                    }
                    v.age += dt;
                    if (v.released) {
                        v.releaseAge += dt;
                    }
                }

                // Master volume and soft limiter/clipping
                sample = sample * masterVolume;
                if (sample > 1.0) sample = 1.0;
                else if (sample < -1.0) sample = -1.0;

                buffer[i] = (short) (sample * 32767);
            }

            audioTrack.write(buffer, 0, buffer.length);
        }
    }

    private double synthesizeSample(Voice v) {
        double p = v.phase;
        switch (v.instrument) {
            case GRAND_PIANO:
                // Rich acoustic harmonic blend with hammer thump
                double p1 = Math.sin(p);
                double p2 = 0.55 * Math.sin(2 * p);
                double p3 = 0.25 * Math.sin(3 * p);
                double p4 = 0.12 * Math.sin(4 * p);
                double p5 = 0.06 * Math.sin(5 * p);
                double hammer = (v.age < 0.02) ? (Math.random() * 0.2 * (1.0 - v.age / 0.02)) : 0.0;
                return (p1 + p2 + p3 + p4 + p5 + hammer) * 0.5;

            case ELECTRIC_PIANO:
                // Rhodes chime + warm fundamental
                double ep1 = Math.sin(p);
                double ep2 = 0.35 * Math.sin(2 * p + 0.2);
                double ep3 = 0.60 * Math.sin(3.01 * p); // Bell overtone
                return (ep1 + ep2 + ep3) * 0.45;

            case CHURCH_ORGAN:
                // Classic organ drawbars (16', 8', 4', 2')
                double o1 = Math.sin(0.5 * p);
                double o2 = Math.sin(p);
                double o3 = 0.7 * Math.sin(2 * p);
                double o4 = 0.4 * Math.sin(4 * p);
                return (o1 + o2 + o3 + o4) * 0.35;

            case SYNTH_PAD:
                // Soft warm detuned synth
                double s1 = Math.sin(p);
                double s2 = 0.5 * Math.sin(1.004 * p);
                double s3 = 0.25 * Math.sin(2.002 * p);
                return (s1 + s2 + s3) * 0.4;

            case MUSIC_BOX:
                // Pure metallic high chime
                double mb1 = Math.sin(p);
                double mb2 = 0.4 * Math.sin(2.75 * p);
                double mb3 = 0.2 * Math.sin(5.4 * p);
                return (mb1 + mb2 + mb3) * 0.45;

            default:
                return Math.sin(p);
        }
    }

    private double computeEnvelope(Voice v) {
        double age = v.age;

        if (v.instrument == Instrument.CHURCH_ORGAN) {
            // Sustained organ envelope
            if (v.released) {
                double relProgress = v.releaseAge / 0.15;
                return Math.max(0.0, 1.0 - relProgress);
            }
            if (age < 0.03) return age / 0.03;
            return 0.9;
        }

        if (v.instrument == Instrument.SYNTH_PAD) {
            // Soft attack, sustained until release
            if (v.released) {
                double relProgress = v.releaseAge / 0.4;
                return Math.max(0.0, 0.75 * (1.0 - relProgress));
            }
            if (age < 0.15) return (age / 0.15) * 0.75;
            return 0.75;
        }

        // Piano / E-Piano / Music Box decay models
        double decayRate = (v.instrument == Instrument.MUSIC_BOX) ? 1.6 : 0.85;
        // Natural exponential decay while held
        double baseEnvelope = Math.exp(-age * decayRate);

        if (v.released) {
            // Fast damper release
            double dampRate = 18.0;
            double damper = Math.exp(-v.releaseAge * dampRate);
            return baseEnvelope * damper;
        }

        // Attack ramp (10ms)
        if (age < 0.01) {
            return (age / 0.01) * baseEnvelope;
        }
        return baseEnvelope;
    }
}
