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
package maud.model.option.scene;

import com.jme3.math.ColorRGBA;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Options for vertex visualizations in scene views.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class VertexOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            VertexOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * color for selected vertices (default: yellow)
     */
    private ColorRGBA color = new ColorRGBA(1f, 1f, 0f, 1f);
    /**
     * point size for selected vertices (in pixels, &ge;0, 0&rarr;hidden)
     */
    private float pointSize = 20f;
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the color for selected vertices.
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
     * Read the size for selected vertices.
     *
     * @return size (in pixels, &ge;0)
     */
    public float getPointSize() {
        assert pointSize >= 0f : pointSize;
        return pointSize;
    }

    /**
     * Alter the color for selected vertices.
     *
     * @param newColor (not null, unaffected)
     */
    public void setColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "color");
        color.set(newColor);
    }

    /**
     * Alter the point size of the bone heads.
     *
     * @param size point size (in pixels, &ge;0, 0&rarr;hidden)
     */
    public void setPointSize(float size) {
        Validate.nonNegative(size, "size");
        pointSize = size;
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
    public VertexOptions clone() throws CloneNotSupportedException {
        VertexOptions clone = (VertexOptions) super.clone();
        clone.color = color.clone();

        return clone;
    }
}