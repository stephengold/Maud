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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.math.MyVector3f;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.LoadedMap;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedSkeleton;

/**
 * Encapsulate an axis/bone/gnomon/keyframe/vertex selection from the user
 * interface. This data is not checkpointed.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Selection {
    // *************************************************************************
    // enums

    private enum Type {
        None, Bone, Gnomon, Keyframe, PoseTransformAxis, SceneAxis, Vertex;
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

        bestDSquared = dSquaredThreshold;
        inputXY = screenXY.clone();
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
    public void considerAxis(Cgm cgm, int axisIndex,
            boolean scoreView, Vector2f screenXY) {
        Validate.nonNull(cgm, "model");
        if (scoreView) {
            Validate.inRange(axisIndex, "axis index", 0, 9);
        } else {
            Validate.inRange(axisIndex, "axis index", MyVector3f.firstAxis,
                    MyVector3f.lastAxis);
        }

        float dSquared = screenXY.distanceSquared(inputXY);
        if (dSquared < bestDSquared) {
            bestDSquared = dSquared;
            bestGeometry = null;
            bestAxisIndex = axisIndex;
            bestBoneIndex = SelectedSkeleton.noBoneIndex;
            bestFrameIndex = -1;
            bestVertexIndex = -1;
            bestCgm = cgm;
            if (scoreView) {
                bestType = Type.PoseTransformAxis;
            } else {
                bestType = Type.SceneAxis;
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
            bestDSquared = dSquared;
            bestGeometry = null;
            bestAxisIndex = -1;
            bestBoneIndex = boneIndex;
            bestFrameIndex = -1;
            bestVertexIndex = -1;
            bestCgm = cgm;
            bestType = Type.Bone;
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
            bestDSquared = dSquared;
            bestGeometry = null;
            bestAxisIndex = -1;
            bestBoneIndex = SelectedSkeleton.noBoneIndex;
            bestFrameIndex = -1;
            bestVertexIndex = -1;
            bestCgm = cgm;
            bestType = Type.Gnomon;
        }
    }

    /**
     * Consider selecting the indexed keyframe of the currently selected bone
     * track.
     *
     * @param cgm C-G model that contains the bone track (not null)
     * @param frameIndex which keyframe to select (&ge;0)
     * @param dSquared squared distance between the keyframe's screen location
     * and {@link #inputXY} (in pixels squared, &ge;0)
     */
    public void considerKeyframe(Cgm cgm, int frameIndex,
            float dSquared) {
        Validate.nonNull(cgm, "model");
        Validate.nonNegative(frameIndex, "frame index");

        if (dSquared < bestDSquared) {
            bestDSquared = dSquared;
            bestGeometry = null;
            bestAxisIndex = -1;
            bestBoneIndex = cgm.getBone().getIndex();
            bestFrameIndex = frameIndex;
            bestVertexIndex = -1;
            bestCgm = cgm;
            bestType = Type.Keyframe;
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
    public void considerVertex(Cgm cgm, Geometry geometry,
            int vertexIndex, Vector2f screenXY) {
        Validate.nonNull(cgm, "model");
        Validate.nonNull(geometry, "geometry");
        Validate.nonNegative(vertexIndex, "vertex index");

        float dSquared = screenXY.distanceSquared(inputXY);
        if (dSquared < bestDSquared) {
            bestDSquared = dSquared;
            bestGeometry = geometry;
            bestAxisIndex = -1;
            bestBoneIndex = SelectedSkeleton.noBoneIndex;
            bestFrameIndex = -1;
            bestVertexIndex = vertexIndex;
            bestCgm = cgm;
            bestType = Type.Vertex;
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
     */
    public void select() {
        switch (bestType) {
            case None:
                break;
            case Bone:
                selectBone();
                break;
            case Gnomon:
                selectGnomon();
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
            case Vertex:
                selectVertex();
                break;
            default:
                throw new IllegalStateException();
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Select the bone.
     */
    private void selectBone() {
        assert bestBoneIndex >= 0 : bestBoneIndex;

        bestCgm.getBone().select(bestBoneIndex);
        EditorModel model = Maud.getModel();
        if (model.getTarget().getAnimation().isRetargetedPose()) {
            /*
             * Also select the mapped bone (if any).
             */
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
     * Select the gnomon in a score view.
     */
    private void selectGnomon() {
        ScoreDrag.setDraggingGnomon(bestCgm);
    }

    /**
     * Select the keyframe in the selected bone track.
     */
    private void selectKeyframe() {
        assert bestFrameIndex >= 0 : bestFrameIndex;

        float keyframeTime = bestCgm.getTrack().keyframeTime(bestFrameIndex);
        bestCgm.getAnimation().setTime(keyframeTime);
        // TODO drag
    }

    /**
     * Select the transform axis of the selected bone in the the current pose.
     */
    private void selectPoseTransformAxis() {
        // TODO drag transform axis
    }

    /**
     * Select the axis of the scene's axis visualizer.
     */
    private void selectSceneAxis() {
        assert bestCgm != null;
        assert bestAxisIndex >= 0 : bestAxisIndex;
        assert bestAxisIndex < 3 : bestAxisIndex;

        SceneView sceneView = bestCgm.getSceneView();
        AxesVisualizer visualizer = sceneView.getAxesVisualizer();
        Spatial spatial = visualizer.getSpatial();
        Vector3f tipWorld = visualizer.tipLocation(bestAxisIndex);
        assert !MySpatial.isIgnoringTransforms(spatial);
        Vector3f tipLocal = spatial.worldToLocal(tipWorld, null);
        float length = tipLocal.length();

        boolean farSide = sceneView.isAxisReceding(bestAxisIndex);
        SceneDrag.start(bestAxisIndex, length, bestCgm, farSide);
    }

    /**
     * Select the vertex of the loaded C-G model.
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
