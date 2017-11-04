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
package maud.view;

import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.Maud;
import maud.model.cgm.Cgm;

/**
 * Drag state for scene views. Currently the only scene objects that can be
 * dragged are bone axes and twist axes.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SceneDrag {
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
     * index of last coordinate axes
     */
    final private static int lastAxis = numAxes - 1;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SceneDrag.class.getName());
    // *************************************************************************
    // fields

    /**
     * which direction the dragged axis is pointing (true &rarr; away from
     * camera, false &rarr; toward camera)
     */
    private static boolean dragFarSide;
    /**
     * which CG model is being manipulated (true &rarr; source, false &rarr;
     * target)
     */
    private static boolean dragSourceCgm;
    /**
     * index of the axis being dragged (&ge;0, &lt;numAxes) or noAxis
     */
    private static int dragAxisIndex = noAxis;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SceneDrag() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Terminate axis dragging.
     */
    public static void clearDragAxis() {
        dragAxisIndex = noAxis;
        assert !isDraggingAxis();
    }

    /**
     * Read the index of the axis being dragged.
     *
     * @return axis index (&ge;0, &lt;numAxes)
     */
    public static int getDragAxis() {
        assert isDraggingAxis();
        assert dragAxisIndex >= 0 : dragAxisIndex;
        assert dragAxisIndex < numAxes : dragAxisIndex;
        return dragAxisIndex;
    }

    /**
     * Access the CG model that's being manipulated.
     *
     * @return the pre-existing instance
     */
    public static Cgm getDragCgm() {
        assert isDraggingAxis();
        if (dragSourceCgm) {
            return Maud.getModel().getSource();
        } else {
            return Maud.getModel().getTarget();
        }
    }

    /**
     * Test whether an axis dragging is active.
     *
     * @return true if selected, otherwise false
     */
    public static boolean isDraggingAxis() {
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
    public static boolean isDraggingFarSide() {
        assert isDraggingAxis();
        return dragFarSide;
    }

    /**
     * Start dragging the specified axis.
     *
     * @param axisIndex which axis to drag: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     * @param cgm which CG model (not null)
     * @param farSideFlag true &rarr; drag on the far side of the axis origin,
     * false to drag on near side
     */
    public static void setDraggingAxis(int axisIndex, Cgm cgm,
            boolean farSideFlag) {
        Validate.inRange(axisIndex, "axis index", 0, lastAxis);
        Validate.nonNull(cgm, "model");

        dragAxisIndex = axisIndex;
        dragFarSide = farSideFlag;
        if (cgm == Maud.getModel().getSource()) {
            dragSourceCgm = true;
        } else {
            dragSourceCgm = false;
        }
        assert isDraggingAxis();
    }

    /**
     * Toggle which side the axis being dragged is on.
     */
    public static void toggleDragSide() {
        dragFarSide = !dragFarSide;
    }
}
