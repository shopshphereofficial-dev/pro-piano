package com.propiano.app;

import java.util.ArrayList;
import java.util.List;

public class PianoRecorder {
    public static class RecordedEvent {
        public final int note;
        public final boolean isDown; // true = noteOn, false = noteOff
        public final long timestampMs;

        public RecordedEvent(int note, boolean isDown, long timestampMs) {
            this.note = note;
            this.isDown = isDown;
            this.timestampMs = timestampMs;
        }
    }

    private final List<RecordedEvent> events = new ArrayList<>();
    private boolean isRecording = false;
    private long startTimeMs = 0;

    public void startRecording() {
        events.clear();
        isRecording = true;
        startTimeMs = System.currentTimeMillis();
    }

    public void stopRecording() {
        isRecording = false;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void recordNoteOn(int note) {
        if (!isRecording) return;
        long time = System.currentTimeMillis() - startTimeMs;
        events.add(new RecordedEvent(note, true, time));
    }

    public void recordNoteOff(int note) {
        if (!isRecording) return;
        long time = System.currentTimeMillis() - startTimeMs;
        events.add(new RecordedEvent(note, false, time));
    }

    public List<RecordedEvent> getEvents() {
        return new ArrayList<>(events);
    }

    public boolean hasRecording() {
        return !events.isEmpty();
    }

    public void clear() {
        events.clear();
        isRecording = false;
    }
}
