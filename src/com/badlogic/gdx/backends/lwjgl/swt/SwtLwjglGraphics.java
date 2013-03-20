package com.badlogic.gdx.backends.lwjgl.swt;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.GLContext;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL11;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GLCommon;

/**
 * @author Michal NkD Nikodim
 *
 */
public class SwtLwjglGraphics implements Graphics {
    static int major, minor;

    private final GLCanvas glCanvas;
    private final AtomicBoolean glCanvasResizeFlag = new AtomicBoolean(false);
    private final BufferFormat bufferFormat;

    private GLCommon gl;
    private LwjglGL10 gl10;
    private LwjglGL11 gl11;
    private LwjglGL20 gl20;

    private String extensions;
    private volatile boolean isContinuous = true;
    private volatile boolean requestRendering = false;

    float deltaTime = 0;
    long frameStart = 0;
    int frames = 0;
    int fps;
    long lastTime = System.nanoTime();

    boolean vsync;

    SwtLwjglGraphics(final Composite parent, final SwtLwjglConfig config) {
        this.vsync = config.vSyncEnabled;

        GLData glData = new GLData();
        glData.redSize = config.r;
        glData.greenSize = config.g;
        glData.blueSize = config.b;
        glData.alphaSize = config.a;
        glData.depthSize = config.depth;
        glData.stencilSize = config.stencil;
        glData.samples = config.samples;
        glData.doubleBuffer = true;
        glCanvas = new GLCanvas(parent, SWT.FLAT, glData);
        if (parent.getLayout() instanceof GridLayout) glCanvas.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        glCanvas.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                glCanvasResizeFlag.set(true);
            }
        });
        setCurrent();
        String version = org.lwjgl.opengl.GL11.glGetString(GL11.GL_VERSION);
        major = Integer.parseInt("" + version.charAt(0));
        minor = Integer.parseInt("" + version.charAt(2));

        if (config.useGL20 && (major >= 2 || version.contains("2.1"))) { // special case for MESA, wtf...
            // FIXME add check whether gl 2.0 is supported
            gl20 = new LwjglGL20();
            gl = gl20;
        } else {
            gl20 = null;
            if (major == 1 && minor < 5) {
                gl10 = new LwjglGL10();
            } else {
                gl11 = new LwjglGL11();
                gl10 = gl11;
            }
            gl = gl10;
        }

        Gdx.gl = gl;
        Gdx.gl10 = gl10;
        Gdx.gl11 = gl11;
        Gdx.gl20 = gl20;

        bufferFormat = new BufferFormat(config.r, config.g, config.b, config.a, config.depth, config.stencil, config.samples, false);

    }

    /**
     * @return the glCanvas
     */
    final GLCanvas getGlCanvas() {
        return glCanvas;
    }

    boolean isResized() {
        return glCanvasResizeFlag.getAndSet(false);
    }

    void setCurrent() {
        if (!glCanvas.isDisposed()) {
            glCanvas.setCurrent();
            try {
                GLContext.useContext(glCanvas);
                Gdx.gl = gl;
                Gdx.gl10 = gl10;
                Gdx.gl11 = gl11;
                Gdx.gl20 = gl20;
            } catch (LWJGLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void swapBuffer() {
        if (!glCanvas.isDisposed()) glCanvas.swapBuffers();
    }

    @Override
    public int getWidth() {
        if (glCanvas.isDisposed()) return 1;
        int w = glCanvas.getSize().x - glCanvas.getBorderWidth() * 2;
        return Math.max(1, w);
    }

    @Override
    public int getHeight() {
        if (glCanvas.isDisposed()) return 1;
        int h = glCanvas.getSize().y - glCanvas.getBorderWidth() * 2;
        return Math.max(1, h);
    }

    @Override
    public float getDeltaTime() {
        return deltaTime;
    }

    @Override
    public float getRawDeltaTime() {
        return deltaTime;
    }

    @Override
    public int getFramesPerSecond() {
        return fps;
    }

    void updateTime() {
        long time = System.nanoTime();
        deltaTime = (time - lastTime) / 1000000000.0f;
        lastTime = time;

        if (time - frameStart >= 1000000000) {
            fps = frames;
            frames = 0;
            frameStart = time;
        }
        frames++;
    }

    @Override
    public GraphicsType getType() {
        return GraphicsType.LWJGL;
    }

    @Override
    public float getPpiX() {
        if (glCanvas.isDisposed()) return 0;
        return glCanvas.getDisplay().getDPI().x;
    }

    @Override
    public float getPpiY() {
        if (glCanvas.isDisposed()) return 0;
        return glCanvas.getDisplay().getDPI().x;
    }

    @Override
    public float getPpcX() {
        return getPpiX() / 2.54f;
    }

    @Override
    public float getPpcY() {
        return getPpiY() / 2.54f;
    }

    @Override
    public float getDensity() {
        return getPpiX() / 160.0f;
    }

    @Override
    public boolean supportsDisplayModeChange() {
        return false;
    }

    @Override
    public DisplayMode[] getDisplayModes() {
        return null;
    }

    @Override
    public DisplayMode getDesktopDisplayMode() {
        return null;
    }

    @Override
    public boolean setDisplayMode(DisplayMode displayMode) {
        return false;
    }

    @Override
    public boolean setDisplayMode(int width, int height, boolean fullscreen) {
        return false;
    }

    @Override
    public void setTitle(String title) {
        //nothing
    }

    @Override
    public void setVSync(boolean vsync) {
        this.vsync = vsync;
    }

    @Override
    public BufferFormat getBufferFormat() {
        return bufferFormat;
    }

    @Override
    public boolean supportsExtension(String extension) {
        if (extensions == null) extensions = Gdx.gl.glGetString(GL10.GL_EXTENSIONS);
        return extensions.contains(extension);
    }

    @Override
    public void setContinuousRendering(boolean isContinuous) {
        this.isContinuous = isContinuous;

    }

    @Override
    public boolean isContinuousRendering() {
        return isContinuous;
    }

    @Override
    public void requestRendering() {
        synchronized (this) {
            requestRendering = true;
        }
    }

    public boolean shouldRender() {
        synchronized (this) {
            boolean rq = requestRendering;
            requestRendering = false;
            return rq || isContinuous;
        }
    }

    @Override
    public boolean isFullscreen() {
        return false;
    }

    @Override
    public boolean isGL11Available() {
        return gl11 != null;
    }

    @Override
    public boolean isGL20Available() {
        return gl20 != null;
    }

    @Override
    public GLCommon getGLCommon() {
        return gl;
    }

    @Override
    public GL10 getGL10() {
        return gl10;
    }

    @Override
    public GL11 getGL11() {
        return gl11;
    }

    @Override
    public GL20 getGL20() {
        return gl20;
    }
}
