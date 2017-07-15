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

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.LineWrapMode;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Line;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.Validate;
import jme3utilities.mesh.RectangleMesh;
import jme3utilities.mesh.RectangleOutlineMesh;
import maud.mesh.Finial;
import maud.mesh.Sparkline;
import maud.model.LoadedCgm;

/**
 * A 2D visualization of a loaded animation in Maud's "score" mode.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScoreView {
    // *************************************************************************
    // constants and loggers

    /**
     * height of a spark line (in world units)
     */
    final private static float sparklineHeight = 0.08f;
    /**
     * end-cap mesh to represent a bone track without scales
     */
    final private static Finial finialNoScales = new Finial(true, true, false,
            sparklineHeight);
    /**
     * end-cap mesh to represent a bone track with scales
     */
    final private static Finial finialWithScales = new Finial(true, true, true,
            sparklineHeight);
    /**
     * horizontal size of hash mark (in world units)
     */
    final private static float hashSize = 0.05f;
    /**
     * world Z-coordinate for lines
     */
    final private static float z = -10f;
    /**
     * hash-mark mesh to represent a bone without a track, or any bone when the
     * POV is zoomed all the way out
     */
    final private static Line hashMark = new Line(
            new Vector3f(-hashSize, 0f, 0f), new Vector3f(0f, 0f, 0f)
    );
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ScoreView.class.getName());
    /**
     * rectangle for a right end cap when the POV is zoomed part way out
     */
    final private static Mesh rectangle = new RectangleOutlineMesh(0f, hashSize,
            -1f, 0f);
    // *************************************************************************
    // fields

    /**
     * font for labels
     */
    private static BitmapFont labelFont = null;
    /**
     * height of this score (in world units, &ge;0)
     */
    private float height = 0f;
    /**
     * array to pass a single X value to
     * {@link #attachSparkline(float[], float[], com.jme3.scene.Mesh.Mode, java.lang.String, int, com.jme3.material.Material)}
     */
    final private float[] tempX = new float[1];
    /**
     * array to pass a single Y value to
     * {@link #attachSparkline(float[], float[], com.jme3.scene.Mesh.Mode, java.lang.String, int, com.jme3.material.Material)}
     */
    final private float[] tempY = new float[1];
    /**
     * temporary storage for bone-track data
     */
    private float[] ts, ws, xs, ys, zs;
    /**
     * index of the bone currently being visualized
     */
    private int currentBone;
    /**
     * the CG model being rendered
     */
    private LoadedCgm cgm;
    /**
     * world Y-coordinate of each bone
     */
    final private Map<Integer, Float> boneYs = new HashMap<>(120);
    /**
     * material for label backgrounds of non-selected bones
     */
    private static Material bgNotSelected = null;
    /**
     * material for label backgrounds of selected bones
     */
    private static Material bgSelected = null;
    /**
     * material for finials of non-selected bones
     */
    private static Material wireNotSelected = null;
    /**
     * material for finials of selected bones
     */
    private static Material wireSelected = null;
    /**
     * material for sparklines of W components
     */
    private static Material wMaterial = null;
    /**
     * material for sparklines of X components
     */
    private static Material xMaterial = null;
    /**
     * material for sparklines of Y components
     */
    private static Material yMaterial = null;
    /**
     * material for sparklines of Z components
     */
    private static Material zMaterial = null;
    /**
     * visualization subtree: attach geometries here
     */
    private Node visuals = null;
    /**
     * view port used when the screen isn't split, or null for none
     */
    final private ViewPort viewPort1;
    /**
     * view port used in score view mode when the screen is split (not null)
     */
    final private ViewPort viewPort2;
    /**
     * view port used in hybrid view mode, or null for none
     */
    final private ViewPort viewPort3;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new visualization.
     *
     * @param port1 initial view port, or null for none (alias created)
     * @param port2 view port to use after the screen is split (not null, alias
     * created)
     * @param port3 view port to use in hybrid view mode (alias created)
     */
    public ScoreView(ViewPort port1, ViewPort port2, ViewPort port3) {
        Validate.nonNull(port2, "port2");

        viewPort1 = port1;
        viewPort2 = port2;
        viewPort3 = port3;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the distance from the specified screen coordinates to the
     * screen segment of the indexed bone.
     *
     * @param boneI which bone (&ge;0)
     * @param p input screen coordinates (not null)
     * @return square of the distance in pixels (&ge;0)
     */
    public float dSquared(int boneI, Vector2f p) {
        Validate.nonNegative(boneI, "bone index");
        Validate.nonNull(p, "input point");

        float boneY = boneYs.get(boneI);
        Vector3f boneInWorld0 = new Vector3f(0f, boneY, z);
        Vector3f boneInWorld1 = new Vector3f(1f, boneY, z);
        /*
         * Calculate the endpoints of the segment in screen space
         * that represent the bone.
         */
        Camera camera = getCamera();
        Vector3f boneInScreen0 = camera.getScreenCoordinates(boneInWorld0);
        Vector3f boneInScreen1 = camera.getScreenCoordinates(boneInWorld1);
        Vector2f a = new Vector2f(boneInScreen0.x, boneInScreen0.y);
        Vector2f b = new Vector2f(boneInScreen1.x, boneInScreen1.y);
        /*
         * Calculate the point on the bone segment closest to the input point.
         */
        Vector2f bma = b.subtract(a);
        Vector2f pma = p.subtract(a);
        float dot = bma.dot(pma);
        float t = dot / bma.lengthSquared();
        t = FastMath.clamp(t, 0f, 1f);
        Vector2f closest = new Vector2f(a.x + t * bma.x, a.y + t * bma.y);

        float dSquared = p.distanceSquared(closest);

        return dSquared;
    }

    /**
     * Access the camera used to render this score.
     *
     * @return a pre-existing instance, or null if none
     */
    public Camera getCamera() {
        Camera result = null;

        ViewPort viewPort = getViewPort();
        if (viewPort != null) {
            result = viewPort.getCamera();
        }

        return result;
    }

    /**
     * Read the height of this score.
     *
     * @return height (in world units, &ge;0)
     */
    public float getHeight() {
        assert height >= 0f : height;
        return height;
    }

    /**
     * Access the view port being used to render this score.
     *
     * @return a pre-existing, enabled view port, or null if none
     */
    public ViewPort getViewPort() {
        ViewPort result = null;
        String viewMode = Maud.model.misc.getViewMode();
        if (viewMode.equals("hybrid")) {
            result = viewPort3;
        } else if (viewMode.equals("score")) {
            if (Maud.model.source.isLoaded()) {
                result = viewPort2;
            } else {
                result = viewPort1;
            }
        }

        return result;
    }

    /**
     * Update prior to rendering. (Invoked once per render pass on each
     * instance.)
     *
     * @param renderCgm which CG model to render (not null)
     */
    void update(LoadedCgm renderCgm) {
        if (wireNotSelected == null) { // TODO add an init method
            Maud application = Maud.getApplication();
            AssetManager assetManager = application.getAssetManager();
            initializeMaterials(assetManager);
            labelFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        }
        boneYs.clear();

        ViewPort viewPort = getViewPort();
        if (viewPort != null && viewPort.isEnabled()) {
            cgm = renderCgm;
            assert renderCgm.isLoaded();

            cgm.scorePov.updateCamera();

            ColorRGBA backgroundColor = Maud.model.misc.backgroundColor(null);
            viewPort.setBackgroundColor(backgroundColor);

            Spatial parentSpatial = viewPort.getScenes().get(0);
            visuals = (Node) parentSpatial;
            visuals.detachAllChildren();
            height = 0f;

            int numBones = cgm.bones.countBones();
            for (currentBone = 0; currentBone < numBones; currentBone++) {
                attachStaff();
                if (currentBone != numBones - 1) {
                    height += 0.1f; // space between staves
                }
            }

            attachGnomon();
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Attach a right-aligned bone label to the visuals.
     *
     * @param rightX world X coordinate for the right edge of the label
     * @param centerY world Y coordinate for the center of the label
     * @param minWidth minimum width of label (in compressed units, &gt;0)
     * @param maxWidth maximum width of label (in compressed units, &gt;0)
     * @param maxHeight minimum height of label (in world units, &gt;0)
     */
    private void attachBoneLabel(float rightX, float centerY, float minWidth,
            float maxWidth, float maxHeight) {
        assert minWidth > 0f : minWidth;
        assert maxWidth >= minWidth : maxWidth;
        assert maxHeight > 0f : maxHeight;

        Material bgMaterial;
        int selectedBoneIndex = cgm.bone.getIndex();
        if (currentBone == selectedBoneIndex) {
            bgMaterial = bgSelected;
        } else {
            bgMaterial = bgNotSelected;
        }

        String text = cgm.bones.getBoneName(currentBone);
        /*
         * Calculate the effective width and height for the label and the size
         * for the text.
         */
        float boxHeight = maxHeight;
        float sizeFactor = 0.042f * boxHeight; // relative to preferred size
        float boxWidth = sizeFactor * (4f + labelFont.getLineWidth(text));
        if (boxWidth > maxWidth) {
            /*
             * Shrink to avoid clipping.
             */
            boxHeight *= maxWidth / boxWidth;
            sizeFactor *= maxWidth / boxWidth;
            boxWidth = sizeFactor * (4f + labelFont.getLineWidth(text));
        }
        boxWidth = FastMath.clamp(boxWidth, minWidth, maxWidth);
        /*
         * Attach a text node.
         */
        BitmapText label = new BitmapText(labelFont);
        visuals.attachChild(label);
        label.setBox(new Rectangle(0f, 0f, boxWidth, boxHeight));
        label.setLineWrapMode(LineWrapMode.Clip);
        float compression = cgm.scorePov.compression();
        label.setLocalScale(compression, 1f, 1f);
        float leftX = rightX - boxWidth * compression;
        float topY = centerY + boxHeight / 2;
        label.setLocalTranslation(leftX, topY, z + 0.2f);
        String labelName = String.format("label%d", currentBone);
        label.setName(labelName);
        label.setQueueBucket(RenderQueue.Bucket.Transparent);
        float size = sizeFactor * labelFont.getPreferredSize();
        label.setSize(size);
        label.setText(text);
        /*
         * Attach a background geometry to the text node.
         */
        String bgName = String.format("bg%d", currentBone);
        Mesh bgMesh = new RectangleMesh(0f, boxWidth, -boxHeight, 0f, 1f);
        Geometry bg = new Geometry(bgName, bgMesh);
        label.attachChild(bg);
        bg.setLocalTranslation(0f, 0f, -0.01f); // slightly behind the text
        bg.setMaterial(bgMaterial);
        bg.setQueueBucket(RenderQueue.Bucket.Opaque);
    }

    /**
     * Attach a pair of finials to indicate an animated bone.
     *
     * @return the height of each finial (in world units, &ge;0)
     */
    private float attachFinials() {
        Finial finial = finialNoScales;
        boolean hasScales = cgm.animation.hasScales(currentBone);
        if (hasScales) {
            finial = finialWithScales;
        }

        Material wireMaterial = wireNotSelected;
        int selectedBoneIndex = cgm.bone.getIndex();
        if (currentBone == selectedBoneIndex) {
            wireMaterial = wireSelected;
        }
        /*
         * Attach the left-hand finial.
         */
        String name = String.format("left finial%d", currentBone);
        Geometry geometry = new Geometry(name, finial);
        geometry.setLocalTranslation(0f, -height, z);
        geometry.setMaterial(wireMaterial);
        visuals.attachChild(geometry);
        /*
         * Attach a bone label to the left of the left-hand finial.
         */
        float leftX = cgm.scorePov.leftX();
        float rightX = -hashSize;
        float staffHeight = finial.getHeight();
        float middleY = -(height + staffHeight / 2);
        float compression = cgm.scorePov.compression();
        float maxWidth = (rightX - leftX) / compression;
        float minWidth = hashSize / compression;
        attachBoneLabel(rightX, middleY, minWidth, maxWidth, staffHeight);
        /*
         * Attach the right-hand finial.
         */
        name = String.format("right finial%d", currentBone);
        geometry = new Geometry(name, finial);
        geometry.setLocalTranslation(1f, -height, z);
        geometry.setLocalScale(-1f, 1f, 1f);
        geometry.setMaterial(wireMaterial);
        visuals.attachChild(geometry);

        return staffHeight;
    }

    /**
     * Attach the time indicator (currently just a vertical line) to the
     * visuals.
     */
    private void attachGnomon() {
        float duration = cgm.animation.getDuration();
        float x = 0f;
        if (duration > 0f) {
            float time = cgm.animation.getTime();
            x = time / duration;
        }

        float handleSize = 0.1f * cgm.scorePov.getHalfHeight();
        Vector3f start = new Vector3f(x, handleSize, z);
        Vector3f end = new Vector3f(x, -height - handleSize, z);
        Line line = new Line(start, end);

        Geometry geometry = new Geometry("gnomon", line);
        geometry.setMaterial(wireNotSelected);
        visuals.attachChild(geometry);
    }

    /**
     * Attach a pair of hash marks to indicate a bone.
     *
     * @param staffHeight (&ge;0)
     */
    private void attachHashes(float staffHeight) {
        float y = -height - staffHeight / 2;

        Material material = wireNotSelected;
        int selectedBoneIndex = cgm.bone.getIndex();
        if (currentBone == selectedBoneIndex) {
            material = wireSelected;
        }

        String name = String.format("left hash%d", currentBone);
        Geometry geometry = new Geometry(name, hashMark);
        geometry.setLocalTranslation(0f, y, z);
        geometry.setMaterial(material);
        visuals.attachChild(geometry);

        name = String.format("right hash%d", currentBone);
        geometry = new Geometry(name, hashMark);
        geometry.setLocalTranslation(1f, y, z);
        geometry.setLocalScale(-1f, 1f, 1f);
        geometry.setMaterial(material);
        visuals.attachChild(geometry);
    }

    /**
     * Attach sparklines to visualize a single data series in the current bone's
     * track.
     *
     * @param pxx array of X-values for points (not null, unaffected)
     * @param pyy array of Y-values for points (not null, unaffected)
     * @param lxx array of X-values for lines (not null, unaffected)
     * @param lyy array of X-values for lines (not null, unaffected)
     * @param suffix suffix for the geometry name (not null)
     * @param yIndex position in the staff (&ge;0, &lt;10, 0&rarr; top position)
     * @param material material for the geometry (not null)
     */
    private void attachPlot(float[] pxx, float[] pyy, float[] lxx, float[] lyy,
            String suffix, int yIndex, Material material) {
        assert pxx != null;
        assert pyy != null;
        assert lxx != null;
        assert lyy != null;
        assert suffix != null;
        assert yIndex >= 0 : yIndex;
        assert yIndex < 10 : yIndex;
        assert material != null;

        if (Util.distinct(pyy)) {
            attachSparkline(pxx, pyy, Mesh.Mode.Points, suffix + "p", yIndex,
                    material);

            float zoom = cgm.scorePov.getHalfHeight();
            if (zoom < 10f) {
                /*
                 * Draw connecting lines only when zoomed in.
                 */
                attachSparkline(lxx, lyy, Mesh.Mode.LineStrip, suffix + "l",
                        yIndex, material);
            }
        } else {
            /*
             * Series consists entirely of single value: show 1st keyframe only.
             */
            tempX[0] = pxx[0];
            tempY[0] = pyy[0];
            attachSparkline(tempX, tempY, Mesh.Mode.Points, suffix + "p",
                    yIndex, material);
        }
    }

    /**
     * Attach a pair of rectangles to indicate an animated bone.
     *
     * @param staffHeight (in world units, &ge;0)
     */
    private void attachRectangles(float staffHeight) {
        assert staffHeight >= 0f : staffHeight;

        Material wireMaterial;
        int selectedBoneIndex = cgm.bone.getIndex();
        if (currentBone == selectedBoneIndex) {
            wireMaterial = wireSelected;
        } else {
            wireMaterial = wireNotSelected;
        }
        /*
         * Attach the left-hand rectangle: a narrow outline.
         */
        String rectName = String.format("left rect%d", currentBone);
        Geometry geometry = new Geometry(rectName, rectangle);
        visuals.attachChild(geometry);
        geometry.setLocalScale(-0.2f, staffHeight, 1f);
        geometry.setLocalTranslation(0f, -height, z);
        geometry.setMaterial(wireMaterial);
        /*
         * Attach a bone label to the left of the left-hand rectangle.
         */
        float leftX = cgm.scorePov.leftX();
        float rightX = -0.2f * hashSize;
        float middleY = -(height + staffHeight / 2);
        float compression = cgm.scorePov.compression();
        float maxWidth = (rightX - leftX) / compression;
        float minWidth = hashSize / compression;
        attachBoneLabel(rightX, middleY, minWidth, maxWidth, staffHeight);
        /*
         * Attach the right-hand rectangle: an outline.
         */
        rectName = String.format("right rect%d", currentBone);
        geometry = new Geometry(rectName, rectangle);
        visuals.attachChild(geometry);
        geometry.setLocalScale(1f, staffHeight, 1f);
        geometry.setLocalTranslation(1f, -height, z);
        geometry.setMaterial(wireMaterial);
    }

    /**
     * Attach 3 plots to visualize bone rotations.
     */
    private void attachRotations() {
        cgm.animation.trackRotations(currentBone, ws, xs, ys, zs);
        Util.normalize(ws);
        Util.normalize(xs);
        Util.normalize(ys);
        Util.normalize(zs);

        // TODO interpolation
        attachPlot(ts, ws, ts, ws, "rw", 3, wMaterial);
        attachPlot(ts, xs, ts, xs, "rx", 4, xMaterial);
        attachPlot(ts, ys, ts, ys, "ry", 5, yMaterial);
        attachPlot(ts, zs, ts, zs, "rz", 6, zMaterial);
    }

    /**
     * Attach 3 plots to visualize bone scales.
     */
    private void attachScales() {
        cgm.animation.trackScales(currentBone, xs, ys, zs);
        Util.normalize(xs);
        Util.normalize(ys);
        Util.normalize(zs);

        attachPlot(ts, xs, ts, xs, "sx", 7, xMaterial);
        attachPlot(ts, ys, ts, ys, "sy", 8, yMaterial);
        attachPlot(ts, zs, ts, zs, "sz", 9, zMaterial);
    }

    /**
     * Attach a single sparkline to the visualization.
     *
     * @param xx array of X-values for the sparkline (not null, unaffected)
     * @param yy array of Y-values for the sparkline (not null, unaffected)
     * @param mode mesh mode for the sparkline (Mode.LineStrip, or Mode.Points)
     * @param suffix suffix for the geometry name (not null)
     * @param yIndex position in the staff (&ge;0, &lt;10, 0&rarr; top position)
     * @param material material for the geometry (not null)
     */
    private void attachSparkline(float[] xx, float[] yy, Mesh.Mode mode,
            String suffix, int yIndex, Material material) {
        assert xx != null;
        assert yy != null;
        assert suffix != null;
        assert yIndex >= 0 : yIndex;
        assert yIndex < 10 : yIndex;
        assert material != null;

        Sparkline sparkline = new Sparkline(xx, yy, sparklineHeight, mode);
        String name = String.format("%d%s", currentBone, suffix);
        Geometry geometry = new Geometry(name, sparkline);

        float yOffset = sparklineHeight + yIndex * (float) Finial.hpf;
        float y = -height - yOffset;
        geometry.setLocalTranslation(0f, y, z);
        geometry.setMaterial(material);
        visuals.attachChild(geometry);
    }

    /**
     * Attach the sparklines for the current bone.
     */
    private void attachSparklines() {
        ts = cgm.animation.trackTimes(currentBone);
        float duration = cgm.animation.getDuration();
        Util.normalize(ts, 0f, duration);

        int numFrames = ts.length;
        if (ws == null || numFrames != ws.length) {
            ws = new float[numFrames];
            xs = new float[numFrames];
            ys = new float[numFrames];
            zs = new float[numFrames];
        }

        attachTranslations();
        attachRotations();
        if (cgm.animation.hasScales(currentBone)) {
            attachScales();
        }
    }

    /**
     * Attach a staff to visualize the current bone.
     */
    private void attachStaff() {
        if (cgm.animation.hasTrackForBone(currentBone)) {
            float staffHeight;
            float zoom = cgm.scorePov.getHalfHeight();
            if (zoom > 4f) { // was 10
                /*
                 * zoomed out too far to include detailed finials
                 */
                boolean hasScales = cgm.animation.hasScales(currentBone);
                int numFeet = hasScales ? 10 : 7;
                staffHeight = (float) (numFeet * Finial.hpf);
                if (zoom > 25f) {
                    attachHashes(staffHeight);
                } else {
                    attachRectangles(staffHeight);
                }
            } else {
                staffHeight = attachFinials();
            }
            attachSparklines();
            boneYs.put(currentBone, -height - staffHeight / 2);
            height += staffHeight;

        } else {
            attachHashes(0f);
            boneYs.put(currentBone, -height);
        }
    }

    /**
     * Attach 3 plots to visualize bone translations.
     */
    private void attachTranslations() {
        cgm.animation.trackTranslations(currentBone, xs, ys, zs);
        Util.normalize(xs);
        Util.normalize(ys);
        Util.normalize(zs);

        attachPlot(ts, xs, ts, xs, "tx", 0, xMaterial);
        attachPlot(ts, ys, ts, ys, "ty", 1, yMaterial);
        attachPlot(ts, zs, ts, zs, "tz", 2, zMaterial);
    }

    /**
     * Initialize materials used in score views.
     *
     * @param assetManager (not null)
     */
    private void initializeMaterials(AssetManager assetManager) {
        assert assetManager != null;

        ColorRGBA black = new ColorRGBA(0f, 0f, 0f, 1f);
        ColorRGBA blue = new ColorRGBA(0f, 0f, 1f, 1f);
        ColorRGBA green = new ColorRGBA(0f, 1f, 0f, 1f);
        ColorRGBA grey = new ColorRGBA(0.5f, 0.5f, 0.5f, 1f);
        ColorRGBA red = new ColorRGBA(1f, 0f, 0f, 1f);
        ColorRGBA white = new ColorRGBA(1f, 1f, 1f, 1f);
        float pointSize = 3f;

        bgNotSelected = MyAsset.createUnshadedMaterial(assetManager, grey);
        bgSelected = MyAsset.createUnshadedMaterial(assetManager, black);

        wireNotSelected = MyAsset.createWireframeMaterial(assetManager, grey);
        wireSelected = MyAsset.createWireframeMaterial(assetManager, black);

        wMaterial = MyAsset.createWireframeMaterial(assetManager, white,
                pointSize);
        xMaterial = MyAsset.createWireframeMaterial(assetManager, red,
                pointSize);
        yMaterial = MyAsset.createWireframeMaterial(assetManager, green,
                pointSize);
        zMaterial = MyAsset.createWireframeMaterial(assetManager, blue,
                pointSize);
    }
}
