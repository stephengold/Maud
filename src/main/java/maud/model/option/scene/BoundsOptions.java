/*
 Copyright (c) 2017-2018, Stephen Gold
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
 * Options for bounds visualizations in scene views.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BoundsOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BoundsOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * flag to enable/disable depth test for the visualization
     */
    private boolean depthTestFlag = true;
    /**
     * color of the visualization
     */
    private ColorRGBA color = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * line width for the visualization (in pixels, &ge;0)
     */
    private float lineWidth = 0f;
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the color of the visualization.
     *
     * @param storeResult (modified if not null)
     * @return color (either storeResult or a new instance)
     */
    public ColorRGBA copyColor(ColorRGBA storeResult) {
        if (storeResult == null) {
            storeResult = new ColorRGBA();
        }
        storeResult.set(color);

        return storeResult;
    }

    /**
     * Read the depth-test flag.
     *
     * @return true to enable test, otherwise false
     */
    public boolean getDepthTestFlag() {
        return depthTestFlag;
    }

    /**
     * Read the width of each line.
     *
     * @return width (in pixels, &ge;0)
     */
    public float getLineWidth() {
        assert lineWidth >= 0f : lineWidth;
        return lineWidth;
    }

    /**
     * Alter the color of the visualization.
     *
     * @param newColor (not null, unaffected)
     */
    public void setColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "color");
        color.set(newColor);
    }

    /**
     * Alter the depth-test flag.
     *
     * @param newState true &rarr; enable depth test, false &rarr; no depth test
     */
    public void setDepthTestFlag(boolean newState) {
        depthTestFlag = newState;
    }

    /**
     * Alter the width.
     *
     * @param newWidth line width for axes (in pixels, &ge;0)
     */
    public void setLineWidth(float newWidth) {
        Validate.inRange(newWidth, "new width", 0f, Float.MAX_VALUE);
        lineWidth = newWidth;
    }

    /**
     * Write the options to a script using the specified writer.
     *
     * @param writer (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    void writeToScript(Writer writer) throws IOException {
        Validate.nonNull(writer, "writer");

        String action = ActionPrefix.sfBoundsDepthTest
                + Boolean.toString(depthTestFlag);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setBoundsColor + color.toString();
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setBoundsLineWidth + Float.toString(lineWidth);
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
    public BoundsOptions clone() throws CloneNotSupportedException {
        BoundsOptions clone = (BoundsOptions) super.clone();
        clone.color = color.clone();

        return clone;
    }
}
