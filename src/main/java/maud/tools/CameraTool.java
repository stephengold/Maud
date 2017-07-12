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
package maud.tools;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.LoadedCgm;
import maud.model.Pov;

/**
 * The controller for the "Camera Tool" window in Maud's editor screen. The
 * camera tool controls camera modes used in "scene" views.
 * <p>
 * Maud's cameras are primarily controlled by turning the scroll wheel and
 * dragging with the middle mouse button (MMB). In scene views, there are two
 * modes: "orbit" mode in which the camera points toward the 3D cursor, and
 * "fly" mode in which the camera turns freely.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CameraTool
        extends WindowController
        implements AnalogListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            CameraTool.class.getName());
    /**
     * name of the signal which controls camera movement
     */
    final public static String cameraSignalName = "moveCamera";
    /**
     * analog event string to move backward
     */
    final private static String moveBackwardEvent = "pov backward";
    /**
     * analog event string to move down
     */
    final private static String moveDownEvent = "pov down";
    /**
     * analog event string to move forward
     */
    final private static String moveForwardEvent = "pov forward";
    /**
     * analog event string to move left
     */
    final private static String moveLeftEvent = "pov left";
    /**
     * analog event string to move right
     */
    final private static String moveRightEvent = "pov right";
    /**
     * analog event string to move up
     */
    final private static String moveUpEvent = "pov up";
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    CameraTool(BasicScreenController screenController) {
        super(screenController, "cameraTool", false);
    }
    // *************************************************************************
    // AnalogListener methods

    /**
     * Map the middle mouse button (MMB) and mouse wheel, which together control
     * the camera position.
     */
    void mapButton() {
        /*
         * Turning the mouse wheel up triggers Pov.moveBackward().
         */
        boolean wheelUp = true;
        MouseAxisTrigger backwardTrigger;
        backwardTrigger = new MouseAxisTrigger(MouseInput.AXIS_WHEEL, wheelUp);
        inputManager.addMapping(moveBackwardEvent, backwardTrigger);
        inputManager.addListener(this, moveBackwardEvent);
        /*
         * Turning the mouse wheel down triggers move forward.
         */
        boolean wheelDown = false;
        MouseAxisTrigger forwardTrigger;
        forwardTrigger = new MouseAxisTrigger(MouseInput.AXIS_WHEEL, wheelDown);
        inputManager.addMapping(moveForwardEvent, forwardTrigger);
        inputManager.addListener(this, moveForwardEvent);
        /*
         * Dragging up with MMB triggers move down.
         */
        boolean up = false;
        MouseAxisTrigger downTrigger;
        downTrigger = new MouseAxisTrigger(MouseInput.AXIS_Y, up);
        inputManager.addMapping(moveDownEvent, downTrigger);
        inputManager.addListener(this, moveDownEvent);
        /*
         * Dragging left with MMB triggers move right.
         */
        boolean left = true;
        MouseAxisTrigger leftTrigger;
        leftTrigger = new MouseAxisTrigger(MouseInput.AXIS_X, left);
        inputManager.addMapping(moveRightEvent, leftTrigger);
        inputManager.addListener(this, moveRightEvent);
        /*
         * Dragging right with MMB triggers Pov.moveLeft().
         */
        boolean right = false;
        MouseAxisTrigger rightTrigger;
        rightTrigger = new MouseAxisTrigger(MouseInput.AXIS_X, right);
        inputManager.addMapping(moveLeftEvent, rightTrigger);
        inputManager.addListener(this, moveLeftEvent);
        /*
         * Dragging down with MMB triggers Pov.moveUp().
         */
        boolean down = true;
        MouseAxisTrigger upTrigger;
        upTrigger = new MouseAxisTrigger(MouseInput.AXIS_Y, down);
        inputManager.addMapping(moveUpEvent, upTrigger);
        inputManager.addListener(this, moveUpEvent);
    }

    /**
     * Process an analog event from the mouse.
     *
     * @param eventString textual description of the analog event (not null)
     * @param amount amount of the event (&ge;0)
     * @param ignored time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAnalog(String eventString, float amount, float ignored) {
        Validate.nonNegative(amount, "amount");
        logger.log(Level.FINE, "Received analog event {0} with amount={1}",
                new Object[]{MyString.quote(eventString), amount});

        LoadedCgm cgm = Maud.gui.mouseCgm();
        if (cgm == null) {
            return;
        }
        Pov pov;
        String viewMode = Maud.gui.mouseViewMode();
        if (viewMode.equals("score")) {
            pov = cgm.scorePov;
        } else {
            pov = cgm.scenePov;
        }

        switch (eventString) {
            case moveBackwardEvent:
                pov.moveBackward(+amount);
                break;

            case moveDownEvent:
                if (signals.test(cameraSignalName)) {
                    /* dragging */
                    pov.moveUp(-amount);
                }
                break;

            case moveForwardEvent:
                pov.moveBackward(-amount);
                break;

            case moveLeftEvent:
                if (signals.test(cameraSignalName)) {
                    /* dragging */
                    pov.moveLeft(+amount);
                }
                break;

            case moveRightEvent:
                if (signals.test(cameraSignalName)) {
                    /* dragging */
                    pov.moveLeft(-amount);
                }
                break;

            case moveUpEvent:
                if (signals.test(cameraSignalName)) {
                    /* dragging */
                    pov.moveUp(+amount);
                }
        }
    }

    /**
     * Unmap the middle mouse button (MMB) and mouse wheel, which together
     * control the camera position. TODO when to invoke?
     */
    void unmapButton() {
        inputManager.deleteMapping(moveForwardEvent);
        inputManager.deleteMapping(moveBackwardEvent);
        inputManager.deleteMapping(moveDownEvent);
        inputManager.deleteMapping(moveRightEvent);
        inputManager.deleteMapping(moveLeftEvent);
        inputManager.deleteMapping(moveUpEvent);
    }
    // *************************************************************************
    // AppState methods

    /**
     * Initialize this controller prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application application that owns the window (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);
        assert Maud.gui.tools.getTool("cursor").isInitialized();
        mapButton();
    }

    /**
     * Callback to update this window prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);
        Maud.gui.setIgnoreGuiChanges(true);

        boolean parallel = Maud.model.camera.isParallelProjection();
        if (parallel) {
            Maud.gui.setRadioButton("parallelRadioButton");
        } else {
            Maud.gui.setRadioButton("perspectiveRadioButton");
        }

        boolean orbit = Maud.model.camera.isOrbitMode();
        if (orbit) {
            Maud.gui.setRadioButton("orbitRadioButton");
        } else {
            Maud.gui.setRadioButton("flyRadioButton");
        }

        Maud.gui.setIgnoreGuiChanges(false);
    }
}
