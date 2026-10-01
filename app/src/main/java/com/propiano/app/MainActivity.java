package com.propiano.app;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AudioEngine audioEngine;
    private PianoKeyboardView pianoView;
    private PianoRecorder recorder;
    private Metronome metronome;

    private Button btnOctaveDown;
    private Button btnOctaveUp;
    private TextView tvCurrentOctave;
    private Button btnInstrument;
    private Button btnSustain;
    private Button btnMetronome;
    private Button btnRecord;
    private Button btnPlayRecord;
    private Button btnSongs;
    private Button btnLabels;

    private int currentOctave = 4; // C4 Middle C
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isPlayingBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_main);

        audioEngine = new AudioEngine();
        audioEngine.start();

        recorder = new PianoRecorder();
        metronome = new Metronome();

        initViews();
        setupPiano();
        setupListeners();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void initViews() {
        pianoView = findViewById(R.id.pianoKeyboardView);
        btnOctaveDown = findViewById(R.id.btnOctaveDown);
        btnOctaveUp = findViewById(R.id.btnOctaveUp);
        tvCurrentOctave = findViewById(R.id.tvCurrentOctave);
        btnInstrument = findViewById(R.id.btnInstrument);
        btnSustain = findViewById(R.id.btnSustain);
        btnMetronome = findViewById(R.id.btnMetronome);
        btnRecord = findViewById(R.id.btnRecord);
        btnPlayRecord = findViewById(R.id.btnPlayRecord);
        btnSongs = findViewById(R.id.btnSongs);
        btnLabels = findViewById(R.id.btnLabels);
    }

    private void setupPiano() {
        pianoView.setOnKeyListener(new PianoKeyboardView.OnKeyListener() {
            @Override
            public void onKeyDown(int midiNote) {
                audioEngine.noteOn(midiNote);
                recorder.recordNoteOn(midiNote);
            }

            @Override
            public void onKeyUp(int midiNote) {
                audioEngine.noteOff(midiNote);
                recorder.recordNoteOff(midiNote);
            }
        });
    }

    private void setupListeners() {
        // Octave navigation
        btnOctaveDown.setOnClickListener(v -> {
            if (currentOctave > 1) {
                currentOctave--;
                tvCurrentOctave.setText("C" + currentOctave);
                pianoView.scrollToOctave(currentOctave);
            }
        });

        btnOctaveUp.setOnClickListener(v -> {
            if (currentOctave < 7) {
                currentOctave++;
                tvCurrentOctave.setText("C" + currentOctave);
                pianoView.scrollToOctave(currentOctave);
            }
        });

        // Instrument switcher
        btnInstrument.setOnClickListener(v -> showInstrumentDialog());

        // Sustain pedal toggle
        btnSustain.setOnClickListener(v -> {
            boolean current = audioEngine.isSustainEnabled();
            boolean newState = !current;
            audioEngine.setSustain(newState);
            if (newState) {
                btnSustain.setText("PEDAL ON");
                btnSustain.setTextColor(Color.parseColor("#D4AF37"));
            } else {
                btnSustain.setText("PEDAL OFF");
                btnSustain.setTextColor(Color.parseColor("#8E92A4"));
            }
        });

        // Metronome setup
        metronome.setOnBeatListener(beatIndex -> {
            if (beatIndex == 0) {
                btnMetronome.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                btnMetronome.setTextColor(Color.parseColor("#D4AF37"));
            }
            mainHandler.postDelayed(() -> {
                btnMetronome.setTextColor(Color.WHITE);
            }, 100);
        });

        btnMetronome.setOnClickListener(v -> showMetronomeDialog());

        // Recording
        btnRecord.setOnClickListener(v -> {
            if (!recorder.isRecording()) {
                recorder.startRecording();
                btnRecord.setText("⏹ STOP");
                btnRecord.setTextColor(Color.parseColor("#FF5252"));
                Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();
            } else {
                recorder.stopRecording();
                btnRecord.setText("🔴 REC");
                btnRecord.setTextColor(Color.parseColor("#FF5252"));
                Toast.makeText(this, "Recording saved (" + recorder.getEvents().size() + " events)", Toast.LENGTH_SHORT).show();
            }
        });

        // Playback recording
        btnPlayRecord.setOnClickListener(v -> {
            if (isPlayingBack) return;
            if (!recorder.hasRecording()) {
                Toast.makeText(this, "No recording yet. Tap REC to record!", Toast.LENGTH_SHORT).show();
                return;
            }
            playRecordedTrack();
        });

        // Tutorial Songs
        btnSongs.setOnClickListener(v -> showSongsDialog());

        // Labels switch
        btnLabels.setOnClickListener(v -> {
            PianoKeyboardView.LabelMode mode = pianoView.getLabelMode();
            if (mode == PianoKeyboardView.LabelMode.NOTE_NAME) {
                pianoView.setLabelMode(PianoKeyboardView.LabelMode.SOLFEGE);
                btnLabels.setText("Do");
            } else if (mode == PianoKeyboardView.LabelMode.SOLFEGE) {
                pianoView.setLabelMode(PianoKeyboardView.LabelMode.NONE);
                btnLabels.setText("Off");
            } else {
                pianoView.setLabelMode(PianoKeyboardView.LabelMode.NOTE_NAME);
                btnLabels.setText("C4");
            }
        });
    }

    private void showInstrumentDialog() {
        AudioEngine.Instrument[] instruments = AudioEngine.Instrument.values();
        String[] titles = new String[instruments.length];
        for (int i = 0; i < instruments.length; i++) {
            titles[i] = instruments[i].title;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Instrument")
                .setItems(titles, (dialog, which) -> {
                    AudioEngine.Instrument selected = instruments[which];
                    audioEngine.setInstrument(selected);
                    btnInstrument.setText("🎹 " + selected.title);
                })
                .show();
    }

    private void showMetronomeDialog() {
        final View dialogView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(200); // 40 + 200 = 240 max
        seekBar.setProgress(metronome.getBpm() - 40);

        final TextView label = new TextView(this);
        label.setText("Tempo: " + metronome.getBpm() + " BPM");
        label.setTextSize(18f);
        label.setPadding(40, 30, 40, 20);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(label);
        layout.addView(seekBar);
        layout.setPadding(30, 20, 30, 20);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int bpm = 40 + progress;
                metronome.setBpm(bpm);
                label.setText("Tempo: " + bpm + " BPM");
                btnMetronome.setText("⏱ " + bpm + " BPM");
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        String toggleText = metronome.isRunning() ? "Stop Metronome" : "Start Metronome";

        new AlertDialog.Builder(this)
                .setTitle("Metronome")
                .setView(layout)
                .setPositiveButton(toggleText, (dialog, which) -> {
                    if (metronome.isRunning()) {
                        metronome.stop();
                        btnMetronome.setText("⏱ " + metronome.getBpm() + " BPM");
                    } else {
                        metronome.start();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showSongsDialog() {
        List<SongManager.Song> songs = SongManager.getSongs();
        String[] titles = new String[songs.size()];
        for (int i = 0; i < songs.size(); i++) {
            titles[i] = songs.get(i).title + " - " + songs.get(i).composer;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select a Song to Learn")
                .setItems(titles, (dialog, which) -> {
                    playSongTutorial(songs.get(which));
                })
                .show();
    }

    private void playSongTutorial(SongManager.Song song) {
        if (isPlayingBack) return;
        isPlayingBack = true;
        Toast.makeText(this, "Playing: " + song.title, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            for (SongManager.SongNote sn : song.notes) {
                if (!isPlayingBack) break;

                // Highlight & press key
                mainHandler.post(() -> {
                    pianoView.highlightSingleNote(sn.midiNote);
                    pianoView.triggerVisualPress(sn.midiNote, true);
                    audioEngine.noteOn(sn.midiNote);
                });

                try {
                    Thread.sleep(sn.durationMs);
                } catch (InterruptedException ignored) {}

                mainHandler.post(() -> {
                    audioEngine.noteOff(sn.midiNote);
                    pianoView.triggerVisualPress(sn.midiNote, false);
                });

                try {
                    Thread.sleep(sn.pauseAfterMs);
                } catch (InterruptedException ignored) {}
            }

            mainHandler.post(() -> {
                pianoView.highlightSingleNote(0);
                isPlayingBack = false;
            });
        }).start();
    }

    private void playRecordedTrack() {
        isPlayingBack = true;
        btnPlayRecord.setText("Playing...");
        List<PianoRecorder.RecordedEvent> events = recorder.getEvents();

        new Thread(() -> {
            long prevTime = 0;
            for (PianoRecorder.RecordedEvent ev : events) {
                if (!isPlayingBack) break;

                long delay = ev.timestampMs - prevTime;
                if (delay > 0) {
                    try {
                        Thread.sleep(Math.min(delay, 4000));
                    } catch (InterruptedException ignored) {}
                }
                prevTime = ev.timestampMs;

                mainHandler.post(() -> {
                    if (ev.isDown) {
                        audioEngine.noteOn(ev.note);
                        pianoView.triggerVisualPress(ev.note, true);
                    } else {
                        audioEngine.noteOff(ev.note);
                        pianoView.triggerVisualPress(ev.note, false);
                    }
                });
            }

            mainHandler.post(() -> {
                btnPlayRecord.setText("▶ PLAY");
                isPlayingBack = false;
            });
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isPlayingBack = false;
        if (metronome != null) metronome.stop();
        if (audioEngine != null) audioEngine.stop();
    }
}
