package com.badlogic.gdx.backends.lwjgl.swt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Pool;

/**
 * @author Michal NkD Nikodim
 *
 */
public class SwtInput implements Input {

    private final GLCanvas glCanvas;

    final static class KeyEvent {
        static final int KEY_DOWN = 0;
        static final int KEY_UP = 1;
        static final int KEY_TYPED = 2;

        long timeStamp;
        int type;
        int keyCode;
        char keyChar;
    }

    final static class TouchEvent {
        static final int TOUCH_DOWN = 0;
        static final int TOUCH_UP = 1;
        static final int TOUCH_DRAGGED = 2;
        static final int TOUCH_SCROLLED = 3;
        static final int TOUCH_MOVED = 4;

        long timeStamp;
        int type;
        int x;
        int y;
        int scrollAmount;
        int button;
        int pointer;
    }

    private static final Pool<KeyEvent> usedKeyEvents = new Pool<KeyEvent>(16, 1000) {
        protected KeyEvent newObject() {
            return new KeyEvent();
        }
    };

    private static final Pool<TouchEvent> usedTouchEvents = new Pool<TouchEvent>(16, 1000) {
        protected TouchEvent newObject() {
            return new TouchEvent();
        }
    };

    final List<KeyEvent> keyEvents = new ArrayList<KeyEvent>();
    final List<TouchEvent> touchEvents = new ArrayList<TouchEvent>();
    boolean mousePressed = false;
    int mouseX, mouseY;
    int deltaX, deltaY;
    final Set<Integer> pressedButtons = new HashSet<Integer>();
    final Set<Integer> pressedKeys = new HashSet<Integer>();
    InputProcessor processor;
    char lastKeyCharPressed;
    float keyRepeatTimer;
    long currentEventTimeStamp;
    boolean justTouched = false;

    SwtInput(final GLCanvas glCanvas) {
        this.glCanvas = glCanvas;
        glCanvas.addListener(SWT.MouseWheel, mouseListener);
        glCanvas.addListener(SWT.MouseDown, mouseListener);
        glCanvas.addListener(SWT.MouseUp, mouseListener);
        glCanvas.addListener(SWT.MouseMove, mouseListener);
        glCanvas.addListener(SWT.KeyDown, keyListener);
        glCanvas.addListener(SWT.KeyUp, keyListener);
    }

    @Override
    public float getAccelerometerX() {
        return 0;
    }

    @Override
    public float getAccelerometerY() {
        return 0;
    }

    @Override
    public float getAccelerometerZ() {
        return 0;
    }

    @Override
    public int getX() {
        return mouseX;
    }

    @Override
    public int getX(int pointer) {
        return getX();
    }

    @Override
    public int getDeltaX() {
        return deltaX;
    }

    @Override
    public int getDeltaX(int pointer) {
        return getDeltaX();
    }

    @Override
    public int getY() {
        return mouseY;
    }

    @Override
    public int getY(int pointer) {
        return getY();
    }

    @Override
    public int getDeltaY() {
        return deltaY;
    }

    @Override
    public int getDeltaY(int pointer) {
        return getDeltaY();
    }

    @Override
    public boolean isTouched() {
        return false;
    }

    @Override
    public boolean justTouched() {
        return justTouched;
    }

    @Override
    public boolean isTouched(int pointer) {
        return isTouched();
    }

    @Override
    public boolean isButtonPressed(int button) {
        return pressedButtons.contains(button);
    }

    @Override
    public boolean isKeyPressed(int key) {
        return pressedKeys.contains(key);
    }

    @Override
    public void getTextInput(TextInputListener listener, String title, String text) {
        // TODO Auto-generated method stub

    }

    @Override
    public void getPlaceholderTextInput(TextInputListener listener, String title, String placeholder) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setOnscreenKeyboardVisible(boolean visible) {
        // nothing
    }

    @Override
    public void vibrate(int milliseconds) {
        // nothing
    }

    @Override
    public void vibrate(long[] pattern, int repeat) {
        // nothing
    }

    @Override
    public void cancelVibrate() {
        // nothing
    }

    @Override
    public float getAzimuth() {
        return 0;
    }

    @Override
    public float getPitch() {
        return 0;
    }

    @Override
    public float getRoll() {
        return 0;
    }

    @Override
    public void getRotationMatrix(float[] matrix) {
        //nothing
    }

