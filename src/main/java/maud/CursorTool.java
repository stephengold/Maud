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
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.nifty.BasicScreenController;

/**
 * The controller for the "Cursor Tool" window in Maud's "3D View" screen.
 *
 * The left mouse button (LMB) positions the 3D cursor.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class CursorTool
        extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * angular size of the 3D cursor (in arbitrary units, &gt;0)
     */
    final private static float cursorSize = 0.2f;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            CursorTool.class.getName());
    // *************************************************************************
    // fields

    /**
     * indicator for the 3D cursor, set by
     * {@link #initialize(com.jme3.app.state.AppStateManager, com.jme3.app.Application)}
     */
    private Geometry geometry = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    CursorTool(BasicScreenController screenController) {
        super(screenController, "cursorTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     *
     * @return
     */
    Vector3f copyWorldLocation() {
        Vector3f location = geometry.getWorldTranslation();
        return location.clone();
    }

    /**
     * Alter the visibility of the cursor.
     *
     * @param newState true &rarr; visible, false &rarr; hidden
     */
    void setVisible(boolean newState) {
        boolean oldState = (geometry.getParent() != null);
        if (oldState && !newState) {
            rootNode.detachChild(geometry);
        } else if (!oldState && newState) {
            rootNode.attachChild(geometry);
        }
    }

    /**
     * Update the cursor after a change.
     */
    void update() {
        /*
         * visibility
         */
        boolean enable = Maud.gui.isChecked("3DCursor");
        setVisible(enable);
        /*
         * color
         */
        ColorRGBA color = Maud.gui.updateColorBank("cursor");
        Material material = geometry.getMaterial();
        material.setColor("Color", color);
        /*
         * Resize the cursor based on its distance from the camera.
         */
        Vector3f cursorLocation = geometry.getWorldTranslation();
        Vector3f cameraLocation = cam.getLocation();
        float range = cameraLocation.distance(cursorLocation);
        float newScale = cursorSize * range;
        MySpatial.setWorldScale(geometry, newScale);
    }

    /**
     * Attempt to warp the cursor to the screen coordinates of the mouse
     * pointer.
     */
    void warpCursor() {
        Vector2f mouseXY = inputManager.getCursorPosition();
        /*
         * Convert screen coordinates of the mouse pointer to a ray in
         * world coordinates.
         */
        Vector3f vertex = cam.getWorldCoordinates(mouseXY, 0f);
        Vector3f far = cam.getWorldCoordinates(mouseXY, 1f);
        Vector3f direction = far.subtract(vertex);
        direction.normalizeLocal();
        Ray ray = new Ray(vertex, direction);
        /*
         * Trace the ray to the view's copy of the CG model.
         */
        Spatial model = Maud.viewState.getSpatial();
        Vector3f contactPoint = findContact(model, ray);
        if (contactPoint != null) {
            MySpatial.setWorldLocation(geometry, contactPoint);
            return;
        }
        /*
         * The ray missed the model; trace it to the platform instead.
         */
        Spatial platform = MySpatial.findChild(rootNode, Maud.platformName);
        contactPoint = findContact(platform, ray);
        if (contactPoint != null) {
            MySpatial.setWorldLocation(geometry, contactPoint);
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
        /*
         * Clicking the left mouse button (LMB) warps the cursor.
         */
        MouseButtonTrigger left = new MouseButtonTrigger(
                MouseInput.BUTTON_LEFT);
        inputManager.addMapping("warp cursor", left);
        inputManager.addListener(Maud.gui.inputMode, "warp cursor");
        /*
         * Load a geometry for the cursor.
         */
        String assetPath = "Models/indicators/3d cursor/3d cursor.blend";
        Node node = (Node) assetManager.loadModel(assetPath);
        Node node2 = (Node) node.getChild(0);
        Node node3 = (Node) node2.getChild(0);
        geometry = (Geometry) node3.getChild(0);
        geometry.removeFromParent();
        /*
         * Create a material for it.
         */
        Material material = MyAsset.createUnshadedMaterial(assetManager);
        geometry.setMaterial(material);
        /*
         * Update the cursor's visibility, color, and size.
         */
        update();
    }
    // *************************************************************************
    // private methods

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
}
