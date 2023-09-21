/*
 Copyright (c) 2018-2022, Stephen Gold
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

import com.jme3.input.InputManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import java.util.logging.Logger;
import maud.Maud;
import maud.model.WhichCgm;
import maud.model.cgm.Cgm;

/**
 * Drag state for boundary dragging. Never checkpointed.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class Drag {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Drag.class.getName());
    // *************************************************************************
    // fields

    /**
     * Is boundary dragging active?
     */
    private static boolean isDraggingBoundary;
    /**
     * the C-G model whose gnomon is being dragged, or null for none
     */
    private static WhichCgm dragGnomonCgm = null;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Drag() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Start dragging the boundary.
     */
    static void startDraggingBoundary() {
        isDraggingBoundary = true;
    }

    /**
     * Start dragging the indexed frame in the selected track of the specified
     * C-G model.
     *
     * @param frameIndex which frame to drag
     * @param cgm a C-G model (not null)
     */
    static void startDraggingFrame(int frameIndex, Cgm cgm) {
        // TODO
    }

    /**
     * Start dragging the gnomon associated with the specified C-G model.
     *
     * @param cgm a C-G model (not null)
     */
    static void startDraggingGnomon(Cgm cgm) {
        assert cgm != null;
        dragGnomonCgm = Maud.getModel().whichCgm(cgm);
    }

    /**
     * Stop dragging the boundary.
     */
    public static void stopDraggingBoundary() {
        isDraggingBoundary = false;
    }

    /**
     * Stop dragging the gnomon.
     */
    public static void stopDraggingGnomon() {
        dragGnomonCgm = null;
    }

    /**
     * Update any active boundary drag.
     */
    public static void updateBoundary() {
        if (isDraggingBoundary) {
            Maud application = Maud.getApplication();
            InputManager inputManager = application.getInputManager();
            Vector2f mouseXY = inputManager.getCursorPosition();
            Camera cam = application.getCamera();
            float x = mouseXY.x / cam.getWidth();
            Maud.getModel().getMisc().setXBoundary(x);
        }
    }

    /**
     * Update any active gnomon drag.
     */
    public static void updateGnomon() {
        Cgm cgm = Maud.getModel().getCgm(dragGnomonCgm);
        if (cgm != null) {
            Maud application = Maud.getApplication();
            InputManager inputManager = application.getInputManager();
            Camera camera = cgm.getScoreView().getCamera();
            Vector2f mouseXY = inputManager.getCursorPosition();
            Vector3f world = camera.getWorldCoordinates(mouseXY, 0f);
            float worldX = FastMath.clamp(world.x, 0f, 1f);
            float duration = cgm.getAnimation().duration();
            float newTime = worldX * duration;
            cgm.getPlay().setTime(newTime);
        }
    }
}
