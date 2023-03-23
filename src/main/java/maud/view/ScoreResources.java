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
package maud.view;

import com.atr.jme.font.TrueTypeFont;
import com.atr.jme.font.asset.TrueTypeKeyMesh;
import com.atr.jme.font.shape.TrueTypeNode;
import com.atr.jme.font.util.Style;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Line;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.mesh.RectangleOutlineMesh;
import jme3utilities.mesh.RoundedRectangle;
import jme3utilities.ui.Locators;
import maud.Maud;

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
     * hash-mark mesh to represent a bone without a track, or any track when the
     * POV is zoomed all the way out
     */
    final static Line hashMark = new Line(
            new Vector3f(-hashSize, 0f, 0f), new Vector3f(0f, 0f, 0f)
    );
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ScoreResources.class.getName());
    /**
     * material for label backgrounds of selected tracks
     */
    final Material bgSelected;
    /**
     * material for the gnomon when the pose is frozen
     */
    final Material gnomonFrozen;
    /**
     * translucent material to highlight all times between the limits
     */
    final Material limitsMaterial;
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
     * material for finials of non-selected tracks
     */
    final Material wireNotSelected;
    /**
     * material for finials of selected tracks
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
    /**
     * font for labels
     */
    final TrueTypeFont<?, ?> labelFont;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new set of resources.
     */
    @SuppressWarnings("unchecked")
    ScoreResources() {
        AssetManager assetManager = Locators.getAssetManager();

        AssetKey<TrueTypeFont> assetKey = new TrueTypeKeyMesh(
                "Interface/Fonts/ProFontWindows.ttf", Style.Plain, 18);
        labelFont = assetManager.loadAsset(assetKey);

        ColorRGBA black = new ColorRGBA(0f, 0f, 0f, 1f);
        ColorRGBA grey = new ColorRGBA(0.5f, 0.5f, 0.5f, 1f);
        ColorRGBA yellow = new ColorRGBA(1f, 1f, 0f, 1f);

        bgSelected = MyAsset.createUnshadedMaterial(assetManager, black);
        gnomonFrozen = MyAsset.createUnshadedMaterial(assetManager, yellow);
        wireNotSelected = MyAsset.createWireframeMaterial(assetManager, grey);
        wireSelected = MyAsset.createWireframeMaterial(assetManager, black);

        ColorRGBA frosted = new ColorRGBA(1f, 1f, 1f, 0.25f);
        limitsMaterial = MyAsset.createUnshadedMaterial(assetManager, frosted);
        RenderState rsLimits = limitsMaterial.getAdditionalRenderState();
        rsLimits.setBlendMode(RenderState.BlendMode.Alpha);
        /*
         * wireframe materials for axes
         */
        float pointSize = 3f;
        AppSettings current = Maud.getApplication().getSettings();
        int msaaSamples = current.getSamples();
        if (msaaSamples == 16) { // work around JME issue #878
            pointSize *= 2f;
        }
        wMaterial = MyAsset.createWireframeMaterial(assetManager, wColor,
                pointSize);
        xMaterial = MyAsset.createWireframeMaterial(assetManager, xColor,
                pointSize);
        yMaterial = MyAsset.createWireframeMaterial(assetManager, yColor,
                pointSize);
        zMaterial = MyAsset.createWireframeMaterial(assetManager, zColor,
                pointSize);
        /*
         * textured materials for track-component icons
         */
        traMaterial = MyAsset.createUnshadedMaterial(assetManager,
                "Textures/icons/translate.png");
        rotMaterial = MyAsset.createUnshadedMaterial(assetManager,
                "Textures/icons/rotate.png");
        scaMaterial = MyAsset.createUnshadedMaterial(assetManager,
                "Textures/icons/scale.png");

        poseMaterial = new Material(assetManager,
                "MatDefs/wireframe/multicolor2.j3md");
        poseMaterial.setBoolean("UseVertexColor", true);
        poseMaterial.setFloat("PointSize", 2f * pointSize); // twice as big
        Texture poseShape = MyAsset.loadTexture(assetManager,
                "Textures/shapes/saltire.png", false);
        poseMaterial.setTexture("PointShape", poseShape);
        RenderState rs = poseMaterial.getAdditionalRenderState();
        rs.setBlendMode(RenderState.BlendMode.Alpha);
        rs.setDepthTest(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Create a node with the text and background for a label, but don't parent
     * it.
     *
     * @param labelText text of the label (not null)
     * @param sizeFactor text size relative to preferred size (&gt;0)
     * @param textColor color for the text (not null, unaffected)
     * @param bgMaterial material for the background, or null for no background
     * @param width size in the local X direction (in local units, &gt;0)
     * @param height size in the local Y direction (in local units, &gt;0)
     * @return a new orphan spatial with its local origin at its upper left
     * corner
     */
    Spatial makeLabel(String labelText,
            float sizeFactor, ColorRGBA textColor, Material bgMaterial,
            float width, float height) {
        assert labelText != null;
        assert sizeFactor > 0f : sizeFactor;
        assert textColor != null;
        assert width > 0f : width;
        assert height > 0f : height;

        Node node = new Node();
        if (bgMaterial != null) {
            /*
             * Create a rounded rectangle for the background geometry.
             */
            float cornerRadius = 0.2f * Math.min(width, height);
            Mesh bgMesh = new RoundedRectangle(0f, width, -height, 0f,
                    cornerRadius, 1f);
            String bgName = "bg"; // + nameSuffix;
            Geometry bgGeometry = new Geometry(bgName, bgMesh);
            bgGeometry.setMaterial(bgMaterial);
            node.attachChild(bgGeometry);
        }
        /*
         * Create a text node, centered on, and slightly in front of, the
         * background.
         */
        TrueTypeNode<?> textNode = labelFont.getText(labelText, 0,
                textColor.clone());
        textNode.setLocalScale(sizeFactor);
        float dx = width - textNode.getWidth() * sizeFactor;
        float dy = height - textNode.getHeight() * sizeFactor;
        textNode.setLocalTranslation(dx / 2f, -dy / 2f, 0.01f);
        //String textName = "text" + nameSuffix;
        //spatial.setName(textName);
        node.attachChild(textNode);

        return node;
    }
}
