/*
 Copyright (c) 2018, Stephen Gold
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
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import java.util.logging.Logger;
import maud.Maud;

/**
 * Drag state for boundary dragging. Never checkpointed. TODO merge with
 * SceneDrag and ScoreDrag
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Drag {
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
     * Stop dragging the boundary.
     */
    public static void stopDraggingBoundary() {
        isDraggingBoundary = false;
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
}
