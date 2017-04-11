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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.animation.BoneTrack;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;

/**
 * Utility methods for the Maud application. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Util {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Util.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Util() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f identityScale = new Vector3f(1f, 1f, 1f);

    /**
     * Calculate the bone transform for the specified track and time, using
     * linear interpolation with no blending.
     *
     * @param track (not null)
     * @param time
     * @param result (modified if not null)
     * @return result
     */
    public static Transform boneTransform(BoneTrack track, float time,
            Transform result) {
        if (result == null) {
            result = new Transform();
        }
        float[] times = track.getTimes();
        int lastFrame = times.length - 1;
        assert lastFrame >= 0 : lastFrame;

        Vector3f[] translations = track.getTranslations();
        Quaternion[] rotations = track.getRotations();
        Vector3f[] scales = track.getScales();

        if (time <= 0f || lastFrame == 0) {
            /*
             * Copy the transform of the first frame.
             */
            result.setTranslation(translations[0]);
            result.setRotation(rotations[0]);
            if (scales == null) {
                result.setScale(identityScale);
            } else {
                result.setScale(scales[0]);
            }

        } else if (time >= times[lastFrame]) {
            /*
             * Copy the transform of the last frame.
             */
            result.setTranslation(translations[lastFrame]);
            result.setRotation(rotations[lastFrame]);
            if (scales == null) {
                result.setScale(identityScale);
            } else {
                result.setScale(scales[lastFrame]);
            }

        } else {
            /*
             * Interpolate between two successive frames.
             */
            int startFrame = -1;
            for (int iFrame = 0; iFrame < lastFrame; iFrame++) {
                if (time >= times[iFrame] && time <= times[iFrame + 1]) {
                    startFrame = iFrame;
                    break;
                }
            }
            assert startFrame >= 0 : startFrame;
            int endFrame = startFrame + 1;
            float frameDuration = times[endFrame] - times[startFrame];
            assert frameDuration > 0f : frameDuration;
            float fraction = (time - times[startFrame]) / frameDuration;

            Vector3f startTranslation = translations[startFrame];
            Vector3f endTranslation = translations[endFrame];
            Vector3f translation = result.getTranslation();
            translation.interpolateLocal(startTranslation, endTranslation,
                    fraction);

            Quaternion rotation = result.getRotation();
            rotation.set(rotations[startFrame]);
            Quaternion endRotation = rotations[endFrame];
            rotation.nlerp(endRotation, fraction);

            if (scales == null) {
                result.setScale(identityScale);
            } else {
                Vector3f startScale = scales[startFrame];
                Vector3f endScale = scales[endFrame];
                Vector3f scale = result.getScale();
                scale.interpolateLocal(startScale, endScale, fraction);
            }
        }

        return result;
    }
}
