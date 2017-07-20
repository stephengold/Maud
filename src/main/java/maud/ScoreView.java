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
import com.jme3.input.InputManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
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
import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.mesh.RectangleMesh;
import jme3utilities.mesh.RectangleOutlineMesh;
import maud.mesh.Finial;
import maud.mesh.RoundedRectangle;
import maud.mesh.Sparkline;
import maud.mesh.YSwarm;
import maud.model.LoadedCgm;

/**
 * A 2D visualization of a loaded animation in a score-mode viewport.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScoreView implements EditorView {
    // *************************************************************************
    // constants and loggers

    /**
     * color for the w-axis (white)
     */
    final private static ColorRGBA wColor = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * color for the X-axis (red)
     */
    final private static ColorRGBA xColor = new ColorRGBA(1f, 0f, 0f, 1f);
    /**
     * color for the Y-axis (green)
     */
    final private static ColorRGBA yColor = new ColorRGBA(0f, 1f, 0f, 1f);
    /**
     * color for the Z-axis (blue)
     */
    final private static ColorRGBA zColor = new ColorRGBA(0f, 0f, 1f, 1f);
    /**
     * horizontal size of hash mark (in world units)
     */
    final private static float hashSize = 0.05f;
    /**
     * height of a spark line (in world units)
     */
    final private static float sparklineHeight = 0.08f;
    /**
     * horizontal gap between visuals and left/right edges of the viewport (in
     * world units)
     */
    final private static float xGap = 0.01f;
    /**
     * world X-coordinate for left edges of sparklines
     */
    final private static float xLeftMargin = 0f;
    /**
     * world X-coordinate for right edges of sparklines
     */
    final private static float xRightMargin = 1f;
    /**
     * vertical gap between staves (in world units)
     */
    final private static float yGap = 0.1f;
    /**
     * world Z-coordinate for lines
     */
    final private static float zLines = -10f;
    /**
     * world Z-coordinate for labels and icons
     */
    final private static float zLabels = zLines + 1f;
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
     * square mesh for a transform icon
     */
    final private static Mesh iconMesh = new RoundedRectangle(0f, 1f, -0.5f,
            0.5f, 0.3f, 1f);
    /**
     * rectangular outline for an end cap when the POV is zoomed part way out
     */
    final private static Mesh outlineMesh = new RectangleOutlineMesh(0f,
            hashSize, -1f, 0f);
    // *************************************************************************
    // fields

    /**
     * font for labels
     */
    private static BitmapFont labelFont = null;
    /**
     * end-cap mesh for a bone track that includes scales
     */
    private static Finial finialComplete;
    /**
     * end-cap mesh for a bone track without scales
     */
    private static Finial finialNoScales;
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
     * index (in the selected skeleton) of the bone currently being visualized
     */
    private int currentBone;
    /**
     * count of plots added to the current staff (&ge;0)
     */
    private int numPlots = 0;
    /**
     * the CG model being rendered
     */
    private LoadedCgm cgm;
    /**
     * world X-coordinates of each keyframe in the selected track
     */
    final private Map<Integer, Float> frameXs = new HashMap<>(40);
    /**
     * min/max world Y-coordinates of each bone in the CG model
     */
    final private Map<Integer, Vector2f> boneYs = new HashMap<>(120);
    /**
     * material for label backgrounds of non-selected bones
     */
    private static Material bgNotSelected = null;
    /**
     * material for label backgrounds of selected bones
     */
    private static Material bgSelected = null;
    /**
     * material for pose markers
     */
    private static Material poseMaterial;
    /**
     * material for rotation icons
     */
    private static Material rotMaterial;
    /**
     * material for scale icons
     */
    private static Material scaMaterial;
    /**
     * material for translation icons
     */
    private static Material traMaterial;
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
    /**
     * mesh for pose markers
     */
    final private YSwarm poseMesh = new YSwarm(10 * 255);
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
    ScoreView(ViewPort port1, ViewPort port2, ViewPort port3) {
        Validate.nonNull(port2, "port2");

        viewPort1 = port1;
        viewPort2 = port2;
        viewPort3 = port3;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the height of this score, not including the gnomon.
     *
     * @return height (in world units, &ge;0)
     */
    public float getHeight() {
        assert height >= 0f : height;
        return height;
    }
    // *************************************************************************
    // EditorView methods

    /**
     * Consider selecting each bone, axis, and keyframe in this view.
     *
     * @param selection best selection found so far (not null, modified)
     */
    @Override
    public void considerAll(Selection selection) {
        Camera camera = getCamera();
        int selectedBone = cgm.bone.getIndex();
        Vector2f inputXY = selection.copyInputXY();

        boolean isSelected = cgm.bone.isSelected();
        if (isSelected) {
            Vector2f minMax = boneYs.get(selectedBone);
            Vector3f world1 = new Vector3f(xRightMargin, minMax.x, zLines);
            Vector3f world2 = new Vector3f(xRightMargin, minMax.y, zLines);
            Vector3f screen1 = camera.getScreenCoordinates(world1);
            Vector3f screen2 = camera.getScreenCoordinates(world2);
            if (Util.isBetween(screen1.y, inputXY.y, screen2.y)) {
                for (Entry<Integer, Float> entry : frameXs.entrySet()) {
                    float frameX = entry.getValue();
                    Vector3f world = new Vector3f(frameX, minMax.y, zLines);
                    Vector3f screen = camera.getScreenCoordinates(world);
                    float dSquared = FastMath.sqr(inputXY.x - screen.x);
                    int frameIndex = entry.getKey();
                    selection.considerKeyframe(cgm, frameIndex, dSquared);
                }
            }
        }

        for (Entry<Integer, Vector2f> entry : boneYs.entrySet()) {
            int boneIndex = entry.getKey();
            if (boneIndex != selectedBone) {
                Vector2f minMax = entry.getValue();

                Vector3f world1 = new Vector3f(xRightMargin, minMax.x, zLines);
                Vector3f world2 = new Vector3f(xRightMargin, minMax.y, zLines);
                Vector3f screen1 = camera.getScreenCoordinates(world1);
                Vector3f screen2 = camera.getScreenCoordinates(world2);

                float dSquared;
                if (Util.isBetween(screen1.y, inputXY.y, screen2.y)) {
                    dSquared = 0f;
                } else {
                    float dSquared1 = FastMath.sqr(inputXY.y - screen1.y);
                    float dSquared2 = FastMath.sqr(inputXY.y - screen2.y);
                    dSquared = Math.min(dSquared1, dSquared2);
                }
                selection.considerBone(cgm, boneIndex, dSquared);
            }
        }
    }

    /**
     * Access the camera used to render this score.
     *
     * @return a pre-existing instance, or null if none
     */
    @Override
    public Camera getCamera() {
        Camera result = null;
        ViewPort viewPort = getViewPort();
        if (viewPort != null) {
            result = viewPort.getCamera();
        }

        return result;
    }

    /**
     * Read the mode of this view.
     *
     * @return "score"
     */
    @Override
    public String getMode() {
        return "score";
    }

    /**
     * Access the view port used to render this view.
     *
     * @return the pre-existing view port
     */
    @Override
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
     * Update this view prior to rendering. (Invoked once per render pass on
     * each instance.)
     *
     * @param renderCgm which CG model to render (not null)
     */
    @Override
    public void update(LoadedCgm renderCgm) {
        Validate.nonNull(renderCgm, "render model");

        if (wireNotSelected == null) { // TODO add an init method
            Maud application = Maud.getApplication();
            AssetManager assetManager = application.getAssetManager();
            initializeMaterials(assetManager);
            labelFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        }
        boneYs.clear();
        frameXs.clear();
        poseMesh.clear();

        ViewPort viewPort = getViewPort();
        if (viewPort != null && viewPort.isEnabled()) {
            cgm = renderCgm;
            assert renderCgm.isLoaded();

            cgm.scorePov.updateCamera();

            ColorRGBA backgroundColor = Maud.model.score.backgroundColor(null);
            viewPort.setBackgroundColor(backgroundColor);

            boolean translations = Maud.model.score.showsTranslations();
            boolean rotations = Maud.model.score.showsRotations();
            boolean scales = Maud.model.score.showsScales();
            finialComplete = new Finial(translations, rotations, scales,
                    sparklineHeight);
            finialNoScales = new Finial(translations, rotations, false,
                    sparklineHeight);

            List<Spatial> roots = viewPort.getScenes();
            int numRoots = roots.size();
            assert numRoots == 1 : numRoots;
            Spatial visualsSpatial = roots.get(0);
            visuals = (Node) visualsSpatial;
            visuals.detachAllChildren();
            height = 0f;

            String bonesShown = Maud.model.score.bonesShown(cgm);
            switch (bonesShown) {
                case "all":
                    attachAllBones();
                    break;
                case "ancestors":
                    List<Integer> indices = cgm.bone.listAncestorIndices();
                    Collections.reverse(indices);
                    attachStaves(indices);
                    break;
                case "family":
                    attachFamilyBones();
                    break;
                case "none":
                    break;
                case "roots":
                    indices = cgm.bones.listRootIndices();
                    attachStaves(indices);
                    break;
                case "selected":
                    currentBone = cgm.bone.getIndex();
                    attachStaff();
                    break;
                case "tracked":
                    indices = cgm.animation.listBoneIndicesWithTracks();
                    Collections.sort(indices);
                    attachStaves(indices);
                    break;
                default:
                    assert false;
            }

            attachGnomon();

            float yLocation = cgm.scorePov.getCameraY();
            cgm.scorePov.setCameraY(yLocation);
        }
    }

    /**
     * Attempt to warp a cursor to the screen coordinates of the mouse pointer.
     */
    @Override
    public void warpCursor() {
        Maud application = Maud.getApplication();
        InputManager inputManager = application.getInputManager();
        Camera camera = getCamera();
        Ray ray = MyCamera.mouseRay(camera, inputManager);
        Vector3f origin = ray.getOrigin();
        float newY = origin.y;
        cgm.scorePov.setCameraY(newY);
    }
    // *************************************************************************
    // private methods

    /**
     * Attach staves for all bones, in index order.
     */
    private void attachAllBones() {
        int numBones = cgm.bones.countBones();
        for (currentBone = 0; currentBone < numBones; currentBone++) {
            if (currentBone > 0) {
                height += yGap;
            }
            attachStaff();
        }
    }

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
        label.setLocalTranslation(leftX, topY, zLabels);
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
     * Attach staves for the selected bone and its parent (if any) and any
     * children, in tree order.
     */
    private void attachFamilyBones() {
        List<Integer> boneIndices = new ArrayList<>(6);

        if (!cgm.bone.isRootBone()) {
            int parentIndex = cgm.bone.parentIndex();
            boneIndices.add(parentIndex);
        }

        int index = cgm.bone.getIndex();
        boneIndices.add(index);

        List<Integer> childIndices = cgm.bone.listChildIndices();
        boneIndices.addAll(childIndices);

        attachStaves(boneIndices);
    }

    /**
     * Attach a pair of finials to indicate an animated bone.
     *
     * @param finial which mesh to use (not null)
     */
    private void attachFinials(Finial finial) {
        assert finial != null;

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
        visuals.attachChild(geometry);
        geometry.setLocalTranslation(xLeftMargin, -height, zLines);
        geometry.setMaterial(wireMaterial);
        /*
         * Attach a bone label to the left of the left-hand finial.
         */
        float leftX = cgm.scorePov.leftX() + xGap;
        float rightX = xLeftMargin - hashSize;
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
        visuals.attachChild(geometry);
        geometry.setLocalTranslation(xRightMargin, -height, zLines);
        geometry.setLocalScale(-1f, 1f, 1f);
        geometry.setMaterial(wireMaterial);
        /*
         * Attach transform icons to the right of the right-hand finial.
         */
        leftX = xRightMargin + hashSize * 2f / 3;
        rightX = cgm.scorePov.rightX() - xGap;
        maxWidth = (rightX - leftX) / compression;
        middleY = -height - sparklineHeight / 2 - (float) Finial.hpf;

        boolean translations = Maud.model.score.showsTranslations();
        if (translations) {
            float maxHeight = 2 * (float) Finial.hpf;
            attachTransformIcon(leftX, middleY, maxWidth, maxHeight, "tra",
                    traMaterial);
            middleY -= 3 * (float) Finial.hpf;
        }

        boolean rotations = Maud.model.score.showsRotations();
        if (rotations) {
            middleY -= 0.5f * (float) Finial.hpf;
            float maxHeight = 3 * (float) Finial.hpf;
            attachTransformIcon(leftX, middleY, maxWidth, maxHeight, "rot",
                    rotMaterial);
            middleY -= 3.5f * (float) Finial.hpf;
        }

        boolean scales = Maud.model.score.showsScales();
        boolean hasScales = cgm.animation.hasScales(currentBone);
        if (scales && hasScales) {
            float maxHeight = 2 * (float) Finial.hpf;
            attachTransformIcon(leftX, middleY, maxWidth, maxHeight, "sca",
                    scaMaterial);
        }
    }

    /**
     * Attach the time indicator to the visuals.
     */
    private void attachGnomon() {
        float duration = cgm.animation.getDuration();
        float x = 0f;
        if (duration > 0f) {
            float time = cgm.animation.getTime();
            x = time / duration;
        }

        float handleSize = 0.1f * cgm.scorePov.getHalfHeight(); // world units
        Vector3f start = new Vector3f(x, handleSize, zLines);
        Vector3f end = new Vector3f(x, -height - handleSize, zLines);
        Line line = new Line(start, end);

        Geometry geometry = new Geometry("gnomon", line);
        visuals.attachChild(geometry);
        geometry.setMaterial(wireNotSelected);

        boolean isEmpty = poseMesh.isEmpty();
        poseMesh.flip();
        if (!isEmpty) {
            /*
             * Attach a point mesh to represent the current pose.
             */
            geometry = new Geometry("pose points", poseMesh);
            visuals.attachChild(geometry);
            geometry.setLocalTranslation(x, 0f, zLines);
            geometry.setMaterial(poseMaterial);
        }
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
        geometry.setLocalTranslation(xLeftMargin, y, zLines);
        geometry.setMaterial(material);
        visuals.attachChild(geometry);

        name = String.format("right hash%d", currentBone);
        geometry = new Geometry(name, hashMark);
        geometry.setLocalTranslation(xRightMargin, y, zLines);
        geometry.setLocalScale(-1f, 1f, 1f); // grows to the right
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
                 * Draw the connecting lines only when zoomed in.
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
        Geometry geometry = new Geometry(rectName, outlineMesh);
        visuals.attachChild(geometry);
        geometry.setLocalScale(-0.2f, staffHeight, 1f);
        geometry.setLocalTranslation(xLeftMargin, -height, zLines);
        geometry.setMaterial(wireMaterial);
        /*
         * Attach a bone label overlapping the left-hand rectangle.
         */
        float leftX = cgm.scorePov.leftX() + xGap;
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
        geometry = new Geometry(rectName, outlineMesh);
        visuals.attachChild(geometry);
        geometry.setLocalScale(1f, staffHeight, 1f);
        geometry.setLocalTranslation(xRightMargin, -height, zLines);
        geometry.setMaterial(wireMaterial);
    }

    /**
     * Attach 3 plots to visualize bone rotations.
     */
    private void attachRotationPlots() {
        cgm.animation.trackRotations(currentBone, ws, xs, ys, zs);
        Quaternion user = cgm.pose.getPose().userRotation(currentBone, null);
        int poseFrame = ts.length;
        ws[poseFrame] = user.getW();
        xs[poseFrame] = user.getX();
        ys[poseFrame] = user.getY();
        zs[poseFrame] = user.getZ();
        MyMath.normalize(ws);
        MyMath.normalize(xs);
        MyMath.normalize(ys);
        MyMath.normalize(zs);

        // TODO interpolation
        attachPlot(ts, ws, ts, ws, "rw", numPlots, wMaterial);
        attachPlot(ts, xs, ts, xs, "rx", numPlots + 1, xMaterial);
        attachPlot(ts, ys, ts, ys, "ry", numPlots + 2, yMaterial);
        attachPlot(ts, zs, ts, zs, "rz", numPlots + 3, zMaterial);

        float scoreY = scoreY(ws[poseFrame], numPlots);
        poseMesh.add(scoreY, wColor);

        scoreY = scoreY(xs[poseFrame], numPlots + 1);
        poseMesh.add(scoreY, xColor);

        scoreY = scoreY(ys[poseFrame], numPlots + 2);
        poseMesh.add(scoreY, yColor);

        scoreY = scoreY(zs[poseFrame], numPlots + 3);
        poseMesh.add(scoreY, zColor);

        numPlots += 4;
    }

    /**
     * Attach 3 plots to visualize bone scales.
     */
    private void attachScalePlots() {
        cgm.animation.trackScales(currentBone, xs, ys, zs);
        Vector3f user = cgm.pose.getPose().userScale(currentBone, null);
        int poseFrame = ts.length;
        xs[poseFrame] = user.x;
        ys[poseFrame] = user.y;
        zs[poseFrame] = user.z;
        MyMath.normalize(xs);
        MyMath.normalize(ys);
        MyMath.normalize(zs);

        attachPlot(ts, xs, ts, xs, "sx", numPlots, xMaterial);
        attachPlot(ts, ys, ts, ys, "sy", numPlots + 1, yMaterial);
        attachPlot(ts, zs, ts, zs, "sz", numPlots + 2, zMaterial);

        float scoreY = scoreY(xs[poseFrame], numPlots);
        poseMesh.add(scoreY, xColor);

        scoreY = scoreY(ys[poseFrame], numPlots + 1);
        poseMesh.add(scoreY, yColor);

        scoreY = scoreY(zs[poseFrame], numPlots + 2);
        poseMesh.add(scoreY, zColor);

        numPlots += 3;
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
        geometry.setLocalTranslation(xLeftMargin, y, zLines);
        geometry.setMaterial(material);
        visuals.attachChild(geometry);
    }

    /**
     * Attach the sparklines for the current bone.
     */
    private void attachSparklines() {
        ts = cgm.animation.trackTimes(currentBone);
        float duration = cgm.animation.getDuration();
        MyMath.normalize(ts, 0f, duration);

        int selectedBone = cgm.bone.getIndex();
        if (currentBone == selectedBone) {
            assert frameXs.isEmpty();
            for (int i = 0; i < ts.length; i++) {
                frameXs.put(i, ts[i]);
            }
        }

        int numFrames = ts.length + 1; // +1 for pose transform
        if (ws == null || numFrames != ws.length) {
            ws = new float[numFrames];
            xs = new float[numFrames];
            ys = new float[numFrames];
            zs = new float[numFrames];
        }

        numPlots = 0;
        boolean showTranslations = Maud.model.score.showsTranslations();
        if (showTranslations) {
            attachTranslationPlots();
        }

        boolean showRotations = Maud.model.score.showsRotations();
        if (showRotations) {
            attachRotationPlots();
        }

        boolean showScales = Maud.model.score.showsScales();
        boolean hasScales = cgm.animation.hasScales(currentBone);
        if (showScales && hasScales) {
            attachScalePlots();
        }
    }

    /**
     * Attach a staff to visualize the current bone.
     */
    private void attachStaff() {
        float zoom = cgm.scorePov.getHalfHeight();
        float staffHeight;

        if (cgm.animation.hasTrackForBone(currentBone)) {
            Finial finial = finialNoScales;
            boolean hasScales = cgm.animation.hasScales(currentBone);
            if (hasScales) {
                finial = finialComplete;
            }
            staffHeight = finial.getHeight();

            if (zoom > 4f) {
                /*
                 * zoomed out too far to render detailed finials
                 */
                if (zoom > 25f) {
                    attachHashes(staffHeight);
                } else {
                    attachRectangles(staffHeight);
                }
            } else {
                attachFinials(finial);
            }

            attachSparklines();

        } else {
            /*
             * no animation track for the current bone
             */
            attachHashes(0f);
            if (zoom < 4f) {
                /*
                 * Attach a bone label overlapping the left-hand hash mark.
                 */
                float leftX = cgm.scorePov.leftX() + xGap;
                float rightX = -0.2f * hashSize;
                float middleY = -height;
                float compression = cgm.scorePov.compression();
                float maxWidth = (rightX - leftX) / compression;
                float minWidth = hashSize / compression;
                attachBoneLabel(rightX, middleY, minWidth, maxWidth, 0.09f);
            }
            staffHeight = 0f;
        }

        float newHeight = height + staffHeight;
        Vector2f minMax = new Vector2f(-newHeight, -height);
        boneYs.put(currentBone, minMax);
        height = newHeight;
    }

    /**
     * Attach staves for the indexed bones in the order specified.
     *
     * @param indices list of bone indices (not null)
     */
    private void attachStaves(List<Integer> indices) {
        assert indices != null;

        int numShown = indices.size();
        for (int i = 0; i < numShown; i++) {
            if (i > 0) {
                height += yGap;
            }
            currentBone = indices.get(i);
            attachStaff();
        }
    }

    /**
     * Attach a left-aligned transform icon to the visuals.
     *
     * @param leftX world X coordinate for the left edge of the icon
     * @param middleY world Y coordinate for the center of the icon
     * @param maxWidth maximum width of icon (in compressed units, &gt;0)
     * @param maxHeight maximum height of icon (in world units, &gt;0)
     * @param prefix prefix for the geometry name (not null)
     * @param material material to apply (not null)
     */
    private void attachTransformIcon(float leftX, float middleY, float maxWidth,
            float maxHeight, String prefix, Material material) {
        assert maxHeight > 0f : maxHeight;
        assert maxWidth > 0f : maxWidth;
        assert material != null;

        float size = Math.min(maxHeight, maxWidth);
        String name = String.format("%s%d", prefix, currentBone);
        Geometry geometry = new Geometry(name, iconMesh);
        visuals.attachChild(geometry);
        float compression = cgm.scorePov.compression();
        geometry.setLocalScale(compression * size, size, 1f);
        geometry.setLocalTranslation(leftX, middleY, zLabels);
        geometry.setMaterial(material);
        geometry.setQueueBucket(RenderQueue.Bucket.Transparent);
    }

    /**
     * Attach 3 plots to visualize bone translations.
     */
    private void attachTranslationPlots() {
        cgm.animation.trackTranslations(currentBone, xs, ys, zs);
        Vector3f user = cgm.pose.getPose().userTranslation(currentBone, null);
        int poseFrame = ts.length;
        xs[poseFrame] = user.x;
        ys[poseFrame] = user.y;
        zs[poseFrame] = user.z;
        MyMath.normalize(xs);
        MyMath.normalize(ys);
        MyMath.normalize(zs);

        attachPlot(ts, xs, ts, xs, "tx", numPlots, xMaterial);
        attachPlot(ts, ys, ts, ys, "ty", numPlots + 1, yMaterial);
        attachPlot(ts, zs, ts, zs, "tz", numPlots + 2, zMaterial);

        float scoreY = scoreY(xs[poseFrame], numPlots);
        poseMesh.add(scoreY, xColor);

        scoreY = scoreY(ys[poseFrame], numPlots + 1);
        poseMesh.add(scoreY, yColor);

        scoreY = scoreY(zs[poseFrame], numPlots + 2);
        poseMesh.add(scoreY, zColor);

        numPlots += 3;
    }

    /**
     * Initialize materials used in score views.
     *
     * @param assetManager (not null)
     */
    private void initializeMaterials(AssetManager assetManager) {
        assert assetManager != null;

        ColorRGBA black = new ColorRGBA(0f, 0f, 0f, 1f);
        ColorRGBA grey = new ColorRGBA(0.5f, 0.5f, 0.5f, 1f);

        bgNotSelected = MyAsset.createUnshadedMaterial(assetManager, grey);
        bgSelected = MyAsset.createUnshadedMaterial(assetManager, black);

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
        poseMaterial.setFloat("PointSize", 2f * pointSize);
        Texture poseShape = MyAsset.loadTexture(assetManager,
                "Textures/shapes/saltire.png");
        poseMaterial.setTexture("PointShape", poseShape);
        RenderState rs = poseMaterial.getAdditionalRenderState();
        rs.setBlendMode(RenderState.BlendMode.Alpha);
        rs.setDepthTest(false);
        rs.setWireframe(true);
    }

    /**
     * Convert a sparkline ordinate value to a world Y coordinate.
     *
     * @param ordinate input sparkline ordinate
     * @param yIndex position in the staff (&ge;0, &lt;10, 0&rarr; top position)
     * @return world Y coordinate
     */
    private float scoreY(float ordinate, int yIndex) {
        float result = -height;
        result -= sparklineHeight * (1f - ordinate);
        result -= yIndex * (float) Finial.hpf;

        return result;
    }
}
