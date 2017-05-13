package com.jme3.scene.plugins.bvh;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.Track;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility class for re-targeting animations between CG models.
 *
 * TODO constructor
 *
 * @author Nehon
 */
public class BVHUtils {

    private static class InnerTrack {

        Vector3f[] positions;
        Quaternion[] rotations;
        Vector3f[] scales;

        public InnerTrack(int length) {
            positions = new Vector3f[length];
            rotations = new Quaternion[length];
            scales = new Vector3f[length];
        }
    }
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BVHUtils.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    private static final Vector3f DEFAULT_SCALE = new Vector3f(Vector3f.UNIT_XYZ);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private BVHUtils() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Find the first bone track in the specified animation.
     *
     * @param animation which animation to search (not null)
     * @return the pre-existing instance, or null if none found
     */
    public static BoneTrack getFirstBoneTrack(Animation animation) {
        for (Track track : animation.getTracks()) {
            if (track instanceof BoneTrack) {
                return (BoneTrack) track;
            }
        }
        return null;
    }

    /**
     * Estimate the height of a skeleton in bind pose.
     *
     * @param targetSkeleton
     * @returh height (in model units, &ge;0)
     */
    public static float getSkeletonHeight(Skeleton targetSkeleton) {
        float maxy = -100000;
        float miny = +100000;
        targetSkeleton.reset();
        targetSkeleton.updateWorldVectors();

        for (int i = 0; i < targetSkeleton.getBoneCount(); i++) {
            Bone bone = targetSkeleton.getBone(i);
            if (bone.getModelSpacePosition().y > maxy) {
                maxy = bone.getModelSpacePosition().y;
            }
            if (bone.getModelSpacePosition().y < miny) {
                miny = bone.getModelSpacePosition().y;
                //System.out.println(bone.getName() + " " + miny);
            }
        }
        //System.out.println(maxy - miny);
        return maxy - miny;
    }

    /**
     * Retarget an animation from one model to a different model.
     *
     * @param sourceModel CG model that contains the source animation (not null)
     * @param targetModel CG model where the animation will be added (not null)
     * @param sourceAnimation which animation to re-target (not null)
     * @param sourceSkeleton the skeleton of the source model (not null)
     * @param boneMapping mapping of bones from source model to target (not
     * null)
     * @param skipFirstKey true &rarr; skip first keyframe, false &rarr; use all
     * keyframes
     * @param targetName name for the resulting animation
     * @return a new instance
     */
    public static Animation reTarget(Spatial sourceModel, Spatial targetModel,
            Animation sourceAnimation, Skeleton sourceSkeleton,
            SkeletonMapping boneMapping, boolean skipFirstKey,
            String targetName) {
        BoneTrack track = getFirstBoneTrack(sourceAnimation);
        if (track == null) {
            throw new IllegalArgumentException(
                    "Animation must contain a boneTrack to be retargeted");
        }
        float timePerFrame = track.getTimes().length / sourceAnimation.getLength();
        return reTarget(sourceModel, targetModel, sourceAnimation,
                sourceSkeleton, timePerFrame, boneMapping, skipFirstKey,
                targetName);
    }

