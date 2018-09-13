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
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.debug.BulletDebugAppState;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.util.Set;
import java.util.TreeSet;
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
     * patch size for terrain quads (in pixels)
     */
    final private static int patchSize = 33;
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
     * asset path of the terrain's height map
     */
    final private static String heightMapAssetPath
            = "Textures/terrain/height/basin.png";
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
     * spatial for the landscape, or null if not initialized
     */
    private Spatial landscape = null;
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
        view = owner;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Find the nearest contact point of the specified ray.
     */
    CollisionResult findCollision(Ray ray) {
        CollisionResult result = null;
        if (spatial != null) {
            result = MaudUtil.findCollision(spatial, ray);
        }

        return result;
    }

    /**
     * Add all physics ids related to the platform to the specified set.
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
     * Alter which view owns this platform. (Invoked only when restoring a
     * checkpoint.)
     *
     * @param newView (not null, alias created)
     */
    void setView(SceneViewCore newView) {
        assert newView != null;
        assert newView != view;
        assert newView.getPlatform() == this;

        view = newView;
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
                case Landscape:
                    updateLandscape(diameter);
                    setPlatform(landscape);
                    break;
                case None:
                    setPlatform(null);
                    break;
                case Square:
                    updateSquare(diameter);
                    setPlatform(square);
                    break;
                default:
                    throw new IllegalStateException();
            }
            oldDiameter = diameter;
            oldType = type;
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
    public boolean displayObject(Savable obj) {
        boolean result = true;
        if (landscape != null) {
            Savable landscapeObject = landscape.getControl(RigidBodyControl.class);
            if (obj == landscapeObject) {
                result = false;
            }
        }
        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Create the landscape spatial.
     */
    private void createLandscape() {
        assert landscape == null;

        AssetManager assetManager = Locators.getAssetManager();
        Material grass = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md"); // TODO use MyAsset
        grass.setBoolean("UseMaterialColors", true);
        ColorRGBA terrainColor
                = new ColorRGBA(0.65f, 0.8f, 0.2f, 1f);
        grass.setColor("Diffuse", terrainColor.clone());

        AbstractHeightMap heightMap = loadHeightMap();
        int terrainDiameter = heightMap.getSize(); // in pixels
        int mapSize = terrainDiameter + 1; // number of samples on a side
        float[] heightArray = heightMap.getHeightMap();
        landscape = new TerrainQuad("landscape", patchSize, mapSize,
                heightArray);
        landscape.setMaterial(grass);
        landscape.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        CollisionShape shape = CollisionShapeFactory.createMeshShape(landscape);
        RigidBodyControl rbc
                = new RigidBodyControl(shape, PhysicsRigidBody.massForStatic);
        rbc.setKinematic(true);
        landscape.addControl(rbc);
    }

    /**
     * Create the square-slab spatial.
     */
    private void createSquare() {
        assert square == null;
        square = new Geometry("square platform", squareMesh);

        AssetManager assetManager = Locators.getAssetManager();
        Texture texture
                = MyAsset.loadTexture(assetManager, textureAssetPath);
        Material material
                = MyAsset.createShadedMaterial(assetManager, texture);
        square.setMaterial(material);
        square.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        BoxCollisionShape shape = new BoxCollisionShape(halfExtents);
        RigidBodyControl rbc
                = new RigidBodyControl(shape, PhysicsRigidBody.massForStatic);
        rbc.setKinematic(true);
        square.addControl(rbc);
    }

    /**
     * Load a height-map asset.
     *
     * @return a new instance (not null)
     */
    private AbstractHeightMap loadHeightMap() {
        AssetManager assetManager = Locators.getAssetManager();

        Texture heightTexture
                = MyAsset.loadTexture(assetManager, heightMapAssetPath);
        Image heightImage = heightTexture.getImage();
        float heightScale = 1f;
        AbstractHeightMap heightMap
                = new ImageBasedHeightMap(heightImage, heightScale);
        heightMap.load();

        return heightMap;
    }

    /**
     * Update the landscape to the specified diameter.
     *
     * @param diameter (&ge;0)
     */
    private void updateLandscape(float diameter) {
        if (landscape == null) {
            createLandscape();
        }

        float yScale = 0.003f * diameter;
        float xzScale = 0.01f * diameter;
        Vector3f scale = new Vector3f(xzScale, yScale, xzScale);
        landscape.setLocalScale(scale);
    }

    /**
     * Update the square slab to the specified diameter.
     *
     * @param diameter (&ge;0)
     * @return a new, orphaned spatial with its own RigidBodyControl, not added
     * to any physics space
     */
    private void updateSquare(float diameter) {
        if (square == null) {
            createSquare();
        }
        Vector3f center = new Vector3f(0f, -diameter * squareThickness, 0f);
        square.setLocalTranslation(center);
        square.setLocalScale(diameter);
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
