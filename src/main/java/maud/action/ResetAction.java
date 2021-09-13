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
package maud.action;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import maud.Maud;
import maud.model.History;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedBone;

/**
 * Process actions that start with the word "reset".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ResetAction {
    // *************************************************************************
    // constants and loggers

    /**
     * local copy of {@link com.jme3.math.ColorRGBA#White}
     */
    final private static ColorRGBA white = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ResetAction.class.getName());
    /**
     * local copy of {@link com.jme3.math.Quaternion#IDENTITY}
     */
    final private static Quaternion rotateIdentity = new Quaternion();
    /**
     * negative Y-axis
     */
    final private static Vector3f negativeUnitY = new Vector3f(0f, -1f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f scaleIdentity = new Vector3f(1f, 1f, 1f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#ZERO}
     */
    final private static Vector3f translateIdentity = new Vector3f(0f, 0f, 0f);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private ResetAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "reset".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        EditableCgm target = Maud.getModel().getTarget();
        SelectedBone bone = target.getBone();
        Cgm mouseCgm = Maud.gui.mouseCgm();

        switch (actionString) {
            case Action.resetBoneAngleToAnimation:
                bone.setRotationToAnimation();
                break;

            case Action.resetBoneAngleToBind:
                bone.resetRotation();
                break;

            case Action.resetBoneOffsetToAnimation:
                bone.setTranslationToAnimation();
                break;

            case Action.resetBoneOffsetToBind:
                bone.resetTranslation();
                break;

            case Action.resetBoneScaleToAnimation:
                bone.setScaleToAnimation();
                break;

            case Action.resetBoneScaleToBind:
                bone.resetScale();
                break;

            case Action.resetBoneSelection:
                mouseCgm.getBone().deselect();
                break;

            case Action.resetHistory:
                History.clear();
                break;

            case Action.resetLightColor:
                target.getLight().setColor(white);
                break;

            case Action.resetLightDir:
                target.getLight().setDirection(negativeUnitY);
                break;

            case Action.resetLightPos:
                target.getLight().setPosition(translateIdentity);
                break;

            case Action.resetSpatialRotation:
                target.setSpatialRotation(rotateIdentity);
                break;

            case Action.resetSpatialScale:
                target.setSpatialScale(scaleIdentity);
                break;

            case Action.resetSpatialTranslation:
                target.setSpatialTranslation(translateIdentity);
                break;

            case Action.resetTwist:
                Maud.getModel().getMap().setTwist(rotateIdentity);
                break;

            case Action.resetVertexSelection:
                mouseCgm.getVertex().deselect();
                break;

            default:
                handled = false;
        }

        return handled;
    }
}