    /**
     * Retarget an animation from one model to a different model.
     *
     * @param sourceModel CG model that contains the source animation (not null)
     * @param targetModel CG model where the animation will be added (not null)
     * @param sourceAnimation which animation to re-target (not null)
     * @param sourceSkeleton the skeleton of the source model (not null)
     * @param timePerFrame (in seconds, &gt;0)
     * @param boneMapping mapping of bones from source model to target (not
     * null)
     * @param skipFirstKey true &rarr; skip first keyframe, false &rarr; use all
     * keyframes
     * @param targetName name for the resulting animation
     * @return a new instance
     */
    public static Animation reTarget(Spatial sourceModel, Spatial targetModel,
            Animation sourceAnimation, Skeleton sourceSkeleton,
            float timePerFrame, SkeletonMapping boneMapping,
            boolean skipFirstKey, String targetName) {
        Skeleton targetSkeleton = targetModel.getControl(
                AnimControl.class).getSkeleton();

        int start = skipFirstKey ? 1 : 0;

//        Animation sourceAnimation = sourceData.getAnimation();
//        Skeleton sourceSkeleton = sourceData.getSkeleton();
        Animation resultAniamtion = new Animation(targetName,
                sourceAnimation.getLength() - start * timePerFrame);
        targetSkeleton.updateWorldVectors();
        float targetHeight = ((BoundingBox) targetModel.getWorldBound()).getYExtent()
                / targetModel.getWorldScale().y;
        float sourceHeight = ((BoundingBox) sourceModel.getWorldBound()).getYExtent()
                / sourceModel.getWorldScale().y;
        float targetWidth = ((BoundingBox) targetModel.getWorldBound()).getXExtent()
                / targetModel.getWorldScale().x;
        float sourceWidth = ((BoundingBox) sourceModel.getWorldBound()).getXExtent()
                / sourceModel.getWorldScale().x;
        float targetDepth = ((BoundingBox) targetModel.getWorldBound()).getZExtent()
                / targetModel.getWorldScale().z;
        float sourceDepth = ((BoundingBox) sourceModel.getWorldBound()).getZExtent()
                / sourceModel.getWorldScale().z;
        Vector3f ratio = new Vector3f(targetHeight / sourceHeight,
                targetWidth / sourceWidth, targetDepth / sourceDepth);
        ratio = Vector3f.UNIT_XYZ;
        System.out.println(ratio);

        Vector3f rootPos = new Vector3f();
        Quaternion rootRot = new Quaternion();
        targetSkeleton.reset();

        BoneTrack track = getFirstBoneTrack(sourceAnimation);
        if (track == null) {
            throw new IllegalArgumentException(
                    "Animation must contain a boneTrack to be retargeted");
        }
        int nbFrames = track.getTimes().length;
        Map<Integer, InnerTrack> tracks = new HashMap<>();

        float[] times = new float[nbFrames];
        System.arraycopy(track.getTimes(), 0, times, 0, nbFrames);
        //for each frame
        for (int frameId = 0; frameId < nbFrames; frameId++) {
            //applying animation for the frame to source skeleton so that model transforms are computed
            for (int i = 0; i < sourceAnimation.getTracks().length; i++) {
                Track t = sourceAnimation.getTracks()[i];
                if (t instanceof BoneTrack) {
                    BoneTrack boneTrack = (BoneTrack) t;
                    Bone sourceBone = sourceSkeleton.getBone(
                            boneTrack.getTargetBoneIndex());
                    sourceBone.setUserControl(true);
                    // some anims doesn't have scale so using the default scale
                    Vector3f scale = DEFAULT_SCALE;
                    if (boneTrack.getScales() != null) {
                        scale = boneTrack.getScales()[frameId];
                    }
                    sourceBone.setUserTransforms(
                            boneTrack.getTranslations()[frameId],
                            boneTrack.getRotations()[frameId], scale);
                    sourceBone.setUserControl(false);
                }
            }
            sourceSkeleton.updateWorldVectors();

            for (Bone bone : targetSkeleton.getRoots()) {
                computeTransforms(bone, sourceSkeleton, targetSkeleton,
                        boneMapping, frameId, tracks, nbFrames, ratio,
                        sourceAnimation);
            }
        }
        sourceSkeleton.reset();
        BoneTrack[] boneTracks = new BoneTrack[tracks.size()];

        int i = 0;
        for (int boneIndex : tracks.keySet()) {
            InnerTrack it = tracks.get(boneIndex);
            BoneTrack bt = new BoneTrack(boneIndex, times.clone(), it.positions,
                    it.rotations, it.scales);
            boneTracks[i] = bt;
            i++;
        }

        resultAniamtion.setTracks(boneTracks);

        return resultAniamtion;
    }
    // *************************************************************************
    // private methods

