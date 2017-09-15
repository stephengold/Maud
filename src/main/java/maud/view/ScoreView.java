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

import com.jme3.font.BitmapText;
import com.jme3.font.LineWrapMode;
import com.jme3.font.Rectangle;
import com.jme3.input.InputManager;
import com.jme3.material.Material;
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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.Validate;
import jme3utilities.math.MyArray;
import jme3utilities.math.MyMath;
import jme3utilities.mesh.RectangleMesh;
import jme3utilities.wes.Pose;
import maud.Maud;
import maud.mesh.Finial;
import maud.mesh.Sparkline;
import maud.mesh.YSwarm;
import maud.model.Cgm;
import maud.model.ShowBones;
import maud.model.option.ScoreOptions;
import maud.model.option.ViewMode;

/**
 * A 2D visualization of a loaded animation in a score-mode viewport.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScoreView implements EditorView {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ScoreView.class.getName());
    // *************************************************************************
    // fields

    /**
     * CG model being rendered
     */
    private Cgm cgm;
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
     * temporary storage for interpolated bone-track samples
     */
    private float[] its, iws, ixs, iys, izs, nits;
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
     * temporary storage for bone-track data (keyframes plus pose)
     */
    private float[] ts, ws, xs, ys, zs;
    /**
     * index (in the selected skeleton) of the bone currently being visualized
     */
    private int currentBone;
    /**
     * number of interpolated samples per sparkline, or 0 to use keyframes
     */
    private int numSamples = 0;
    /**
     * count of plots added to the current staff (&ge;0)
     */
    private int numPlots = 0;
    /**
     * world X-coordinates of each keyframe in the selected track
     */
    final private Map<Integer, Float> frameXs = new HashMap<>(40);
    /**
     * min/max world Y-coordinates of each bone in the CG model
     */
    final private Map<Integer, Vector2f> boneYs = new HashMap<>(120);
    /**
     * visualization subtree: attach geometries here
     */
    private Node visuals = null;
    /**
     * reusable resources for visualization
     */
    private ScoreResources r = null;
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
    public ScoreView(ViewPort port1, ViewPort port2, ViewPort port3) {
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
     * Consider selecting each axis in this view.
     *
     * @param selection best selection found so far (not null, modified)
     */
    @Override
    public void considerAxes(Selection selection) {
        Validate.nonNull(selection, "selection");
        // TODO
    }

    /**
     * Consider selecting each visualized bone in this view. The selected bone
     * is excluded from consideration.
     *
     * @param selection best selection found so far (not null, modified)
     */
    @Override
    public void considerBones(Selection selection) {
        Validate.nonNull(selection, "selection");

        Camera camera = getCamera();
        int selectedBone = cgm.getBone().getIndex();
        Vector2f inputXY = selection.copyInputXY();
        for (Entry<Integer, Vector2f> entry : boneYs.entrySet()) {
            int boneIndex = entry.getKey();
            if (boneIndex != selectedBone) {
                Vector2f minMax = entry.getValue();

                Vector3f world1 = new Vector3f(r.xRightMargin, minMax.x, r.zLines);
                Vector3f world2 = new Vector3f(r.xRightMargin, minMax.y, r.zLines);
                Vector3f screen1 = camera.getScreenCoordinates(world1);
                Vector3f screen2 = camera.getScreenCoordinates(world2);

                float dSquared;
                if (MyMath.isBetween(screen1.y, inputXY.y, screen2.y)) {
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
     * Consider selecting the gnomon in this view.
     *
     * @param selection best selection found so far
     */
    @Override
    public void considerGnomons(Selection selection) {
        Validate.nonNull(selection, "selection");

        Camera camera = getCamera();
        Vector2f inputXY = selection.copyInputXY();
        float gnomonX = gnomonX();
        Vector3f world = new Vector3f(gnomonX, 0f, r.zLines);
        Vector3f screen = camera.getScreenCoordinates(world);
        float dSquared = FastMath.sqr(inputXY.x - screen.x);
        selection.considerGnomon(cgm, dSquared);
    }

    /**
     * Consider selecting each keyframe in this view.
     *
     * @param selection best selection found so far
     */
    @Override
    public void considerKeyframes(Selection selection) {
        Validate.nonNull(selection, "selection");

        Camera camera = getCamera();
        int selectedBone = cgm.getBone().getIndex();
        Vector2f inputXY = selection.copyInputXY();

        boolean isSelected = cgm.getBone().isSelected();
        if (isSelected) {
            Vector2f minMax = boneYs.get(selectedBone);
            Vector3f world1 = new Vector3f(r.xRightMargin, minMax.x, r.zLines);
            Vector3f world2 = new Vector3f(r.xRightMargin, minMax.y, r.zLines);
            Vector3f screen1 = camera.getScreenCoordinates(world1);
            Vector3f screen2 = camera.getScreenCoordinates(world2);
            if (MyMath.isBetween(screen1.y, inputXY.y, screen2.y)) {
                for (Entry<Integer, Float> entry : frameXs.entrySet()) {
                    float frameX = entry.getValue();
                    Vector3f world = new Vector3f(frameX, minMax.y, r.zLines);
                    Vector3f screen = camera.getScreenCoordinates(world);
                    float dSquared = FastMath.sqr(inputXY.x - screen.x);
                    int frameIndex = entry.getKey();
                    selection.considerKeyframe(cgm, frameIndex, dSquared);
                }
            }
        }
    }

    /**
     * Consider selecting each mesh vertex in this view.
     *
     * @param selection best selection found so far (not null, modified)
     */
    @Override
    public void considerVertices(Selection selection) {
        // no mesh vertices in scene view
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
     * Read what type of view this is.
     *
     * @return Score
     */
    @Override
    public ViewType getType() {
        return ViewType.Score;
    }

    /**
     * Access the view port used to render this view.
     *
     * @return the pre-existing view port
     */
    @Override
    public ViewPort getViewPort() {
        ViewPort result = null;
        ViewMode viewMode = Maud.getModel().getMisc().getViewMode();
        if (viewMode.equals(ViewMode.Hybrid)) {
            result = viewPort3;
        } else if (viewMode.equals(ViewMode.Score)) {
            if (Maud.getModel().getSource().isLoaded()) {
                result = viewPort2;
            } else {
                result = viewPort1;
            }
        }

        return result;
    }

    /**
     * Calculate the range of Y coordinates occupied by the selected bone.
     *
     * @return min/max Y coordinates (in world units), or null if no bone
     * selected
     */
    public Vector2f selectedMinMaxY() {
        Vector2f result = null;
        if (cgm.getBone().isSelected()) {
            int selectedBoneIndex = cgm.getBone().getIndex();
            result = boneYs.get(selectedBoneIndex);
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
    public void update(Cgm renderCgm) {
        Validate.nonNull(renderCgm, "render model");

        if (r == null) {
            r = new ScoreResources();
        }
        boneYs.clear();
        frameXs.clear();
        poseMesh.clear();

        ViewPort viewPort = getViewPort();
        if (viewPort != null && viewPort.isEnabled()) {
            cgm = renderCgm;
            assert renderCgm.isLoaded();

            ScoreOptions options = Maud.getModel().getScore();
            ColorRGBA backgroundColor;
            backgroundColor = options.backgroundColor(null);
            viewPort.setBackgroundColor(backgroundColor);
            /*
             * Configure finials.
             */
            boolean translations = options.showsTranslations();
            boolean rotations = options.showsRotations();
            boolean scales = options.showsScales();
            finialComplete = new Finial(translations, rotations, scales,
                    r.sparklineHeight);
            finialNoScales = new Finial(translations, rotations, false,
                    r.sparklineHeight);

            cgm.getScorePov().updatePartial();
            /*
             * Determine the number of interpolated samples for each sparkline.
             */
            float duration = cgm.getAnimation().getDuration();
            if (duration > 0f) {
                ScoreView view = cgm.getScoreView();
                Camera camera = view.getCamera();
                Vector3f world = new Vector3f(r.xLeftMargin, 0f, r.zLines);
                Vector3f left = camera.getScreenCoordinates(world);
                world.x = r.xRightMargin;
                Vector3f right = camera.getScreenCoordinates(world);
                float dx = right.x - left.x;
                numSamples = 1 + Math.round(dx);
                assert numSamples > 0f : numSamples;
            } else {
                numSamples = 0;
            }

            List<Spatial> roots = viewPort.getScenes();
            int numRoots = roots.size();
            assert numRoots == 1 : numRoots;
            Spatial visualsSpatial = roots.get(0);
            visuals = (Node) visualsSpatial;
            visuals.detachAllChildren();
            height = 0f;

            ShowBones showBones = options.bonesShown(cgm);
            BitSet selectSet;
            switch (showBones) {
                case All:
                case Influencers:
                case Leaves:
                case Mapped:
                case Roots:
                case Selected:
                case Tracked:
                case Unmapped:
                    selectSet = cgm.getSkeleton().listShown(showBones, null);
                    attachBonesIndexOrder(selectSet);
                    break;

                case Ancestry:
                case Family:
                case Subtree:
                    selectSet = cgm.getSkeleton().listShown(showBones, null);
                    attachBonesPreOrder(selectSet);
                    break;

                case None:
                    break;

                default:
                    throw new IllegalStateException();
            }

            attachGnomon();

            cgm.getScorePov().updateCamera();
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
        cgm.getScorePov().setCameraY(newY);
    }
    // *************************************************************************
    // private methods

    /**
     * Attach a right-aligned bone label to the visuals.
     *
     * @param rightX world X coordinate for the right edge of the label
     * @param centerY world Y coordinate for the center of the label
     * @param minWidth minimum width of label (in compressed units, &gt;0)
     * @param maxWidth maximum width of label (in compressed units,
     * &ge;minWidth)
     * @param maxHeight maximum height of label (in world units, &gt;0)
     */
    private void attachBoneLabel(float rightX, float centerY, float minWidth,
            float maxWidth, float maxHeight) {
        assert minWidth > 0f : minWidth;
        assert maxWidth >= minWidth : maxWidth;
        assert maxHeight > 0f : maxHeight;

        Material bgMaterial;
        int selectedBoneIndex = cgm.getBone().getIndex();
        if (currentBone == selectedBoneIndex) {
            bgMaterial = r.bgSelected;
        } else {
            bgMaterial = r.bgNotSelected;
        }

        String boneName = cgm.getSkeleton().getBoneName(currentBone);
        float boneNameSize = 4f + r.labelFont.getLineWidth(boneName);
        /*
         * Calculate the effective width and height for the label and the size
         * for the text assuming horizontal (normal) text.
         */
        float h1 = maxHeight;
        float sizeFactor1 = 0.042f * h1; // relative to preferred size
        float w1 = sizeFactor1 * boneNameSize;
        if (w1 > maxWidth) {
            /*
             * Shrink to avoid clipping.
             */
            h1 *= maxWidth / w1;
            sizeFactor1 *= maxWidth / w1;
            w1 = sizeFactor1 * boneNameSize;
        }
        w1 = FastMath.clamp(w1, minWidth, maxWidth);
        /*
         * Calculate the effective width and height for the label and the size
         * for the text assuming vertical (rotated) text.
         */
        float w2 = maxWidth;
        float sizeFactor2 = 0.042f * w2; // relative to preferred size
        float h2 = sizeFactor2 * boneNameSize;
        if (h2 > maxHeight) {
            /*
             * Shrink to avoid clipping.
             */
            w2 *= maxHeight / h2;
            sizeFactor2 *= maxHeight / h2;
            h2 = sizeFactor2 * boneNameSize;
        }
        h2 = FastMath.clamp(h2, minWidth, maxHeight);
        /*
         * Decide whether to rotate the label.
         */
        if (h2 * w2 > h1 * w1) {
            attachBoneLabelVertical(boneName, sizeFactor2, bgMaterial,
                    rightX, centerY, w2, h2);
        } else {
            attachBoneLabelHorizontal(boneName, sizeFactor1, bgMaterial,
                    rightX, centerY, w1, h1);
        }
    }

    /**
     * Attach a horizontal label to the visuals.
     *
     * @param labelText text of the label (not null)
     * @param sizeFactor text size relative to preferred size (&gt;0)
     * @param bgMaterial (not null)
     * @param rightX world X coordinate for the right edge of the label
     * @param centerY world Y coordinate for the center of the label
     * @param xWidth width of label (in compressed units, &gt;0)
     * @param yHeight height of label (in world units, &gt;0)
     */
    private void attachBoneLabelHorizontal(String labelText, float sizeFactor,
            Material bgMaterial, float rightX, float centerY, float xWidth,
            float yHeight) {
        assert labelText != null;
        assert sizeFactor > 0f : sizeFactor;
        assert bgMaterial != null;
        assert xWidth > 0f : xWidth;
        assert yHeight > 0f : yHeight;

        String nameSuffix = String.format("%d", currentBone);
        Spatial label = makeLabel(labelText, nameSuffix, sizeFactor, bgMaterial,
                xWidth, yHeight);
        visuals.attachChild(label);
        float compression = cgm.getScorePov().compression();
        label.setLocalScale(compression, 1f, 1f);
        float x = rightX - xWidth * compression;
        float y = centerY + yHeight / 2;
        label.setLocalTranslation(x, y, r.zLabels);
    }

    /**
     * Attach a vertical label to the visuals.
     *
     * @param labelText text of the label (not null)
     * @param sizeFactor text size relative to preferred size (&gt;0)
     * @param bgMaterial (not null)
     * @param bottomX world X coordinate for the bottom edge of the label
     * @param centerY world Y coordinate for the center of the label
     * @param xHeight height of label (in compressed units, &gt;0)
     * @param yWidth width of label (in world units, &gt;0)
     */
    private void attachBoneLabelVertical(String labelText, float sizeFactor,
            Material bgMaterial, float bottomX, float centerY, float xHeight,
            float yWidth) {
        assert labelText != null;
        assert sizeFactor > 0f : sizeFactor;
        assert bgMaterial != null;
        assert xHeight > 0f : xHeight;
        assert yWidth > 0f : yWidth;

        String nameSuffix = String.format("%d", currentBone);
        Spatial label = makeLabel(labelText, nameSuffix, sizeFactor, bgMaterial,
                yWidth, xHeight);
        visuals.attachChild(label);
        label.setLocalRotation(r.quarterZ);
        float compression = cgm.getScorePov().compression();
        label.setLocalScale(1f, compression, 1f);
        float x = bottomX - xHeight * compression;
        float y = centerY - yWidth / 2;
        label.setLocalTranslation(x, y, r.zLabels);
    }

    /**
     * Attach staves for the indexed bones in index order.
     *
     * @param selectSet which bones (not null)
     */
    private void attachBonesIndexOrder(BitSet selectSet) {
        int numShown = selectSet.cardinality();
        List<Integer> boneIndices = new ArrayList<>(numShown);
        for (int boneIndex = 0; boneIndex < selectSet.size(); boneIndex++) {
            if (selectSet.get(boneIndex)) {
                boneIndices.add(boneIndex);
            }
        }

        attachStaves(boneIndices);
    }

    /**
     * Attach staves for the indexed bones in depth-first order.
     *
     * @param selectSet which bones (not null)
     */
    private void attachBonesPreOrder(BitSet selectSet) {
        int numShown = selectSet.cardinality();
        List<Integer> boneIndices = new ArrayList<>(numShown);

        Pose pose = cgm.getPose().get();
        int[] order = pose.preOrderIndices();
        for (int boneIndex : order) {
            if (selectSet.get(boneIndex)) {
                boneIndices.add(boneIndex);
            }
        }

        attachStaves(boneIndices);
    }

    /**
     * Attach a pair of finials to indicate an animated bone.
     *
     * @param finial which mesh to use (not null)
     */
    private void attachFinials(Finial finial) {
        assert finial != null;

        Material wireMaterial = r.wireNotSelected;
        int selectedBoneIndex = cgm.getBone().getIndex();
        if (currentBone == selectedBoneIndex) {
            wireMaterial = r.wireSelected;
        }
        /*
         * Attach the left-hand finial.
         */
        String name = String.format("left finial%d", currentBone);
        Geometry geometry = new Geometry(name, finial);
        visuals.attachChild(geometry);
        geometry.setLocalTranslation(r.xLeftMargin, -height, r.zLines);
        geometry.setMaterial(wireMaterial);
        /*
         * Attach a bone label to the left of the left-hand finial.
         */
        float leftX = cgm.getScorePov().leftX() + r.xGap;
        float rightX = r.xLeftMargin - r.hashSize;
        assert leftX < rightX : leftX;
        float staffHeight = finial.getHeight();
        float middleY = -(height + staffHeight / 2);
        float compression = cgm.getScorePov().compression();
        float maxWidth = (rightX - leftX) / compression;
        float minWidth = r.hashSize / compression;
        attachBoneLabel(rightX, middleY, minWidth, maxWidth, staffHeight);
        /*
         * Attach the right-hand finial.
         */
        name = String.format("right finial%d", currentBone);
        geometry = new Geometry(name, finial);
        visuals.attachChild(geometry);
        geometry.setLocalTranslation(r.xRightMargin, -height, r.zLines);
        geometry.setLocalScale(-1f, 1f, 1f);
        geometry.setMaterial(wireMaterial);
        /*
         * Attach transform icons to the right of the right-hand finial.
         */
        leftX = r.xRightMargin + r.hashSize * 2f / 3;
        rightX = cgm.getScorePov().rightX() - r.xGap;
        assert rightX > leftX : rightX;
        maxWidth = (rightX - leftX) / compression;
        middleY = -height - r.sparklineHeight / 2 - (float) Finial.hpf;

        ScoreOptions options = Maud.getModel().getScore();
        boolean translations = options.showsTranslations();
        if (translations) {
            float maxHeight = 2 * (float) Finial.hpf;
            attachTransformIcon(leftX, middleY, maxWidth, maxHeight, "tra",
                    r.traMaterial);
            middleY -= 3 * (float) Finial.hpf;
        }

        boolean rotations = options.showsRotations();
        if (rotations) {
            middleY -= 0.5f * (float) Finial.hpf;
            float maxHeight = 3 * (float) Finial.hpf;
            attachTransformIcon(leftX, middleY, maxWidth, maxHeight, "rot",
                    r.rotMaterial);
            middleY -= 3.5f * (float) Finial.hpf;
        }

        boolean scales = options.showsScales();
        boolean hasScales = cgm.getAnimation().hasScales(currentBone);
        if (scales && hasScales) {
            float maxHeight = 2 * (float) Finial.hpf;
            attachTransformIcon(leftX, middleY, maxWidth, maxHeight, "sca",
                    r.scaMaterial);
        }
    }

    /**
     * Attach the gnomon (time/pose indicator) to the visuals.
     */
    private void attachGnomon() {
        float gnomonX = gnomonX();
        float handleSize = 0.1f * cgm.getScorePov().getHalfHeight(); // world units
        Vector3f start = new Vector3f(gnomonX, handleSize, r.zLines);
        Vector3f end = new Vector3f(gnomonX, -height - handleSize, r.zLines);
        Line line = new Line(start, end);
        Geometry geometry = new Geometry("gnomon", line);
        visuals.attachChild(geometry);
        if (cgm.getPose().isFrozen()) {
            geometry.setMaterial(r.gnomonFrozen);
        } else {
            geometry.setMaterial(r.wireNotSelected);
        }

        boolean isEmpty = poseMesh.isEmpty();
        poseMesh.flip();
        if (!isEmpty) {
            /*
             * Attach a point mesh to represent the current pose.
             */
            geometry = new Geometry("pose points", poseMesh);
            visuals.attachChild(geometry);
            geometry.setLocalTranslation(gnomonX, 0f, r.zLines);
            geometry.setMaterial(r.poseMaterial);
        }
    }

    /**
     * Attach a pair of hash marks to indicate a bone.
     *
     * @param staffHeight (&ge;0)
     */
    private void attachHashes(float staffHeight) {
        float y = -height - staffHeight / 2;

        Material material = r.wireNotSelected;
        int selectedBoneIndex = cgm.getBone().getIndex();
        if (currentBone == selectedBoneIndex) {
            material = r.wireSelected;
        }

        String name = String.format("left hash%d", currentBone);
        Geometry geometry = new Geometry(name, r.hashMark);
        geometry.setLocalTranslation(r.xLeftMargin, y, r.zLines);
        geometry.setMaterial(material);
        visuals.attachChild(geometry);

        name = String.format("right hash%d", currentBone);
        geometry = new Geometry(name, r.hashMark);
        geometry.setLocalTranslation(r.xRightMargin, y, r.zLines);
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
    private void attachPlot(int numPoints, float[] pxx, float[] pyy,
            int numLineVertices, float[] lxx, float[] lyy,
            String suffix, int yIndex, Material material) {
        assert numPoints >= 0 : numPoints;
        assert pxx != null;
        assert pxx.length >= numPoints : pxx.length;
        assert pyy != null;
        assert pyy.length >= numPoints : pyy.length;
        assert numLineVertices >= 1 : numLineVertices;
        assert lxx != null;
        assert lxx.length >= numLineVertices : lxx.length;
        assert lyy != null;
        assert lyy.length >= numLineVertices : lyy.length;
        assert suffix != null;
        assert yIndex >= 0 : yIndex;
        assert yIndex < 10 : yIndex;
        assert material != null;

        assert pxx[0] == 0f : pxx[0];
        assert lxx[0] == 0f : lxx[0];

        if (MyArray.distinct(pyy, pxx.length)) {
            attachSparkline(numPoints, pxx, pyy, Mesh.Mode.Points,
                    suffix + "p", yIndex, material);
            float zoom = cgm.getScorePov().getHalfHeight();
            if (zoom < 10f) {
                /*
                 * Draw connecting lines only when zoomed in.
                 */
                attachSparkline(numLineVertices, lxx, lyy, Mesh.Mode.LineStrip,
                        suffix + "l", yIndex, material);
            }

        } else {
            /*
             * Series consists of a single value: draw the 1st keyframe only.
             */
            tempX[0] = pxx[0];
            tempY[0] = pyy[0];
            attachSparkline(1, tempX, tempY, Mesh.Mode.Points, suffix + "p",
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
        int selectedBoneIndex = cgm.getBone().getIndex();
        if (currentBone == selectedBoneIndex) {
            wireMaterial = r.wireSelected;
        } else {
            wireMaterial = r.wireNotSelected;
        }
        /*
         * Attach the left-hand rectangle: a narrow outline.
         */
        String rectName = String.format("left rect%d", currentBone);
        Geometry geometry = new Geometry(rectName, r.outlineMesh);
        visuals.attachChild(geometry);
        geometry.setLocalScale(-0.2f, staffHeight, 1f);
        geometry.setLocalTranslation(r.xLeftMargin, -height, r.zLines);
        geometry.setMaterial(wireMaterial);
        /*
         * Attach a bone label overlapping the left-hand rectangle.
         */
        float leftX = cgm.getScorePov().leftX() + r.xGap;
        float rightX = -0.2f * r.hashSize;
        float middleY = -(height + staffHeight / 2);
        float compression = cgm.getScorePov().compression();
        float maxWidth = (rightX - leftX) / compression;
        float minWidth = r.hashSize / compression;
        attachBoneLabel(rightX, middleY, minWidth, maxWidth, staffHeight);
        /*
         * Attach the right-hand rectangle: an outline.
         */
        rectName = String.format("right rect%d", currentBone);
        geometry = new Geometry(rectName, r.outlineMesh);
        visuals.attachChild(geometry);
        geometry.setLocalScale(1f, staffHeight, 1f);
        geometry.setLocalTranslation(r.xRightMargin, -height, r.zLines);
        geometry.setMaterial(wireMaterial);
    }

    /**
     * Attach 4 plots to visualize bone rotations.
     */
    private void attachRotationPlots() {
        if (numSamples > 0) {
            cgm.getAnimation().trackInterpolateRotations(numSamples, its,
                    currentBone, iws, ixs, iys, izs);
        }
        cgm.getAnimation().trackRotations(currentBone, ws, xs, ys, zs);
        Pose pose = cgm.getPose().get();
        Quaternion user = pose.userRotation(currentBone, null);
        int poseFrame = ts.length;
        ws[poseFrame] = user.getW();
        xs[poseFrame] = user.getX();
        ys[poseFrame] = user.getY();
        zs[poseFrame] = user.getZ();
        /*
         * Normalize the data.
         */
        normalize(poseFrame + 1, ws, numSamples, iws);
        normalize(poseFrame + 1, xs, numSamples, ixs);
        normalize(poseFrame + 1, ys, numSamples, iys);
        normalize(poseFrame + 1, zs, numSamples, izs);

        if (numSamples > 0) {
            attachPlot(poseFrame, ts, ws, numSamples, nits, iws,
                    "rw", numPlots, r.wMaterial);
            attachPlot(poseFrame, ts, xs, numSamples, nits, ixs,
                    "rx", numPlots + 1, r.xMaterial);
            attachPlot(poseFrame, ts, ys, numSamples, nits, iys,
                    "ry", numPlots + 2, r.yMaterial);
            attachPlot(poseFrame, ts, zs, numSamples, nits, izs,
                    "rz", numPlots + 3, r.zMaterial);
        } else {
            attachPlot(poseFrame, ts, ws, poseFrame, ts, ws,
                    "rw", numPlots, r.wMaterial);
            attachPlot(poseFrame, ts, xs, poseFrame, ts, xs,
                    "rx", numPlots + 1, r.xMaterial);
            attachPlot(poseFrame, ts, ys, poseFrame, ts, ys,
                    "ry", numPlots + 2, r.yMaterial);
            attachPlot(poseFrame, ts, zs, poseFrame, ts, zs,
                    "rz", numPlots + 3, r.zMaterial);
        }

        float scoreY = scoreY(ws[poseFrame], numPlots);
        poseMesh.add(scoreY, r.wColor);

        scoreY = scoreY(xs[poseFrame], numPlots + 1);
        poseMesh.add(scoreY, r.xColor);

        scoreY = scoreY(ys[poseFrame], numPlots + 2);
        poseMesh.add(scoreY, r.yColor);

        scoreY = scoreY(zs[poseFrame], numPlots + 3);
        poseMesh.add(scoreY, r.zColor);

        numPlots += 4;
    }

    /**
     * Attach 3 plots to visualize bone scales.
     */
    private void attachScalePlots() {
        if (numSamples > 0) {
            cgm.getAnimation().trackInterpolateScales(numSamples, its,
                    currentBone, ixs, iys, izs);
        }
        cgm.getAnimation().trackScales(currentBone, xs, ys, zs);
        Vector3f user = cgm.getPose().get().userScale(currentBone, null);
        int poseFrame = ts.length;
        xs[poseFrame] = user.x;
        ys[poseFrame] = user.y;
        zs[poseFrame] = user.z;
        /*
         * Normalize the data.
         */
        normalize(poseFrame + 1, xs, numSamples, ixs);
        normalize(poseFrame + 1, ys, numSamples, iys);
        normalize(poseFrame + 1, zs, numSamples, izs);

        if (numSamples > 0) {
            attachPlot(poseFrame, ts, xs, numSamples, nits, ixs,
                    "sx", numPlots, r.xMaterial);
            attachPlot(poseFrame, ts, ys, numSamples, nits, iys,
                    "sy", numPlots + 1, r.yMaterial);
            attachPlot(poseFrame, ts, zs, numSamples, nits, izs,
                    "sz", numPlots + 2, r.zMaterial);

        } else {
            attachPlot(poseFrame, ts, xs, poseFrame, ts, xs,
                    "sx", numPlots, r.xMaterial);
            attachPlot(poseFrame, ts, ys, poseFrame, ts, ys,
                    "sy", numPlots + 1, r.yMaterial);
            attachPlot(poseFrame, ts, zs, poseFrame, ts, zs,
                    "sz", numPlots + 2, r.zMaterial);
        }

        float scoreY = scoreY(xs[poseFrame], numPlots);
        poseMesh.add(scoreY, r.xColor);

        scoreY = scoreY(ys[poseFrame], numPlots + 1);
        poseMesh.add(scoreY, r.yColor);

        scoreY = scoreY(zs[poseFrame], numPlots + 2);
        poseMesh.add(scoreY, r.zColor);

        numPlots += 3;
    }

    /**
     * Attach a single sparkline to the visualization.
     *
     * @param numVertices number of values to use (&ge;0)
     * @param xx array of X-values for the sparkline (not null, unaffected)
     * @param yy array of Y-values for the sparkline (not null, unaffected)
     * @param mode mesh mode for the sparkline (Mode.LineStrip, or Mode.Points)
     * @param suffix suffix for the geometry name (not null)
     * @param yIndex position in the staff (&ge;0, &lt;10, 0&rarr; top position)
     * @param material material for the geometry (not null)
     */
    private void attachSparkline(int numVertices, float[] xx, float[] yy,
            Mesh.Mode mode, String suffix, int yIndex, Material material) {
        assert numVertices >= 0 : numVertices;
        assert xx != null;
        assert yy != null;
        assert suffix != null;
        assert yIndex >= 0 : yIndex;
        assert yIndex < 10 : yIndex;
        assert material != null;

        Sparkline sparkline = new Sparkline(numVertices, xx, yy,
                r.sparklineHeight, mode);
        String name = String.format("%d%s", currentBone, suffix);
        Geometry geometry = new Geometry(name, sparkline);
        visuals.attachChild(geometry);

        float yOffset = r.sparklineHeight + yIndex * (float) Finial.hpf;
        float y = -height - yOffset;
        geometry.setLocalTranslation(r.xLeftMargin, y, r.zLines);
        geometry.setMaterial(material);
    }

    /**
     * Attach the sparklines for the current bone.
     */
    private void attachSparklines() {
        ts = cgm.getAnimation().trackTimes(currentBone);
        float duration = cgm.getAnimation().getDuration();
        if (duration > 0f) {
            MyArray.normalize(ts, 0f, duration);
        }

        int selectedBone = cgm.getBone().getIndex();
        if (currentBone == selectedBone) {
            /*
             * Record the X-coordinates of all keyframes in the selected track.
             */
            assert frameXs.isEmpty();
            for (int i = 0; i < ts.length; i++) {
                frameXs.put(i, ts[i]);
            }
        }

        int numFrames = ts.length + 1;
        if (ws == null || numFrames > ws.length) {
            /*
             * Allocate larger buffers for keyframe data.
             */
            ws = new float[numFrames];
            xs = new float[numFrames];
            ys = new float[numFrames];
            zs = new float[numFrames];
        }
        if (its == null || numSamples > its.length) {
            /*
             * Allocate larger buffers for interpolated samples.
             */
            its = new float[numSamples];
            iws = new float[numSamples];
            ixs = new float[numSamples];
            iys = new float[numSamples];
            izs = new float[numSamples];
            nits = new float[numSamples];
        }
        /*
         * Calculate sample times.
         */
        if (numSamples > 0) {
            for (int i = 0; i < numSamples; i++) {
                nits[i] = i / (float) (numSamples - 1);
                its[i] = duration * nits[i];
            }
        }

        numPlots = 0;
        ScoreOptions options = Maud.getModel().getScore();
        boolean showTranslations = options.showsTranslations();
        if (showTranslations) {
            attachTranslationPlots();
        }

        boolean showRotations = options.showsRotations();
        if (showRotations) {
            attachRotationPlots();
        }

        boolean showScales = options.showsScales();
        boolean hasScales = cgm.getAnimation().hasScales(currentBone);
        if (showScales && hasScales) {
            attachScalePlots();
        }
    }

    /**
     * Attach a staff to visualize the current bone.
     */
    private void attachStaff() {
        float staffHeight;
        Finial finial;
        boolean trackedBone = cgm.getAnimation().hasTrackForBone(currentBone);
        if (trackedBone) {
            finial = finialNoScales;
            boolean hasScales = cgm.getAnimation().hasScales(currentBone);
            if (hasScales) {
                finial = finialComplete;
            }
            staffHeight = finial.getHeight();
            assert staffHeight > 0f : staffHeight;
        } else {
            finial = null;
            staffHeight = 0f;
        }
        /*
         * Calculate the range of (world) Ys that the staff occupies.
         */
        float newHeight = height + staffHeight;
        float minY = -newHeight;
        float maxY = -height;
        boneYs.put(currentBone, new Vector2f(minY, maxY));
        /*
         * Determine whether the staff is visible.
         */
        float cameraY = cgm.getScorePov().getCameraY();
        float halfHeight = cgm.getScorePov().getHalfHeight();
        assert halfHeight > 0f : halfHeight;
        float bottomY = cameraY - halfHeight;
        float topY = cameraY + halfHeight;
        if (maxY >= bottomY && minY <= topY) {
            /*
             * It's at least partly visible.
             */
            if (trackedBone) {
                attachTrackedStaff(finial);
            } else {
                attachTracklessStaff();
            }
        }

        height = newHeight;
    }

    /**
     * Attach staves for the indexed bones in the order specified. TODO rename
     * attachBoneStaves
     *
     *
     * @param indices list of bone indices (not null)
     */
    private void attachStaves(List<Integer> indices) {
        assert indices != null;

        int numShown = indices.size();
        for (int i = 0; i < numShown; i++) {
            if (i > 0) {
                height += r.yGap;
            }
            currentBone = indices.get(i);
            attachStaff();
        }
    }

    /**
     * Attach a staff to visualize a trackless bone.
     */
    private void attachTracklessStaff() {
        attachHashes(0f);
        float zoom = cgm.getScorePov().getHalfHeight();
        if (zoom < 4f) {
            /*
             * Attach a bone label overlapping the left-hand hash mark.
             */
            float leftX = cgm.getScorePov().leftX() + r.xGap;
            float rightX = -0.2f * r.hashSize;
            float middleY = -height;
            float compression = cgm.getScorePov().compression();
            float maxWidth = (rightX - leftX) / compression;
            float minWidth = r.hashSize / compression;
            attachBoneLabel(rightX, middleY, minWidth, maxWidth, 0.09f);
        }
    }

    /**
     * Attach a staff to visualize a tracked bone.
     *
     * @param finial (not null)
     */
    private void attachTrackedStaff(Finial finial) {
        float zoom = cgm.getScorePov().getHalfHeight();
        if (zoom > 4f) {
            /*
             * zoomed out too far to render detailed finials
             */
            float staffHeight = finial.getHeight();
            if (zoom > 25f) {
                attachHashes(staffHeight);
            } else {
                attachRectangles(staffHeight);
            }
        } else {
            attachFinials(finial);
        }
        attachSparklines();
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
        Geometry geometry = new Geometry(name, r.iconMesh);
        visuals.attachChild(geometry);
        float compression = cgm.getScorePov().compression();
        geometry.setLocalScale(compression * size, size, 1f);
        geometry.setLocalTranslation(leftX, middleY, r.zLabels);
        geometry.setMaterial(material);
        geometry.setQueueBucket(RenderQueue.Bucket.Transparent);
    }

    /**
     * Attach 3 plots to visualize bone translations.
     */
    private void attachTranslationPlots() {
        if (numSamples > 0) {
            cgm.getAnimation().trackInterpolateTranslations(numSamples, its,
                    currentBone, ixs, iys, izs);
        }
        cgm.getAnimation().trackTranslations(currentBone, xs, ys, zs);
        Pose pose = cgm.getPose().get();
        Vector3f user = pose.userTranslation(currentBone, null);
        int poseFrame = ts.length;
        xs[poseFrame] = user.x;
        ys[poseFrame] = user.y;
        zs[poseFrame] = user.z;
        /*
         * Normalize the data.
         */
        normalize(poseFrame + 1, xs, numSamples, ixs);
        normalize(poseFrame + 1, ys, numSamples, iys);
        normalize(poseFrame + 1, zs, numSamples, izs);

        if (numSamples > 0) {
            attachPlot(poseFrame, ts, xs, numSamples, nits, ixs, "tx",
                    numPlots, r.xMaterial);
            attachPlot(poseFrame, ts, ys, numSamples, nits, iys, "ty",
                    numPlots + 1, r.yMaterial);
            attachPlot(poseFrame, ts, zs, numSamples, nits, izs, "tz",
                    numPlots + 2, r.zMaterial);
        } else {
            attachPlot(poseFrame, ts, xs, poseFrame, ts, xs, "tx",
                    numPlots, r.xMaterial);
            attachPlot(poseFrame, ts, ys, poseFrame, ts, ys, "ty",
                    numPlots + 1, r.yMaterial);
            attachPlot(poseFrame, ts, zs, poseFrame, ts, zs, "tz",
                    numPlots + 2, r.zMaterial);
        }

        float scoreY = scoreY(xs[poseFrame], numPlots);
        poseMesh.add(scoreY, r.xColor);

        scoreY = scoreY(ys[poseFrame], numPlots + 1);
        poseMesh.add(scoreY, r.yColor);

        scoreY = scoreY(zs[poseFrame], numPlots + 2);
        poseMesh.add(scoreY, r.zColor);

        numPlots += 3;
    }

    /**
     * Calculate the location of the gnomon (time indicator).
     *
     * @return global X coordinate (&ge;xLeftMargin, &le;xRightMargin)
     */
    private float gnomonX() {
        float result;
        float duration = cgm.getAnimation().getDuration();
        if (duration > 0f) {
            float time = cgm.getAnimation().getTime();
            result = time / duration;
        } else {
            result = 0f;
        }

        assert result >= r.xLeftMargin : result;
        assert result <= r.xRightMargin : result;
        return result;
    }

    /**
     * Normalize keyframe data and sampled data collectively.
     *
     * @param numK number of keyframe data points (&ge;0)
     * @param kData keyframe data (not null, length >= numK)
     * @param numS number of sampled data points (&ge;0)
     * @param sData sampled data (not null, length >= numS)
     */
    private static void normalize(int numK, float[] kData, int numS,
            float[] sData) {
        assert numK >= 0 : numK;
        assert numS >= 0 : numS;

        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < numK; i++) {
            float value = kData[i];
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }

        for (int i = 0; i < numS; i++) {
            float value = sData[i];
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }

        MyArray.normalize(kData, max, min);
        MyArray.normalize(sData, max, min);
    }

    /**
     * Create the bitmap text and background for a label, but don't parent or
     * transform it.
     *
     * @param labelText text of the label (not null)
     * @param sizeFactor text size relative to preferred size (&gt;0)
     * @param bgMaterial (not null)
     * @param width size in the local X direction (in local units, &gt;0)
     * @param height size in the local Y direction (in local units, &gt;0)
     * @return a new orphan spatial with its local origin at its upper left
     * corner
     */
    private Spatial makeLabel(String labelText, String nameSuffix,
            float sizeFactor, Material bgMaterial, float width, float height) {
        assert labelText != null;
        assert sizeFactor > 0f : sizeFactor;
        assert bgMaterial != null;
        assert width > 0f : width;
        assert height > 0f : height;
        /*
         * Create a bitmap text node.
         */
        BitmapText spatial = new BitmapText(r.labelFont);
        spatial.setBox(new Rectangle(0f, 0f, width, height));
        spatial.setLineWrapMode(LineWrapMode.Clip);
        String labelName = "label" + nameSuffix;
        spatial.setName(labelName);
        spatial.setQueueBucket(RenderQueue.Bucket.Transparent);
        float size = sizeFactor * r.labelFont.getPreferredSize();
        spatial.setSize(size);
        spatial.setText(labelText);
        /*
         * Attach a rectangular background geometry to the node.
         */
        String bgName = "bg" + nameSuffix;
        Mesh bgMesh = new RectangleMesh(0f, width, -height, 0f, 1f);
        Geometry bg = new Geometry(bgName, bgMesh);
        spatial.attachChild(bg);
        bg.setLocalTranslation(0f, 0f, -0.01f); // slightly behind the text
        bg.setMaterial(bgMaterial);
        bg.setQueueBucket(RenderQueue.Bucket.Opaque);

        return spatial;
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
        result -= r.sparklineHeight * (1f - ordinate);
        result -= yIndex * (float) Finial.hpf;

        return result;
    }
}
