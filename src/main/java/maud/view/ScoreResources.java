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
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Line;
import com.jme3.texture.Texture;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.mesh.RectangleOutlineMesh;
import jme3utilities.ui.Locators;
import maud.mesh.RoundedRectangle;

/**
 * Constants, fonts, materials, and meshes used for 2-D visualization of
 * animations.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScoreResources {
    // *************************************************************************
    // constants and loggers

    /**
     * font for labels
     */
    final BitmapFont labelFont;
    /**
     * color for the w-axis (white)
     */
    final static ColorRGBA wColor = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * color for the X-axis (red)
     */
    final static ColorRGBA xColor = new ColorRGBA(1f, 0f, 0f, 1f);
    /**
     * color for the Y-axis (green)
     */
    final static ColorRGBA yColor = new ColorRGBA(0f, 1f, 0f, 1f);
    /**
     * color for the Z-axis (blue)
     */
    final static ColorRGBA zColor = new ColorRGBA(0f, 0f, 1f, 1f);
    /**
     * horizontal size of hash mark (in world units)
     */
    final static float hashSize = 0.05f;
    /**
     * hash-mark mesh to represent a bone without a track, or any bone when the
     * POV is zoomed all the way out
     */
    final static Line hashMark = new Line(
            new Vector3f(-hashSize, 0f, 0f), new Vector3f(0f, 0f, 0f)
    );
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ScoreResources.class.getName());
    /**
     * material for label backgrounds of non-selected bones
     */
    final Material bgNotSelected;
    /**
     * material for label backgrounds of selected bones
     */
    final Material bgSelected;
    /**
     * material for the gnomon when the pose is frozen
     */
    final Material gnomonFrozen;
    /**
     * material for pose markers
     */
    final Material poseMaterial;
    /**
     * material for rotation icons
     */
    final Material rotMaterial;
    /**
     * material for scale icons
     */
    final Material scaMaterial;
    /**
     * material for translation icons
     */
    final Material traMaterial;
    /**
     * material for finials of non-selected bones
     */
    final Material wireNotSelected;
    /**
     * material for finials of selected bones
     */
    final Material wireSelected;
    /**
     * material for sparklines of W components of quaternions
     */
    final public Material wMaterial;
    /**
     * material for sparklines of X components
     */
    final public Material xMaterial;
    /**
     * material for sparklines of Y components
     */
    final public Material yMaterial;
    /**
     * material for sparklines of Z components
     */
    final public Material zMaterial;
    /**
     * square mesh for a transform icon
     */
    final static Mesh iconMesh = new RoundedRectangle(0f, 1f, -0.5f, 0.5f, 0.3f,
            1f);
    /**
     * rectangular outline for an end cap when the POV is zoomed part way out
     */
    final static Mesh outlineMesh = new RectangleOutlineMesh(0f, hashSize, -1f,
            0f);
    /**
     * rotation of 90 degrees around the Z axis
     */
    final static Quaternion quarterZ = new Quaternion().fromAngles(0f, 0f,
            FastMath.HALF_PI);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new set of resources.
     */
    ScoreResources() {
        AssetManager assetManager = Locators.getAssetManager();

        labelFont = assetManager.loadFont("Interface/Fonts/Default.fnt");

        ColorRGBA black = new ColorRGBA(0f, 0f, 0f, 1f);
        ColorRGBA grey = new ColorRGBA(0.5f, 0.5f, 0.5f, 1f);
        ColorRGBA yellow = new ColorRGBA(1f, 1f, 0f, 1f);

        bgNotSelected = MyAsset.createUnshadedMaterial(assetManager, grey);
        bgSelected = MyAsset.createUnshadedMaterial(assetManager, black);
        gnomonFrozen = MyAsset.createUnshadedMaterial(assetManager, yellow);
        wireNotSelected = MyAsset.createWireframeMaterial(assetManager, grey);
        wireSelected = MyAsset.createWireframeMaterial(assetManager, black);

        float pointSize = 3f;
        wMaterial = MyAsset.createWireframeMaterial(assetManager, wColor,
                pointSize);
        xMaterial = MyAsset.createWireframeMaterial(assetManager, xColor,
                pointSize);
        yMaterial = MyAsset.createWireframeMaterial(assetManager, yColor,
                pointSize);
        zMaterial = MyAsset.createWireframeMaterial(assetManager, zColor,
                pointSize);

        traMaterial = MyAsset.createUnshadedMaterial(assetManager,
                "Textures/icons/translate.png");
        rotMaterial = MyAsset.createUnshadedMaterial(assetManager,
                "Textures/icons/rotate.png");
        scaMaterial = MyAsset.createUnshadedMaterial(assetManager,
                "Textures/icons/scale.png");

        poseMaterial = new Material(assetManager,
                "MatDefs/wireframe/multicolor2.j3md");
        poseMaterial.setBoolean("UseVertexColor", true);
        poseMaterial.setFloat("PointSize", 2f * pointSize);
        Texture poseShape = MyAsset.loadTexture(assetManager,
                "Textures/shapes/saltire.png");
        poseMaterial.setTexture("PointShape", poseShape);
        RenderState rs = poseMaterial.getAdditionalRenderState();
        rs.setBlendMode(RenderState.BlendMode.Alpha);
        rs.setDepthTest(false);
    }
}
