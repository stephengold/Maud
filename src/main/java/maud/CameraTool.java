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
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;

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
     * rate to dolly in/out (orbit mode only, percentage points per wheel notch)
     */
    final private static float dollyInOutRate = 15f;
    /**
     * distance to the far plane of the view frustum (in world units, &gt;0)
     */
    final private static float frustumFar = 100f;
    /**
     * distance to the near plane of the view frustum (in world units, &gt;0)
     */
    final private static float frustumNear = 0.01f;
    /**
     * vertical angle of the frustum (in degrees of arc, &gt;0)
     */
    final private static float frustumYDegrees = 45f;
    /**
     * Disorientation occurs when the camera looks straight up, so we limit its
     * elevation angle to just under 90 degrees. (orbit mode only, in radians)
     */
    final private static float maxElevationAngle = 1.5f;
    /**
     * minimum elevation angle (orbit mode only, in radians)
     */
    final private static float minElevationAngle = -0.5f;
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
    // fields

    /**
     * true &rarr; orbit mode, false &rarr; fly mode
     */
    private boolean orbitMode = false;
    /**
     * azimuth angle of the camera as seen from the 3D cursor (orbit mode only,
     * in radians east of north)
     */
    private float azimuthAngle = 0f;
    /**
     * elevation angle of the camera as seen from the 3D cursor (orbit mode
     * only, in radians east of north)
     */
    private float elevationAngle = 0f;
    /**
     * movement rate (fly mode only, world units per wheel notch)
     */
    private float flyRate = 0.1f;
    /**
     * maximum distance of camera from the 3D cursor (orbit mode only, in world
     * units, &gt;0)
     */
    private float maxRange = 10f;
    /**
     * minimum distance of camera from the 3D cursor (orbit mode only, in world
     * units, &gt;0)
     */
    private float minRange = 0.2f;
    /**
     * current distance of camera from the 3D cursor (orbit mode only, in world
     * units, &gt;0)
     */
    private float range = maxRange;
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
     * In orbit mode, re-orient the camera to center the 3D cursor in the view
     * port.
     * <p>
     * This method may dolly the camera in or out in order to clamp its distance
     * from the 3D cursor.
     */
    void aim() {
        assert isOrbitMode();
        /*
         * Calculate the camera's offset relative to the 3D cursor.
         */
        Vector3f location = cam.getLocation().clone();
        Vector3f cursorLocation = Maud.gui.cursor.copyWorldLocation();
        Vector3f offset = location.subtract(cursorLocation);
        /*
         * Convert the offset to spherical coordinates.
         */
        elevationAngle = MyVector3f.altitude(offset);
        azimuthAngle = MyVector3f.azimuth(offset);
        range = offset.length();
        /*
         * Limit the range and elevation angle.
         */
        range = FastMath.clamp(range, minRange, maxRange);
        elevationAngle = FastMath.clamp(elevationAngle, minElevationAngle,
                maxElevationAngle);

        updateOrbit();
    }

    /**
     * Test whether the camera is in orbit mode.
     *
     * @return true if in orbit mode, otherwise false
     */
    boolean isOrbitMode() {
        return orbitMode;
    }

    /**
     * Alter the camera mode.
     *
     * @param newSetting true &rarr; orbit mode, false &rarr; fly mode
     */
    void setMode(String newMode) {
        boolean goOrbit;
        switch (newMode) {
            case "fly":
                goOrbit = false;
                break;
            case "orbit":
                goOrbit = true;
                break;
            default:
                logger.log(Level.SEVERE, "newMode={0}",
                        MyString.quote(newMode));
                throw new IllegalArgumentException(
                        "newMode must be \"fly\" or \"orbit\"");
        }

        if (!orbitMode && goOrbit) {
            /*
             * When switching from fly mode to orbit mode, re-aim
             * the camera at the 3D cursor.
             */
            orbitMode = true;
            aim();
        } else if (orbitMode && !goOrbit) {
            orbitMode = false;
        }
        update();
    }

    /**
     * Update after a change.
     */
    void update() {
        if (!orbitMode) {
            /*
             * Calculate the cursor's distance from the camera.
             */
            Vector3f cameraLocation = cam.getLocation();
            Vector3f cursorLocation = Maud.gui.cursor.copyWorldLocation();
            range = cameraLocation.distance(cursorLocation);
        }
        Maud.gui.cursor.update();
    }

    /**
     * Move the camera to a horizontal view.
     */
    public void viewHorizontal() {
        if (orbitMode) {
            elevationAngle = 0f;
            updateOrbit();
        } else {
            Vector3f direction = cam.getDirection().clone();
            direction.y = 0f;
            if (MyVector3f.isZero(direction)) {
                direction.x = 1f;
            } else {
                direction.normalizeLocal();
            }
            MyCamera.look(cam, direction);
        }
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
                new Object[]{
                    MyString.quote(eventString), amount
                });

        switch (eventString) {
            case moveBackwardEvent:
                moveBackward(+amount);
                return;
            case moveDownEvent:
                moveUp(-amount);
                return;
            case moveForwardEvent:
                moveBackward(-amount);
                return;
            case moveLeftEvent:
                moveLeft(+amount);
                return;
            case moveRightEvent:
                moveLeft(-amount);
                return;
            case moveUpEvent:
                moveUp(+amount);
        }
    }
    // *************************************************************************
    // AppState methods

    /**
     * Initialize this controller prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application application which owns the window (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);
        assert Maud.gui.cursor.isInitialized();

        signals.add(cameraSignalName);
        setFrustum();
        mapButton();

        setMode("orbit");
    }
    // *************************************************************************
    // private methods

    /**
     * Map the middle mouse button (MMB) and mouse wheel, which together control
     * the camera position.
     */
    private void mapButton() {
        String actionString = String.format("signal %s 0", cameraSignalName);
        MouseButtonTrigger middle = new MouseButtonTrigger(
                MouseInput.BUTTON_MIDDLE);
        inputManager.addMapping(actionString, middle);
        inputManager.addListener(signals, actionString);
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
     * Move the camera forward or backward when the mouse wheel turns. This
     * results in dollying in or out. Effective only when the mouse cursor is
     * outside the HUD.
     *
     * @param amount positive to dolly out (backward or away from the 3D
     * cursor), negative to dolly in (forward or toward the 3D cursor)
     */
    private void moveBackward(float amount) {
        if (Maud.gui.isMouseInsideElement("hud")) {
            /* not dragging */
            return;
        }

        if (orbitMode) {
            float rate = 1f + dollyInOutRate / 100f;
            float factor = FastMath.pow(rate, amount);
            range = FastMath.clamp(range * factor, minRange, maxRange);
            updateOrbit();

        } else {
            Vector3f direction = cam.getDirection();
            direction.multLocal(amount * flyRate);
            Vector3f location = cam.getLocation().clone();
            location.addLocal(direction);
            cam.setLocation(location);
        }
        update();
    }

    /**
     * Move the camera left or right when the middle mouse button is dragged
     * from side to side. In orbit mode, this involves orbiting the 3D cursor in
     * a horizontal plane. In fly mode, it involves yawing (panning the camera)
     * around the vertical (Y) axis.
     *
     * @param amount positive to orbit right/yaw left/turn left; negative to
     * orbit left/yaw right/turn right
     */
    private void moveLeft(float amount) {
        if (!signals.test(cameraSignalName)) {
            /* not dragging */
            return;
        }

        if (orbitMode) {
            azimuthAngle += 2f * amount;
            azimuthAngle = MyMath.standardizeAngle(azimuthAngle);
            updateOrbit();

        } else {
            Quaternion rotate = new Quaternion();
            rotate.fromAngleAxis(amount, Vector3f.UNIT_Y);
            Quaternion oldRotation = cam.getRotation();
            Quaternion newRotation = rotate.mult(oldRotation);
            cam.setRotation(newRotation);
        }
        update();
    }

    /**
     * Move the camera up or down when the middle mouse button is dragged from
     * side to side. In orbit mode, this involves orbiting the 3D cursor in a
     * vertical plane. In fly mode, it involves pitching (tilting the camera).
     *
     * @param amount positive to orbit up/tilt up/tilt back; negative to orbit
     * down/tilt down/tilt forward
     */
    private void moveUp(float amount) {
        if (!signals.test(cameraSignalName)) {
            /* not dragging */
            return;
        }

        if (orbitMode) {
            elevationAngle += amount;
            elevationAngle = FastMath.clamp(elevationAngle, minElevationAngle,
                    maxElevationAngle);
            updateOrbit();

        } else {
            Quaternion rotate = new Quaternion();
            rotate.fromAngleAxis(-amount, cam.getLeft());
            Quaternion oldRotation = cam.getRotation();
            Quaternion newRotation = rotate.mult(oldRotation);
            cam.setRotation(newRotation);
        }
        update();
    }

    /**
     * Initialize the frustum of the render camera.
     */
    private void setFrustum() {
        float aspectRatio = MyCamera.aspectRatio(cam);
        cam.setFrustumPerspective(frustumYDegrees, aspectRatio,
                frustumNear, frustumFar);
    }

    /**
     * Unmap the middle mouse button (MMB) and mouse wheel, which together
     * control the camera position.
     */
    private void unmapButton() {
        String actionString = String.format("signal %s 0", cameraSignalName);
        inputManager.deleteMapping(actionString);

        inputManager.deleteMapping(moveForwardEvent);
        inputManager.deleteMapping(moveBackwardEvent);
        inputManager.deleteMapping(moveDownEvent);
        inputManager.deleteMapping(moveRightEvent);
        inputManager.deleteMapping(moveLeftEvent);
        inputManager.deleteMapping(moveUpEvent);
    }

    /**
     * In orbit mode, move the camera based on azimuth, elevation, and range.
     */
    private void updateOrbit() {
        assert isOrbitMode();

        Vector3f direction = MyVector3f.fromAltAz(elevationAngle,
                azimuthAngle);
        Vector3f offset = direction.mult(range);
        Vector3f cursorLocation = Maud.gui.cursor.copyWorldLocation();
        Vector3f newLocation = cursorLocation.add(offset);
        cam.setLocation(newLocation);

        direction.negateLocal();
        MyCamera.look(cam, direction);
    }
}
