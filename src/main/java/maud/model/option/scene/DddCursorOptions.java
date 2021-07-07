/*
 Copyright (c) 2017-2021, Stephen Gold
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
package maud.model.option.scene;

import com.jme3.math.ColorRGBA;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.MaudUtil;
import maud.action.ActionPrefix;

/**
 * Options for 3-D cursors in scene views.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DddCursorOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DddCursorOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * visibility of the cursor (true &rarr; visible, false &rarr; hidden)
     */
    private boolean visible = true;
    /**
     * colors of the cursor
     */
    private ColorRGBA[] colors = {
        new ColorRGBA(1f, 1f, 0f, 1f),
        new ColorRGBA(0f, 0f, 0f, 1f)
    };
    /**
     * cycle time of the cursor (in seconds, &gt;0)
     */
    private float cycleTime = 2f;
    /**
     * angular size of the cursor (in arbitrary units, &gt;0)
     */
    private float size = 0.2f;
    // *************************************************************************
    // new methods exposed

    /**
     * Copy one of the colors of the cursor.
     *
     * @param index which color to copy (0 or 1)
     * @param storeResult storage for the result (modified if not null)
     * @return color (either storeResult or a new instance)
     */
    public ColorRGBA copyColor(int index, ColorRGBA storeResult) {
        Validate.inRange(index, "index", 0, 1);

        ColorRGBA result
                = (storeResult == null) ? new ColorRGBA() : storeResult;
        result.set(colors[index]);

        return result;
    }

    /**
     * Read the cycle time of the cursor.
     *
     * @return cycle time (in seconds, &gt;0)
     */
    public float getCycleTime() {
        assert cycleTime > 0f : cycleTime;
        return cycleTime;
    }

    /**
     * Read the size of the cursor.
     *
     * @return size (in arbitrary units, &gt;0)
     */
    public float getSize() {
        assert size > 0f : size;
        return size;
    }

    /**
     * Test whether the cursor is visible.
     *
     * @return true if visible, otherwise false
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Alter one of the colors.
     *
     * @param index which color to alter (0 or 1)
     * @param newColor (not null, unaffected)
     */
    public void setColor(int index, ColorRGBA newColor) {
        Validate.inRange(index, "index", 0, 1);
        Validate.nonNull(newColor, "color");

        colors[index].set(newColor);
    }

    /**
     * Alter the cycle time of the cursor.
     *
     * @param newCycleTime (in seconds, &gt;0)
     */
    public void setCycleTime(float newCycleTime) {
        Validate.positive(newCycleTime, "new cycle time");
        cycleTime = newCycleTime;
    }

    /**
     * Alter the size of the cursor.
     *
     * @param newSize (in arbitrary units, &gt;0)
     */
    public void setSize(float newSize) {
        Validate.positive(newSize, "new size");
        size = newSize;
    }

    /**
     * Alter the visibility of the cursor.
     *
     * @param newState true &rarr; visible, false &rarr; hidden
     */
    public void setVisible(boolean newState) {
        visible = newState;
    }

    /**
     * Write the options to a script using the specified writer.
     *
     * @param writer (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    void writeToScript(Writer writer) throws IOException {
        assert writer != null;

        String action = ActionPrefix.sf3DCursorVisible
                + Boolean.toString(visible);
        MaudUtil.writePerformAction(writer, action);

        for (int colorIndex = 0; colorIndex < 2; colorIndex++) {
            action = String.format("%s%d %s", ActionPrefix.set3DCursorColor,
                    colorIndex, colors[colorIndex]);
            MaudUtil.writePerformAction(writer, action);
        }

        action = ActionPrefix.set3DCursorCycleTime + Float.toString(cycleTime);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.set3DCursorSize + Float.toString(size);
        MaudUtil.writePerformAction(writer, action);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public DddCursorOptions clone() throws CloneNotSupportedException {
        DddCursorOptions clone = (DddCursorOptions) super.clone();
        clone.colors = new ColorRGBA[2];
        for (int i = 0; i < colors.length; i++) {
            clone.colors[i] = colors[i].clone();
        }

        return clone;
    }
}
