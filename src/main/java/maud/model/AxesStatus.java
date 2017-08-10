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

import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.Maud;

/**
 * The status of the visible coordinate axes in the Maud application. TODO move
 * drag state out of MVC model
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AxesStatus implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * dummy axis index used to indicate that no axis is being dragged
     */
    final private static int noAxis = -1;
    /**
     * number of coordinate axes
     */
    final private static int numAxes = 3;
    /**
     * index of the last coordinate axes
     */
    final private static int lastAxis = numAxes - 1;
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
     * which direction the drag axis is pointing (true &rarr; away from camera,
     * false &rarr; toward camera)
     */
    private boolean dragFarSide;
    /**
     * CG model for axis dragging (true &rarr; source, false &rarr; target)
     */
    private boolean dragSourceCgm;
    /**
     * line width for the axes (in pixels, &ge;1)
     */
    private float lineWidth = 4f;
    /**
     * index of the axis being dragged (&ge;0, &lt;3) or noAxis
     */
    private int dragAxisIndex = noAxis;
    /**
     * which set of axes is active (either "bone", "model", "none", "spatial",
     * or "world")
     */
    private String mode = "bone";
    // *************************************************************************
    // new methods exposed

    /**
     * Deselect axis dragging.
     */
    public void clearDragAxis() {
        dragAxisIndex = noAxis;
        assert !isDraggingAxis();
    }

    /**
     * Read the depth test flag.
     *
     * @return true to enable test, otherwise false
     */
    public boolean getDepthTestFlag() {
        return depthTestFlag;
    }

    /**
     * Read the index of the axis being dragged.
     *
     * @return axis index (&ge;0, &lt;3)
     */
    public int getDragAxis() {
        assert isDraggingAxis();
        assert dragAxisIndex >= 0 : dragAxisIndex;
        assert dragAxisIndex < numAxes : dragAxisIndex;
        return dragAxisIndex;
    }

    /**
     * Access the CG model whose axes are being dragged.
     *
     * @return the pre-existing instance
     */
    public LoadedCgm getDragCgm() {
        assert isDraggingAxis();
        if (dragSourceCgm) {
            return Maud.model.source;
        } else {
            return Maud.model.target;
        }
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
     * Read the current mode string.
     *
     * @return mode string (not null)
     */
    public String getMode() {
        assert mode != null;
        return mode;
    }

    /**
     * Test whether an axis is selected for dragging.
     *
     * @return true if selected, otherwise false
     */
    public boolean isDraggingAxis() {
        if (dragAxisIndex == noAxis) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the axis being dragged points away from the camera.
     *
     * @return true if pointing away from camera, otherwise false
     */
    public boolean isDraggingFarSide() {
        assert isDraggingAxis();
        return dragFarSide;
    }

    /**
     * Alter the depth-test flag.
     *
     * @param newState true &rarr; enable depth test, false &rarr; no depth test
     */
    public void setDepthTestFlag(boolean newState) {
        this.depthTestFlag = newState;
    }

    /**
     * Start dragging the specified axis.
     *
     * @param axisIndex which axis to drag (&ge;0, &lt;3)
     * @param cgm which CG model (not null)
     * @param farSideFlag true &rarr; drag on the far side of the axis origin,
     * false to drag on near side
     */
    public void setDraggingAxis(int axisIndex, LoadedCgm cgm,
            boolean farSideFlag) {
        Validate.inRange(axisIndex, "axis index", 0, lastAxis);
        Validate.nonNull(cgm, "model");

        dragAxisIndex = axisIndex;
        dragFarSide = farSideFlag;
        if (cgm == Maud.model.source) {
            dragSourceCgm = true;
        } else {
            dragSourceCgm = false;
        }
        assert isDraggingAxis();
    }

    /**
     * Alter the width.
     *
     * @param width line width for axes (in pixels, &ge;1)
     */
    public void setLineWidth(float width) {
        Validate.inRange(width, "width", 1f, Float.MAX_VALUE);
        this.lineWidth = width;
    }

    /**
     * Alter the display mode.
     *
     * @param modeName name of new axes display mode (not null)
     */
    public void setMode(String modeName) {
        Validate.nonNull(modeName, "mode name");

        switch (modeName) {
            case "bone":
            case "model":
            case "none":
            case "spatial":
            case "world":
                mode = modeName;
                break;
            default:
                logger.log(Level.SEVERE, "mode name={0}", modeName);
                throw new IllegalArgumentException("invalid mode name");
        }
    }

    /**
     * Toggle which side the axis being dragged is on.
     */
    public void toggleDragSide() {
        dragFarSide = !dragFarSide;
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
    public AxesStatus clone() throws CloneNotSupportedException {
        AxesStatus clone = (AxesStatus) super.clone();
        return clone;
    }
}
