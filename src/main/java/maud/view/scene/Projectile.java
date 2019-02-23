/*
 Copyright (c) 2017-2019, Stephen Gold
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
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.InputManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.ui.Locators;
import maud.Maud;
import maud.model.cgm.ScenePov;

/**
 * The test projectile in a scene view.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Projectile {
    // *************************************************************************
    // constants and loggers

    /**
     * size (in world units)
     */
    final private static float radius = 0.04f;
    /**
     * physics collision shape
     */
    final private static CollisionShape shape
            = new SphereCollisionShape(radius);
    /**
     * color for visualization (gray)
     */
    final private static ColorRGBA color = new ColorRGBA(0.5f, 0.5f, 0.5f, 1f);
    /**
     * travel time to the POV's center (in seconds)
     */
    final private static float latency = 0.3f;
    /**
     * mass of each projectile (in kg)
     */
    final private static float mass = 0.001f;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Projectile.class.getName());
    /**
     * visualization mesh
     */
    final private static Mesh mesh = new Sphere(32, 32, radius, true, false);
    /**
     * name for spatial
     */
    final private static String name = "projectile";
    /**
     * gravity force vector (none)
     */
    final private static Vector3f gravity = new Vector3f(0f, 0f, 0f);
    // *************************************************************************
    // fields

    /**
     * view that owns this projectile (not null, set by constructor or
     * {@link #setView(SceneViewCore)})
     */
    private SceneViewCore view;
    /**
     * spatial attached to the scene graph for visualization, or null if none
     */
    private Spatial spatial = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new projectile.
     *
     * @param owner the view that will own this projectile (not null, alias
     * created)
     */
    Projectile(SceneViewCore owner) {
        assert owner != null;
        view = owner;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Delete any pre-existing projectile.
     */
    void delete() {
        if (spatial != null) {
            RigidBodyControl rbc
                    = spatial.getControl(RigidBodyControl.class);
            rbc.setEnabled(false);
            spatial.removeFromParent();
            spatial = null;
        }
    }

    /**
     * Launch the projectile from the screen coordinates of the mouse pointer.
     * If there's no projectile, create one.
     */
    public void launch() {
        if (spatial == null) {
            create();
        }

        Camera camera = view.getCamera();
        InputManager inputManager = Maud.getApplication().getInputManager();
        Ray ray = MyCamera.mouseRay(camera, inputManager);
        spatial.setLocalTranslation(ray.origin);

        ScenePov pov = view.getPov();
        float initialSpeed = pov.range() / latency; // world units per second
        Vector3f initialVelocity = ray.direction.mult(initialSpeed);
        RigidBodyControl rbc = spatial.getControl(RigidBodyControl.class);
        rbc.setPhysicsLocation(ray.origin);
        rbc.setLinearVelocity(initialVelocity);

        BulletAppState bulletAppState = view.getBulletAppState();
        bulletAppState.setSpeed(1f);
    }

    /**
     * Alter which view owns this projectile. (Invoked only when restoring a
     * checkpoint.)
     *
     * @param newView (not null, alias created)
     */
    void setView(SceneViewCore newView) {
        assert newView != null;
        assert newView != view;
        assert newView.getProjectile() == this;

        view = newView;
    }
    // *************************************************************************
    // private methods

    /**
     * Create a projectile and add it to the view.
     */
    private void create() {
        assert spatial == null;

        RigidBodyControl rbc = new RigidBodyControl(shape, mass);
        rbc.setCcdSweptSphereRadius(0.2f * radius);
        rbc.setCcdMotionThreshold(1e-10f);
        view.getPhysicsSpace().add(rbc);
        rbc.setGravity(gravity);

        AssetManager assetManager = Locators.getAssetManager();
        Material material = MyAsset.createShinyMaterial(assetManager, color);

        spatial = new Geometry(name, mesh);
        spatial.addControl(rbc);
        spatial.setMaterial(material);
        spatial.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        view.attachToSceneRoot(spatial);
    }
}
