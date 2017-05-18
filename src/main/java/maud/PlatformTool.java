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
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.nifty.WindowController;

/**
 * The controller for the "Platform Tool" window in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class PlatformTool extends WindowController {
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
    final private static Logger logger = Logger.getLogger(
            PlatformTool.class.getName());
    /**
     * path to texture asset for the platform
     */
    final private static String textureAssetPath = "Textures/Terrain/splat/dirt.jpg";
    // *************************************************************************
    // fields

    /**
     * the platform's spatial in the scene graph, or null if none
     */
    private Spatial spatial = null;
    /**
     * geometry for the square platform
     */
    private Spatial square = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    PlatformTool(DddGui screenController) {
        super(screenController, "platformTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the platform's spatial in the view's scene graph.
     *
     * @return the pre-existing instance, or null if none
     */
    Spatial getSpatial() {
        return spatial;
    }

    /**
     * Update the view's scene graph from the MVC model.
     */
    void updateScene() {
        String mode = Maud.model.misc.getPlatformMode();
        switch (mode) {
            case "none":
                if (spatial == square) {
                    spatial = null;
                    rootNode.detachChild(square);
                }
                break;

            case "square":
                if (spatial == null) {
                    spatial = square;
                    rootNode.attachChild(square);
                }
                break;

            default:
                throw new IllegalStateException();
        }

        if (spatial != null) {
            float diameter = Maud.model.misc.getPlatformDiameter();
            spatial.setLocalScale(diameter);

            Vector3f center = Maud.model.misc.copyPlatformLocation();
            center.y -= diameter * squareThickness;
            spatial.setLocalTranslation(center);
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
        createPlatforms();
    }
    // *************************************************************************
    // AppState methods

    /**
     * Create various platforms for the model to rest upon, but don't add them
     * to the scene graph.
     */
    private void createPlatforms() {
        Mesh platformMesh = new Box(radius, squareThickness, radius);
        square = new Geometry("square platform", platformMesh);

        Texture dirt = MyAsset.loadTexture(assetManager, textureAssetPath);
        Material mat = MyAsset.createShadedMaterial(assetManager, dirt);
        square.setMaterial(mat);

        square.setShadowMode(RenderQueue.ShadowMode.Receive);
    }
}
