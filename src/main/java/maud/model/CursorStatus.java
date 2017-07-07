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
 * The status of the 3D cursor in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CursorStatus implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            CursorStatus.class.getName());
    // *************************************************************************
    // fields

    /**
     * visibility of the cursor (true &rarr; visible, false &rarr; hidden)
     */
    private boolean visible = true;
    /**
     * color of the cursor
     */
    private ColorRGBA color = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * angular size of the cursor (in arbitrary units, &gt;0)
     */
    private float size = 0.2f;
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the color of the cursor.
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
     * Alter the color of the 3D cursor.
     *
     * @param newColor (not null, unaffected)
     */
    public void setColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "color");
        color.set(newColor);
    }

    /**
     * Alter the size of the cursor.
     *
     * @param newSize (in arbitrary units, &ge;0)
     */
    public void setSize(float newSize) {
        Validate.nonNegative(newSize, "size");
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
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public CursorStatus clone() throws CloneNotSupportedException {
        CursorStatus clone = (CursorStatus) super.clone();
        clone.color = color.clone();
        return clone;
    }
}