    @Override
    public long getCurrentEventTime() {
        return currentEventTimeStamp;
    }

    @Override
    public void setCatchBackKey(boolean catchBack) {
        //nothing
    }

    @Override
    public void setCatchMenuKey(boolean catchMenu) {
        //nothing
    }

    @Override
    public void setInputProcessor(InputProcessor processor) {
        this.processor = processor;

    }

    @Override
    public InputProcessor getInputProcessor() {
        return processor;
    }

    @Override
    public boolean isPeripheralAvailable(Peripheral peripheral) {
        if (peripheral == Peripheral.HardwareKeyboard) return true;
        return false;
    }

    @Override
    public int getRotation() {
        return 0;
    }

    @Override
    public Orientation getNativeOrientation() {
        return Orientation.Landscape;
    }

    @Override
    public void setCursorCatched(boolean catched) {
        // nothing
    }

    @Override
    public boolean isCursorCatched() {
        return false;
    }

    @Override
    public void setCursorPosition(int x, int y) {
        Display dis = glCanvas.getDisplay();
        Shell s = glCanvas.getShell();
        if (dis != null && s != null) {
            dis.setCursorLocation(s.getLocation().x + x, s.getLocation().y + y);
        }
    }

    void processEvents() {
        synchronized (this) {
            if (processor != null) {
                InputProcessor processor = this.processor;
                int len = keyEvents.size();
                for (int i = 0; i < len; i++) {
                    KeyEvent e = keyEvents.get(i);
                    currentEventTimeStamp = e.timeStamp;
                    switch (e.type) {
                        case KeyEvent.KEY_DOWN:
                            processor.keyDown(e.keyCode);
                            break;
                        case KeyEvent.KEY_UP:
                            processor.keyUp(e.keyCode);
                            break;
                        case KeyEvent.KEY_TYPED:
                            processor.keyTyped(e.keyChar);
                    }
                    usedKeyEvents.free(e);
                }

                len = touchEvents.size();
                for (int i = 0; i < len; i++) {
                    TouchEvent e = touchEvents.get(i);
                    currentEventTimeStamp = e.timeStamp;
                    switch (e.type) {
                        case TouchEvent.TOUCH_DOWN:
                            processor.touchDown(e.x, e.y, e.pointer, e.button);
                            break;
                        case TouchEvent.TOUCH_UP:
                            processor.touchUp(e.x, e.y, e.pointer, e.button);
                            break;
                        case TouchEvent.TOUCH_DRAGGED:
                            processor.touchDragged(e.x, e.y, e.pointer);
                            break;
                        case TouchEvent.TOUCH_MOVED:
                            processor.mouseMoved(e.x, e.y);
                            break;
                        case TouchEvent.TOUCH_SCROLLED:
                            processor.scrolled(e.scrollAmount);
                    }
                    usedTouchEvents.free(e);
                }
            } else {
                int len = touchEvents.size();
                for (int i = 0; i < len; i++) {
                    usedTouchEvents.free(touchEvents.get(i));
                }

                len = keyEvents.size();
                for (int i = 0; i < len; i++) {
                    usedKeyEvents.free(keyEvents.get(i));
                }
            }

            keyEvents.clear();
            touchEvents.clear();
        }
    }

