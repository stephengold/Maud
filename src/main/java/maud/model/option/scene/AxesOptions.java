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

import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Options for visible coordinate axes in scene views.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AxesOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AxesOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * effect produced by dragging an axis tip
     */
    private AxesDragEffect dragEffect = AxesDragEffect.Rotate;
    /**
     * which set of axes is visualized
     */
    private AxesMode subject = AxesMode.SelectedBone;
    /**
     * flag to enable the depth test for visibility of the axes
     */
    private boolean depthTestFlag = false;
    /**
     * line width for the arrows (in pixels, &ge;1) or 0 for solid arrows
     */
    private float lineWidth = 0f;
    // *************************************************************************
    // new methods exposed

    /**
     * Read the depth-test flag.
     *
     * @return true to enable test, otherwise false
     */
    public boolean getDepthTestFlag() {
        return depthTestFlag;
    }

    /**
     * Read the drag effect.
     *
     * @return an enum value (not null)
     */
    public AxesDragEffect getDragEffect() {
        assert dragEffect != null;
        return dragEffect;
    }

    /**
     * Read the width of each line.
     *
     * @return width (in pixels, &ge;1) or 0 for solid arrows
     */
    public float getLineWidth() {
        assert lineWidth >= 0f : lineWidth;
        return lineWidth;
    }

    /**
     * Read the current visualization subject. TODO rename getSubject
     *
     * @return an enum value (not null)
     */
    public AxesMode getMode() {
        assert subject != null;
        return subject;
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
     * Alter the drag effect.
     *
     * @param newEffect an enum value (not null)
     */
    public void setDragEffect(AxesDragEffect newEffect) {
        Validate.nonNull(newEffect, "new effect");
        dragEffect = newEffect;
    }

    /**
     * Alter the line width.
     *
     * @param width line width for axes (in pixels, &ge;1) or 0 for solid arrows
     */
    public void setLineWidth(float width) {
        Validate.nonNegative(width, "width");
        lineWidth = width;
    }

    /**
     * Alter the visualization subject. TODO rename setSubject
     *
     * @param newSubject an enum value (not null)
     */
    public void setMode(AxesMode newSubject) {
        Validate.nonNull(newSubject, "new subject");
        subject = newSubject;
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
    public AxesOptions clone() throws CloneNotSupportedException {
        AxesOptions clone = (AxesOptions) super.clone();
        return clone;
    }
}
