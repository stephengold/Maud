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

import com.jme3.math.ColorRGBA;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * The status of the visible skeleton in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SkeletonStatus implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SkeletonStatus.class.getName());
    // *************************************************************************
    // fields

    /**
     * visibility of the skeleton (true &rarr; visible, false &rarr; hidden)
     */
    private boolean visibleFlag = true;
    /**
     * color of the skeleton
     */
    private ColorRGBA color = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * point size (in pixels, &ge;1)
     */
    private float pointSize = 4f;
    /**
     * line width for connectors (in pixels, &ge;1)
     */
    private float lineWidth = 1f;
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the color of the skeleton.
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
     * Read the width of each line.
     *
     * @return width (in pixels, &ge;1)
     */
    public float getLineWidth() {
        assert lineWidth >= 1f : lineWidth;
        return lineWidth;
    }

    /**
     * Read the size of each point.
     *
     * @return size (in pixels, &ge;1)
     */
    public float getPointSize() {
        assert pointSize >= 1f : pointSize;
        return pointSize;
    }

    /**
     * Test whether the skeleton is visible.
     *
     * @return true if visible, otherwise false
     */
    public boolean isVisible() {
        return visibleFlag;
    }

    /**
     * Alter the color.
     *
     * @param newColor (not null, unaffected)
     */
    public void setColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "color");
        color.set(newColor);
    }

    /**
     * Alter the line width.
     *
     * @param width line width (in pixels, &ge;1)
     */
    public void setLineWidth(float width) {
        Validate.inRange(width, "line width", 1f, Float.MAX_VALUE);
        lineWidth = width;
    }

    /**
     * Alter the point size.
     *
     * @param size point size (in pixels, &ge;1)
     */
    public void setPointSize(float size) {
        Validate.inRange(size, "point size", 1f, Float.MAX_VALUE);
        pointSize = size;
    }

    /**
     * Alter the visibility.
     *
     * @param newSetting true &rarr; visible, false &rarr; hidden
     */
    public void setVisible(boolean newSetting) {
        visibleFlag = newSetting;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        SkeletonStatus clone = (SkeletonStatus) super.clone();
        clone.color = color.clone();

        return clone;
    }
}
