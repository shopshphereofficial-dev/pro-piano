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
        public float left, top, right, bottom;

        public KeyInfo(int midiNote, boolean isBlack, String name, String solfege) {
            this.midiNote = midiNote;
            this.isBlack = isBlack;
            this.name = name;
            this.solfege = solfege;
        }
    }

    public static final int MIN_NOTE = 21;  // A0
    public static final int MAX_NOTE = 108; // C8

    private final List<KeyInfo> allKeys = new ArrayList<>();
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
    private Paint highlightBlackPaint;
    private Paint borderPaint;
    private Paint labelPaint;
    private Paint blackLabelPaint;
    private Paint miniMapBgPaint;
    private Paint miniMapWhitePaint;
    private Paint miniMapBlackPaint;
    private Paint miniMapViewportPaint;
    private Paint middleCDotPaint;

    private float scrollX = 0f;
    private float maxScrollX = 0f;
    private float keyWidth = 105f;
    private float blackKeyWidth;
    private float blackKeyHeight;
    private float miniMapHeight = 40f;

    private LabelMode labelMode = LabelMode.NOTE_NAME;
    private OnKeyListener keyListener;
    private Vibrator vibrator;
    private boolean hapticsEnabled = true;
    private boolean isDraggingMiniMap = false;

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
        whiteKeyPressedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteKeyPressedPaint.setColor(Color.parseColor("#DDD4C4"));

        blackKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackKeyPressedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackKeyPressedPaint.setColor(Color.parseColor("#3C3E52"));

        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.parseColor("#2ECC71")); // Emerald Green

        highlightBlackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightBlackPaint.setColor(Color.parseColor("#27AE60"));

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.parseColor("#1B1C22"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.parseColor("#5A5B6E"));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(26f);
        labelPaint.setFakeBoldText(true);

        blackLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackLabelPaint.setColor(Color.parseColor("#E0E0EE"));
        blackLabelPaint.setTextAlign(Paint.Align.CENTER);
        blackLabelPaint.setTextSize(20f);

        middleCDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        middleCDotPaint.setColor(Color.parseColor("#D4AF37")); // Gold dot for C4

        miniMapBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        miniMapBgPaint.setColor(Color.parseColor("#14151C"));

        miniMapWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        miniMapWhitePaint.setColor(Color.parseColor("#A8ABB8"));

        miniMapBlackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        miniMapBlackPaint.setColor(Color.parseColor("#0C0C0E"));

        miniMapViewportPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        miniMapViewportPaint.setColor(Color.parseColor("#55D4AF37"));
        miniMapViewportPaint.setStyle(Paint.Style.FILL);
    }

    private void buildKeyList() {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        String[] solfegeNames = {"Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si"};
        boolean[] isBlackArray = {false, true, false, true, false, false, true, false, true, false, true, false};

        for (int midi = MIN_NOTE; midi <= MAX_NOTE; midi++) {
            int noteIndex = midi % 12;
            int octave = (midi / 12) - 1;
            boolean isBlack = isBlackArray[noteIndex];
            String name = noteNames[noteIndex] + octave;
            String solfege = solfegeNames[noteIndex];

            KeyInfo key = new KeyInfo(midi, isBlack, name, solfege);
            allKeys.add(key);
            keyMap.put(midi, key);

            if (isBlack) {
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

    public void setKeyWidth(float width) {
        this.keyWidth = Math.max(70f, Math.min(180f, width));
        layoutKeys();
        invalidate();
    }

    public float getKeyWidth() {
        return keyWidth;
    }

    private void layoutKeys() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        miniMapHeight = 38f;
        blackKeyWidth = keyWidth * 0.62f;
        blackKeyHeight = (h - miniMapHeight) * 0.60f;

        // Position white keys side by side
        for (int i = 0; i < whiteKeys.size(); i++) {
            KeyInfo key = whiteKeys.get(i);
            key.left = i * keyWidth;
            key.top = miniMapHeight;
            key.right = key.left + keyWidth;
            key.bottom = h;
        }

        // Position black keys over the boundary between corresponding white keys
        int currentWhiteIndex = 0;
        for (int i = 0; i < allKeys.size(); i++) {
            KeyInfo key = allKeys.get(i);
            if (!key.isBlack) {
                currentWhiteIndex = whiteKeys.indexOf(key);
            } else {
                // Black key sits between (currentWhiteIndex) and (currentWhiteIndex + 1)
                float boundaryX = (currentWhiteIndex + 1) * keyWidth;
                key.left = boundaryX - (blackKeyWidth / 2.0f);
                key.top = miniMapHeight;
                key.right = key.left + blackKeyWidth;
                key.bottom = miniMapHeight + blackKeyHeight;
            }
        }

        float totalKeyboardWidth = whiteKeys.size() * keyWidth;
        maxScrollX = Math.max(0, totalKeyboardWidth - w);

        // Clamp current scroll
        if (scrollX > maxScrollX) scrollX = maxScrollX;
        if (scrollX < 0) scrollX = 0;
    }

    public void scrollToOctave(int octave) {
        // Find C note for this octave
        int targetMidi = (octave + 1) * 12;
        KeyInfo key = keyMap.get(targetMidi);
        if (key != null) {
            float targetScroll = key.left - (getWidth() / 2f) + (keyWidth / 2f);
            scrollX = Math.max(0f, Math.min(maxScrollX, targetScroll));
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

    public void setHighlightedNote(int midiNote) {
        highlightedNotes.clear();
        if (midiNote >= MIN_NOTE && midiNote <= MAX_NOTE) {
            highlightedNotes.add(midiNote);
            // Auto scroll to target note if outside viewport
            KeyInfo key = keyMap.get(midiNote);
            if (key != null) {
                if (key.left < scrollX || key.right > scrollX + getWidth()) {
                    scrollX = Math.max(0f, Math.min(maxScrollX, key.left - (getWidth() / 2f)));
                }
            }
        }
        invalidate();
    }

    public void clearHighlights() {
        highlightedNotes.clear();
        invalidate();
    }

    public void pressKeyExternal(int midiNote) {
        pressedNotes.add(midiNote);
        invalidate();
    }

    public void releaseKeyExternal(int midiNote) {
        pressedNotes.remove(midiNote);
        invalidate();
    }

    private void vibrate() {
        if (hapticsEnabled && vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(12);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();

        // 1. Handle Mini-Map interaction
        float firstTouchY = event.getY(actionIndex);
        if (firstTouchY <= miniMapHeight || isDraggingMiniMap) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                isDraggingMiniMap = true;
                float touchX = event.getX(actionIndex);
                float fraction = Math.max(0f, Math.min(1f, touchX / (float) getWidth()));
                scrollX = fraction * maxScrollX;
                invalidate();
                return true;
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                isDraggingMiniMap = false;
                return true;
            }
        }

        // 2. Handle Piano Keys Multi-touch
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int pointerId = event.getPointerId(actionIndex);
                float touchX = event.getX(actionIndex);
                float touchY = event.getY(actionIndex);

                int hitNote = getNoteAt(touchX, touchY);
                if (hitNote != -1) {
                    pointerToNote.put(pointerId, hitNote);
                    if (!pressedNotes.contains(hitNote)) {
                        pressedNotes.add(hitNote);
                        vibrate();
                        if (keyListener != null) keyListener.onKeyDown(hitNote);
                    }
                    invalidate();
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                int pointerCount = event.getPointerCount();
                boolean changed = false;

                for (int p = 0; p < pointerCount; p++) {
                    int pointerId = event.getPointerId(p);
                    float touchX = event.getX(p);
                    float touchY = event.getY(p);

                    int newNote = getNoteAt(touchX, touchY);
                    Integer oldNote = pointerToNote.get(pointerId);

                    if (oldNote != null && oldNote != newNote) {
                        // Lift old note if no other finger is pressing it
                        if (!isNoteHeldByOtherPointer(oldNote, pointerId)) {
                            pressedNotes.remove(oldNote);
                            if (keyListener != null) keyListener.onKeyUp(oldNote);
                        }

                        if (newNote != -1) {
                            pointerToNote.put(pointerId, newNote);
                            if (!pressedNotes.contains(newNote)) {
                                pressedNotes.add(newNote);
                                vibrate();
                                if (keyListener != null) keyListener.onKeyDown(newNote);
                            }
                        } else {
                            pointerToNote.remove(pointerId);
                        }
                        changed = true;
                    } else if (oldNote == null && newNote != -1) {
                        pointerToNote.put(pointerId, newNote);
                        if (!pressedNotes.contains(newNote)) {
                            pressedNotes.add(newNote);
                            vibrate();
                            if (keyListener != null) keyListener.onKeyDown(newNote);
                        }
                        changed = true;
                    }
                }

                if (changed) invalidate();
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int pointerId = event.getPointerId(actionIndex);
                Integer releasedNote = pointerToNote.remove(pointerId);

                if (releasedNote != null) {
                    if (!isNoteHeldByOtherPointer(releasedNote, pointerId)) {
                        pressedNotes.remove(releasedNote);
                        if (keyListener != null) keyListener.onKeyUp(releasedNote);
                    }
                    invalidate();
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                for (int note : pressedNotes) {
                    if (keyListener != null) keyListener.onKeyUp(note);
                }
                pointerToNote.clear();
                pressedNotes.clear();
                invalidate();
                break;
            }
        }
        return true;
    }

    private boolean isNoteHeldByOtherPointer(int note, int currentPointerId) {
        for (Map.Entry<Integer, Integer> entry : pointerToNote.entrySet()) {
            if (entry.getKey() != currentPointerId && entry.getValue() == note) {
                return true;
            }
        }
        return false;
    }

    private int getNoteAt(float screenX, float screenY) {
        if (screenY < miniMapHeight) return -1;

        float worldX = screenX + scrollX;
        float worldY = screenY;

        // 1. Black keys have priority since they are in front
        if (worldY <= miniMapHeight + blackKeyHeight) {
            for (KeyInfo bk : blackKeys) {
                if (worldX >= bk.left && worldX <= bk.right && worldY >= bk.top && worldY <= bk.bottom) {
                    return bk.midiNote;
                }
            }
        }

        // 2. White keys
        int whiteIndex = (int) (worldX / keyWidth);
        if (whiteIndex >= 0 && whiteIndex < whiteKeys.size()) {
            return whiteKeys.get(whiteIndex).midiNote;
        }

        return -1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int viewW = getWidth();
        int viewH = getHeight();
        if (viewW == 0 || viewH == 0) return;

        // Draw Mini-Map first at top
        drawMiniMap(canvas, viewW);

        // Save canvas for scrolled piano keys
        canvas.save();
        canvas.translate(-scrollX, 0);

        // Draw White Keys
        for (KeyInfo key : whiteKeys) {
            // Cull offscreen keys
            if (key.right < scrollX || key.left > scrollX + viewW) continue;

            boolean isPressed = pressedNotes.contains(key.midiNote);
            boolean isHighlighted = highlightedNotes.contains(key.midiNote);

            RectF rect = new RectF(key.left, key.top, key.right, key.bottom);

            if (isHighlighted) {
                canvas.drawRoundRect(rect, 4f, 4f, highlightPaint);
            } else if (isPressed) {
                canvas.drawRoundRect(rect, 4f, 4f, whiteKeyPressedPaint);
            } else {
                // Realistic ivory key gradient
                LinearGradient grad = new LinearGradient(
                        key.left, key.top, key.left, key.bottom,
                        Color.parseColor("#FFFFFF"), Color.parseColor("#F4F1EA"),
                        Shader.TileMode.CLAMP
                );
                whiteKeyPaint.setShader(grad);
                canvas.drawRoundRect(rect, 4f, 4f, whiteKeyPaint);
            }

            // Divider border
            canvas.drawRect(rect, borderPaint);

            // Middle C (C4) indicator dot
            if (key.midiNote == 60) {
                canvas.drawCircle(key.left + (keyWidth / 2f), key.bottom - 48f, 6f, middleCDotPaint);
            }

            // Key labels
            if (labelMode != LabelMode.NONE) {
                String text = (labelMode == LabelMode.NOTE_NAME) ? key.name : key.solfege;
                canvas.drawText(text, key.left + (keyWidth / 2f), key.bottom - 18f, labelPaint);
            }
        }

        // Draw Black Keys (on top of white keys)
        for (KeyInfo key : blackKeys) {
            // Cull offscreen keys
            if (key.right < scrollX || key.left > scrollX + viewW) continue;

            boolean isPressed = pressedNotes.contains(key.midiNote);
            boolean isHighlighted = highlightedNotes.contains(key.midiNote);

            RectF rect = new RectF(key.left, key.top, key.right, key.bottom);

            if (isHighlighted) {
                canvas.drawRoundRect(rect, 6f, 6f, highlightBlackPaint);
            } else if (isPressed) {
                canvas.drawRoundRect(rect, 6f, 6f, blackKeyPressedPaint);
            } else {
                LinearGradient grad = new LinearGradient(
                        key.left, key.top, key.left, key.bottom,
                        Color.parseColor("#2B2D38"), Color.parseColor("#101014"),
                        Shader.TileMode.CLAMP
                );
                blackKeyPaint.setShader(grad);
                canvas.drawRoundRect(rect, 6f, 6f, blackKeyPaint);
            }

            // Subtle 3D bottom bevel lip
            canvas.drawRect(key.left, key.bottom - 6f, key.right, key.bottom, borderPaint);

            // Black key labels
            if (labelMode != LabelMode.NONE) {
                String text = (labelMode == LabelMode.NOTE_NAME) ? key.name : key.solfege;
                canvas.drawText(text, key.left + (blackKeyWidth / 2f), key.bottom - 14f, blackLabelPaint);
            }
        }

        canvas.restore();
    }

    private void drawMiniMap(Canvas canvas, int viewW) {
        // Mini Map Background
        canvas.drawRect(0, 0, viewW, miniMapHeight, miniMapBgPaint);

        float miniWhiteW = viewW / (float) whiteKeys.size();
        float miniBlackW = miniWhiteW * 0.70f;
        float miniBlackH = miniMapHeight * 0.60f;

        // Draw miniature white keys
        for (int i = 0; i < whiteKeys.size(); i++) {
            float x1 = i * miniWhiteW;
            float x2 = x1 + miniWhiteW;
            canvas.drawRect(x1 + 0.5f, 2f, x2 - 0.5f, miniMapHeight - 2f, miniMapWhitePaint);
        }

        // Draw miniature black keys
        int currentWhite = 0;
        for (int i = 0; i < allKeys.size(); i++) {
            KeyInfo key = allKeys.get(i);
            if (!key.isBlack) {
                currentWhite = whiteKeys.indexOf(key);
            } else {
                float bx = (currentWhite + 1) * miniWhiteW - (miniBlackW / 2.0f);
                canvas.drawRect(bx, 2f, bx + miniBlackW, miniBlackH, miniMapBlackPaint);
            }
        }

        // Draw Viewport Rect (shows current visible octave area)
        float totalKeyboardWidth = whiteKeys.size() * keyWidth;
        float viewportWidthRatio = Math.min(1.0f, (float) viewW / totalKeyboardWidth);
        float vpW = Math.max(28f, viewW * viewportWidthRatio);

        float scrollFraction = (maxScrollX > 0) ? (scrollX / maxScrollX) : 0f;
        float vpX = scrollFraction * (viewW - vpW);

        RectF vpRect = new RectF(vpX, 1f, vpX + vpW, miniMapHeight - 1f);
        canvas.drawRoundRect(vpRect, 4f, 4f, miniMapViewportPaint);

        // Viewport golden border
        Paint vpBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        vpBorder.setColor(Color.parseColor("#D4AF37"));
        vpBorder.setStyle(Paint.Style.STROKE);
        vpBorder.setStrokeWidth(2f);
        canvas.drawRoundRect(vpRect, 4f, 4f, vpBorder);
    }
}
