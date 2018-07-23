/*
 Copyright (c) 2017-2018, Stephen Gold
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
package maud.view.scene;

import com.jme3.asset.AssetManager;
import com.jme3.collision.CollisionResult;
import com.jme3.input.InputManager;
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
import jme3utilities.Validate;
import jme3utilities.math.MyColor;
import jme3utilities.ui.Locators;
import maud.Maud;
import maud.MaudUtil;
import maud.model.cgm.Cgm;
import maud.model.option.scene.DddCursorOptions;

/**
 * The 3-D cursor in a scene view.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DddCursor {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DddCursor.class.getName());
    /**
     * asset path to the C-G model for a star-shaped cursor
     */
    final private static String starAssetPath
            = "Models/indicators/3d cursor/3d cursor.j3o";
    // *************************************************************************
    // fields

    /**
     * elapsed time in the current color cycle (&ge;0)
     */
    private double colorTime = 0.0;
    /**
     * indicator geometry, or null if none
     */
    private Geometry geometry;
    /**
     * view that owns this cursor (not null, set by constructor or
     * {@link #setView(SceneViewCore)})
     */
    private SceneViewCore view;
    /**
     * location (in world coordinates, not null)
     */
    final private Vector3f location = new Vector3f();
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new cursor.
     *
     * @param owner the view that will own this cursor (not null, alias created)
     */
    DddCursor(SceneViewCore owner) {
        assert owner != null;
        view = owner;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the location of the cursor.
     *
     * @param storeResult (modified if not null)
     * @return world coordinates (either storeResult or a new vector)
     */
    public Vector3f location(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        storeResult.set(location);
        return storeResult;
    }

    /**
     * Relocate the cursor.
     *
     * @param newLocation (in world coordinates, not null, unaffected)
     */
    void setLocation(Vector3f newLocation) {
        Validate.nonNull(newLocation, "new location");
        location.set(newLocation);
    }

    /**
     * Alter which view owns this cursor. (Invoked only when restoring a
     * checkpoint.)
     *
     * @param newView (not null, alias created)
     */
    void setView(SceneViewCore newView) {
        assert newView != null;
        assert newView != view;
        assert newView.getCursor() == this;

        view = newView;
    }

    /**
     * Update the cursor based on the MVC model.
     *
     * @param cgm which C-G model (not null)
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    void update(Cgm cgm, float updateInterval) {
        /*
         * visibility
         */
        boolean wasVisible = (geometry != null);
        DddCursorOptions options = Maud.getModel().getScene().getCursor();
        boolean visible = options.isVisible();
        if (wasVisible && !visible) {
            setGeometry(null);
            geometry = null;
        } else if (!wasVisible && visible) {
            geometry = createStar();
            setGeometry(geometry);
        }

        if (geometry != null) {
            /*
             * color
             */
            float cycleTime = options.getCycleTime();
            colorTime = (colorTime + updateInterval) % cycleTime;
            double t = Math.sin(Math.PI * colorTime / cycleTime);
            double t2 = t * t;
            float fraction = (float) (t2 * t2); // 4th power of sine
            ColorRGBA color0 = options.copyColor(0, null);
            ColorRGBA color1 = options.copyColor(1, null);
            ColorRGBA newColor
                    = MyColor.interpolateLinear(fraction, color0, color1);
            Material material = geometry.getMaterial();
            material.setColor("Color", newColor); // note: creates alias
            /*
             * location
             */
            MySpatial.setWorldLocation(geometry, location);
            /*
             * scale
             */
            float newScale = worldScale();
            if (newScale > 0f) {
                MySpatial.setWorldScale(geometry, newScale);
            }
        }
    }

    /**
     * Attempt to warp the cursor to the screen coordinates of the mouse
     * pointer.
     */
    void warp() {
        Camera camera = view.getCamera();
        InputManager inputManager = Maud.getApplication().getInputManager();
        Ray ray = MyCamera.mouseRay(camera, inputManager);
        /*
         * Trace the ray to the C-G model.
         */
        Spatial cgmRoot = view.getCgmRoot();
        CollisionResult collision = MaudUtil.findCollision(cgmRoot, ray);
        if (collision == null) {
            /*
             * The ray missed the C-G model; try to trace it to the platform.
             */
            collision = view.getPlatform().findCollision(ray);
        }

        if (collision != null) {
            Vector3f contactPoint = collision.getContactPoint();
            location.set(contactPoint);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Create a star-shaped indicator geometry.
     *
     * @return a new, orphaned spatial
     */
    private static Geometry createStar() {
        AssetManager assetManager = Locators.getAssetManager();
        Node node = (Node) assetManager.loadModel(starAssetPath);
        Node node2 = (Node) node.getChild(0);
        Node node3 = (Node) node2.getChild(0);
        Geometry result = (Geometry) node3.getChild(0);

        result.removeFromParent();

        Material material = MyAsset.createUnshadedMaterial(assetManager);
        result.setMaterial(material);

        return result;
    }

    /**
     * Alter which indicator geometry is attached to the scene graph.
     *
     * @param newGeometry (may be null)
     */
    private void setGeometry(Geometry newGeometry) {
        if (geometry != null) {
            geometry.removeFromParent();
        }
        if (newGeometry != null) {
            view.attachToSceneRoot(newGeometry);
        }
        geometry = newGeometry;
    }

    /**
     * Calculate a uniform scale factor for the cursor, based on its distance
     * from the POV.
     *
     * @return world scale factor (&ge;0)
     */
    private float worldScale() {
        Cgm cgm = view.getCgm();
        Vector3f povLocation = cgm.getScenePov().location(null);
        float range = povLocation.distance(location);

        DddCursorOptions cursor = Maud.getModel().getScene().getCursor();
        float worldScale = cursor.getSize() * range;

        assert worldScale >= 0f : worldScale;
        return worldScale;
    }
}
