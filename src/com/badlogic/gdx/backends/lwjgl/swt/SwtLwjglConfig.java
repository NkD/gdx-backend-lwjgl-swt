package com.badlogic.gdx.backends.lwjgl.swt;

import com.badlogic.gdx.Graphics;

/**
 * @author Michal NkD Nikodim
 *
 */
public class SwtLwjglConfig {

    /** whether to attempt to use OpenGL ES 2.0. Note GL2 may not be available even if this is true. default: false **/
    public boolean useGL20 = true;
    /** number of bits per color channel **/
    public int r = 8, g = 8, b = 8, a = 8;
    /** number of bits for depth and stencil buffer **/
    public int depth = 16, stencil = 0;
    /** number of samples for MSAA **/
    public int samples = 4;
    /** whether to enable vsync, can be changed at runtime via {@link Graphics#setVSync(boolean)} **/
    public boolean vSyncEnabled = true;

    public SwtLwjglConfig() {
        //explicit empty constructor
    }

    public SwtLwjglConfig(final boolean useGL20) {
        this.useGL20 = useGL20;
    }

    public SwtLwjglConfig(final boolean useGL20, final boolean vSyncEnabled) {
        this.useGL20 = useGL20;
        this.vSyncEnabled = vSyncEnabled;
    }
}
