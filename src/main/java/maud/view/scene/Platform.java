/*
 Copyright (c) 2017-2023, Stephen Gold
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
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.debug.BulletDebugAppState;
import com.jme3.collision.CollisionResult;
import com.jme3.material.Material;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.ui.Locators;
import maud.Maud;
import maud.MaudUtil;
import maud.model.EditorModel;
import maud.model.WhichCgm;
import maud.model.cgm.Cgm;
import maud.model.option.scene.PlatformType;
import maud.model.option.scene.SceneOptions;

/**
 * The supporting platform in a scene view.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Platform implements BulletDebugAppState.DebugAppStateFilter {
    // *************************************************************************
    // constants and loggers

    /**
     * half-extent of a square slab (in model units, &gt;0)
     */
    final private static float squareHalfExtent = 0.5f;
    /**
     * thickness of a square slab (in model units, &gt;0)
     */
    final private static float squareThickness = 0.01f;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(Platform.class.getName());
    /**
     * mesh for building square slabs
     */
    final private static Mesh squareMesh
            = new Box(squareHalfExtent, squareThickness, squareHalfExtent);
    /**
     * asset path to the texture for platforms
     */
    final private static String textureAssetPath
            = "Textures/platform/rock_11474.jpg";
    /**
     * vector for building square slabs
     */
    final private static Vector3f halfExtents
            = new Vector3f(squareHalfExtent, squareThickness, squareHalfExtent);
    // *************************************************************************
    // fields

    /**
     * diameter as of the previous update (&ge;0)
     */
    private float oldDiameter = 0f;
    /**
     * type as of the previous update (not null)
     */
    private PlatformType oldType = PlatformType.None;
    /**
     * view that owns this platform (not null, set by constructor or
     * {@link #setView(SceneViewCore)})
     */
    private SceneViewCore view;
    /**
     * spatial attached to the scene graph for visualization, or null if none
     */
    private Spatial spatial = null;
    /**
     * spatial for the square slab, or null if not initialized
     */
    private Spatial square = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new platform.
     *
     * @param owner the view that will own this platform (not null, alias
     * created)
     */
    Platform(SceneViewCore owner) {
        assert owner != null;
        this.view = owner;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Find the nearest contact point of the specified ray.
     *
     * @param ray camera ray (not null, unaffected)
     * @return collision result, or null of no collision with a triangle facing
     * the camera
     */
    CollisionResult findCollision(Ray ray) {
        CollisionResult result = null;
        if (spatial != null) {
            result = MaudUtil.findCollision(spatial, ray);
        }

        return result;
    }

    /**
     * Alter which view owns this platform. (Invoked only when restoring a
     * checkpoint.)
     *
     * @param newView (not null, alias created)
     */
    void setView(SceneViewCore newView) {
        assert newView != null;
        assert newView != view;
        assert newView.getPlatform() == this;

        this.view = newView;
    }

    /**
     * Update the platform based on the MVC model.
     */
    void update() {
        EditorModel model = Maud.getModel();
        SceneOptions options = model.getScene();
        Cgm cgm = view.getCgm();
        WhichCgm whichCgm = model.whichCgm(cgm);
        float diameter = options.getPlatformDiameter(whichCgm);
        PlatformType type = options.getPlatformType();

        if (diameter != oldDiameter || type != oldType) {
            switch (type) {
                case None:
                    setPlatform(null);
                    break;
                case Square:
                    updateSquare(diameter);
                    setPlatform(square);
                    break;
                default:
                    throw new IllegalStateException("type = " + type);
            }
            this.oldDiameter = diameter;
            this.oldType = type;
        }
    }
    // *************************************************************************
    // DebugAppStateFilter methods

    /**
     * Test whether the specified physics object should be displayed.
     *
     * @param obj the joint or collision object to test (unaffected)
     * @return return true if the object should be displayed, false if not
     */
    @Override
    public boolean displayObject(Object obj) {
        return true;
    }
    // *************************************************************************
    // private methods

    /**
     * Create the square-slab spatial.
     */
    private void createSquare() {
        assert square == null;
        square = new Geometry("square platform", squareMesh);

        AssetManager assetManager = Locators.getAssetManager();
        Texture texture
                = MyAsset.loadTexture(assetManager, textureAssetPath, true);
        Material material
                = MyAsset.createShadedMaterial(assetManager, texture);
        square.setMaterial(material);
        square.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        BoxCollisionShape shape = new BoxCollisionShape(halfExtents);
        RigidBodyControl rbc = new RigidBodyControl(shape);
        rbc.setApplyScale(true);
        rbc.setKinematic(true);
        square.addControl(rbc);
    }

    /**
     * Alter which spatial is attached to the scene graph.
     *
     * @param platformSpatial (may be null, alias created)
     */
    private void setPlatform(Spatial platformSpatial) {
        if (spatial != platformSpatial) {
            if (spatial != null) {
                RigidBodyControl rbc
                        = spatial.getControl(RigidBodyControl.class);
                if (rbc != null) {
                    rbc.setEnabled(false);
                }
                spatial.removeFromParent();
            }
            if (platformSpatial != null) {
                view.attachToSceneRoot(platformSpatial);
                RigidBodyControl rbc
                        = platformSpatial.getControl(RigidBodyControl.class);
                if (rbc != null) {
                    PhysicsSpace space = view.getPhysicsSpace();
                    rbc.setPhysicsSpace(space);
                    rbc.setEnabled(true);
                }
            }
            this.spatial = platformSpatial;
        }
    }

    /**
     * Update the square slab to the specified diameter.
     *
     * @param diameter (&ge;0)
     */
    private void updateSquare(float diameter) {
        if (square == null) {
            createSquare();
        }
        Vector3f center = new Vector3f(0f, -diameter * squareThickness, 0f);
        square.setLocalTranslation(center);
        square.setLocalScale(diameter);
    }
}
