/*
 Copyright (c) 2017, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud.model;

import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.Maud;

/**
 * The status of the visible coordinate axes in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AxesStatus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            AxesStatus.class.getName());
    // *************************************************************************
    // fields

    /**
     * flag to enable depth test for visibility of the axes
     */
    private boolean depthTestFlag = false;
    /**
     * length of the axes (units depend on mode, &ge;0)
     */
    private float length = 1f;
    /**
     * line width for the axes (in pixels, &ge;1)
     */
    private float width = 1f;
    /**
     * which set of axes is active (either "none", "world", "model", or "bone")
     */
    private String mode = "bone";
    // *************************************************************************
    // new methods exposed

    /**
     * Read the depth test flag.
     *
     * @return true to enable test, otherwise false
     */
    public boolean getDepthTestFlag() {
        return depthTestFlag;
    }

    /**
     * Read the length of each axis.
     *
     * @return length (&ge;0)
     */
    public float getLength() {
        assert length >= 0f : length;
        return length;
    }

    /**
     * Read the current mode string.
     *
     * @return mode string (not null)
     */
    public String getMode() {
        assert mode != null;
        return mode;
    }

    /**
     * Read the width of each line.
     *
     * @return width (in pixels, &ge;1)
     */
    public float getWidth() {
        assert width >= 1f : width;
        return width;
    }

    /**
     * Alter the length, width, and depth test flag (and update the view).
     *
     * @param length length of axes (units depend on mode, &ge;0)
     * @param flag true &rarr; enable depth test, false &rarr; no depth test
     * @param width line width for axes (in pixels, &ge;1)
     */
    public void set(float length, boolean flag, float width) {
        Validate.nonNegative(length, "length");
        Validate.inRange(width, "width", 1f, Float.MAX_VALUE);

        this.length = length;
        this.depthTestFlag = flag;
        this.width = width;

        Maud.gui.axes.updateLabels();
        Maud.gui.axes.updateCGModel();
    }

    /**
     * Alter the display mode (and update the view).
     *
     * @param newMode new value for axes display mode (not null)
     */
    public void setMode(String newMode) {
        assert newMode != null;
        mode = newMode;

        Maud.gui.axes.updateLabels();
        Maud.gui.axes.updateCGModel();
    }
}
