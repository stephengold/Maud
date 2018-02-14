/*
 Copyright (c) 2017-2018, Stephen Gold
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
package maud.model.cgm;

import com.jme3.animation.AnimControl;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;
import jme3utilities.math.MyArray;
import jme3utilities.wes.Pose;
import jme3utilities.wes.RotationCurve;
import jme3utilities.wes.TweenRotations;
import jme3utilities.wes.TweenTransforms;
import jme3utilities.wes.TweenVectors;
import jme3utilities.wes.VectorCurve;
import maud.Maud;
import maud.view.ScoreResources;
import maud.view.ScoreView;

/**
 * Data for the visualizing a bone/spatial track as a staff in a score view.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class StaffTrack {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(StaffTrack.class.getName());
    // *************************************************************************
    // fields

    /**
     * the C-G model currently being visualized, set by
     * {@link #setCgm(maud.model.Cgm)}
     */
    private static Cgm cgm;
    /**
     * reusable parallel arrays for interpolated samples
     */
    private static float[] its = null, iws, ixs, iys, izs, nits;
    /**
     * array of normalized keyframe times, parallel with ws/xs/ys/zs
     */
    private static float[] nts;
    /**
     * reusable parallel arrays for keyframes and displayed pose
     */
    private static float[] ws = null, xs, ys, zs;
    /**
     * number of interpolated samples per sparkline, or 0 for no interpolation
     */
    private static int numSamples = 0;
    /**
     * text for the track label
     */
    private static String labelText = null;
    /**
     * bone/spatial track currently loaded for visualization
     */
    private static Track track = null;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private StaffTrack() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the normalized track time for the indexed keyframe.
     *
     * @param frameIndex, which keyframe (&ge;0)
     * @return normalized track time (&ge;0, &le;1)
     */
    public static float getFrameT(int frameIndex) {
        float result = nts[frameIndex];

        assert result >= 0f : result;
        assert result <= 1f : result;
        return result;
    }

    /**
     * Test whether the current track has rotation data.
     *
     * @return true if present, otherwise false
     */
    public static boolean hasRotations() {
        Quaternion[] rotations = MyAnimation.getRotations(track);
        if (rotations == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the current track has scale data.
     *
     * @return true if present, otherwise false
     */
    public static boolean hasScales() {
        Vector3f[] scales = MyAnimation.getScales(track);
        if (scales == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the current track has translation data.
     *
     * @return true if present, otherwise false
     */
    public static boolean hasTranslations() {
        Vector3f[] translations = MyAnimation.getTranslations(track);
        if (translations == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Read the text for the track label.
     *
     * @return text (not null)
     */
    public static String labelText() {
        assert labelText != null;
        return labelText;
    }

    /**
     * Load the bone track (from the loaded animation) for the indexed bone.
     *
     * @param boneIndex which bone (&ge;0)
     */
    public static void loadBoneTrack(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        labelText = cgm.getSkeleton().getBoneName(boneIndex);
        track = cgm.getAnimation().findTrackForBone(boneIndex);
        loadTrack();
    }

    /**
     * Load the indexed spatial track from the loaded animation.
     *
     * @param spatialTrackIndex which spatial track (&ge;0)
     */
    public static void loadSpatialTrack(int spatialTrackIndex) {
        Validate.nonNegative(spatialTrackIndex, "spatial track index");

        SpatialTrack spatialTrack
                = cgm.getAnimation().findSpatialTrack(spatialTrackIndex);
        AnimControl animControl = cgm.getAnimControl().find();
        labelText = MyAnimation.describe(spatialTrack, animControl);
        track = spatialTrack;
        loadTrack();
    }

    /**
     * Plot the rotation data for the currently loaded track.
     *
     * @param numPlots number of plots in the staff thus far (&ge;0)
     * @param resources provides the material for each axis (not null,
     * unaffected)
     */
    public static void plotRotations(int numPlots, ScoreResources resources) {
        Validate.nonNegative(numPlots, "number of plots");
        Validate.nonNull(resources, "resources");

        Quaternion[] rotations = MyAnimation.getRotations(track);
        /*
         * copy frame values
         */
        int numFrames = rotations.length;
        for (int i = 0; i < numFrames; i++) {
            ws[i] = rotations[i].getW();
            xs[i] = rotations[i].getX();
            ys[i] = rotations[i].getY();
            zs[i] = rotations[i].getZ();
        }

        if (numSamples > 0) {
            /*
             * interpolate to obtain sample values
             */
            TweenTransforms tt = Maud.getModel().getTweenTransforms();
            TweenRotations technique = tt.getTweenRotations();
            float times[] = track.getKeyFrameTimes();
            float duration = cgm.getAnimation().getDuration();
            RotationCurve parms;
            parms = technique.precompute(times, duration, rotations);
            Quaternion tempQ = new Quaternion();

            for (int iSample = 0; iSample < numSamples; iSample++) {
                float time = its[iSample];
                technique.interpolate(time, parms, tempQ);
                iws[iSample] = tempQ.getW();
                ixs[iSample] = tempQ.getX();
                iys[iSample] = tempQ.getY();
                izs[iSample] = tempQ.getZ();
            }
        }

        int numToNormalize; // number of ws/xs/ys/zs to normalize
        if (track instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) track;
            int targetBoneIndex = boneTrack.getTargetBoneIndex();
            Pose pose = cgm.getPose().get();
            Quaternion user = pose.userRotation(targetBoneIndex, null);
            int poseFrame = numFrames;
            ws[poseFrame] = user.getW();
            xs[poseFrame] = user.getX();
            ys[poseFrame] = user.getY();
            zs[poseFrame] = user.getZ();
            numToNormalize = numFrames + 1;
        } else {
            numToNormalize = nts.length;
        }
        /*
         * Normalize frames, samples, and (if applicable) displayed pose.
         */
        normalize(numToNormalize, ws, numSamples, iws);
        normalize(numToNormalize, xs, numSamples, ixs);
        normalize(numToNormalize, ys, numSamples, iys);
        normalize(numToNormalize, zs, numSamples, izs);

        ScoreView view = cgm.getScoreView();
        if (numSamples > 0) {
            view.attachPlot(numFrames, nts, ws, numSamples, nits, iws, "rw",
                    numPlots, resources.wMaterial);
            view.attachPlot(numFrames, nts, xs, numSamples, nits, ixs, "rx",
                    numPlots + 1, resources.xMaterial);
            view.attachPlot(numFrames, nts, ys, numSamples, nits, iys, "ry",
                    numPlots + 2, resources.yMaterial);
            view.attachPlot(numFrames, nts, zs, numSamples, nits, izs, "rz",
                    numPlots + 3, resources.zMaterial);
        } else {
            view.attachPlot(numFrames, nts, ws, numFrames, nts, ws, "rw",
                    numPlots, resources.wMaterial);
            view.attachPlot(numFrames, nts, xs, numFrames, nts, xs, "rx",
                    numPlots + 1, resources.xMaterial);
            view.attachPlot(numFrames, nts, ys, numFrames, nts, ys, "ry",
                    numPlots + 2, resources.yMaterial);
            view.attachPlot(numFrames, nts, zs, numFrames, nts, zs, "rz",
                    numPlots + 3, resources.zMaterial);
        }

        if (track instanceof BoneTrack) {
            int poseFrame = numFrames;
            view.addPoseRotation(ws[poseFrame], xs[poseFrame], ys[poseFrame],
                    zs[poseFrame]);
        }
    }

    /**
     * Plot the scale data for the currently loaded track.
     *
     * @param numPlots number of plots in the staff thus far (&ge;0)
     * @param resources provides the material for each axis (not null,
     * unaffected)
     */
    public static void plotScales(int numPlots, ScoreResources resources) {
        Validate.nonNegative(numPlots, "number of plots");
        Validate.nonNull(resources, "resources");

        Vector3f[] scales = MyAnimation.getScales(track);
        /*
         * copy frame values
         */
        int numFrames = scales.length;
        for (int i = 0; i < numFrames; i++) {
            xs[i] = scales[i].x;
            ys[i] = scales[i].y;
            zs[i] = scales[i].z;
        }

        if (numSamples > 0) {
            /*
             * interpolate to obtain sample values
             */
            TweenTransforms tt = Maud.getModel().getTweenTransforms();
            TweenVectors technique = tt.getTweenScales();
            float times[] = track.getKeyFrameTimes();
            float duration = cgm.getAnimation().getDuration();
            VectorCurve parms = technique.precompute(times, duration, scales);
            Vector3f tempV = new Vector3f();

            for (int iSample = 0; iSample < numSamples; iSample++) {
                float time = its[iSample];
                technique.interpolate(time, parms, tempV);
                ixs[iSample] = tempV.x;
                iys[iSample] = tempV.y;
                izs[iSample] = tempV.z;
            }
        }

        int numToNormalize; // number of xs/ys/zs to normalize
        if (track instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) track;
            int targetBoneIndex = boneTrack.getTargetBoneIndex();
            Pose pose = cgm.getPose().get();
            Vector3f user = pose.userScale(targetBoneIndex, null);
            int poseFrame = numFrames;
            xs[poseFrame] = user.x;
            ys[poseFrame] = user.y;
            zs[poseFrame] = user.z;
            numToNormalize = numFrames + 1;
        } else {
            numToNormalize = nts.length;
        }
        /*
         * Normalize frames, samples, and (if applicable) displayed pose.
         */
        normalize(numToNormalize, xs, numSamples, ixs);
        normalize(numToNormalize, ys, numSamples, iys);
        normalize(numToNormalize, zs, numSamples, izs);

        ScoreView view = cgm.getScoreView();
        if (numSamples > 0) {
            view.attachPlot(numFrames, nts, xs, numSamples, nits, ixs, "sx",
                    numPlots, resources.xMaterial);
            view.attachPlot(numFrames, nts, ys, numSamples, nits, iys, "sy",
                    numPlots + 1, resources.yMaterial);
            view.attachPlot(numFrames, nts, zs, numSamples, nits, izs, "sz",
                    numPlots + 2, resources.zMaterial);

        } else {
            view.attachPlot(numFrames, nts, xs, numFrames, nts, xs, "sx",
                    numPlots, resources.xMaterial);
            view.attachPlot(numFrames, nts, ys, numFrames, nts, ys, "sy",
                    numPlots + 1, resources.yMaterial);
            view.attachPlot(numFrames, nts, zs, numFrames, nts, zs, "sz",
                    numPlots + 2, resources.zMaterial);
        }

        if (track instanceof BoneTrack) {
            int poseFrame = numFrames;
            view.addPoseVector(xs[poseFrame], ys[poseFrame], zs[poseFrame]);
        }
    }

    /**
     * Plot the translation data for the currently loaded track.
     *
     * @param numPlots number of plots in the staff thus far (&ge;0)
     * @param resources provides the material for each axis (not null,
     * unaffected)
     */
    public static void plotTranslations(int numPlots, ScoreResources resources) {
        Validate.nonNegative(numPlots, "number of plots");
        Validate.nonNull(resources, "resources");

        Vector3f[] translations = MyAnimation.getTranslations(track);
        /*
         * copy frame values
         */
        int numFrames = translations.length;
        for (int i = 0; i < numFrames; i++) {
            xs[i] = translations[i].x;
            ys[i] = translations[i].y;
            zs[i] = translations[i].z;
        }

        if (numSamples > 0) {
            /*
             * interpolate to obtain sample values
             */
            TweenTransforms tt = Maud.getModel().getTweenTransforms();
            TweenVectors technique = tt.getTweenTranslations();
            float times[] = track.getKeyFrameTimes();
            float duration = cgm.getAnimation().getDuration();
            VectorCurve parms
                    = technique.precompute(times, duration, translations);
            Vector3f tempV = new Vector3f();

            for (int iSample = 0; iSample < numSamples; iSample++) {
                float time = its[iSample];
                technique.interpolate(time, parms, tempV);
                ixs[iSample] = tempV.x;
                iys[iSample] = tempV.y;
                izs[iSample] = tempV.z;
            }
        }

        int numToNormalize; // number of xs/ys/zs to normalize
        if (track instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) track;
            int targetBoneIndex = boneTrack.getTargetBoneIndex();
            Pose pose = cgm.getPose().get();
            Vector3f user = pose.userTranslation(targetBoneIndex, null);
            int poseFrame = numFrames;
            xs[poseFrame] = user.x;
            ys[poseFrame] = user.y;
            zs[poseFrame] = user.z;
            numToNormalize = numFrames + 1;
        } else {
            numToNormalize = nts.length;
        }
        /*
         * Normalize frames, samples, and (if applicable) displayed pose.
         */
        normalize(numToNormalize, xs, numSamples, ixs);
        normalize(numToNormalize, ys, numSamples, iys);
        normalize(numToNormalize, zs, numSamples, izs);

        ScoreView view = cgm.getScoreView();
        if (numSamples > 0) {
            view.attachPlot(numFrames, nts, xs, numSamples, nits, ixs, "tx",
                    numPlots, resources.xMaterial);
            view.attachPlot(numFrames, nts, ys, numSamples, nits, iys, "ty",
                    numPlots + 1, resources.yMaterial);
            view.attachPlot(numFrames, nts, zs, numSamples, nits, izs, "tz",
                    numPlots + 2, resources.zMaterial);
        } else {
            view.attachPlot(numFrames, nts, xs, numFrames, nts, xs, "tx",
                    numPlots, resources.xMaterial);
            view.attachPlot(numFrames, nts, ys, numFrames, nts, ys, "ty",
                    numPlots + 1, resources.yMaterial);
            view.attachPlot(numFrames, nts, zs, numFrames, nts, zs, "tz",
                    numPlots + 2, resources.zMaterial);
        }

        if (track instanceof BoneTrack) {
            int poseFrame = numFrames;
            view.addPoseVector(xs[poseFrame], ys[poseFrame], zs[poseFrame]);
        }
    }

    /**
     * Alter which C-G model is being visualized.
     *
     * @param newCgm (not null, alias created)
     */
    public static void setCgm(Cgm newCgm) {
        Validate.nonNull(newCgm, "new model");
        cgm = newCgm;
    }

    /**
     * Alter how many samples to interpolate for each plot.
     *
     * @param newNumSamples how many samples (&ge;0)
     */
    public static void setNumSamples(int newNumSamples) {
        Validate.nonNegative(newNumSamples, "new number of samples");
        numSamples = newNumSamples;
    }

    /**
     * Unload the loaded track and update the label text for the indexed bone,
     * which must not have a track in the loaded animation.
     *
     * @param boneIndex which bone (&ge;0)
     */
    public static void setTracklessBone(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");
        assert !cgm.getAnimation().hasTrackForBone(boneIndex);

        labelText = cgm.getSkeleton().getBoneName(boneIndex);
        track = null;
    }
    // *************************************************************************
    // private methods

    /**
     * Prepare a bone/spatial track for visualization.
     */
    private static void loadTrack() {
        /*
         * Copy keyframe times to nts[].
         */
        float[] times = track.getKeyFrameTimes();
        assert times[0] == 0f : times[0];
        int numFrames = times.length;
        nts = new float[numFrames];
        System.arraycopy(times, 0, nts, 0, numFrames);

        float duration = cgm.getAnimation().getDuration();
        if (duration > 0f) {
            /*
             * Normalize nts[] to range from 0 to 1.
             */
            MyArray.normalize(nts, 0f, duration);
        }
        assert nts[0] == 0f : nts[0];

        if (track instanceof BoneTrack) {
            ++numFrames; // make sure there's room for displayed-pose data
        }

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
                nits[i] = i / (float) (numSamples - 1); // normalized times
                its[i] = duration * nits[i]; // non-normalized times
            }
        }
    }

    /**
     * Normalize keyframe data and interpolated data together, to range from 0
     * to 1.
     *
     * @param numK number of keyframe data points (&ge;0)
     * @param keyframeData keyframe data (not null, length >= numK)
     * @param numI number of interpolated data points (&ge;0)
     * @param interpolatedData interpolated data (not null, length >= numI)
     */
    private static void normalize(int numK, float[] keyframeData, int numI,
            float[] interpolatedData) {
        assert numK >= 0 : numK;
        assert numI >= 0 : numI;

        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < numK; i++) {
            float value = keyframeData[i];
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }

        for (int i = 0; i < numI; i++) {
            float value = interpolatedData[i];
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }

        MyArray.normalize(keyframeData, max, min);
        MyArray.normalize(interpolatedData, max, min);
    }
}
