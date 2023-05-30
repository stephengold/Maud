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

import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.LoadedMap;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedSkeleton;
import maud.view.scene.SceneDrag;
import maud.view.scene.SceneView;

/**
 * Encapsulate an axis/bone/boundary/gnomon/keyframe/track/vertex selection by
 * the user. Never checkpointed because it's temporary.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Selection {
    // *************************************************************************
    // enums

    private enum Type {
        None, Bone, Boundary, Gnomon, Keyframe,
        PoseTransformAxis, SceneAxis, Track, Vertex
    }
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Selection.class.getName());
    // *************************************************************************
    // fields

    /**
     * squared distance between the selection's screen location and
     * {@link #inputXY} (in pixels squared, &ge;0)
     */
    private float bestDSquared;
    /**
     * the best geometry to be selected, or null for none
     */
    private Geometry bestGeometry = null;
    /**
     * the index of the axis to be selected, or -1 for none
     */
    private int bestAxisIndex = -1;
    /**
     * the index of the bone to be selected, or noBoneIndex for none
     */
    private int bestBoneIndex = SelectedSkeleton.noBoneIndex;
    /**
     * the index of the keyframe to be selected, or -1 for none
     */
    private int bestFrameIndex = -1;
    /**
     * the index of the vertex to be selected, or -1 for none
     */
    private int bestVertexIndex = -1;
    /**
     * the C-G model to be selected, or null for none
     */
    private Cgm bestCgm = null;
    /**
     * the description of the track to be selected, or null for none
     */
    private String bestTrackDesc = null;
    /**
     * type of object selected (not null)
     */
    private Type bestType = Type.None;
    /**
     * screen coordinates used to compare selections
     */
    final private Vector2f inputXY;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an empty selection for the specified screen location and
     * distance threshold.
     *
     * @param screenXY screen coordinates (in pixels, not null, unaffected)
     * @param dSquaredThreshold maximum distance to consider (in pixels squared,
     * &gt;0)
     */
    public Selection(Vector2f screenXY, float dSquaredThreshold) {
        Validate.positive(dSquaredThreshold, "D-squared threshold");

        this.bestDSquared = dSquaredThreshold;
        this.inputXY = screenXY.clone();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Consider selecting the indexed axis in the view.
     *
     * @param cgm C-G model that contains the axis (not null)
     * @param axisIndex which axis to select (&ge;0)
     * @param scoreView true &rarr; axisIndex refers to a transform axis (score
     * view), false &rarr; it refers to a rotational axis (scene view)
     * @param screenXY screen coordinates of the axis (in pixels, not null,
     * unaffected)
     */
    public void considerAxis(Cgm cgm, int axisIndex, boolean scoreView,
            Vector2f screenXY) {
        Validate.nonNull(cgm, "model");
        if (scoreView) {
            Validate.inRange(axisIndex, "axis index", 0, 9);
        } else {
            Validate.inRange(axisIndex, "axis index", MyVector3f.firstAxis,
                    MyVector3f.lastAxis);
        }

        float dSquared = screenXY.distanceSquared(inputXY);
        if (dSquared < bestDSquared) {
            this.bestDSquared = dSquared;
            clear();
            this.bestAxisIndex = axisIndex;
            this.bestCgm = cgm;
            if (scoreView) {
                this.bestType = Type.PoseTransformAxis;
            } else {
                this.bestType = Type.SceneAxis;
            }
        }
    }

    /**
     * Consider selecting the indexed bone.
     *
     * @param cgm C-G model that contains the bone (not null)
     * @param boneIndex which bone to consider (&ge;0)
     * @param dSquared squared distance between the bone's screen location and
     * {@link #inputXY} (in pixels squared, &ge;0)
     */
    public void considerBone(Cgm cgm, int boneIndex, float dSquared) {
        Validate.nonNull(cgm, "model");
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNegative(dSquared, "distance squared");

        if (dSquared < bestDSquared) {
            this.bestDSquared = dSquared;
            clear();
            this.bestBoneIndex = boneIndex;
            this.bestCgm = cgm;
            bestType = Type.Bone;
        }
    }

    /**
     * Consider selecting the view-port boundary.
     *
     * @param dSquared squared distance between the boundary's screen location
     * and {@link #inputXY} (in pixels squared, &ge;0)
     */
    public void considerBoundary(float dSquared) {
        Validate.nonNegative(dSquared, "distance squared");

        if (dSquared < bestDSquared) {
            this.bestDSquared = dSquared;
            clear();
            this.bestType = Type.Boundary;
        }
    }

    /**
     * Consider selecting the gnomon.
     *
     * @param cgm C-G model that contains the gnomon (not null)
     * @param dSquared squared distance between the gnomon's screen location and
     * {@link #inputXY} (in pixels squared, &ge;0)
     */
    public void considerGnomon(Cgm cgm, float dSquared) {
        Validate.nonNull(cgm, "model");
        Validate.nonNegative(dSquared, "distance squared");

        if (dSquared < bestDSquared) {
            this.bestDSquared = dSquared;
            clear();
            this.bestCgm = cgm;
            this.bestType = Type.Gnomon;
        }
    }

    /**
     * Consider selecting the indexed keyframe of the currently selected track.
     *
     * @param cgm C-G model that contains the track (not null)
     * @param frameIndex which keyframe to select (&ge;0)
     * @param dSquared squared distance between the keyframe's screen location
     * and {@link #inputXY} (in pixels squared, &ge;0)
     */
    public void considerKeyframe(Cgm cgm, int frameIndex, float dSquared) {
        Validate.nonNull(cgm, "model");
        Validate.nonNegative(frameIndex, "frame index");

        if (dSquared < bestDSquared) {
            this.bestDSquared = dSquared;
            clear();
            this.bestFrameIndex = frameIndex;
            this.bestCgm = cgm;
            this.bestType = Type.Keyframe;
        }
    }

    /**
     * Consider selecting the specified track.
     *
     * @param cgm C-G model that contains the track (not null)
     * @param description description of the track to select (not null, not
     * empty)
     * @param dSquared squared distance between the track's screen location and
     * {@link #inputXY} (in pixels squared, &ge;0)
     */
    public void considerTrack(Cgm cgm, String description, float dSquared) {
        Validate.nonNull(cgm, "model");
        Validate.nonEmpty(description, "description");

        if (dSquared < bestDSquared) {
            this.bestDSquared = dSquared;
            clear();
            this.bestTrackDesc = description;
            this.bestCgm = cgm;
            this.bestType = Type.Track;
        }
    }

    /**
     * Consider selecting the indexed vertex in the specified geometry in the
     * specified C-G model.
     *
     * @param cgm C-G model that contains the geometry (not null)
     * @param geometry which geometry to select (not null)
     * @param vertexIndex which vertex to select (&ge;0)
     * @param screenXY screen coordinates of the axis (in pixels, not null,
     * unaffected)
     */
    public void considerVertex(Cgm cgm, Geometry geometry, int vertexIndex,
            Vector2f screenXY) {
        Validate.nonNull(cgm, "model");
        Validate.nonNull(geometry, "geometry");
        Validate.nonNegative(vertexIndex, "vertex index");

        float dSquared = screenXY.distanceSquared(inputXY);
        if (dSquared < bestDSquared) {
            this.bestDSquared = dSquared;
            clear();
            this.bestGeometry = geometry;
            this.bestVertexIndex = vertexIndex;
            this.bestCgm = cgm;
            this.bestType = Type.Vertex;
        }
    }

    /**
     * Copy the screen position used to compare selections.
     *
     * @return a new vector
     */
    public Vector2f copyInputXY() {
        Vector2f result = inputXY.clone();
        return result;
    }

    /**
     * Select the best selection found (so far).
     *
     * @return true if successful, false if bestType==None
     */
    public boolean select() {
        boolean success = true;
        switch (bestType) {
            case None:
                success = false;
                break;

            case Bone:
                selectBone();
                break;

            case Boundary:
                Drag.startDraggingBoundary();
                break;

            case Gnomon:
                Drag.startDraggingGnomon(bestCgm);
                break;

            case Keyframe:
                selectKeyframe();
                break;

            case PoseTransformAxis:
                selectPoseTransformAxis();
                break;

            case SceneAxis:
                selectSceneAxis();
                break;

            case Track:
                assert bestTrackDesc != null;
                bestCgm.getTrack().selectWithDescription(bestTrackDesc);
                break;

            case Vertex:
                selectVertex();
                break;

            default:
                throw new IllegalStateException("bestType = " + bestType);
        }

        return success;
    }
    // *************************************************************************
    // private methods

    /**
     * Reset fields to "none" before making a new selection.
     */
    private void clear() {
        this.bestGeometry = null;
        this.bestAxisIndex = -1;
        this.bestBoneIndex = SelectedSkeleton.noBoneIndex;
        this.bestFrameIndex = -1;
        this.bestVertexIndex = -1;
        this.bestCgm = null;
        this.bestTrackDesc = null;
    }

    /**
     * Select a bone.
     */
    private void selectBone() {
        assert bestBoneIndex >= 0 : bestBoneIndex;

        bestCgm.getBone().select(bestBoneIndex);
        EditorModel model = Maud.getModel();
        if (model.getTarget().getAnimation().isRetargetedPose()) {
            // Also select the mapped bone (if any).
            LoadedMap map = model.getMap();
            if (bestCgm == model.getSource()
                    && map.isSourceBoneMapped(bestBoneIndex)) {
                map.selectFromSource();
            } else if (bestCgm == model.getTarget()
                    && map.isTargetBoneMapped(bestBoneIndex)) {
                map.selectFromTarget();
            }
        }
    }

    /**
     * Select a keyframe in the selected bone track.
     */
    private void selectKeyframe() {
        assert bestFrameIndex >= 0 : bestFrameIndex;

        float keyframeTime = bestCgm.getTrack().keyframeTime(bestFrameIndex);
        bestCgm.getPlay().setTime(keyframeTime);
        if (bestFrameIndex > 0) {
            Drag.startDraggingFrame(bestFrameIndex, bestCgm);
        }
    }

    /**
     * Select a transform axis of the selected bone in the current pose.
     */
    private void selectPoseTransformAxis() {
        // TODO drag transform axis
    }

    /**
     * Select an axis of the scene's axis visualizer.
     */
    private void selectSceneAxis() {
        assert bestCgm != null;
        assert bestAxisIndex >= MyVector3f.firstAxis : bestAxisIndex;
        assert bestAxisIndex < MyVector3f.numAxes : bestAxisIndex;

        SceneView sceneView = bestCgm.getSceneView();
        float length = sceneView.axisLength(bestAxisIndex);
        boolean farSide = sceneView.isAxisReceding(bestAxisIndex);
        SceneDrag.start(bestAxisIndex, length, bestCgm, farSide);
    }

    /**
     * Select a vertex of the loaded C-G model.
     */
    private void selectVertex() {
        assert bestCgm != null;
        assert bestGeometry != null;
        assert bestVertexIndex != -1;

        List<Integer> treePosition
                = bestCgm.getSceneView().findPosition(bestGeometry);
        assert treePosition != null;
        bestCgm.getSpatial().select(treePosition);
        bestCgm.getVertex().select(bestVertexIndex);
    }
}