    /**
     * Apply the model-space transform for the indexed keyframe to each target
     * bone. The source skeleton must be updated to that keyframe.
     *
     * @param targetBone (not null)
     * @param sourceSkeleton the skeleton of the source model (not null)
     * @param targetSkeleton
     * @param boneMapping mapping of bones from source model to target (not
     * null)
     * @param frameId which keyframe to re-target (&ge;0)
     * @param tracks
     * @param animLength (in seconds, &ge;0)
     * @param ratio
     * @param anim
     */
    //this method recursively computes the transforms for each bone for a given
    //frame, from a given sourceSkeleton (with bones updated to that frame)
    //
    //the Bind transforms are the transforms of the bone when it's in the rest
    //pose (aka T pose). Wrongly called worldBindRotation in Bone implementation
    //those transforms are expressed in model space
    //
    //the Model space transforms are the transforms of the bone in model space
    //once the frame transforms has been applied
    private static void computeTransforms(Bone targetBone,
            Skeleton sourceSkeleton, Skeleton targetSkeleton,
            SkeletonMapping boneMapping, int frameId,
            Map<Integer, InnerTrack> tracks, int animLength, Vector3f ratio,
            Animation anim) {

        BoneMapping mapping = boneMapping.get(targetBone.getName());
        Bone sourceBone = null;
        if (mapping != null) {
            sourceBone = sourceSkeleton.getBone(
                    mapping.getSourceNames().get(0));
        }

        Quaternion rootRot = new Quaternion();
        targetSkeleton.updateWorldVectors();
        //we want the target bone to have the same model transforms as the source Bone (scaled to the correct ratio as models may not have the same scale)
        //the ratio only affects position
        if (sourceBone != null) {
            if (sourceBone.getParent() == null) {
                //case of a root bone, just combine the source model transforms with the inverse target bind transforms
                InnerTrack t = getInnerTrack(
                        targetSkeleton.getBoneIndex(targetBone), tracks,
                        animLength);

                //scaling the modelPosition
                Vector3f scaledPos = sourceBone.getModelSpacePosition().mult(ratio);
                //subtract target's bind position to the source's scaled model position
                t.positions[frameId] = new Vector3f();//scaledPos.subtractLocal(targetBone.getBindPosition());
                // t.positions[frameId] = new Vector3f();
                //multiplying the source's model rotation witht the target's inverse bind rotation
                TempVars vars = TempVars.get();
                Quaternion q = vars.quat1.set(
                        targetBone.getBindRotation()).inverseLocal();
                t.rotations[frameId] = q.mult(
                        sourceBone.getModelSpaceRotation());
                rootRot.set(q);

                vars.release();

                //dividing by the target's bind scale
                t.scales[frameId] = sourceBone.getModelSpaceScale().divide(
                        targetBone.getBindScale());
                targetBone.setUserControl(true);
                targetBone.setUserTransforms(t.positions[frameId],
                        t.rotations[frameId], t.scales[frameId]);
                targetBone.updateModelTransforms();
                targetBone.setUserControl(false);
            } else {
                //general case
                //Combine source model transforms with target's parent inverse model transform and inverse target's bind transforms

                Bone parentBone = targetBone.getParent();
                InnerTrack t = getInnerTrack(
                        targetSkeleton.getBoneIndex(targetBone), tracks,
                        animLength);

                BoneTrack boneTrack = findBoneTrack(
                        sourceSkeleton.getBoneIndex(sourceBone), anim);
                if (boneTrack != null) {
                    Vector3f animPosition = boneTrack.getTranslations()[frameId];
                    t.positions[frameId] = animPosition.clone();
                } else {
                    t.positions[frameId] = new Vector3f();
                }
                t.positions[frameId] = new Vector3f();

                TempVars vars = TempVars.get();
//                // computing target's parent's inverse model rotation
                Quaternion inverseTargetParentModelRot = vars.quat1;
                inverseTargetParentModelRot.set(
                        parentBone.getModelSpaceRotation()).inverseLocal().normalizeLocal();
//
//                //ANIMATION POSITION
//                //first we aplly the ratio
//                Vector3f scaledPos = sourceBone.getModelSpacePosition().mult(ratio);
//                //Retrieving target's local pos then subtracting the target's bind pos
//                t.positions[frameId] = inverseTargetParentModelRot.mult(scaledPos)
//                        .multLocal(parentBone.getModelSpaceScale())
//                        .subtract(parentBone.getModelSpacePosition());
//                //made in 2 steps for the sake of readability
//                //here t.positions[frameId] is containing the target's local position (frame position regarding the parent bone).
//                //now we're subtracting target's bind position
//                t.positions[frameId].subtractLocal(targetBone.getBindPosition());
                // now t.positions[frameId] is what we are looking for.

                //ANIMATION ROTATION
                //Computing target's local rotation by multiplying source's model
                //rotation with target's parent's inverse model rotation and multiplying
                //with the target's inverse world bind rotation.
                //
                //The twist quaternion is here to fix the twist on Y axis some
                //bones may have after the rotation in model space has been computed
                //For now the problem is worked around by some constant twist
                //rotation that you set in the bone mapping.
                //This is probably a predictable behavior that could be detected
                //and automatically corrected, but as for now I don't have a clue where it comes from.
                //Don't use inverseTargetParentModelRot as is after this point as the
                //following line compromizes its value. multlocal is used instead of mult for obvious optimization reason.
                Quaternion targetLocalRot = inverseTargetParentModelRot.multLocal(
                        sourceBone.getModelSpaceRotation()).normalizeLocal();
                Quaternion targetInverseBindRotation = vars.quat2.set(
                        targetBone.getBindRotation()).inverseLocal().normalizeLocal();
                Quaternion twist = boneMapping.get(
                        targetBone.getName()).getTwist();
                //finally computing the animation rotation for the current frame. Note that the first "mult" instanciate a new Quaternion.
                t.rotations[frameId] = targetInverseBindRotation.mult(
                        targetLocalRot).multLocal(twist).normalizeLocal(); //

                //releasing tempVars
                vars.release();

                //ANIMATION SCALE
                // dividing by the target's parent's model scale then dividing by the target's bind scale
                t.scales[frameId] = sourceBone.getModelSpaceScale().divide(
                        parentBone.getModelSpaceScale()).divideLocal(
                        targetBone.getBindScale());

                //Applying the computed transforms for the current frame to the bone and updating its model transforms
                targetBone.setUserControl(true);
                targetBone.setUserTransforms(t.positions[frameId],
                        t.rotations[frameId], t.scales[frameId]);
                targetBone.updateModelTransforms();
                targetBone.setUserControl(false);
            }
        }

        //recurse through children bones
        for (Bone childBone : targetBone.getChildren()) {
            computeTransforms(childBone, sourceSkeleton, targetSkeleton,
                    boneMapping, frameId, tracks, animLength, ratio, anim);
        }
    }

    private static BoneTrack findBoneTrack(int index, Animation anim) {
        for (int i = 0; i < anim.getTracks().length; i++) {
            Track t = anim.getTracks()[i];
            if (t instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) t;
                if (boneTrack.getTargetBoneIndex() == index) {
                    return boneTrack;
                }
            }
        }
        return null;
    }

    private static InnerTrack getInnerTrack(int index,
            Map<Integer, InnerTrack> tracks, int length) {
        InnerTrack t = tracks.get(index);
        if (t == null) {
            t = new InnerTrack(length);
            tracks.put(index, t);
        }
        return t;
    }
}
