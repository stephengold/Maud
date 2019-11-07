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
package maud.view;

import com.jme3.input.InputManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
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
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.LoadedAnimation;
import maud.model.cgm.PlayOptions;
import maud.model.cgm.StaffTrack;
import maud.model.option.MiscOptions;
import maud.model.option.ScoreOptions;
import maud.model.option.ShowBones;
import maud.model.option.ViewMode;

/**
 * An editor view containing a 2-D visualization of a loaded animation. TODO
 * split off ScoreViewCore
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScoreView implements EditorView {
    // *************************************************************************
    // constants and loggers

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
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ScoreView.class.getName());
    /**
     * message to explain why there are no tracks
     */
    final private static String clueMessage = "No animation loaded!";
    // *************************************************************************
    // fields

    /**
     * true when the selected track is being visualized, otherwise false
     */
    private boolean isSelectedTrack;
    /**
     * CG model being visualized
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
     * count plots added to the current staff (&ge;0)
     */
    private int numPlots = 0;
    /**
     * number of interpolated samples per sparkline, or 0 to use keyframes
     */
    private int numSamples = 0;
    /**
     * index of the staff currently being visualized, used to name geometries
     */
    private int staffIndex = 0;
    /**
     * world X-coordinates of each keyframe in the selected bone track
     */
    final private Map<Integer, Float> frameXs = new HashMap<>(40);
    /**
     * min/max world Y-coordinates of each track in the CG model
     */
    final private Map<String, Vector2f> trackYs = new HashMap<>(120);
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
     * Instantiate a new view.
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
     * Add points to the pose mesh to represent a bone rotation in the displayed
     * pose.
     *
     * @param w normalized W component of the rotation
     * @param x normalized X component of the rotation
     * @param y normalized Y component of the rotation
     * @param z normalized Z component of the rotation
     */
    public void addPoseRotation(float w, float x, float y, float z) {
        float scoreY = scoreY(w, numPlots);
        poseMesh.add(scoreY, ScoreResources.wColor);

        scoreY = scoreY(x, numPlots + 1);
        poseMesh.add(scoreY, ScoreResources.xColor);

        scoreY = scoreY(y, numPlots + 2);
        poseMesh.add(scoreY, ScoreResources.yColor);

        scoreY = scoreY(z, numPlots + 3);
        poseMesh.add(scoreY, ScoreResources.zColor);
    }

    /**
     * Add points to the pose mesh to represent a bone scale/translation in the
     * displayed pose.
     *
     * @param x normalized X component of the vector
     * @param y normalized Y component of the vector
     * @param z normalized Z component of the vector
     */
    public void addPoseVector(float x, float y, float z) {
        float scoreY = scoreY(x, numPlots);
        poseMesh.add(scoreY, ScoreResources.xColor);

        scoreY = scoreY(y, numPlots + 1);
        poseMesh.add(scoreY, ScoreResources.yColor);

        scoreY = scoreY(z, numPlots + 2);
        poseMesh.add(scoreY, ScoreResources.zColor);
    }

    /**
     * Attach sparklines to visualize a single data series in the current track.
     *
     * @param numPoints number of points (&ge;0)
     * @param pxx array of normalized X-values for points (not null, unaffected)
     * @param pyy array of normalized Y-values for points (not null, unaffected)
     * @param numLineVertices number of line vertices (&ge;0)
     * @param lxx array of normalized X-values for lines (not null, unaffected)
     * @param lyy array of normalized Y-values for lines (not null, unaffected)
     * @param suffix suffix for the geometry name (not null)
     * @param plotIndex position in the staff (&ge;0, &lt;10, 0&rarr; top
     * position)
     * @param material material for the geometry (not null)
     */
    public void attachPlot(int numPoints, float[] pxx, float[] pyy,
            int numLineVertices, float[] lxx, float[] lyy,
            String suffix, int plotIndex, Material material) {
        Validate.nonNegative(numPoints, "number of points");
        Validate.nonNull(pxx, "point Xs");
        Validate.nonNull(pyy, "point Ys");
        Validate.positive(numLineVertices, "number of line vertices");
        Validate.nonNull(lxx, "line Xs");
        Validate.nonNull(lyy, "line Ys");
        Validate.nonNull(suffix, "suffix");
        Validate.inRange(plotIndex, "plot index", 0, 9);
        Validate.nonNull(material, "material");

        assert pxx.length >= numPoints : pxx.length;
        assert pyy.length >= numPoints : pyy.length;
        assert pxx[0] == 0f : pxx[0];
        assert lxx.length >= numLineVertices : lxx.length;
        assert lyy.length >= numLineVertices : lyy.length;
        assert lxx[0] == 0f : lxx[0];

        if (MyArray.distinct(pyy, pxx.length)) {
            attachSparkline(numPoints, pxx, pyy, Mesh.Mode.Points,
                    suffix + "p", plotIndex, material);

            float zoom = cgm.getScorePov().getHalfHeight();
            if (zoom < 10f) {
                /*
                 * Draw connecting lines only when zoomed in.
                 */
                attachSparkline(numLineVertices, lxx, lyy, Mesh.Mode.LineStrip,
                        suffix + "l", plotIndex, material);
            }

        } else {
            /*
             * Series consists entirely of a single value:
             * draw the first keyframe only.
             */
            tempX[0] = pxx[0];
            tempY[0] = pyy[0];
            attachSparkline(1, tempX, tempY, Mesh.Mode.Points, suffix + "p",
                    plotIndex, material);
        }
    }

    /**
     * Read the height of this score, not including the gnomon.
     *
     * @return height (in world units, &ge;0)
     */
    public float getHeight() {
        assert height >= 0f : height;
        return height;
    }

    /**
     * Calculate the range of Y coordinates occupied by the selected track.
     *
     * @return min/max Y coordinates (in world units), or null if no track is
     * selected
     */
    public Vector2f selectedMinMaxY() {
        String description = cgm.getTrack().describe();
        Vector2f result = trackYs.get(description);

        return result;
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
     * Consider selecting each hone in this view.
     *
     * @param selection best selection found so far (not null, modified)
     */
    @Override
    public void considerBones(Selection selection) {
        // no bones in scene view, only tracks
    }

    /**
     * Consider selecting the boundary of this view.
     *
     * @param selection best selection found so far (not null, modified)
     */
    @Override
    public void considerBoundaries(Selection selection) {
        Validate.nonNull(selection, "selection");

        Camera camera = getCamera();
        if (!MyCamera.isFullWidth(camera)) {
            MiscOptions misc = Maud.getModel().getMisc();
            int width = camera.getWidth(); // in pixels
            float boundaryX = misc.xBoundary() * width;
            Vector2f inputXY = selection.copyInputXY();
            float dSquared = FastMath.sqr(inputXY.x - boundaryX);
            selection.considerBoundary(dSquared);
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
        Vector3f world = new Vector3f(gnomonX, 0f, zLines);
        Vector3f screen = camera.getScreenCoordinates(world);
        float dSquared = FastMath.sqr(inputXY.x - screen.x);
        selection.considerGnomon(cgm, dSquared);
    }

    /**
     * Consider selecting each keyframe in the selected track.
     *
     * @param selection best selection found so far
     */
    @Override
    public void considerKeyframes(Selection selection) {
        Validate.nonNull(selection, "selection");

        boolean isSelected = cgm.getTrack().isSelected();
        if (isSelected) {
            Camera camera = getCamera();
            Vector2f inputXY = selection.copyInputXY();
            String selectedDesc = cgm.getTrack().describe();
            Vector2f minMax = trackYs.get(selectedDesc);
            Vector3f world1 = new Vector3f(xRightMargin, minMax.x, zLines);
            Vector3f world2 = new Vector3f(xRightMargin, minMax.y, zLines);
            Vector3f screen1 = camera.getScreenCoordinates(world1);
            Vector3f screen2 = camera.getScreenCoordinates(world2);
            if (MyMath.isBetween(screen1.y, inputXY.y, screen2.y)) {
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
    }

    /**
     * Consider selecting each visualized track in this view. The currently
     * selected track is excluded from consideration.
     *
     * @param selection best selection found so far (not null, modified)
     */
    @Override
    public void considerTracks(Selection selection) {
        Camera camera = getCamera();
        String selectedDesc = cgm.getTrack().describe();
        Vector2f inputXY = selection.copyInputXY();
        for (Entry<String, Vector2f> entry : trackYs.entrySet()) {
            String trackDesc = entry.getKey();
            if (!selectedDesc.equals(trackDesc)) {
                Vector2f minMax = entry.getValue();

                Vector3f world1 = new Vector3f(-0.15f, minMax.x, zLines);
                Vector3f world2 = new Vector3f(xRightMargin, minMax.y, zLines);
                Vector3f screen1 = camera.getScreenCoordinates(world1);
                Vector3f screen2 = camera.getScreenCoordinates(world2);

                float dSquared = 0f;
                if (!MyMath.isBetween(screen1.y, inputXY.y, screen2.y)) {
                    float dSquared1 = FastMath.sqr(inputXY.y - screen1.y);
                    float dSquared2 = FastMath.sqr(inputXY.y - screen2.y);
                    dSquared += Math.min(dSquared1, dSquared2);
                }
                if (!MyMath.isBetween(screen1.x, inputXY.x, screen2.x)) {
                    float dSquared1 = FastMath.sqr(inputXY.x - screen1.x);
                    float dSquared2 = FastMath.sqr(inputXY.x - screen2.x);
                    dSquared += Math.min(dSquared1, dSquared2);
                }
                selection.considerTrack(cgm, trackDesc, dSquared);
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
        ViewMode viewMode = Maud.getModel().getMisc().viewMode();
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
     * Update this view prior to rendering. Invoked once per frame on each score
     * view.
     *
     * @param viewCgm which C-G model occupies the view (not null)
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(Cgm viewCgm, float tpf) {
        Validate.nonNull(viewCgm, "view model");

        if (r == null) {
            r = new ScoreResources();
        }
        trackYs.clear();
        frameXs.clear();
        poseMesh.clear();

        ViewPort viewPort = getViewPort();
        if (viewPort != null && viewPort.isEnabled()) {
            assert viewCgm.isLoaded();
            cgm = viewCgm;
            StaffTrack.setCgm(viewCgm);

            EditorModel model = Maud.getModel();
            ScoreOptions options = model.getScore();
            ColorRGBA backgroundColor;
            if (cgm == model.getSource()) {
                backgroundColor = options.sourceBackgroundColor(null);
            } else {
                assert cgm == model.getTarget();
                backgroundColor = options.targetBackgroundColor(null);
            }
            viewPort.setBackgroundColor(backgroundColor);
            /*
             * Pre-configure finials for bone tracks.
             */
            boolean translations = options.showsTranslations();
            boolean rotations = options.showsRotations();
            boolean scales = options.showsScales();
            finialComplete = new Finial(translations, rotations, scales,
                    sparklineHeight);
            finialNoScales = new Finial(translations, rotations, false,
                    sparklineHeight);

            cgm.getScorePov().updatePartial();
            /*
             * Determine the number of interpolated samples for each sparkline.
             */
            float duration = cgm.getAnimation().duration();
            if (duration > 0f) {
                ScoreView view = cgm.getScoreView();
                Camera camera = view.getCamera();
                Vector3f world = new Vector3f(xLeftMargin, 0f, zLines);
                Vector3f left = camera.getScreenCoordinates(world);
                world.x = xRightMargin;
                Vector3f right = camera.getScreenCoordinates(world);
                float dx = right.x - left.x;
                numSamples = 1 + Math.round(dx);
                assert numSamples > 0f : numSamples;
            } else {
                numSamples = 0;
            }
            StaffTrack.setNumSamples(numSamples);

            List<Spatial> roots = viewPort.getScenes();
            int numRoots = roots.size();
            assert numRoots == 1 : numRoots;
            Spatial visualsSpatial = roots.get(0);
            visuals = (Node) visualsSpatial;
            visuals.detachAllChildren();
            height = 0f;
            staffIndex = 0;

            attachBones();
            attachSpatialTracks();
            attachGnomon();
            attachLimits();

            cgm.getScorePov().update(tpf);

            boolean isBindPose = cgm.getAnimation().isBindPose();
            if (isBindPose) {
                /*
                 * Explain why there are no tracks.
                 */
                float compression = cgm.getScorePov().compression();
                float xWidth = 1f / compression;
                float lineWidth = 4f + r.labelFont.getLineWidth(clueMessage, 0);
                float sizeFactor = xWidth / lineWidth;
                float preferredSize = r.labelFont.getActualLineHeight();
                float yHeight = 2.4f * xWidth / preferredSize;
                float centerY = cgm.getScorePov().getCameraY();
                float rightX = 1f;
                attachLabelHorizontal(clueMessage, sizeFactor, ColorRGBA.White,
                        r.bgSelected, rightX, centerY, xWidth, yHeight);
            }
        }
    }

    /**
     * Attempt to warp a cursor to the screen coordinates of the mouse pointer.
     */
    @Override
    public void warpCursor() {
        Camera camera = getCamera();
        InputManager inputManager = Maud.getApplication().getInputManager();
        Ray ray = MyCamera.mouseRay(camera, inputManager);

        float newY = ray.origin.y;
        cgm.getScorePov().setCameraY(newY);
    }
    // *************************************************************************
    // private methods

    /**
     * Attach staves for bones.
     */
    private void attachBones() {
        int targetBoneIndex = cgm.getTrack().targetBoneIndex();
        BitSet selectSet;

        ScoreOptions options = Maud.getModel().getScore();
        ShowBones showBones = options.bonesShown(cgm);
        switch (showBones) {
            case All:
            case Influencers:
            case Leaves:
            case Mapped:
            case Roots:
            case Selected:
            case Tracked:
            case Unmapped:
                selectSet = cgm.getSkeleton().listShown(showBones,
                        targetBoneIndex, null);
                attachBonesIndexOrder(selectSet);
                break;

            case Ancestry:
            case Family:
            case Subtree:
                selectSet = cgm.getSkeleton().listShown(showBones,
                        targetBoneIndex, null);
                attachBonesPreOrder(selectSet);
                break;

            case None:
                break;

            default:
                throw new IllegalStateException();
        }
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
        attachBoneStaves(boneIndices);
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
        attachBoneStaves(boneIndices);
    }

    /**
     * Attach a staff to visualize the indexed bone and its track, if any.
     *
     * @param boneIndex which bone (&ge;0)
     */
    private void attachBoneStaff(int boneIndex) {
        assert boneIndex >= 0 : boneIndex;

        float staffHeight;
        Finial finial;
        boolean trackedBone = cgm.getAnimation().hasTrackForBone(boneIndex);
        if (trackedBone) {
            StaffTrack.loadBoneTrack(boneIndex);
            boolean hasScales = StaffTrack.hasScales();
            finial = hasScales ? finialComplete : finialNoScales;
            staffHeight = finial.getHeight();
            if (staffHeight == 0f) {
                finial = null;
                StaffTrack.setNoData();
            }
        } else {
            finial = null;
            staffHeight = 0f;
            StaffTrack.setTracklessBone(boneIndex);
        }
        /*
         * Calculate the range of (world) Ys that the staff occupies.
         */
        float newHeight = height + staffHeight;
        float minY = -newHeight;
        float maxY = -height;
        assert minY <= maxY;
        if (trackedBone) {
            String trackDesc = cgm.getAnimation().describeBoneTrack(boneIndex);
            trackYs.put(trackDesc, new Vector2f(minY, maxY));
        }

        isSelectedTrack = false;
        boolean isVisible = isStaffVisible(minY, maxY);
        if (isVisible) {
            int targetBoneIndex = cgm.getTrack().targetBoneIndex();
            if (boneIndex == targetBoneIndex) {
                isSelectedTrack = true;
            }
            if (finial == null) {
                attachTracklessStaff();
            } else {
                attachTrackedStaff(finial);
            }
            ++staffIndex;
        }
        height = newHeight;
    }

    /**
     * Attach staves for the indexed bones in the order specified.
     *
     * @param indices list of bone indices (not null)
     */
    private void attachBoneStaves(List<Integer> indices) {
        int numShown = indices.size();
        for (int listIndex = 0; listIndex < numShown; listIndex++) {
            if (staffIndex > 0) {
                height += yGap;
            }
            int boneIndex = indices.get(listIndex);
            attachBoneStaff(boneIndex);
        }
    }

    /**
     * Attach a pair of finials to indicate a bone/spatial track.
     *
     * @param finial which mesh to use (not null)
     */
    private void attachFinials(Finial finial) {
        assert finial != null;

        Material wireMaterial
                = isSelectedTrack ? r.wireSelected : r.wireNotSelected;
        /*
         * Attach the left-hand finial.
         */
        String name = String.format("left finial%d", staffIndex);
        Geometry geometry = new Geometry(name, finial);
        visuals.attachChild(geometry);
        geometry.setLocalTranslation(xLeftMargin, -height, zLines);
        geometry.setMaterial(wireMaterial);
        /*
         * Attach a track label to the left of the left-hand finial.
         */
        float leftX = cgm.getScorePov().leftX() + xGap;
        float rightX = xLeftMargin - ScoreResources.hashSize;
        assert leftX < rightX : leftX;
        float staffHeight = finial.getHeight();
        float middleY = -(height + staffHeight / 2);
        float compression = cgm.getScorePov().compression();
        float maxWidth = (rightX - leftX) / compression;
        float minWidth = ScoreResources.hashSize / compression;
        attachLabel(rightX, middleY, minWidth, maxWidth, staffHeight);
        /*
         * Attach the right-hand finial.
         */
        name = String.format("right finial%d", staffIndex);
        geometry = new Geometry(name, finial);
        visuals.attachChild(geometry);
        geometry.setLocalTranslation(xRightMargin, -height, zLines);
        geometry.setLocalScale(-1f, 1f, 1f);
        geometry.setMaterial(wireMaterial);
        /*
         * Attach transform icons to the right of the right-hand finial.
         */
        leftX = xRightMargin + ScoreResources.hashSize * 2f / 3;
        rightX = cgm.getScorePov().rightX() - xGap;
        assert rightX > leftX : rightX;
        maxWidth = (rightX - leftX) / compression;
        middleY = -height - sparklineHeight / 2 - (float) Finial.hpf;

        ScoreOptions options = Maud.getModel().getScore();
        boolean translations = options.showsTranslations();
        boolean hasTranslations = StaffTrack.hasTranslations();
        if (translations && hasTranslations) {
            float maxHeight = 2 * (float) Finial.hpf;
            attachTransformIcon(leftX, middleY, maxWidth, maxHeight, "tra",
                    r.traMaterial);
            middleY -= 3 * (float) Finial.hpf;
        }

        boolean rotations = options.showsRotations();
        boolean hasRotations = StaffTrack.hasRotations();
        if (rotations && hasRotations) {
            middleY -= 0.5f * (float) Finial.hpf;
            float maxHeight = 3 * (float) Finial.hpf;
            attachTransformIcon(leftX, middleY, maxWidth, maxHeight, "rot",
                    r.rotMaterial);
            middleY -= 3.5f * (float) Finial.hpf;
        }

        boolean scales = options.showsScales();
        boolean hasScales = StaffTrack.hasScales();
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
        /*
         * The gnomon is taller than the main score, by 10% of the
         * viewport height.  The excess height at the top and bottom of
         * a gnomon is referred to as its "handles".
         */
        float handleSize = 0.1f * cgm.getScorePov().getHalfHeight(); // world units
        Vector3f start = new Vector3f(gnomonX, handleSize, zLines);
        Vector3f end = new Vector3f(gnomonX, -height - handleSize, zLines);
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
            geometry.setLocalTranslation(gnomonX, 0f, zLines);
            geometry.setMaterial(r.poseMaterial);
        }
    }

    /**
     * Attach a pair of hash marks to indicate a track or bone.
     *
     * @param staffHeight (&ge;0)
     */
    private void attachHashes(float staffHeight) {
        float y = -height - staffHeight / 2;
        Material material
                = isSelectedTrack ? r.wireSelected : r.wireNotSelected;

        String name = String.format("left hash%d", staffIndex);
        Geometry geometry = new Geometry(name, ScoreResources.hashMark);
        geometry.setLocalTranslation(xLeftMargin, y, zLines);
        geometry.setMaterial(material);
        visuals.attachChild(geometry);

        name = String.format("right hash%d", staffIndex);
        geometry = new Geometry(name, ScoreResources.hashMark);
        geometry.setLocalTranslation(xRightMargin, y, zLines);
        geometry.setLocalScale(-1f, 1f, 1f); // grows to the right
        geometry.setMaterial(material);
        visuals.attachChild(geometry);
    }

    /**
     * Attach a right-aligned track label to the visuals.
     *
     * @param rightX world X coordinate for the right edge of the label
     * @param centerY world Y coordinate for the center of the label
     * @param minWidth minimum width of label (in compressed units, &gt;0)
     * @param maxWidth maximum width of label (in compressed units,
     * &ge;minWidth)
     * @param maxHeight maximum height of label (in world units, &gt;0)
     */
    private void attachLabel(float rightX, float centerY, float minWidth,
            float maxWidth, float maxHeight) {
        assert minWidth > 0f : minWidth;
        assert maxWidth >= minWidth : maxWidth;
        assert maxHeight > 0f : maxHeight;

        Material bgMaterial = isSelectedTrack ? r.bgSelected : null;
        ColorRGBA textColor
                = isSelectedTrack ? ColorRGBA.White : ColorRGBA.Black;

        String labelText = StaffTrack.labelText();
        float textSize = 4f + r.labelFont.getLineWidth(labelText, 0);
        float preferredSize = r.labelFont.getActualLineHeight();
        /*
         * Calculate the effective width and height for the label and the size
         * for the text assuming horizontal (normal) text.
         */
        float h1 = maxHeight;
        float sizeFactor1 = h1 / preferredSize; // relative to preferred size
        float w1 = sizeFactor1 * textSize;
        if (w1 > maxWidth) {
            /*
             * Shrink to avoid clipping.
             */
            h1 *= maxWidth / w1;
            sizeFactor1 *= maxWidth / w1;
            w1 = sizeFactor1 * textSize;
        }
        w1 = FastMath.clamp(w1, minWidth, maxWidth);
        /*
         * Calculate the effective width and height for the label and the size
         * for the text assuming vertical (rotated) text.
         */
        float w2 = maxWidth;
        float sizeFactor2 = w2 / preferredSize; // relative to preferred size
        float h2 = sizeFactor2 * textSize;
        if (h2 > maxHeight) {
            /*
             * Shrink to avoid clipping.
             */
            w2 *= maxHeight / h2;
            sizeFactor2 *= maxHeight / h2;
            h2 = sizeFactor2 * textSize;
        }
        h2 = FastMath.clamp(h2, minWidth, maxHeight);
        /*
         * Decide whether to rotate the label.
         */
        if (h2 * w2 > h1 * w1) {
            attachLabelVertical(labelText, sizeFactor2, textColor, bgMaterial,
                    rightX, centerY, w2, h2);
        } else {
            attachLabelHorizontal(labelText, sizeFactor1, textColor, bgMaterial,
                    rightX, centerY, w1, h1);
        }
    }

    /**
     * Attach a horizontal label to the visuals.
     *
     * @param labelText text of the label (not null)
     * @param sizeFactor text size relative to preferred size (&gt;0)
     * @param textColor color for the text (not null, unaffected)
     * @param bgMaterial material for the background, or null for no background
     * @param rightX world X coordinate for the right edge of the label
     * @param centerY world Y coordinate for the center of the label
     * @param xWidth width of label (in compressed units, &gt;0)
     * @param yHeight height of label (in world units, &gt;0)
     */
    private void attachLabelHorizontal(String labelText, float sizeFactor,
            ColorRGBA textColor, Material bgMaterial, float rightX,
            float centerY, float xWidth, float yHeight) {
        assert labelText != null;
        assert sizeFactor > 0f : sizeFactor;
        assert textColor != null;
        assert xWidth > 0f : xWidth;
        assert yHeight > 0f : yHeight;

        String nameSuffix = String.format("%d", staffIndex);
        Spatial label = r.makeLabel(labelText, nameSuffix, sizeFactor,
                textColor, bgMaterial, xWidth, yHeight);
        visuals.attachChild(label);
        float compression = cgm.getScorePov().compression();
        label.setLocalScale(compression, 1f, 1f);
        float x = rightX - xWidth * compression;
        float y = centerY + yHeight / 2;
        label.setLocalTranslation(x, y, zLabels);
    }

    /**
     * Attach a vertical label to the visuals.
     *
     * @param labelText text of the label (not null)
     * @param sizeFactor text size relative to preferred size (&gt;0)
     * @param textColor color for the text (not null, unaffected)
     * @param bgMaterial material for the background, or null for no background
     * @param bottomX world X coordinate for the bottom edge of the label
     * @param centerY world Y coordinate for the center of the label
     * @param xHeight height of label (in compressed units, &gt;0)
     * @param yWidth width of label (in world units, &gt;0)
     */
    private void attachLabelVertical(String labelText, float sizeFactor,
            ColorRGBA textColor, Material bgMaterial, float bottomX,
            float centerY, float xHeight, float yWidth) {
        assert labelText != null;
        assert sizeFactor > 0f : sizeFactor;
        assert textColor != null;
        assert xHeight > 0f : xHeight;
        assert yWidth > 0f : yWidth;

        String nameSuffix = String.format("%d", staffIndex);
        Spatial label = r.makeLabel(labelText, nameSuffix, sizeFactor,
                textColor, bgMaterial, yWidth, xHeight); // swap X and Y
        visuals.attachChild(label);
        label.setLocalRotation(ScoreResources.quarterZ);
        float compression = cgm.getScorePov().compression();
        label.setLocalScale(1f, compression, 1f);
        float x = bottomX - xHeight * compression;
        float y = centerY - yWidth / 2;
        label.setLocalTranslation(x, y, zLabels);
    }

    /**
     * Attach time limits to the visuals.
     */
    private void attachLimits() {
        float lowerX, upperX;
        float duration = cgm.getAnimation().duration();
        if (duration > 0f) {
            PlayOptions options = cgm.getPlay();
            lowerX = options.getLowerLimit() / duration;
            float upperLimit = Math.min(options.getUpperLimit(), duration);
            upperX = upperLimit / duration;
        } else {
            lowerX = xLeftMargin;
            upperX = xLeftMargin;
        }

        RectangleMesh limitsMesh
                = new RectangleMesh(lowerX, upperX, -height, 0f, 1f);
        Geometry limitsGeometry = new Geometry("limits", limitsMesh);
        limitsGeometry.setLocalTranslation(0f, 0f, zLines);
        visuals.attachChild(limitsGeometry);
        limitsGeometry.setMaterial(r.limitsMaterial);
    }

    /**
     * Attach a pair of rectangles to indicate a spatial track or an animated
     * bone.
     *
     * @param staffHeight (in world units, &ge;0)
     */
    private void attachRectangles(float staffHeight) {
        assert staffHeight >= 0f : staffHeight;

        Material wireMaterial
                = isSelectedTrack ? r.wireSelected : r.wireNotSelected;
        /*
         * Attach the left-hand rectangle: a narrow outline.
         */
        String rectName = String.format("left rect%d", staffIndex);
        Geometry geometry = new Geometry(rectName, ScoreResources.outlineMesh);
        visuals.attachChild(geometry);
        geometry.setLocalScale(-0.2f, staffHeight, 1f);
        geometry.setLocalTranslation(xLeftMargin, -height, zLines);
        geometry.setMaterial(wireMaterial);
        /*
         * Attach a track label overlapping the left-hand rectangle.
         */
        float leftX = cgm.getScorePov().leftX() + xGap;
        float rightX = -0.2f * ScoreResources.hashSize;
        float middleY = -(height + staffHeight / 2);
        float compression = cgm.getScorePov().compression();
        float maxWidth = (rightX - leftX) / compression;
        float minWidth = ScoreResources.hashSize / compression;
        attachLabel(rightX, middleY, minWidth, maxWidth, staffHeight);
        /*
         * Attach the right-hand rectangle: an outline.
         */
        rectName = String.format("right rect%d", staffIndex);
        geometry = new Geometry(rectName, ScoreResources.outlineMesh);
        visuals.attachChild(geometry);
        geometry.setLocalScale(1f, staffHeight, 1f);
        geometry.setLocalTranslation(xRightMargin, -height, zLines);
        geometry.setMaterial(wireMaterial);
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
                sparklineHeight, mode);
        String name = String.format("%d%s", staffIndex, suffix);
        Geometry geometry = new Geometry(name, sparkline);
        visuals.attachChild(geometry);

        float yOffset = sparklineHeight + yIndex * (float) Finial.hpf;
        float y = -height - yOffset;
        geometry.setLocalTranslation(xLeftMargin, y, zLines);
        geometry.setMaterial(material);
    }

    /**
     * Attach the sparklines for the current bone/spatial track.
     */
    private void attachSparklines() {
        if (isSelectedTrack) {
            /*
             * Record the X-coordinates of all keyframes in the selected track.
             */
            assert frameXs.isEmpty();
            int numFrames = cgm.getTrack().countKeyframes();
            for (int i = 0; i < numFrames; i++) {
                float x = StaffTrack.getFrameT(i);
                frameXs.put(i, x);
            }
        }

        numPlots = 0;
        ScoreOptions options = Maud.getModel().getScore();

        boolean hasTranslations = StaffTrack.hasTranslations();
        boolean showTranslations = options.showsTranslations();
        if (hasTranslations && showTranslations) {
            StaffTrack.plotTranslations(numPlots, r);
            numPlots += 3;
        }

        boolean hasRotations = StaffTrack.hasRotations();
        boolean showRotations = options.showsRotations();
        if (hasRotations && showRotations) {
            StaffTrack.plotRotations(numPlots, r);
            numPlots += 4;
        }

        boolean hasScales = StaffTrack.hasScales();
        boolean showScales = options.showsScales();
        if (hasScales && showScales) {
            StaffTrack.plotScales(numPlots, r);
            numPlots += 3;
        }
    }

    /**
     * Attach a staff to visualize the indexed spatial track.
     *
     * @param spatialTrackIndex which spatial track (&ge;0)
     */
    private void attachSpatialStaff(int spatialTrackIndex) {
        assert spatialTrackIndex >= 0 : spatialTrackIndex;

        StaffTrack.loadSpatialTrack(spatialTrackIndex);
        boolean hasTranslations = StaffTrack.hasTranslations();
        boolean hasRotations = StaffTrack.hasRotations();
        boolean hasScales = StaffTrack.hasScales();

        ScoreOptions options = Maud.getModel().getScore();
        boolean translations = hasTranslations && options.showsTranslations();
        boolean rotations = hasRotations && options.showsRotations();
        boolean scales = hasScales && options.showsScales();

        float staffHeight;
        Finial finial = new Finial(translations, rotations, scales,
                sparklineHeight);
        staffHeight = finial.getHeight();
        if (staffHeight == 0f) {
            finial = null;
            StaffTrack.setNoData();
        }
        /*
         * Calculate the range of (world) Ys that the staff occupies.
         */
        float newHeight = height + staffHeight;
        float minY = -newHeight;
        float maxY = -height;
        assert minY <= maxY;

        String trackDesc = StaffTrack.labelText();
        trackYs.put(trackDesc, new Vector2f(minY, maxY));

        isSelectedTrack = false;
        boolean isVisible = isStaffVisible(minY, maxY);
        if (isVisible) {
            String desc = cgm.getTrack().describe();
            if (trackDesc.equals(desc)) {
                isSelectedTrack = true;
            }
            if (finial == null) {
                attachTracklessStaff();
            } else {
                attachTrackedStaff(finial);
            }
            ++staffIndex;
        }
        height = newHeight;
    }

    /**
     * Attach staves for spatial tracks.
     */
    private void attachSpatialTracks() {
        LoadedAnimation animation = cgm.getAnimation();
        int numSpatialTracks = animation.countSpatialTracks();
        for (int trackIndex = 0; trackIndex < numSpatialTracks; trackIndex++) {
            if (staffIndex > 0) {
                height += yGap;
            }
            attachSpatialStaff(trackIndex);
        }
    }

    /**
     * Attach a staff to visualize a bone/spatial track.
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
     * Attach a staff with neither finials nor tracks.
     */
    private void attachTracklessStaff() {
        float zoom = cgm.getScorePov().getHalfHeight();
        if (zoom < 4f) {
            /*
             * Attach a track label on the left side.
             */
            float leftX = cgm.getScorePov().leftX() + xGap;
            float rightX = -0.2f * ScoreResources.hashSize;
            float middleY = -height;
            float compression = cgm.getScorePov().compression();
            float maxWidth = (rightX - leftX) / compression;
            float minWidth = ScoreResources.hashSize / compression;
            attachLabel(rightX, middleY, minWidth, maxWidth, 0.09f);
        } else {
            attachHashes(0f);
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
        String name = String.format("%s%d", prefix, staffIndex);
        Geometry geometry = new Geometry(name, ScoreResources.iconMesh);
        visuals.attachChild(geometry);
        float compression = cgm.getScorePov().compression();
        geometry.setLocalScale(compression * size, size, 1f);
        geometry.setLocalTranslation(leftX, middleY, zLabels);
        geometry.setMaterial(material);
        geometry.setQueueBucket(RenderQueue.Bucket.Transparent);
    }

    /**
     * Calculate the location of the gnomon (time indicator).
     *
     * @return global X coordinate (&ge;xLeftMargin, &le;xRightMargin)
     */
    private float gnomonX() {
        float result;
        float duration = cgm.getAnimation().duration();
        if (duration > 0f) {
            float time = cgm.getPlay().getTime();
            result = time / duration;
        } else {
            result = 0f;
        }

        assert result >= xLeftMargin : result;
        assert result <= xRightMargin : result;
        return result;
    }

    /**
     * Test visibility of a range of Y values.
     *
     * @param minY bottom of range (world Y coordinate)
     * @param maxY top of range (world Y coordinate)
     * @return true if visible, otherwise false
     */
    private boolean isStaffVisible(float minY, float maxY) {
        float cameraY = cgm.getScorePov().getCameraY();
        float halfHeight = cgm.getScorePov().getHalfHeight();
        assert halfHeight > 0f : halfHeight;
        float bottomY = cameraY - halfHeight;
        float topY = cameraY + halfHeight;
        if (maxY >= bottomY && minY <= topY) {
            return true;
        } else {
            return false;
        }
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