    private final Listener keyListener = new Listener() {
        public void handleEvent(Event e) {

            synchronized (SwtInput.this) {
                if (lastKeyCharPressed != 0) {
                    keyRepeatTimer -= Gdx.graphics.getDeltaTime();
                    if (keyRepeatTimer < 0) {
                        keyRepeatTimer = 0.15f;
                        KeyEvent event = usedKeyEvents.obtain();
                        event.keyCode = 0;
                        event.keyChar = lastKeyCharPressed;
                        event.type = KeyEvent.KEY_TYPED;
                        event.timeStamp = TimeUnit.MILLISECONDS.toNanos(e.time);
                        keyEvents.add(event);
                    }
                }

                int keyCode = getGdxKeyCode(e.keyCode, e.keyLocation, e.character);
                long timeStamp = TimeUnit.MILLISECONDS.toNanos(e.time);
                KeyEvent event = usedKeyEvents.obtain();

                switch (e.type) {
                    case SWT.KeyDown:
                        char keyChar = e.character;

                        switch (keyCode) {
                            case Keys.FORWARD_DEL:
                                keyChar = 127;
                                break;
                        }

                        event.keyCode = keyCode;
                        event.keyChar = 0;
                        event.type = KeyEvent.KEY_DOWN;
                        event.timeStamp = timeStamp;
                            keyEvents.add(event);
                        
                        event = usedKeyEvents.obtain();
                        event.keyCode = 0;
                        event.keyChar = keyChar;
                        event.type = KeyEvent.KEY_TYPED;

                        lastKeyCharPressed = keyChar;
                        pressedKeys.add(keyCode);
                        keyRepeatTimer = 0.4f;
                        break;
                    case SWT.KeyUp:
                        event.keyCode = keyCode;
                        event.keyChar = 0;
                        event.type = KeyEvent.KEY_UP;

                        lastKeyCharPressed = 0;
                        if (pressedKeys.contains(keyCode)) pressedKeys.remove(keyCode);
                        break;
                }

                event.timeStamp = timeStamp;
                keyEvents.add(event);
            }
        }

        private int getGdxKeyCode(int swtKeyCode, int location, char character) {
            switch (swtKeyCode) {
                case SWT.KEYPAD_0:
                    return Input.Keys.NUM_0;
                case SWT.KEYPAD_1:
                    return Input.Keys.NUM_1;
                case SWT.KEYPAD_2:
                    return Input.Keys.NUM_2;
                case SWT.KEYPAD_3:
                    return Input.Keys.NUM_3;
                case SWT.KEYPAD_4:
                    return Input.Keys.NUM_4;
                case SWT.KEYPAD_5:
                    return Input.Keys.NUM_5;
                case SWT.KEYPAD_6:
                    return Input.Keys.NUM_6;
                case SWT.KEYPAD_7:
                    return Input.Keys.NUM_7;
                case SWT.KEYPAD_8:
                    return Input.Keys.NUM_8;
                case SWT.KEYPAD_9:
                    return Input.Keys.NUM_9;
                case SWT.ALPHA:
                    switch (character) {
                        case '0':
                            return Input.Keys.NUM_0;
                        case '1':
                            return Input.Keys.NUM_1;
                        case '2':
                            return Input.Keys.NUM_2;
                        case '3':
                            return Input.Keys.NUM_3;
                        case '4':
                            return Input.Keys.NUM_4;
                        case '5':
                            return Input.Keys.NUM_5;
                        case '6':
                            return Input.Keys.NUM_6;
                        case '7':
                            return Input.Keys.NUM_7;
                        case '8':
                            return Input.Keys.NUM_8;
                        case '9':
                            return Input.Keys.NUM_9;
                        case 'a':
                            return Input.Keys.A;
                        case 'b':
                            return Input.Keys.B;
                        case 'c':
                            return Input.Keys.C;
                        case 'd':
                            return Input.Keys.D;
                        case 'e':
                            return Input.Keys.E;
                        case 'f':
                            return Input.Keys.F;
                        case 'g':
                            return Input.Keys.G;
                        case 'h':
                            return Input.Keys.H;
                        case 'i':
                            return Input.Keys.I;
                        case 'j':
                            return Input.Keys.J;
                        case 'k':
                            return Input.Keys.K;
                        case 'l':
                            return Input.Keys.L;
                        case 'm':
                            return Input.Keys.M;
                        case 'n':
                            return Input.Keys.N;
                        case 'o':
                            return Input.Keys.O;
                        case 'p':
                            return Input.Keys.P;
                        case 'q':
                            return Input.Keys.Q;
                        case 'r':
                            return Input.Keys.R;
                        case 's':
                            return Input.Keys.S;
                        case 't':
                            return Input.Keys.T;
                        case 'u':
                            return Input.Keys.U;
                        case 'v':
                            return Input.Keys.V;
                        case 'w':
                            return Input.Keys.W;
                        case 'x':
                            return Input.Keys.X;
                        case 'y':
                            return Input.Keys.Y;
                        case 'z':
                            return Input.Keys.Z;
                        case '/':
                            return Input.Keys.SLASH;
                        case '\\':
                            return Input.Keys.BACKSLASH;
                        case ',':
                            return Input.Keys.COMMA;
                        case ';':
                            return Input.Keys.SEMICOLON;
                        case '+':
                            return Input.Keys.PLUS;
                        case '-':
                            return Input.Keys.MINUS;
                        case '.':
                            return Input.Keys.PERIOD;
                        case '\n':
                            return Input.Keys.ENTER;
                        case '\'':
                            return Input.Keys.APOSTROPHE;
                        case ':':
                            return Input.Keys.COLON;
                        case SWT.SPACE:
                            return Input.Keys.SPACE;
                        case SWT.DEL:
                            if (location == SWT.LEFT)
                                return Input.Keys.FORWARD_DEL;
                            else
                                return Input.Keys.DEL;
                        case SWT.TAB:
                            return Input.Keys.TAB;
                        case SWT.ESC:
                            return Input.Keys.ESCAPE;
                        default:
                            return Input.Keys.UNKNOWN;
                    }
                case SWT.ALT:
                    if (location == SWT.LEFT)
                        return Input.Keys.ALT_LEFT;
                    else
                        return Input.Keys.ALT_RIGHT;
                case SWT.ARROW_LEFT:
                    return Input.Keys.DPAD_LEFT;
                case SWT.ARROW_RIGHT:
                    return Input.Keys.DPAD_RIGHT;
                case SWT.ARROW_UP:
                    return Input.Keys.DPAD_UP;
                case SWT.ARROW_DOWN:
                    return Input.Keys.DPAD_DOWN;
                case SWT.HOME:
                    return Input.Keys.HOME;
                case SWT.SHIFT:
                    if (location == SWT.LEFT)
                        return Input.Keys.SHIFT_LEFT;
                    else
                        return Input.Keys.SHIFT_RIGHT;
                case SWT.CTRL:
                    if (location == SWT.LEFT)
                        return Input.Keys.CONTROL_LEFT;
                    else
                        return Input.Keys.CONTROL_RIGHT;
                case SWT.END:
                    return Input.Keys.END;
                case SWT.INSERT:
                    return Input.Keys.INSERT;
                case SWT.F1:
                    return Input.Keys.F1;
                case SWT.F2:
                    return Input.Keys.F2;
                case SWT.F3:
                    return Input.Keys.F3;
                case SWT.F4:
                    return Input.Keys.F4;
                case SWT.F5:
                    return Input.Keys.F5;
                case SWT.F6:
                    return Input.Keys.F6;
                case SWT.F7:
                    return Input.Keys.F7;
                case SWT.F8:
                    return Input.Keys.F8;
                case SWT.F9:
                    return Input.Keys.F9;
                case SWT.F10:
                    return Input.Keys.F10;
                case SWT.F11:
                    return Input.Keys.F11;
                case SWT.F12:
                    return Input.Keys.F12;
                default:
                    return Input.Keys.UNKNOWN;
            }
        }
    };

