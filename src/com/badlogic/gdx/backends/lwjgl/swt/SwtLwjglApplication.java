package com.badlogic.gdx.backends.lwjgl.swt;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl.LwjglClipboard;
import com.badlogic.gdx.backends.lwjgl.LwjglFiles;
import com.badlogic.gdx.backends.lwjgl.LwjglNativesLoader;
import com.badlogic.gdx.backends.lwjgl.LwjglPreferences;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;

/**
 * @author Michal NkD Nikodim
 *
 */
public class SwtLwjglApplication implements Application {

    private static int instanceCount = 0;

    public static SwtLwjglApplication createCanvas(final Composite parent, final ApplicationListener applicationListener, SwtLwjglConfig config) {
        if (instanceCount == 0) {
            LwjglNativesLoader.load();
            Gdx.files = new LwjglFiles();
        }
        instanceCount++;
        return new SwtLwjglApplication(parent, applicationListener, config);
    }

    private final SwtLwjglGraphics graphics;
    private final SwtInput input;
    private final ApplicationListener applicationListener;
    private final Map<String, Preferences> preferences = new HashMap<String, Preferences>();
    private final Clipboard clipboard = new LwjglClipboard();
    private final Array<LifecycleListener> lifecycleListeners = new Array<LifecycleListener>();
    private final Array<Runnable> runnables = new Array<Runnable>();
    private final Array<Runnable> executedRunnables = new Array<Runnable>();

    private int logLevel = LOG_INFO;
    private boolean running = true;

    private SwtLwjglApplication(final Composite parent, final ApplicationListener applicationListener, SwtLwjglConfig config) {
        this.graphics = new SwtLwjglGraphics(parent, config);
        this.input = new SwtInput(graphics.getGlCanvas());
        this.applicationListener = applicationListener;
        Gdx.app = this;
        Gdx.graphics = graphics;
        Gdx.input = input;

        graphics.getGlCanvas().addDisposeListener(new DisposeListener() {
            
            @Override
            public void widgetDisposed(DisposeEvent e) {
                running = false;
                bindAllGdxObjects();
                applicationListener.pause();
                applicationListener.dispose();
                for (LifecycleListener l : lifecycleListeners) {
                    l.pause();
                    l.dispose();
                }
            }
        });
        
        graphics.getGlCanvas().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                bindAllGdxObjects();
                applicationListener.create();
                applicationListener.resize(graphics.getWidth(), graphics.getHeight());
                Gdx.gl.glViewport(0, 0, graphics.getWidth(), graphics.getHeight());
                graphics.isResized(); //clear resized flag
                graphics.requestRendering();
                graphics.getGlCanvas().forceFocus();
            }
        });

        graphics.getGlCanvas().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (running && !graphics.getGlCanvas().isDisposed()) mainLoop();
                if (running && !graphics.getGlCanvas().isDisposed()) graphics.getGlCanvas().getDisplay().asyncExec(this);
            }
        });
    }

    private void mainLoop() {
        boolean shouldRender = false;
        
        bindAllGdxObjects();

        //graphics.setCurrent();
        if (graphics.isResized()) {
            Gdx.gl.glViewport(0, 0, graphics.getWidth(), graphics.getHeight());
            applicationListener.resize(graphics.getWidth(), graphics.getHeight());
            shouldRender = true;
        }

        synchronized (runnables) {
            executedRunnables.clear();
            executedRunnables.addAll(runnables);
            runnables.clear();
        }

        for (int i = 0; i < executedRunnables.size; i++) {
            shouldRender = true;
            executedRunnables.get(i).run(); // calls out to random app code that could do anything ...
        }

        // If one of the runnables set running to false, for example after an exit().
        if (!running) return;

        shouldRender |= graphics.shouldRender();
        input.processEvents();
        if (shouldRender) {
            graphics.updateTime();
            applicationListener.render();
            graphics.swapBuffer();
        }
    }

    private void bindAllGdxObjects(){
        Gdx.app = SwtLwjglApplication.this;
        Gdx.graphics = graphics;
        Gdx.input = input;
        graphics.setCurrent();
    }
    
    @Override
    public ApplicationListener getApplicationListener() {
        return applicationListener;
    }

    @Override
    public Graphics getGraphics() {
        return graphics;
    }

    @Override
    public Audio getAudio() {
        return null;
    }

    @Override
    public Input getInput() {
        return input;
    }

    @Override
    public Files getFiles() {
        return Gdx.files;
    }

    @Override
    public Net getNet() {
        return null;
    }

    @Override
    public ApplicationType getType() {
        return ApplicationType.Desktop;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public long getJavaHeap() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    @Override
    public long getNativeHeap() {
        return getJavaHeap();
    }

    @Override
    public Preferences getPreferences(String name) {
        if (preferences.containsKey(name)) {
            return preferences.get(name);
        } else {
            Preferences prefs = new LwjglPreferences(name);
            preferences.put(name, prefs);
            return prefs;
        }
    }

    @Override
    public Clipboard getClipboard() {
        return clipboard;
    }

    @Override
    public void postRunnable(Runnable runnable) {
        synchronized (runnables) {
            runnables.add(runnable);
            Gdx.graphics.requestRendering();
        }
    }

    @Override
    public void exit() {
        postRunnable(new Runnable() {
            @Override
            public void run() {
                running = false;
            }
        });
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.add(listener);
        }

    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.removeValue(listener, true);
        }

    }

    @Override
    public void debug(String tag, String message) {
        if (logLevel >= LOG_DEBUG) {
            System.out.println(tag + ": " + message);
        }
    }

    @Override
    public void debug(String tag, String message, Throwable exception) {
        if (logLevel >= LOG_DEBUG) {
            System.out.println(tag + ": " + message);
            exception.printStackTrace(System.out);
        }
    }

    public void log(String tag, String message) {
        if (logLevel >= LOG_INFO) {
            System.out.println(tag + ": " + message);
        }
    }

    @Override
    public void log(String tag, String message, Exception exception) {
        if (logLevel >= LOG_INFO) {
            System.out.println(tag + ": " + message);
            exception.printStackTrace(System.out);
        }
    }

    @Override
    public void error(String tag, String message) {
        if (logLevel >= LOG_ERROR) {
            System.err.println(tag + ": " + message);
        }
    }

    @Override
    public void error(String tag, String message, Throwable exception) {
        if (logLevel >= LOG_ERROR) {
            System.err.println(tag + ": " + message);
            exception.printStackTrace(System.err);
        }
    }

    @Override
    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

}
