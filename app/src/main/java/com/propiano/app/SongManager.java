package com.propiano.app;

import java.util.ArrayList;
import java.util.List;

public class SongManager {

    public static class SongNote {
        public final int midiNote;
        public final long durationMs;
        public final long pauseAfterMs;

        public SongNote(int midiNote, long durationMs, long pauseAfterMs) {
            this.midiNote = midiNote;
            this.durationMs = durationMs;
            this.pauseAfterMs = pauseAfterMs;
        }
    }

    public static class Song {
        public final String title;
        public final String composer;
        public final List<SongNote> notes;

        public Song(String title, String composer, List<SongNote> notes) {
            this.title = title;
            this.composer = composer;
            this.notes = notes;
        }
    }

    public static List<Song> getSongs() {
        List<Song> songs = new ArrayList<>();

        // 1. Für Elise - Beethoven
        List<SongNote> furElise = new ArrayList<>();
        // E5, D#5, E5, D#5, E5, B4, D5, C5, A4
        furElise.add(new SongNote(76, 250, 100)); // E5
        furElise.add(new SongNote(75, 250, 100)); // D#5
        furElise.add(new SongNote(76, 250, 100)); // E5
        furElise.add(new SongNote(75, 250, 100)); // D#5
        furElise.add(new SongNote(76, 250, 100)); // E5
        furElise.add(new SongNote(71, 250, 100)); // B4
        furElise.add(new SongNote(74, 250, 100)); // D5
        furElise.add(new SongNote(72, 250, 100)); // C5
        furElise.add(new SongNote(69, 500, 300)); // A4
        // C4, E4, A4, B4
        furElise.add(new SongNote(60, 250, 80));  // C4
        furElise.add(new SongNote(64, 250, 80));  // E4
        furElise.add(new SongNote(69, 250, 80));  // A4
        furElise.add(new SongNote(71, 500, 300)); // B4
        // E4, G#4, B4, C5
        furElise.add(new SongNote(64, 250, 80));  // E4
        furElise.add(new SongNote(68, 250, 80));  // G#4
        furElise.add(new SongNote(71, 250, 80));  // B4
        furElise.add(new SongNote(72, 500, 300)); // C5
        songs.add(new Song("Für Elise", "L. v. Beethoven", furElise));

        // 2. Ode to Joy - Beethoven
        List<SongNote> odeToJoy = new ArrayList<>();
        // E4 E4 F4 G4 G4 F4 E4 D4 C4 C4 D4 E4 E4 D4 D4
        int[] odeNotes = {64, 64, 65, 67, 67, 65, 64, 62, 60, 60, 62, 64, 64, 62, 62};
        long[] odeDur = {300, 300, 300, 300, 300, 300, 300, 300, 300, 300, 300, 450, 180, 450, 600};
        for (int i = 0; i < odeNotes.length; i++) {
            odeToJoy.add(new SongNote(odeNotes[i], odeDur[i], 120));
        }
        songs.add(new Song("Ode to Joy", "L. v. Beethoven", odeToJoy));

        // 3. Twinkle Twinkle Little Star
        List<SongNote> twinkle = new ArrayList<>();
        // C4 C4 G4 G4 A4 A4 G4 F4 F4 E4 E4 D4 D4 C4
        int[] twNotes = {60, 60, 67, 67, 69, 69, 67, 65, 65, 64, 64, 62, 62, 60};
        for (int note : twNotes) {
            twinkle.add(new SongNote(note, 350, 150));
        }
        songs.add(new Song("Twinkle Little Star", "Traditional", twinkle));

        // 4. Canon in D - Pachelbel
        List<SongNote> canon = new ArrayList<>();
        // F#5 E5 D5 C#5 B4 A4 B4 C#5
        int[] cNotes = {74, 73, 71, 69, 67, 65, 67, 69};
        for (int note : cNotes) {
            canon.add(new SongNote(note, 450, 100));
        }
        songs.add(new Song("Canon in D", "J. Pachelbel", canon));

        return songs;
    }
}