    private final Listener mouseListener = new Listener() {
        public void handleEvent(Event e) {

            synchronized (SwtInput.this) {
                TouchEvent event = usedTouchEvents.obtain();
                event.x = e.x;
                event.y = e.y;// Gdx.graphics.getHeight() - e.y - 1;
                event.button = getGdxButton(e.button);
                event.pointer = 0;
                event.timeStamp = TimeUnit.MILLISECONDS.toNanos(e.time);

                switch (e.type) {
                    case SWT.MouseDown:
                        event.type = TouchEvent.TOUCH_DOWN;
                        pressedButtons.add(event.button);
                        justTouched = true;
                        break;
                    case SWT.MouseMove:
                        if (pressedButtons.size() > 0)
                            event.type = TouchEvent.TOUCH_DRAGGED;
                        else
                            event.type = TouchEvent.TOUCH_MOVED;
                        break;
                    case SWT.MouseUp:
                        event.type = TouchEvent.TOUCH_UP;
                        pressedButtons.remove(event.button);
                        break;
                    case SWT.MouseWheel:
                        event.type = TouchEvent.TOUCH_SCROLLED;
                        event.scrollAmount = e.count;
                        break;    
                }
                touchEvents.add(event);
                mouseX = event.x;
                mouseY = event.y;
                deltaX = 0;
                deltaY = 0;
            }
        }

        private int getGdxButton(int button) {
            if (button == 1) return Buttons.LEFT;
            if (button == 3) return Buttons.RIGHT;
            if (button == 2) return Buttons.MIDDLE;
            return -1;
        }

    };

}
