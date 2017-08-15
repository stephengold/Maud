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

import com.jme3.math.Vector2f;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.model.LoadedCgm;
import maud.model.LoadedMap;
import maud.view.SceneDrag;
import maud.view.ScoreDrag;

/**
 * Encapsulate a bone/keyframe/axis/gnomon selection from the user interface.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Selection {

    // *************************************************************************
    // enums
    private enum Type {
        None, Bone, Gnomon, Keyframe, PoseTransformAxis, SceneAxis;
    }
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Selection.class.getName());
    // *************************************************************************
    // fields

    /**
     * squared distance between the selection's screen location and
     * {@link #inputXY} (in pixels squared, &ge;0)
     */
    private float bestDSquared;
    /**
     * the index of the axis to be selected, or -1 for none
     */
    private int bestAxisIndex = -1;
    /**
     * the index of the bone to be selected, or -1 for none
     */
    private int bestBoneIndex = -1;
    /**
     * the index of the keyframe to be selected, or -1 for none
     */
    private int bestFrameIndex = -1;
    /**
     * the CG model to be selected, or null for none
     */
    private LoadedCgm bestCgm = null;
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
        bestCgm = null;
        inputXY = screenXY.clone();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Consider selecting the indexed bone.
     *
     * @param cgm CG model that contains the bone (not null)
     * @param boneIndex which bone to consider (&ge;0)
     * @param dSquared squared distance between the bone's screen location and
     * {@link #inputXY} (in pixels squared, &ge;0)
     */
    public void considerBone(LoadedCgm cgm, int boneIndex, float dSquared) {
        Validate.nonNull(cgm, "model");
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNegative(dSquared, "distance squared");

        if (dSquared < bestDSquared) {
            bestDSquared = dSquared;
            bestAxisIndex = -1;
            bestBoneIndex = boneIndex;
            bestFrameIndex = -1;
            bestCgm = cgm;
            bestType = Type.Bone;
        }
    }

    /**
     * Consider selecting the gnomon.
     *
     * @param cgm CG model that contains the bone (not null)
     * @param dSquared squared distance between the gnomon's screen location and
     * {@link #inputXY} (in pixels squared, &ge;0)
     */
    public void considerGnomon(LoadedCgm cgm, float dSquared) {
        Validate.nonNull(cgm, "model");
        Validate.nonNegative(dSquared, "distance squared");

        if (dSquared < bestDSquared) {
            bestDSquared = dSquared;
            bestAxisIndex = -1;
            bestBoneIndex = -1;
            bestFrameIndex = -1;
            bestCgm = cgm;
            bestType = Type.Gnomon;
        }
    }

    /**
     * Consider selecting the indexed keyframe of the currently selected bone
     * track.
     *
     * @param cgm CG model that contains the bone track (not null)
     * @param frameIndex which keyframe to select (&ge;0)
     * @param dSquared squared distance between the keyframe's screen location
     * and {@link #inputXY} (in pixels squared, &ge;0)
     */
    public void considerKeyframe(LoadedCgm cgm, int frameIndex,
            float dSquared) {
        Validate.nonNull(cgm, "model");
        Validate.nonNegative(frameIndex, "frame index");

        if (dSquared < bestDSquared) {
            bestDSquared = dSquared;
            bestAxisIndex = -1;
            bestBoneIndex = cgm.bone.getIndex();
            bestFrameIndex = frameIndex;
            bestCgm = cgm;
            bestType = Type.Keyframe;
        }
    }

    /**
     * Consider selecting the indexed axis in the view.
     *
     * @param cgm CG model that contains the bone (not null)
     * @param axisIndex which axis to select (&ge;0)
     * @param scoreView true &rarr; axisIndex refers to a transform axis (score
     * view), false &rarr; it refers to a rotational axis (scene view)
     * @param screenXY screen coordinates of the axis (in pixels, not null,
     * unaffected)
     */
    public void considerAxis(LoadedCgm cgm, int axisIndex,
            boolean scoreView, Vector2f screenXY) {
        Validate.nonNull(cgm, "model");
        if (scoreView) {
            Validate.inRange(axisIndex, "axis index", 0, 9);
        } else {
            Validate.inRange(axisIndex, "axis index", 0, 2);
        }

        float dSquared = screenXY.distanceSquared(inputXY);
        if (dSquared < bestDSquared) {
            bestDSquared = dSquared;
            bestAxisIndex = axisIndex;
            bestBoneIndex = -1;
            bestFrameIndex = -1;
            bestCgm = cgm;
            if (scoreView) {
                bestType = Type.PoseTransformAxis;
            } else {
                bestType = Type.SceneAxis;
            }
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

        bestCgm.bone.select(bestBoneIndex);
        if (Maud.getModel().getTarget().animation.isRetargetedPose()) {
            /*
             * Also select the mapped bone (if any).
             */
            LoadedMap map = Maud.getModel().getMap();
            if (bestCgm == Maud.getModel().getSource()
                    && map.isSourceBoneMapped(bestBoneIndex)) {
                map.selectFromSource();
            } else if (bestCgm == Maud.getModel().getTarget()
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

        float keyframeTime = bestCgm.track.keyframeTime(bestFrameIndex);
        bestCgm.animation.setTime(keyframeTime);
        // TODO drag
    }

    /**
     * Select an axis of the scene's axis visualizer.
     */
    private void selectSceneAxis() {
        assert bestCgm != null;
        assert bestAxisIndex >= 0 : bestAxisIndex;
        assert bestAxisIndex < 3 : bestAxisIndex;

        boolean farSide = Maud.gui.tools.axes.isAxisReceding(bestCgm,
                bestAxisIndex);
        SceneDrag.setDraggingAxis(bestAxisIndex, bestCgm, farSide);
    }

    /**
     * Select the transform axis of the selected bone in the the current pose.
     */
    private void selectPoseTransformAxis() {
        // TODO drag transform axis
    }
}
