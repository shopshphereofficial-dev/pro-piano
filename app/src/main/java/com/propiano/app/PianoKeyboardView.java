package com.propiano.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PianoKeyboardView extends View {

    public interface OnKeyListener {
        void onKeyDown(int midiNote);
        void onKeyUp(int midiNote);
    }

    public enum LabelMode {
        NOTE_NAME, // C4, D4
        SOLFEGE,   // Do, Re
        NONE
    }

    public static class KeyInfo {
        public final int midiNote;
        public final boolean isBlack;
        public final String name;
        public final String solfege;
        public RectF rect = new RectF();

        public KeyInfo(int midiNote, boolean isBlack, String name, String solfege) {
            this.midiNote = midiNote;
            this.isBlack = isBlack;
            this.name = name;
            this.solfege = solfege;
        }
    }

    public static final int MIN_NOTE = 21;  // A0
    public static final int MAX_NOTE = 108; // C8

    private final List<KeyInfo> whiteKeys = new ArrayList<>();
    private final List<KeyInfo> blackKeys = new ArrayList<>();
    private final Map<Integer, KeyInfo> keyMap = new HashMap<>();

    private final Set<Integer> pressedNotes = new HashSet<>();
    private final Set<Integer> highlightedNotes = new HashSet<>();
    private final Map<Integer, Integer> pointerToNote = new HashMap<>();

    private Paint whiteKeyPaint;
    private Paint whiteKeyPressedPaint;
    private Paint blackKeyPaint;
    private Paint blackKeyPressedPaint;
    private Paint highlightPaint;
    private Paint strokePaint;
    private Paint labelPaint;
    private Paint miniMapPaint;
    private Paint miniMapViewportPaint;

    private float scrollX = 0f;
    private float maxScrollX = 0f;
    private float keyWidth = 90f;
    private float blackKeyWidth;
    private float blackKeyHeight;

    private LabelMode labelMode = LabelMode.NOTE_NAME;
    private OnKeyListener keyListener;
    private Vibrator vibrator;
    private boolean hapticsEnabled = true;

    // Mini map bar height at top
    private final float MINI_MAP_HEIGHT = 44f;
    private boolean draggingMiniMap = false;

    public PianoKeyboardView(Context context) {
        super(context);
        init();
    }

    public PianoKeyboardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PianoKeyboardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        buildKeyList();

        whiteKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteKeyPaint.setColor(Color.WHITE);

        whiteKeyPressedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteKeyPressedPaint.setColor(Color.parseColor("#E0DDD5"));

        blackKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackKeyPaint.setColor(Color.parseColor("#18181A"));

        blackKeyPressedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackKeyPressedPaint.setColor(Color.parseColor("#383A4A"));

        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.parseColor("#4CAF50"));

        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.parseColor("#2B2B33"));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2f);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(Color.parseColor("#777788"));
        labelPaint.setTextSize(26f);

        miniMapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        miniMapViewportPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        miniMapViewportPaint.setStyle(Paint.Style.STROKE);
        miniMapViewportPaint.setStrokeWidth(4f);
        miniMapViewportPaint.setColor(Color.parseColor("#D4AF37"));
    }

    private void buildKeyList() {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        String[] solfege = {"Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si"};
        boolean[] isBlack = {false, true, false, true, false, false, true, false, true, false, true, false};

        for (int midi = MIN_NOTE; midi <= MAX_NOTE; midi++) {
            int noteIndex = midi % 12;
            int octave = (midi / 12) - 1;
            boolean black = isBlack[noteIndex];
            String name = noteNames[noteIndex] + octave;
            String solf = solfege[noteIndex];

            KeyInfo key = new KeyInfo(midi, black, name, solf);
            keyMap.put(midi, key);
            if (black) {
                blackKeys.add(key);
            } else {
                whiteKeys.add(key);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutKeys();
    }

    private void layoutKeys() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Show roughly 10 white keys on screen
        keyWidth = Math.max(75f, (float) w / 10.5f);
        blackKeyWidth = keyWidth * 0.58f;
        float keyboardHeight = h - MINI_MAP_HEIGHT;
        blackKeyHeight = keyboardHeight * 0.62f;

        float left = 0;
        for (KeyInfo wk : whiteKeys) {
            wk.rect.set(left, MINI_MAP_HEIGHT, left + keyWidth, h);
            left += keyWidth;
        }

        float totalWhiteWidth = whiteKeys.size() * keyWidth;
        maxScrollX = Math.max(0, totalWhiteWidth - w);

        // Position black keys based on adjacent white keys
        for (KeyInfo bk : blackKeys) {
            int noteInOctave = bk.midiNote % 12;
            // Find white key right before it
            KeyInfo prevWhite = keyMap.get(bk.midiNote - 1);
            if (prevWhite != null) {
                float center = prevWhite.rect.right;
                if (noteInOctave == 1) center = prevWhite.rect.right - 2; // C#
                else if (noteInOctave == 3) center = prevWhite.rect.right + 2; // D#
                else if (noteInOctave == 6) center = prevWhite.rect.right - 3; // F#
                else if (noteInOctave == 10) center = prevWhite.rect.right + 3; // A#

                bk.rect.set(center - blackKeyWidth / 2, MINI_MAP_HEIGHT, center + blackKeyWidth / 2, MINI_MAP_HEIGHT + blackKeyHeight);
            }
        }

        // Default scroll position to Middle C (C4 = 60)
        KeyInfo c4 = keyMap.get(60);
        if (c4 != null && scrollX == 0) {
            scrollX = Math.max(0, Math.min(c4.rect.left - w / 2f + keyWidth / 2, maxScrollX));
        }
    }

    public void scrollToOctave(int octave) {
        int targetMidi = (octave + 1) * 12; // C of that octave
        KeyInfo key = keyMap.get(targetMidi);
        if (key != null) {
            scrollX = Math.max(0, Math.min(key.rect.left - getWidth() / 2f + keyWidth / 2f, maxScrollX));
            invalidate();
        }
    }

    public void setOnKeyListener(OnKeyListener listener) {
        this.keyListener = listener;
    }

    public void setLabelMode(LabelMode mode) {
        this.labelMode = mode;
        invalidate();
    }

    public LabelMode getLabelMode() {
        return labelMode;
    }

    public void setHapticsEnabled(boolean enabled) {
        this.hapticsEnabled = enabled;
    }

    public void setHighlightedNotes(Set<Integer> notes) {
        highlightedNotes.clear();
        if (notes != null) highlightedNotes.addAll(notes);
        invalidate();
    }

    public void highlightSingleNote(int midiNote) {
        highlightedNotes.clear();
        if (midiNote > 0) {
            highlightedNotes.add(midiNote);
            // Auto scroll to bring tutorial note into view if out of sight
            KeyInfo k = keyMap.get(midiNote);
            if (k != null) {
                if (k.rect.left < scrollX + 100 || k.rect.right > scrollX + getWidth() - 100) {
                    scrollX = Math.max(0, Math.min(k.rect.centerX() - getWidth() / 2f, maxScrollX));
                }
            }
        }
        invalidate();
    }

    public void triggerVisualPress(int midiNote, boolean pressed) {
        if (pressed) {
            pressedNotes.add(midiNote);
        } else {
            pressedNotes.remove(midiNote);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        // 1. Draw Mini-Map / Octave strip at top
        drawMiniMap(canvas, w);

        // 2. Draw 88 keys with scrolling
        canvas.save();
        canvas.translate(-scrollX, 0);

        // Draw White Keys
        for (KeyInfo wk : whiteKeys) {
            // Cull offscreen keys
            if (wk.rect.right < scrollX - 50 || wk.rect.left > scrollX + w + 50) continue;

            boolean isPressed = pressedNotes.contains(wk.midiNote);
            boolean isHighlighted = highlightedNotes.contains(wk.midiNote);

            if (isHighlighted) {
                canvas.drawRect(wk.rect, highlightPaint);
            } else if (isPressed) {
                canvas.drawRect(wk.rect, whiteKeyPressedPaint);
            } else {
                canvas.drawRect(wk.rect, whiteKeyPaint);
            }

            // Outline & 3D shadow at bottom of white key
            canvas.drawRect(wk.rect, strokePaint);

            // Note Labels
            if (labelMode != LabelMode.NONE) {
                String label = (labelMode == LabelMode.NOTE_NAME) ? wk.name : wk.solfege;
                canvas.drawText(label, wk.rect.centerX(), wk.rect.bottom - 24f, labelPaint);
            }
        }

        // Draw Black Keys on top
        for (KeyInfo bk : blackKeys) {
            if (bk.rect.right < scrollX - 50 || bk.rect.left > scrollX + w + 50) continue;

            boolean isPressed = pressedNotes.contains(bk.midiNote);
            boolean isHighlighted = highlightedNotes.contains(bk.midiNote);

            if (isHighlighted) {
                canvas.drawRoundRect(bk.rect, 8f, 8f, highlightPaint);
            } else if (isPressed) {
                canvas.drawRoundRect(bk.rect, 8f, 8f, blackKeyPressedPaint);
            } else {
                canvas.drawRoundRect(bk.rect, 8f, 8f, blackKeyPaint);
            }

            canvas.drawRoundRect(bk.rect, 8f, 8f, strokePaint);
        }

        canvas.restore();
    }

    private void drawMiniMap(Canvas canvas, int w) {
        // Background strip for mini map
        miniMapPaint.setColor(Color.parseColor("#15161C"));
        canvas.drawRect(0, 0, w, MINI_MAP_HEIGHT, miniMapPaint);

        float totalPianoWidth = whiteKeys.size() * keyWidth;
        float scale = (float) w / totalPianoWidth;

        // Draw miniature white/black bars
        for (KeyInfo wk : whiteKeys) {
            float mx = wk.rect.left * scale;
            float mw = wk.rect.width() * scale;
            miniMapPaint.setColor(Color.parseColor("#7A7E91"));
            canvas.drawRect(mx, 4, mx + mw, MINI_MAP_HEIGHT - 4, miniMapPaint);
        }
        for (KeyInfo bk : blackKeys) {
            float mx = bk.rect.left * scale;
            float mw = bk.rect.width() * scale;
            miniMapPaint.setColor(Color.parseColor("#0A0B0E"));
            canvas.drawRect(mx, 4, mx + mw, MINI_MAP_HEIGHT * 0.65f, miniMapPaint);
        }

        // Draw viewport rect showing current visible area
        float viewLeft = scrollX * scale;
        float viewRight = (scrollX + w) * scale;
        canvas.drawRoundRect(new RectF(viewLeft, 2, viewRight, MINI_MAP_HEIGHT - 2), 6, 6, miniMapViewportPaint);
    }

    private float lastScrollTouchX = 0f;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        // Handle MiniMap scrub / navigation
        float touchY = event.getY(pointerIndex);
        if (touchY <= MINI_MAP_HEIGHT || draggingMiniMap) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                draggingMiniMap = true;
                float touchX = event.getX(pointerIndex);
                float totalPianoWidth = whiteKeys.size() * keyWidth;
                float scale = (float) getWidth() / totalPianoWidth;
                scrollX = Math.max(0, Math.min((touchX / scale) - getWidth() / 2f, maxScrollX));
                invalidate();
                return true;
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                draggingMiniMap = false;
                return true;
            }
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                float x = event.getX(pointerIndex) + scrollX;
                float y = event.getY(pointerIndex);
                int note = findKeyAt(x, y);
                if (note != -1) {
                    pointerToNote.put(pointerId, note);
                    pressedNotes.add(note);
                    if (keyListener != null) keyListener.onKeyDown(note);
                    doHaptic();
                    invalidate();
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pId = event.getPointerId(i);
                    float x = event.getX(i) + scrollX;
                    float y = event.getY(i);
                    int currentNote = findKeyAt(x, y);
                    Integer oldNote = pointerToNote.get(pId);

                    if (oldNote != null && oldNote != currentNote) {
                        pressedNotes.remove(oldNote);
                        if (keyListener != null) keyListener.onKeyUp(oldNote);
                        if (currentNote != -1) {
                            pointerToNote.put(pId, currentNote);
                            pressedNotes.add(currentNote);
                            if (keyListener != null) keyListener.onKeyDown(currentNote);
                            doHaptic();
                        } else {
                            pointerToNote.remove(pId);
                        }
                        invalidate();
                    } else if (oldNote == null && currentNote != -1) {
                        pointerToNote.put(pId, currentNote);
                        pressedNotes.add(currentNote);
                        if (keyListener != null) keyListener.onKeyDown(currentNote);
                        doHaptic();
                        invalidate();
                    }
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                Integer note = pointerToNote.remove(pointerId);
                if (note != null) {
                    pressedNotes.remove(note);
                    if (keyListener != null) keyListener.onKeyUp(note);
                    invalidate();
                }
                if (action == MotionEvent.ACTION_UP) {
                    draggingMiniMap = false;
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                for (Integer note : pointerToNote.values()) {
                    if (keyListener != null) keyListener.onKeyUp(note);
                }
                pointerToNote.clear();
                pressedNotes.clear();
                draggingMiniMap = false;
                invalidate();
                break;
            }
        }
        return true;
    }

    private int findKeyAt(float x, float y) {
        // Priority to black keys because they sit on top of white keys
        for (KeyInfo bk : blackKeys) {
            if (bk.rect.contains(x, y)) {
                return bk.midiNote;
            }
        }
        for (KeyInfo wk : whiteKeys) {
            if (wk.rect.contains(x, y)) {
                return wk.midiNote;
            }
        }
        return -1;
    }

    private void doHaptic() {
        if (hapticsEnabled && vibrator != null && vibrator.hasVibrator()) {
            try {
                vibrator.vibrate(12);
            } catch (Exception ignored) {}
        }
    }
}
