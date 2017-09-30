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
package maud.tool;

import com.jme3.input.InputManager;
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
import maud.model.Pov;
import maud.model.option.scene.CameraStatus;
import maud.model.option.scene.OrbitCenter;
import org.lwjgl.input.Mouse;

/**
 * The controller for the "Camera Tool" window in Maud's editor screen. The
 * camera tool controls camera modes used in "scene" views.
 * <p>
 * Maud's cameras are primarily controlled by turning the scroll wheel and
 * dragging with the middle mouse button (MMB). In scene views, there are 2
 * movement modes: "orbit" mode, in which the camera always faces the 3-D
 * cursor, and "fly" mode, in which the camera turns freely.
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
    final private static Logger logger
            = Logger.getLogger(CameraTool.class.getName());
    /**
     * name of the signal that controls camera movement
     */
    final public static String cameraSignalName = "moveCamera";
    /**
     * analog event string to move backward
     */
    final private static String moveBackwardEvent = "pov backward";
    /**
     * analog event string to move forward
     */
    final private static String moveForwardEvent = "pov forward";
    // *************************************************************************
    // fields

    /**
     * the POV that is being dragged, or null for none
     */
    private Pov dragPov = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController (not null)
     */
    CameraTool(BasicScreenController screenController) {
        super(screenController, "cameraTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * If a POV is being dragged, update it.
     */
    public void updatePov() {
        if (signals.test(cameraSignalName)) { // dragging a POV
            if (dragPov == null) { // a brand-new drag
                dragPov = Maud.gui.mousePov();
            } else {
                float dx = Mouse.getDX();
                float dy = Mouse.getDY();
                dragPov.moveUp(-dy / 1024f);
                dragPov.moveLeft(dx / 1024f);
            }
        } else {
            dragPov = null;
        }
    }
    // *************************************************************************
    // AnalogListener methods

    /**
     * Map the mouse wheel.
     */
    public void mapButton() {
        Maud application = Maud.getApplication();
        InputManager inputMgr = application.getInputManager();
        /*
         * Turning the mouse wheel up triggers positive Pov.moveBackward().
         */
        boolean wheelUp = true;
        MouseAxisTrigger backwardTrigger;
        backwardTrigger = new MouseAxisTrigger(MouseInput.AXIS_WHEEL, wheelUp);
        inputMgr.addMapping(moveBackwardEvent, backwardTrigger);
        inputMgr.addListener(this, moveBackwardEvent);
        /*
         * Turning the mouse wheel down triggers negative Pov.moveBackward().
         */
        boolean wheelDown = false;
        MouseAxisTrigger forwardTrigger;
        forwardTrigger = new MouseAxisTrigger(MouseInput.AXIS_WHEEL, wheelDown);
        inputMgr.addMapping(moveForwardEvent, forwardTrigger);
        inputMgr.addListener(this, moveForwardEvent);
    }

    /**
     * Unmap the mouse wheel.
     */
    public void unmapButton() {
        inputManager.deleteMapping(moveForwardEvent);
        inputManager.deleteMapping(moveBackwardEvent);
    }
    // *************************************************************************
    // AnalogListener methods

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

        Pov pov = Maud.gui.mousePov();
        if (pov == null) {
            return;
        }

        switch (eventString) {
            case moveBackwardEvent:
                pov.moveBackward(+amount);
                break;

            case moveForwardEvent:
                pov.moveBackward(-amount);
                break;

            default:
                logger.log(Level.WARNING, "unexpected analog event {0}",
                        MyString.quote(eventString));
        }
    }
    // *************************************************************************
    // AppState methods

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

        CameraStatus status = Maud.getModel().getScene().getCamera();
        boolean parallel = status.isParallelProjection();
        if (parallel) {
            Maud.gui.setRadioButton("parallelRadioButton");
        } else {
            Maud.gui.setRadioButton("perspectiveRadioButton");
        }

        boolean orbit = status.isOrbitMode();
        if (orbit) {
            Maud.gui.setRadioButton("orbitRadioButton");
        } else {
            Maud.gui.setRadioButton("flyRadioButton");
        }

        OrbitCenter orbitCenter = status.getOrbitCenter();
        String ocButton = orbitCenter.toString();
        Maud.gui.setButtonLabel("orbitCenterButton", ocButton);

        Maud.gui.setIgnoreGuiChanges(false);
    }
}
