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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.ui.ActionAppState;

/**
 * Action app state to manage the camera for the Maud application.
 * <p>
 * The camera is primarily controlled by turning the scroll wheel and dragging
 * with the middle mouse button (MMB). There are two modes: "orbit" mode in
 * which the camera stays pointed at a 3D cursor, and "fly" mode in which the
 * camera turns freely. The 3D cursor is controlled by the left mouse button
 * (LMB).
 *
 * @author Stephen Gold sgold@sonic.net
 */
class CameraState
        extends ActionAppState
        implements ActionListener, AnalogListener {
    // *************************************************************************
    // constants and loggers

    /**
     * angular size of the 3D cursor (in arbitrary units, &gt;0)
     */
    final private static float cursorSize = 0.2f;
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
            CameraState.class.getName());
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
    /**
     * name of signal which rotates the model counter-clockwise around +Y
     */
    final public static String modelCCWSignalName = "modelLeft";
    /**
     * name of signal which rotates the model clockwise around +Y
     */
    final public static String modelCWSignalName = "modelRight";
    /**
     * action string to warp the 3D cursor
     */
    final private static String warpCursorAction = "warp cursor";
    // *************************************************************************
    // fields

    /**
     * true &rarr; orbit mode, false &rarr; fly mode
     */
    private boolean orbitMode = false;
    /**
     * true &rarr; show the HUD when this state is enabled
     */
    private boolean showHud = true;
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
    /**
     * indicator for the 3D cursor, set by
     * {@link #initialize(com.jme3.app.state.AppStateManager, com.jme3.app.Application)}
     */
    private Spatial cursor = null;
    // *************************************************************************
    // constructor

    /**
     * Instantiate a disabled state which will be enabled for orbit mode during
     * initialization.
     */
    CameraState() {
        super(false);
    }
    // *************************************************************************
    // new methods exposed

    void cursorSetEnabled(boolean enable) {
        boolean active = (cursor.getParent() != null);
        if (active && !enable) {
            rootNode.detachChild(cursor);
        } else if (!active && enable) {
            rootNode.attachChild(cursor);
        }
    }

    /**
     * Test whether the state is in orbit mode.
     *
     * @return true if in orbit mode, otherwise false
     */
    boolean isOrbitMode() {
        return orbitMode;
    }

    /**
     * Alter the mode of this state.
     *
     * @param newValue true &rarr; orbit mode, false &rarr; fly mode
     */
    void setOrbitMode(boolean newValue) {
        if (!orbitMode && newValue) {
            /*
             * When switching from fly mode to orbit mode, re-aim
             * the camera at the 3D cursor.
             */
            orbitMode = true;
            aim();
        }
        orbitMode = newValue;
    }

    /**
     * Toggle the visibility of the HUD.
     */
    void toggleHud() {
        showHud = !showHud;
        if (isEnabled()) {
            Maud.hudState.setEnabled(showHud);
        }
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
    // ActionAppState methods

    /**
     * Initialize this app state on the 1st update after it gets attached.
     *
     * @param sm application's state manager (not null)
     * @param app application which owns this state (not null)
     */
    @Override
    public void initialize(AppStateManager sm, Application app) {
        super.initialize(sm, app);
        /**
         * Create a white 3D cursor at the origin.
         */
        String assetPath = "Models/indicators/3d cursor/3d cursor.blend";
        cursor = assetManager.loadModel(assetPath);
        Material white = MyAsset.createUnshadedMaterial(assetManager,
                new ColorRGBA(1f, 1f, 1f, 1f));
        cursor.setMaterial(white);

        signals.add(cameraSignalName);
        signals.add(modelCCWSignalName);
        signals.add(modelCWSignalName);

        assert !isEnabled();
        setEnabled(true);
        setOrbitMode(true);
    }

    /**
     * Enable or disable the functionality of this state.
     * <p>
     * The flyby camera provided by SimpleApplication should be disabled before
     * enabling any instance of this state.
     *
     * @param newSetting true &rarr; enable, false &rarr; disable
     */
    @Override
    public void setEnabled(boolean newSetting) {
        if (!isInitialized()) {
            return;
        }
        if (isEnabled() && !newSetting) {
            Maud.hudState.setEnabled(false);
            unmapButton();
            inputManager.deleteMapping(warpCursorAction);

        } else if (!isEnabled() && newSetting) {
            setFrustum();
            Maud.hudState.setEnabled(showHud);
            mapButton();
            /*
             * Clicking the left mouse button warps the 3D cursor.
             */
            MouseButtonTrigger left = new MouseButtonTrigger(
                    MouseInput.BUTTON_LEFT);
            inputManager.addMapping(warpCursorAction, left);
            inputManager.addListener(this, warpCursorAction);
        }

        super.setEnabled(newSetting);
    }

    /**
     * Callback to update this state prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (!orbitMode) {
            /*
             * Calculate the 3D cursor's distance from the camera.
             */
            Vector3f cameraLocation = cam.getLocation();
            Vector3f cursorLocation = MySpatial.getWorldLocation(cursor);
            range = cameraLocation.distance(cursorLocation);
        }
        /*
         * Resize the 3D cursor based on its distance from the camera.
         */
        float newScale = cursorSize * range;
        MySpatial.setWorldScale(cursor, newScale);
        /*
         * Rotate the model around the Y-axis.
         */
        if (signals.test(modelCCWSignalName)) {
            Maud.modelState.rotateY(tpf);
        }
        if (signals.test(modelCWSignalName)) {
            Maud.modelState.rotateY(-tpf);
        }
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process a mouse button action.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        logger.log(Level.INFO, "Got action {0}", MyString.quote(actionString));

        if (ongoing && actionString.equals(warpCursorAction)) {
            warpCursor();
            if (isOrbitMode()) {
                aim();
            }
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
    // private methods

    /**
     * In orbit mode, re-orient the camera to center the 3D cursor in the view
     * port.
     * <p>
     * This method may dolly the camera in or out in order to clamp its distance
     * from the 3D cursor.
     */
    private void aim() {
        assert isOrbitMode();
        /*
         * Calculate the camera's offset relative to the 3D cursor.
         */
        Vector3f location = cam.getLocation().clone();
        Vector3f cursorLocation = MySpatial.getWorldLocation(cursor);
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
     * For the specified camera ray, find the 1st point of contact on a triangle
     * that faces the camera.
     *
     * @param spatial (not null, unaffected)
     * @param ray (not null, unaffected)
     * @return a new vector in world coordinates, or null if none found
     */
    private Vector3f findContact(Spatial spatial, Ray ray) {
        CollisionResults results = new CollisionResults();
        spatial.collideWith(ray, results);
        /*
         * Collision results are sorted by increaing distance from the camera,
         * so the first result is also the nearest one.
         */
        Vector3f cameraLocation = cam.getLocation();
        for (int i = 0; i < results.size(); i++) {
            /*
             * Calculate the offset from the camera to the point of contact.
             */
            CollisionResult result = results.getCollision(i);
            Vector3f contactPoint = result.getContactPoint();
            Vector3f offset = contactPoint.subtract(cameraLocation);
            /*
             * If the dot product of the normal with the offset is negative,
             * then the triangle faces the camera.  Return the point of contact.
             */
            Vector3f normal = result.getContactNormal();
            float dotProduct = offset.dot(normal);
            if (dotProduct < 0f) {
                return contactPoint;
            }
        }
        return null;
    }

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
        if (Maud.hudState.isMouseInsideElement("hud")) {
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
        Vector3f cursorLocation = cursor.getWorldTranslation();
        Vector3f newLocation = cursorLocation.add(offset);
        cam.setLocation(newLocation);

        direction.negateLocal();
        MyCamera.look(cam, direction);
    }

    /**
     * Attempt to warp the 3D cursor to the current screen coordinates of the
     * mouse.
     */
    private void warpCursor() {
        Vector2f mouseXY = inputManager.getCursorPosition();
        /*
         * Convert screen coordinates of mouse to a ray in world coordinates.
         */
        Vector3f vertex = cam.getWorldCoordinates(mouseXY, 0f);
        Vector3f far = cam.getWorldCoordinates(mouseXY, 1f);
        Vector3f direction = far.subtract(vertex);
        direction.normalizeLocal();
        Ray ray = new Ray(vertex, direction);
        /*
         * Trace the ray to the nearest geometry in the model.
         */
        Spatial model = Maud.modelState.getSpatial();
        Vector3f contactPoint = findContact(model, ray);
        if (contactPoint != null) {
            MySpatial.setWorldLocation(cursor, contactPoint);
            return;
        }
        /*
         * The ray missed the model; trace it to the platform instead.
         */
        Spatial platform = MySpatial.findChild(rootNode, Maud.platformName);
        contactPoint = findContact(platform, ray);
        if (contactPoint != null) {
            MySpatial.setWorldLocation(cursor, contactPoint);
        }
    }
}
