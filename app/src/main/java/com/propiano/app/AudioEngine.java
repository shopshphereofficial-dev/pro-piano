package com.propiano.app;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

public class AudioEngine {
    public static final int SAMPLE_RATE = 44100;
    public static final int BUFFER_SIZE = 512;
    public static final int MAX_VOICES = 24;
    private static final int SINE_TABLE_SIZE = 4096;
    private static final float[] SINE_TABLE = new float[SINE_TABLE_SIZE];

    static {
        for (int i = 0; i < SINE_TABLE_SIZE; i++) {
            SINE_TABLE[i] = (float) Math.sin(2.0 * Math.PI * i / SINE_TABLE_SIZE);
        }
    }

    public enum Instrument {
        GRAND_PIANO("Grand Piano", "🎹"),
        BRIGHT_PIANO("Bright Piano", "🌟"),
        ELECTRIC_PIANO("Electric Piano", "⚡"),
        CHURCH_ORGAN("Church Organ", "⛪"),
        SYNTH_PAD("Warm Synth", "🎻"),
        MUSIC_BOX("Music Box", "🔔"),
        HARPSICHORD("Harpsichord", "🎼"),
        VIBRAPHONE("Vibraphone", "🎷");

        public final String title;
        public final String icon;

        Instrument(String title, String icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    private static class Voice {
        boolean active = false;
        int note = 60;
        Instrument instrument = Instrument.GRAND_PIANO;
        boolean isHeld = false;
        boolean released = false;

        float freq = 440f;
        float phaseInc = 0f;
        float phase1 = 0f;
        float phase2 = 0f;
        float phase3 = 0f;
        float phase4 = 0f;
        float phase5 = 0f;

        float amp = 0f;
        float decayPerSample = 0.9999f;
        float releaseDecayPerSample = 0.998f;
        float attackSamples = 0f;
        float currentAttack = 0f;

        // Instrument specific
        float brightness = 1.0f;
        float modPhase = 0f;
        long samplesPlayed = 0;

        void reset(int midiNote, Instrument instr, boolean sustain) {
            this.active = true;
            this.note = midiNote;
            this.instrument = instr;
            this.isHeld = true;
            this.released = false;
            this.samplesPlayed = 0;

            // Pitch formula
            this.freq = (float) (440.0 * Math.pow(2.0, (midiNote - 69) / 12.0));
            this.phaseInc = (float) (2.0 * Math.PI * freq / SAMPLE_RATE);

            this.phase1 = 0f;
            this.phase2 = 0f;
            this.phase3 = 0f;
            this.phase4 = 0f;
            this.phase5 = 0f;
            this.modPhase = 0f;

            this.currentAttack = 0f;
            // Short 3ms attack to avoid audio click/pop
            this.attackSamples = Math.max(1f, SAMPLE_RATE * 0.003f);

            // Natural piano acoustic string length: lower strings ring 4.5s, higher strings ring 1.2s
            float sustainSec;
            switch (instr) {
                case CHURCH_ORGAN:
                    // Organ sustains indefinitely while held
                    sustainSec = 60.0f;
                    break;
                case SYNTH_PAD:
                    sustainSec = 30.0f;
                    this.attackSamples = SAMPLE_RATE * 0.05f; // soft 50ms swell
                    break;
                case MUSIC_BOX:
                    sustainSec = 2.8f;
                    break;
                case HARPSICHORD:
                    sustainSec = 1.4f;
                    break;
                case VIBRAPHONE:
                    sustainSec = 3.5f;
                    break;
                case ELECTRIC_PIANO:
                    sustainSec = 3.0f;
                    break;
                default: // GRAND_PIANO, BRIGHT_PIANO
                    float noteFraction = (108f - midiNote) / 88.0f;
                    sustainSec = 1.2f + (noteFraction * 3.2f);
                    break;
            }

            // Exponential decay per sample: amp(t) = exp(-t / tau)
            this.decayPerSample = (float) Math.exp(-1.0 / (SAMPLE_RATE * sustainSec));
            // Damper cut-off release (when key is lifted and sustain is off)
            this.releaseDecayPerSample = (float) Math.exp(-1.0 / (SAMPLE_RATE * 0.10));
            this.amp = 1.0f;
            this.brightness = 1.0f;
        }

        private static float fastSin(float phase) {
            // Keep phase in [0, 2*PI)
            float norm = phase * (float) (1.0 / (2.0 * Math.PI));
            norm = norm - (float) Math.floor(norm);
            int idx = (int) (norm * SINE_TABLE_SIZE) & (SINE_TABLE_SIZE - 1);
            return SINE_TABLE[idx];
        }

        float nextSample() {
            if (!active) return 0f;

            samplesPlayed++;

            // Attack envelope
            float currentGain = amp;
            if (currentAttack < attackSamples) {
                currentAttack += 1.0f;
                currentGain *= (currentAttack / attackSamples);
            }

            // Decay calculation
            if (released) {
                amp *= releaseDecayPerSample;
            } else if (instrument != Instrument.CHURCH_ORGAN && instrument != Instrument.SYNTH_PAD) {
                amp *= decayPerSample;
            }

            if (amp < 0.0003f) {
                active = false;
                return 0f;
            }

            float s = 0f;
            float p1 = phase1;

            switch (instrument) {
                case GRAND_PIANO: {
                    // Fundamental + rich acoustic harmonics with frequency-dependent decay
                    s = 0.65f * fastSin(p1)
                            + 0.25f * fastSin(phase2)
                            + 0.12f * fastSin(phase3)
                            + 0.05f * fastSin(phase4);

                    // Felt hammer strike impulse on attack
                    if (samplesPlayed < 300) {
                        float strike = (1.0f - (samplesPlayed / 300.0f)) * 0.15f;
                        s += strike * ((samplesPlayed % 2 == 0) ? 1f : -1f);
                    }

                    phase1 += phaseInc;
                    phase2 += phaseInc * 2.001f;
                    phase3 += phaseInc * 3.003f;
                    phase4 += phaseInc * 4.006f;
                    break;
                }

                case BRIGHT_PIANO: {
                    // Twin slightly detuned chorused strings
                    s = 0.50f * fastSin(p1)
                            + 0.40f * fastSin(phase2)
                            + 0.18f * fastSin(phase3)
                            + 0.08f * fastSin(phase4);

                    phase1 += phaseInc;
                    phase2 += phaseInc * 1.002f; // subtle unison detune
                    phase3 += phaseInc * 2.0f;
                    phase4 += phaseInc * 3.0f;
                    break;
                }

                case ELECTRIC_PIANO: {
                    // Rhodes tines: core fundamental + bell chime 7th harmonic fading quickly
                    float bellAmp = Math.max(0f, 1.0f - (samplesPlayed / (SAMPLE_RATE * 0.35f))) * 0.35f;
                    s = 0.75f * fastSin(p1)
                            + 0.25f * fastSin(phase2)
                            + bellAmp * fastSin(phase3);

                    phase1 += phaseInc;
                    phase2 += phaseInc * 2.0f;
                    phase3 += phaseInc * 7.0f;
                    break;
                }

                case CHURCH_ORGAN: {
                    // Classic pipe drawbars: 16', 8', 4', 2'
                    s = 0.35f * fastSin(p1)
                            + 0.30f * fastSin(phase2)
                            + 0.20f * fastSin(phase3)
                            + 0.15f * fastSin(phase4);

                    phase1 += phaseInc;
                    phase2 += phaseInc * 0.5f; // sub-octave
                    phase3 += phaseInc * 2.0f;
                    phase4 += phaseInc * 4.0f;
                    break;
                }

                case SYNTH_PAD: {
                    // Warm stereo unison saw simulation with warm low-pass feel
                    s = 0.50f * fastSin(p1)
                            + 0.35f * fastSin(phase2)
                            + 0.15f * fastSin(phase3);

                    phase1 += phaseInc * 0.997f;
                    phase2 += phaseInc * 1.003f;
                    phase3 += phaseInc * 2.001f;
                    break;
                }

                case MUSIC_BOX: {
                    // Pure crystalline chime
                    s = 0.60f * fastSin(p1)
                            + 0.30f * fastSin(phase2)
                            + 0.15f * fastSin(phase3);

                    phase1 += phaseInc;
                    phase2 += phaseInc * 3.0f;
                    phase3 += phaseInc * 8.0f;
                    break;
                }

                case HARPSICHORD: {
                    // Crisp plucked bright timbre
                    s = 0.40f * fastSin(p1)
                            + 0.30f * fastSin(phase2)
                            + 0.20f * fastSin(phase3)
                            + 0.15f * fastSin(phase4)
                            + 0.10f * fastSin(phase5);

                    phase1 += phaseInc;
                    phase2 += phaseInc * 2.0f;
                    phase3 += phaseInc * 3.0f;
                    phase4 += phaseInc * 4.0f;
                    phase5 += phaseInc * 5.0f;
                    break;
                }

                case VIBRAPHONE: {
                    // Mallet tone with 5.5Hz gentle tremolo
                    modPhase += (float) (2.0 * Math.PI * 5.5 / SAMPLE_RATE);
                    float tremolo = 0.8f + 0.2f * fastSin(modPhase);
                    s = (0.75f * fastSin(p1) + 0.25f * fastSin(phase2)) * tremolo;

                    phase1 += phaseInc;
                    phase2 += phaseInc * 4.0f;
                    break;
                }
            }

            return s * currentGain;
        }
    }

    private AudioTrack audioTrack;
    private Thread audioThread;
    private volatile boolean isRunning = false;
    private final Voice[] voices = new Voice[MAX_VOICES];

    private volatile Instrument currentInstrument = Instrument.GRAND_PIANO;
    private volatile boolean sustainEnabled = false;
    private volatile float masterVolume = 0.90f;

    public AudioEngine() {
        for (int i = 0; i < MAX_VOICES; i++) {
            voices[i] = new Voice();
        }
    }

    public synchronized void start() {
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

        audioThread = new Thread(this::renderLoop, "ProPianoAudioThread");
        audioThread.setPriority(Thread.MAX_PRIORITY);
        audioThread.start();
    }

    public synchronized void stop() {
        isRunning = false;
        if (audioThread != null) {
            try {
                audioThread.join(300);
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
        for (Voice v : voices) {
            v.active = false;
        }
    }

    public synchronized void noteOn(int midiNote) {
        if (midiNote < 21 || midiNote > 108) return;

        // Check if note is already active in a voice - retrigger it
        Voice targetVoice = null;
        for (Voice v : voices) {
            if (v.active && v.note == midiNote) {
                targetVoice = v;
                break;
            }
        }

        // If not found, find an inactive voice slot
        if (targetVoice == null) {
            for (Voice v : voices) {
                if (!v.active) {
                    targetVoice = v;
                    break;
                }
            }
        }

        // If all voices full, steal the quietest voice
        if (targetVoice == null) {
            float minAmp = Float.MAX_VALUE;
            for (Voice v : voices) {
                if (v.amp < minAmp) {
                    minAmp = v.amp;
                    targetVoice = v;
                }
            }
        }

        if (targetVoice != null) {
            targetVoice.reset(midiNote, currentInstrument, sustainEnabled);
        }
    }

    public synchronized void noteOff(int midiNote) {
        for (Voice v : voices) {
            if (v.active && v.note == midiNote) {
                v.isHeld = false;
                if (!sustainEnabled) {
                    v.released = true;
                }
            }
        }
    }

    public synchronized void setSustain(boolean enabled) {
        this.sustainEnabled = enabled;
        if (!enabled) {
            for (Voice v : voices) {
                if (v.active && !v.isHeld) {
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

    public void setVolume(float volume) {
        this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public float getVolume() {
        return masterVolume;
    }

    private void renderLoop() {
        short[] pcmBuffer = new short[BUFFER_SIZE];

        while (isRunning) {
            for (int i = 0; i < BUFFER_SIZE; i++) {
                float mixedSample = 0f;

                synchronized (this) {
                    for (Voice v : voices) {
                        if (v.active) {
                            mixedSample += v.nextSample();
                        }
                    }
                }

                // Master gain
                mixedSample *= masterVolume;

                // Soft limiter curve (tanh style) to eliminate harsh digital clipping
                if (mixedSample > 1.2f) mixedSample = 1.0f;
                else if (mixedSample < -1.2f) mixedSample = -1.0f;
                else {
                    // Soft saturation
                    mixedSample = mixedSample - (0.15f * mixedSample * mixedSample * mixedSample);
                }

                // Clamp to 16-bit PCM short range
                if (mixedSample > 1.0f) mixedSample = 1.0f;
                if (mixedSample < -1.0f) mixedSample = -1.0f;

                pcmBuffer[i] = (short) (mixedSample * 32767.0f);
            }

            if (audioTrack != null && isRunning) {
                audioTrack.write(pcmBuffer, 0, BUFFER_SIZE);
            }
        }
    }
}
