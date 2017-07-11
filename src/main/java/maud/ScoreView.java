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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Line;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.Rectangle;
import jme3utilities.Validate;
import maud.model.LoadedCgm;

/**
 * A 2D visualization of a loaded animation in a view port.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScoreView {
    // *************************************************************************
    // constants and loggers

    /**
     * horizontal size of hash mark (in world units)
     */
    final private static float hashSize = 0.05f;
    /**
     * height of a spark line (in world units, &ge;0)
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
     * Z-coordinate for lines
     */
    final private static float z = -10f;
    /**
     * hash-mark mesh to represent a bone without a track
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
     * rectangle to represent a bone track when the POV is zoomed out
     */
    final private static Rectangle rectangle = new Rectangle(0f, 1f, 0f, 1f,
            -hashSize, 0f, -1f, 0f, 1f);
    // *************************************************************************
    // fields

    /**
     * background color for this view TODO configure it
     */
    private final ColorRGBA backgroundColor = new ColorRGBA(
            0.84f, 0.84f, 0.72f, 1f);
    /**
     * height of this score (in world units, &ge;0)
     */
    private float height = 0f;
    /**
     * array to pass a single X value to makeSparkline()
     */
    final private float[] tempX = new float[1];
    /**
     * array to pass a single Y value to makeSparkline()
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
     * world Y-coordinate of each bone
     */
    final private Map<Integer, Float> boneYs = new HashMap<>(120);
    /**
     * CG model that owns this view (not null)
     */
    final private LoadedCgm cgm;
    /**
     * material for finials of non-selected bones
     */
    private static Material notSelected = null;
    /**
     * material for finials of selected bones
     */
    private static Material selected = null;
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
     * view port used when the screen is split (not null)
     */
    final private ViewPort viewPort2;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new visualization.
     *
     * @param loadedCgm loaded CG model that will own this view (not null, alias
     * created)
     * @param port1 initial view port, or null for none (alias created)
     * @param port2 view port to use after the screen is split (not null, alias
     * created)
     */
    public ScoreView(LoadedCgm loadedCgm, ViewPort port1, ViewPort port2) {
        Validate.nonNull(loadedCgm, "loaded model");
        Validate.nonNull(port2, "port2");

        cgm = loadedCgm;
        viewPort1 = port1;
        viewPort2 = port2;
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
        ViewPort result;
        if (Maud.model.source.isLoaded()) {
            result = viewPort2;
        } else {
            result = viewPort1;
        }

        return result;
    }

    /**
     * Update prior to rendering. (Invoked once per render pass on each
     * instance.)
     */
    void update() {
        if (notSelected == null) {
            initializeMaterials();
        }
        boneYs.clear();

        ViewPort viewPort = getViewPort();
        if (viewPort != null && viewPort.isEnabled()) {
            assert cgm.isLoaded();
            viewPort.setBackgroundColor(backgroundColor);

            Spatial parentSpatial = viewPort.getScenes().get(0);
            visuals = (Node) parentSpatial;
            visuals.detachAllChildren();
            height = 0f;

            int numBones = cgm.bones.countBones();
            for (currentBone = 0; currentBone < numBones;
                    currentBone++) {
                makeStaff();
                if (currentBone != numBones - 1) {
                    height += 0.1f; // space between staves
                }
            }

            makeGnomon();
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize wireframe materials.
     */
    private void initializeMaterials() {
        Maud application = Maud.getApplication();
        AssetManager assetManager = application.getAssetManager();

        ColorRGBA black = new ColorRGBA(0f, 0f, 0f, 1f);
        notSelected = MyAsset.createWireframeMaterial(assetManager, black);

        ColorRGBA white = new ColorRGBA(1f, 1f, 1f, 1f);
        selected = MyAsset.createWireframeMaterial(assetManager, white);

        wMaterial = MyAsset.createWireframeMaterial(assetManager, white);
        wMaterial.setFloat("PointSize", 3f);

        ColorRGBA red = new ColorRGBA(1f, 0f, 0f, 1f);
        xMaterial = MyAsset.createWireframeMaterial(assetManager, red);
        xMaterial.setFloat("PointSize", 3f);

        ColorRGBA green = new ColorRGBA(0f, 1f, 0f, 1f);
        yMaterial = MyAsset.createWireframeMaterial(assetManager, green);
        yMaterial.setFloat("PointSize", 3f);

        ColorRGBA blue = new ColorRGBA(0f, 0f, 1f, 1f);
        zMaterial = MyAsset.createWireframeMaterial(assetManager, blue);
        zMaterial.setFloat("PointSize", 3f);
    }

    /**
     * Add a pair of finials to the visuals.
     *
     * @return the height of each finial (in world units, &ge;0)
     */
    private float makeFinials() {
        Finial finial = finialNoScales;
        boolean hasScales = cgm.animation.hasScales(currentBone);
        if (hasScales) {
            finial = finialWithScales;
        }

        Material material = notSelected;
        int selectedBoneIndex = cgm.bone.getIndex();
        if (currentBone == selectedBoneIndex) {
            material = selected;
        }

        String name = String.format("left finial%d", currentBone);
        Geometry geometry = new Geometry(name, finial);
        geometry.setLocalTranslation(0f, -height, z);
        geometry.setMaterial(material);
        visuals.attachChild(geometry);

        name = String.format("right finial%d", currentBone);
        geometry = new Geometry(name, finial);
        geometry.setLocalTranslation(1f, -height, z);
        geometry.setLocalScale(-1f, 1f, 1f);
        geometry.setMaterial(material);
        visuals.attachChild(geometry);

        float staffHeight = finial.getHeight();
        return staffHeight;
    }

    /**
     * Add the time indicator (currently just a vertical line) to the visuals.
     */
    private void makeGnomon() {
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
        geometry.setMaterial(notSelected);
        visuals.attachChild(geometry);
    }

    /**
     * Add a pair of hash marks to indicate a bone.
     *
     * @param staffHeight (&ge;0)
     */
    private void makeHashes(float staffHeight) {
        float y = -height - staffHeight / 2;

        Material material = notSelected;
        int selectedBoneIndex = cgm.bone.getIndex();
        if (currentBone == selectedBoneIndex) {
            material = selected;
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
     * Add sparklines to visualize the data in the current bone's track.
     *
     * @param pxx array of X-values for points (not null, unaffected)
     * @param pyy array of Y-values for points (not null, unaffected)
     * @param lxx array of X-values for lines (not null, unaffected)
     * @param lyy array of X-values for lines (not null, unaffected)
     * @param suffix suffix for the geometry name (not null)
     * @param yIndex position in the staff (&ge;0, 0&rarr; top position)
     * @param material material for the geometry (not null)
     */
    private void makePlot(float[] pxx, float[] pyy, float[] lxx, float[] lyy,
            String suffix, int yIndex, Material material) {
        assert pxx != null;
        assert pyy != null;
        assert lxx != null;
        assert lyy != null;
        assert suffix != null;
        assert yIndex >= 0 : yIndex;
        assert material != null;

        if (Util.distinct(pyy)) {
            makeSparkline(pxx, pyy, Mesh.Mode.Points, suffix + "p", yIndex,
                    material);
            makeSparkline(lxx, lyy, Mesh.Mode.Lines, suffix + "l", yIndex,
                    material);
        } else {
            tempX[0] = pxx[0];
            tempY[0] = pyy[0];
            makeSparkline(tempX, tempY, Mesh.Mode.Points, suffix + "p", yIndex,
                    material);
        }
    }

    /**
     * Add a pair of rectangles to indicate an animated bone.
     *
     * @param staffHeight (&ge;0)
     */
    private void makeRectangles(float staffHeight) {
        assert staffHeight >= 0f : staffHeight;

        Material material = notSelected;
        int selectedBoneIndex = cgm.bone.getIndex();
        if (currentBone == selectedBoneIndex) {
            material = selected;
        }

        String name = String.format("left rect%d", currentBone);
        Geometry geometry = new Geometry(name, rectangle);
        geometry.setLocalTranslation(0f, -height, z);
        geometry.setLocalScale(1f, staffHeight, 1f);
        geometry.setMaterial(material);
        visuals.attachChild(geometry);

        name = String.format("right rect%d", currentBone);
        geometry = new Geometry(name, rectangle);
        geometry.setLocalTranslation(1f + hashSize, -height, z);
        geometry.setLocalScale(1f, staffHeight, 1f);
        geometry.setMaterial(material);
        visuals.attachChild(geometry);
    }

    /**
     * Add sparklines to visualize bone rotations.
     */
    private void makeRotation() {
        cgm.animation.trackRotations(currentBone, ws, xs, ys, zs);
        Util.normalize(ws);
        Util.normalize(xs);
        Util.normalize(ys);
        Util.normalize(zs);

        // TODO interpolation
        makePlot(ts, ws, ts, ws, "rw", 3, wMaterial);
        makePlot(ts, xs, ts, xs, "rx", 4, xMaterial);
        makePlot(ts, ys, ts, ys, "ry", 5, yMaterial);
        makePlot(ts, zs, ts, zs, "rz", 6, zMaterial);
    }

    /**
     * Add sparklines to visualize bone scales.
     */
    private void makeScale() {
        cgm.animation.trackScales(currentBone, xs, ys, zs);
        Util.normalize(xs);
        Util.normalize(ys);
        Util.normalize(zs);

        makePlot(ts, xs, ts, xs, "sx", 7, xMaterial);
        makePlot(ts, ys, ts, ys, "sy", 8, yMaterial);
        makePlot(ts, zs, ts, zs, "sz", 9, zMaterial);
    }

    /**
     * Add a single sparkline to the visualization.
     *
     * @param xx array of X-values for the sparkline (not null, unaffected)
     * @param yy array of Y-values for the sparkline (not null, unaffected)
     * @param mode mesh mode for the sparkline (Mode.LineStrip, or Mode.Points)
     * @param suffix suffix for the geometry name (not null)
     * @param yIndex position in the staff (&ge;0, 0&rarr; top position)
     * @param material material for the geometry (not null)
     */
    private void makeSparkline(float[] xx, float[] yy, Mesh.Mode mode,
            String suffix, int yIndex, Material material) {
        assert xx != null;
        assert yy != null;
        assert suffix != null;
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
     * Add the sparklines for the current bone.
     */
    private void makeSparklines() {
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

        makeTranslation();
        makeRotation();
        if (cgm.animation.hasScales(currentBone)) {
            makeScale();
        }
    }

    /**
     * Add a staff to visualize the current bone.
     */
    private void makeStaff() {
        if (cgm.animation.hasTrackForBone(currentBone)) {
            float staffHeight;
            float zoom = cgm.scorePov.getHalfHeight();
            if (zoom > 10f) {
                /*
                 * zoomed out too far to include detailed finials
                 */
                boolean hasScales = cgm.animation.hasScales(currentBone);
                int numFeet = hasScales ? 10 : 7;
                staffHeight = (float) (numFeet * Finial.hpf);
                if (zoom > 25f) {
                    makeHashes(staffHeight);
                } else {
                    makeRectangles(staffHeight);
                }
            } else {
                staffHeight = makeFinials();
            }
            makeSparklines();
            boneYs.put(currentBone, -height - staffHeight / 2);
            height += staffHeight;

        } else {
            makeHashes(0f);
            boneYs.put(currentBone, -height);
        }
    }

    /**
     * Add 3 sparklines to visualize bone translations.
     */
    private void makeTranslation() {
        cgm.animation.trackTranslations(currentBone, xs, ys, zs);
        Util.normalize(xs);
        Util.normalize(ys);
        Util.normalize(zs);

        makePlot(ts, xs, ts, xs, "tx", 0, xMaterial);
        makePlot(ts, ys, ts, ys, "ty", 1, yMaterial);
        makePlot(ts, zs, ts, zs, "tz", 2, zMaterial);
    }
}
