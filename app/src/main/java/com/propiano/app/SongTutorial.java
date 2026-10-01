package com.propiano.app;

import java.util.ArrayList;
import java.util.List;

public class SongTutorial {

    public static class SongNote {
        public final int midiNote;
        public final long durationMs;

        public SongNote(int midiNote, long durationMs) {
            this.midiNote = midiNote;
            this.durationMs = durationMs;
        }
    }

    public static class Song {
        public final String title;
        public final String composer;
        public final String difficulty;
        public final List<SongNote> notes;

        public Song(String title, String composer, String difficulty, List<SongNote> notes) {
            this.title = title;
            this.composer = composer;
            this.difficulty = difficulty;
            this.notes = notes;
        }
    }

    public static List<Song> getBuiltInSongs() {
        List<Song> list = new ArrayList<>();

        // 1. Für Elise (Beethoven)
        List<SongNote> furElise = new ArrayList<>();
        int E5 = 76, Ds5 = 75, D5 = 74, C5 = 72, B4 = 71, A4 = 69, C4 = 60, E4 = 64, Gs4 = 68;
        furElise.add(new SongNote(E5, 300));
        furElise.add(new SongNote(Ds5, 300));
        furElise.add(new SongNote(E5, 300));
        furElise.add(new SongNote(Ds5, 300));
        furElise.add(new SongNote(E5, 300));
        furElise.add(new SongNote(B4, 300));
        furElise.add(new SongNote(D5, 300));
        furElise.add(new SongNote(C5, 300));
        furElise.add(new SongNote(A4, 600));
        furElise.add(new SongNote(C4, 300));
        furElise.add(new SongNote(E4, 300));
        furElise.add(new SongNote(A4, 300));
        furElise.add(new SongNote(B4, 600));
        furElise.add(new SongNote(E4, 300));
        furElise.add(new SongNote(Gs4, 300));
        furElise.add(new SongNote(B4, 300));
        furElise.add(new SongNote(C5, 600));
        list.add(new Song("Für Elise", "L. v. Beethoven", "Intermediate", furElise));

        // 2. Ode to Joy (Beethoven)
        List<SongNote> odeToJoy = new ArrayList<>();
        int E4_n = 64, F4 = 65, G4 = 67, D4 = 62;
        odeToJoy.add(new SongNote(E4_n, 400));
        odeToJoy.add(new SongNote(E4_n, 400));
        odeToJoy.add(new SongNote(F4, 400));
        odeToJoy.add(new SongNote(G4, 400));
        odeToJoy.add(new SongNote(G4, 400));
        odeToJoy.add(new SongNote(F4, 400));
        odeToJoy.add(new SongNote(E4_n, 400));
        odeToJoy.add(new SongNote(D4, 400));
        odeToJoy.add(new SongNote(C4, 400));
        odeToJoy.add(new SongNote(C4, 400));
        odeToJoy.add(new SongNote(D4, 400));
        odeToJoy.add(new SongNote(E4_n, 400));
        odeToJoy.add(new SongNote(E4_n, 600));
        odeToJoy.add(new SongNote(D4, 200));
        odeToJoy.add(new SongNote(D4, 800));
        list.add(new Song("Ode to Joy", "L. v. Beethoven", "Beginner", odeToJoy));

        // 3. Canon in D (Pachelbel)
        List<SongNote> canon = new ArrayList<>();
        int Fs5 = 78, D5_n = 74, B4_n = 71, A4_n = 69, G4_n = 67;
        canon.add(new SongNote(Fs5, 500));
        canon.add(new SongNote(E5, 500));
        canon.add(new SongNote(D5_n, 500));
        canon.add(new SongNote(C5, 500));
        canon.add(new SongNote(B4_n, 500));
        canon.add(new SongNote(A4_n, 500));
        canon.add(new SongNote(B4_n, 500));
        canon.add(new SongNote(C5, 500));
        canon.add(new SongNote(D5_n, 500));
        canon.add(new SongNote(A4_n, 500));
        canon.add(new SongNote(B4_n, 500));
        canon.add(new SongNote(Fs4, 500));
        canon.add(new SongNote(G4_n, 500));
        canon.add(new SongNote(D4, 500));
        canon.add(new SongNote(G4_n, 500));
        canon.add(new SongNote(A4_n, 500));
        list.add(new Song("Canon in D", "J. Pachelbel", "Intermediate", canon));

        // 4. Moonlight Sonata (Beethoven)
        List<SongNote> moonlight = new ArrayList<>();
        int Cs4 = 61, E4_s = 64, Gs4_s = 68, Cs5 = 73;
        moonlight.add(new SongNote(Cs4, 350));
        moonlight.add(new SongNote(E4_s, 350));
        moonlight.add(new SongNote(Gs4_s, 350));
        moonlight.add(new SongNote(Cs4, 350));
        moonlight.add(new SongNote(E4_s, 350));
        moonlight.add(new SongNote(Gs4_s, 350));
        moonlight.add(new SongNote(Cs4, 350));
        moonlight.add(new SongNote(E4_s, 350));
        moonlight.add(new SongNote(Gs4_s, 350));
        moonlight.add(new SongNote(Cs5, 800));
        list.add(new Song("Moonlight Sonata", "L. v. Beethoven", "Intermediate", moonlight));

        // 5. Twinkle Twinkle Little Star
        List<SongNote> twinkle = new ArrayList<>();
        twinkle.add(new SongNote(C4, 400));
        twinkle.add(new SongNote(C4, 400));
        twinkle.add(new SongNote(G4, 400));
        twinkle.add(new SongNote(G4, 400));
        twinkle.add(new SongNote(A4, 400));
        twinkle.add(new SongNote(A4, 400));
        twinkle.add(new SongNote(G4, 800));
        twinkle.add(new SongNote(F4, 400));
        twinkle.add(new SongNote(F4, 400));
        twinkle.add(new SongNote(E4, 400));
        twinkle.add(new SongNote(E4, 400));
        twinkle.add(new SongNote(D4, 400));
        twinkle.add(new SongNote(D4, 400));
        twinkle.add(new SongNote(C4, 800));
        list.add(new Song("Twinkle Little Star", "Traditional", "Easy", twinkle));

        // 6. Happy Birthday
        List<SongNote> bday = new ArrayList<>();
        bday.add(new SongNote(C4, 300));
        bday.add(new SongNote(C4, 300));
        bday.add(new SongNote(D4, 500));
        bday.add(new SongNote(C4, 500));
        bday.add(new SongNote(F4, 500));
        bday.add(new SongNote(E4, 900));
        bday.add(new SongNote(C4, 300));
        bday.add(new SongNote(C4, 300));
        bday.add(new SongNote(D4, 500));
        bday.add(new SongNote(C4, 500));
        bday.add(new SongNote(G4, 500));
        bday.add(new SongNote(F4, 900));
        list.add(new Song("Happy Birthday", "Traditional", "Easy", bday));

        return list;
    }

    private static final int Fs4 = 66;
}
