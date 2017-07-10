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
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MySpatial;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.SceneView;
import maud.model.LoadedCgm;

/**
 * The controller for the "Cursor Tool" window in Maud's editor screen.
 *
 * The left mouse button (LMB) positions the 3D cursor.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CursorTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            CursorTool.class.getName());
    /**
     * asset path of the CG model for the 3-D cursor
     */
    final private static String assetPath = "Models/indicators/3d cursor/3d cursor.blend";
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController (not null)
     */
    CursorTool(BasicScreenController screenController) {
        super(screenController, "cursorTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        ColorRGBA color = Maud.gui.readColorBank("cursor");
        Maud.model.cursor.setColor(color);
    }

    /**
     * Update a CG model's scene graph based on the MVC model.
     *
     * @param cgm which CG model (not null)
     */
    void updateScene(LoadedCgm cgm) {
        SceneView sceneView = cgm.getSceneView();
        Geometry cursor = sceneView.getCursor();
        /*
         * visibility
         */
        boolean wasVisible = (cursor != null);
        boolean visible = Maud.model.cursor.isVisible();
        if (wasVisible && !visible) {
            sceneView.setCursor(null);
            cursor = null;
        } else if (!wasVisible && visible) {
            cursor = createCursor();
            sceneView.setCursor(cursor);
        }

        if (cursor != null) {
            /*
             * color
             */
            ColorRGBA newColor = Maud.model.cursor.copyColor(null);
            Material material = cursor.getMaterial();
            material.setColor("Color", newColor); // note: creates alias
            /*
             * location
             */
            Vector3f newLocation = cgm.scenePov.cursorLocation(null);
            MySpatial.setWorldLocation(cursor, newLocation);
            /*
             * scale
             */
            float newScale = cgm.scenePov.worldScaleForCursor();
            if (newScale != 0f) {
                MySpatial.setWorldScale(cursor, newScale);
            }
        }
    }

    /**
     * Attempt to warp the 3D cursor to the screen coordinates of the mouse
     * pointer.
     */
    public void warpCursor() {
        LoadedCgm cgm = Maud.gui.mouseCgm();
        SceneView sceneView = cgm.getSceneView();
        Camera camera = sceneView.getCamera();
        Ray ray = MyCamera.mouseRay(camera, inputManager);
        /*
         * Trace the ray to the CG model's visualization.
         */
        Spatial cgmRoot = sceneView.getCgmRoot();
        Vector3f targetContactPoint = findContact(cgmRoot, ray);

        if (targetContactPoint != null) {
            cgm.scenePov.setCursorLocation(targetContactPoint);
        } else {
            /*
             * The ray missed the CG model; try to trace it to the platform.
             */
            Spatial platform = sceneView.getPlatform();
            if (platform != null) {
                Vector3f platformContactPoint = findContact(platform, ray);
                if (platformContactPoint != null) {
                    cgm.scenePov.setCursorLocation(platformContactPoint);
                }
            }
        }
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
    }

    /**
     * Callback to update this window prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        boolean visible = Maud.model.cursor.isVisible();
        Maud.gui.setChecked("3DCursor", visible);

        ColorRGBA color = Maud.model.cursor.copyColor(null);
        Maud.gui.setColorBank("cursor", color);
    }
    // *************************************************************************
    // private methods

    /**
     * Create a star-shaped 3D cursor.
     *
     * @return a new, orphaned spatial
     */
    private Geometry createCursor() {
        Node node = (Node) assetManager.loadModel(assetPath);
        Node node2 = (Node) node.getChild(0);
        Node node3 = (Node) node2.getChild(0);
        Geometry result = (Geometry) node3.getChild(0);

        result.removeFromParent();

        Material material = MyAsset.createUnshadedMaterial(assetManager);
        result.setMaterial(material);

        return result;
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
        for (int resultIndex = 0; resultIndex < results.size(); resultIndex++) {
            /*
             * Calculate the offset from the camera to the point of contact.
             */
            CollisionResult result = results.getCollision(resultIndex);
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
