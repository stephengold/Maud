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
package maud;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.model.CameraStatus;

/**
 * The controller for the "Camera Tool" window in Maud's "3D View" screen.
 * <p>
 * The camera is primarily controlled by turning the scroll wheel and dragging
 * with the middle mouse button (MMB). There are two modes: "orbit" mode in
 * which the camera stays pointed at the 3D cursor, and "fly" mode in which the
 * camera turns freely.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class CameraTool
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
    // new methods exposed

    /**
     * Update the JME camera from the MVC model.
     */
    void updateCamera() {
        CameraStatus model = Maud.model.camera;

        float aspectRatio = MyCamera.aspectRatio(cam);
        float yDegrees = model.getFrustumYDegrees();
        float near = model.getFrustumNear();
        float far = model.getFrustumFar();
        cam.setFrustumPerspective(yDegrees, aspectRatio, near, far);

        if (model.isOrbitMode()) {
            model.aim();
        }
        Vector3f location = model.copyLocation(null);
        cam.setLocation(location);
        Quaternion orientation = model.copyOrientation(null);
        cam.setRotation(orientation);
    }
    // *************************************************************************
    // AnalogListener methods

    /**
     * Map the middle mouse button (MMB) and mouse wheel, which together control
     * the camera position.
     */
    void mapButton() {
        /*
         * Turning the mouse wheel up triggers move backward.
         */
        boolean wheelUp = true;
        MouseAxisTrigger backwardTrigger = new MouseAxisTrigger(
                MouseInput.AXIS_WHEEL, wheelUp);
        inputManager.addMapping(moveBackwardEvent, backwardTrigger);
        inputManager.addListener(this, moveBackwardEvent);
        /*
         * Turning the mouse wheel down triggers move forward.
         */
        boolean wheelDown = false;
        MouseAxisTrigger forwardTrigger = new MouseAxisTrigger(
                MouseInput.AXIS_WHEEL, wheelDown);
        inputManager.addMapping(moveForwardEvent, forwardTrigger);
        inputManager.addListener(this, moveForwardEvent);
        /*
         * Dragging up with MMB triggers move down.
         */
        boolean up = false;
        MouseAxisTrigger downTrigger = new MouseAxisTrigger(
                MouseInput.AXIS_Y, up);
        inputManager.addMapping(moveDownEvent, downTrigger);
        inputManager.addListener(this, moveDownEvent);
        /*
         * Dragging left with MMB triggers move right.
         */
        boolean left = true;
        MouseAxisTrigger leftTrigger = new MouseAxisTrigger(
                MouseInput.AXIS_X, left);
        inputManager.addMapping(moveRightEvent, leftTrigger);
        inputManager.addListener(this, moveRightEvent);
        /*
         * Dragging right with MMB triggers move left.
         */
        boolean right = false;
        MouseAxisTrigger rightTrigger = new MouseAxisTrigger(
                MouseInput.AXIS_X, right);
        inputManager.addMapping(moveLeftEvent, rightTrigger);
        inputManager.addListener(this, moveLeftEvent);
        /*
         * Dragging down with MMB triggers move up.
         */
        boolean down = true;
        MouseAxisTrigger upTrigger = new MouseAxisTrigger(
                MouseInput.AXIS_Y, down);
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
                new Object[]{
                    MyString.quote(eventString), amount
                });

        switch (eventString) {
            case moveBackwardEvent:
                Maud.model.camera.moveBackward(+amount);
                break;

            case moveDownEvent:
                if (signals.test(cameraSignalName)) {
                    /* dragging */
                    Maud.model.camera.moveUp(-amount);
                }
                break;

            case moveForwardEvent:
                Maud.model.camera.moveBackward(-amount);
                break;

            case moveLeftEvent:
                if (signals.test(cameraSignalName)) {
                    /* dragging */
                    Maud.model.camera.moveLeft(+amount);
                }
                break;

            case moveRightEvent:
                if (signals.test(cameraSignalName)) {
                    /* dragging */
                    Maud.model.camera.moveLeft(-amount);
                }
                break;

            case moveUpEvent:
                if (signals.test(cameraSignalName)) {
                    /* dragging */
                    Maud.model.camera.moveUp(+amount);
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
        assert Maud.gui.cursor.isInitialized();

        mapButton();

        Vector3f initialLocation = new Vector3f(-2.4f, 1f, 1.6f);
        Maud.model.camera.setLocation(initialLocation);
        Maud.model.camera.setMode("orbit");
        assert Maud.model.camera.isOrbitMode();
    }
}
