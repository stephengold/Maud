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
import maud.CgmView;
import maud.EditorScreen;
import maud.Maud;
import maud.model.LoadedCgm;

/**
 * The controller for the "Platform Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PlatformTool extends WindowController {
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
     * mesh for generating square platforms
     */
    final private static Mesh squareMesh = new Box(radius, squareThickness,
            radius);
    /**
     * path to texture asset for the platform
     */
    final private static String textureAssetPath = "Textures/Terrain/splat/dirt.jpg";
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    PlatformTool(EditorScreen screenController) {
        super(screenController, "platformTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update a CG model's scene graph based on the MVC model.
     *
     * @param cgm which CG model (not null)
     */
    void updateScene(LoadedCgm cgm) {
        CgmView sceneView = cgm.getView();
        Spatial platform = sceneView.getPlatform();

        String mode = Maud.model.misc.getPlatformMode();
        switch (mode) {
            case "none":
                if (platform != null) {
                    sceneView.setPlatform(null);
                    platform = null;
                }
                break;

            case "square":
                if (platform == null) {
                    platform = createSquare();
                    sceneView.setPlatform(platform);
                }
                break;

            default:
                throw new IllegalStateException();
        }

        if (platform != null) { // TODO use MySpatial
            float diameter = Maud.model.misc.getPlatformDiameter();
            platform.setLocalScale(diameter);

            Vector3f center = new Vector3f(0f, -diameter * squareThickness, 0f);
            platform.setLocalTranslation(center);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Create a square platform.
     *
     * @return a new, orphaned spatial
     */
    private Spatial createSquare() {
        Spatial result = new Geometry("square platform", squareMesh);

        Texture dirt = MyAsset.loadTexture(assetManager, textureAssetPath);
        Material mat = MyAsset.createShadedMaterial(assetManager, dirt);
        result.setMaterial(mat);
        result.setShadowMode(RenderQueue.ShadowMode.Receive);

        return result;
    }
}
