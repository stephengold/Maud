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
package maud.view;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.ui.Locators;
import maud.Maud;
import maud.model.option.scene.PlatformType;

/**
 * A supporting platform in a scene view.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class Platform {
    // *************************************************************************
    // constants and loggers

    /**
     * radius of the platform (in model units, &gt;0)
     */
    final private static float radius = 0.5f;
    /**
     * thickness of the square (in model units, &gt;0)
     */
    final private static float squareThickness = 0.01f;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Platform.class.getName());
    /**
     * mesh for generating square platforms
     */
    final private static Mesh squareMesh
            = new Box(radius, squareThickness, radius);
    /**
     * asset path to the texture for platforms
     */
    final private static String textureAssetPath
            = "Textures/platform/rock_11474.jpg";
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f scaleIdentity = new Vector3f(1f, 1f, 1f);
    // *************************************************************************
    // fields

    /**
     * view that owns this platform (not null)
     */
    final private SceneView view;
    /**
     * spatial attached to the scene graph for visualization, or null if none
     */
    private Spatial spatial = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new platform.
     *
     * @param owner the view that will own this platform (not null, alias
     * created)
     */
    Platform(SceneView owner) {
        assert owner != null;
        view = owner;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add all physics ids used by the platform to the specified set.
     *
     * @param addResult (added to if not null)
     * @return an expanded set (either addResult or a new instance)
     */
    Set<Long> listIds(Set<Long> addResult) {
        if (addResult == null) {
            addResult = new TreeSet<>();
        }

        if (spatial != null) {
            RigidBodyControl rbc = spatial.getControl(RigidBodyControl.class);
            long id = rbc.getObjectId();
            addResult.add(id);

            CollisionShape pShape = rbc.getCollisionShape();
            id = pShape.getObjectId();
            addResult.add(id);
        }

        return addResult;
    }

    /**
     * Update the platform based on the MVC model.
     */
    void update() {
        PlatformType type = Maud.getModel().getScene().getPlatformType();
        switch (type) {
            case None:
                if (spatial != null) {
                    setPlatform(null);
                }
                break;

            case Square:
                if (spatial == null) {
                    Spatial square = createSquare();
                    setPlatform(square);
                }
                break;

            default:
                throw new IllegalStateException();
        }

        if (spatial != null) {
            float diameter = Maud.getModel().getScene().getPlatformDiameter();
            Vector3f center = new Vector3f(0f, -diameter * squareThickness, 0f);

            RigidBodyControl rbc = spatial.getControl(RigidBodyControl.class);
            rbc.setPhysicsLocation(center);

            CollisionShape shape = rbc.getCollisionShape();
            Vector3f scale = scaleIdentity.mult(diameter);
            shape.setScale(scale);
            rbc.setCollisionShape(shape);

            spatial.setLocalTranslation(center);
            spatial.setLocalScale(diameter);
        }
    }

    /**
     * Attempt to warp a cursor to the contact point of the specified ray.
     */
    void warpCursor(Ray ray) {
        if (spatial != null) {
            CollisionResult collision = view.findCollision(spatial, ray);
            if (collision != null) {
                Vector3f contactPoint = collision.getContactPoint();
                view.getPov().setCursorLocation(contactPoint);
            }
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Create a square slab platform.
     *
     * @return a new, orphaned spatial with its own RigidBodyControl, not added
     * to any physics space
     */
    private static Spatial createSquare() {
        Spatial result = new Geometry("square platform", squareMesh);

        AssetManager assetManager = Locators.getAssetManager();
        Texture texture = MyAsset.loadTexture(assetManager, textureAssetPath);
        Material material = MyAsset.createShadedMaterial(assetManager, texture);
        result.setMaterial(material);
        result.setShadowMode(RenderQueue.ShadowMode.Receive);

        Vector3f halfExtents = new Vector3f(radius, squareThickness, radius);
        BoxCollisionShape shape = new BoxCollisionShape(halfExtents);
        float mass = 0f;
        RigidBodyControl rbc = new RigidBodyControl(shape, mass);
        result.addControl(rbc);

        return result;
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
            spatial = platformSpatial;
        }
    }
}
