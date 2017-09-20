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
package maud.action;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.wes.TweenRotations;
import jme3utilities.wes.TweenVectors;
import maud.Maud;
import maud.dialog.EditorDialogs;
import maud.model.EditableCgm;
import maud.model.EditorModel;
import maud.model.ShowBones;

/**
 * Process an action string that begins with "set".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SetAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SetAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SetAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an action string that begin with "set".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        ShowBones currentOption;
        switch (actionString) {
            case Action.setBatchHint:
                Maud.gui.showMenus.setBatchHint();
                break;
            case Action.setCullHint:
                Maud.gui.showMenus.setCullHint();
                break;
            case Action.setQueueBucket:
                Maud.gui.showMenus.setQueueBucket();
                break;
            case Action.setSceneBones:
                currentOption = model.getScene().getSkeleton().getShowBones();
                Maud.gui.showMenus.setShowBones(ActionPrefix.setSceneBones,
                        currentOption);
                break;
            case Action.setScoreBonesNone:
                currentOption = model.getScore().getShowNoneSelected();
                Maud.gui.showMenus.setShowBones(ActionPrefix.setScoreBonesNone,
                        currentOption);
                break;
            case Action.setScoreBonesWhen:
                currentOption = model.getScore().getShowWhenSelected();
                Maud.gui.showMenus.setShowBones(ActionPrefix.setScoreBonesWhen,
                        currentOption);
                break;
            case Action.setShadowMode:
                Maud.gui.showMenus.setShadowMode();
                break;
            case Action.setTrackRotationAll:
                target.getTrack().setRotationAll();
                break;
            case Action.setTrackScaleAll:
                target.getTrack().setScaleAll();
                break;
            case Action.setTrackTranslationAll:
                target.getTrack().setTranslationAll();
                break;
            case Action.setTweenRotations:
                Maud.gui.showMenus.setTweenRotations();
                break;
            case Action.setTweenScales:
                Maud.gui.showMenus.setTweenScales();
                break;
            case Action.setTweenTranslations:
                Maud.gui.showMenus.setTweenTranslations();
                break;
            case Action.setTwistCardinal:
                model.getMap().cardinalizeTwist();
                break;
            case Action.setTwistSnapX:
                model.getMap().snapTwist(PhysicsSpace.AXIS_X);
                break;
            case Action.setTwistSnapY:
                model.getMap().snapTwist(PhysicsSpace.AXIS_Y);
                break;
            case Action.setTwistSnapZ:
                model.getMap().snapTwist(PhysicsSpace.AXIS_Z);
                break;
            case Action.setUserData:
                EditorDialogs.setUserData();
                break;

            default:
                handled = testForPrefixes(actionString);
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an action that starts with "set" -- 2nd part: test for prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean testForPrefixes(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        String arg;
        if (actionString.startsWith(ActionPrefix.setBatchHint)) {
            arg = MyString.remainder(actionString, ActionPrefix.setBatchHint);
            Spatial.BatchHint value = Spatial.BatchHint.valueOf(arg);
            target.setBatchHint(value);

        } else if (actionString.startsWith(ActionPrefix.setCullHint)) {
            arg = MyString.remainder(actionString, ActionPrefix.setCullHint);
            Spatial.CullHint value = Spatial.CullHint.valueOf(arg);
            target.setCullHint(value);

        } else if (actionString.startsWith(
                ActionPrefix.setDurationProportional)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setDurationProportional);
            float value = Float.parseFloat(arg);
            target.getAnimation().setDurationProportional(value);

        } else if (actionString.startsWith(ActionPrefix.setDurationSame)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setDurationSame);
            float value = Float.parseFloat(arg);
            target.getAnimation().setDurationSame(value);

        } else if (actionString.startsWith(ActionPrefix.setQueueBucket)) {
            arg = MyString.remainder(actionString, ActionPrefix.setQueueBucket);
            RenderQueue.Bucket value = RenderQueue.Bucket.valueOf(arg);
            target.setQueueBucket(value);

        } else if (actionString.startsWith(ActionPrefix.setSceneBones)) {
            arg = MyString.remainder(actionString, ActionPrefix.setSceneBones);
            ShowBones value = ShowBones.valueOf(arg);
            model.getScene().getSkeleton().setShowBones(value);

        } else if (actionString.startsWith(ActionPrefix.setScoreBonesNone)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setScoreBonesNone);
            ShowBones value = ShowBones.valueOf(arg);
            model.getScore().setShowNoneSelected(value);

        } else if (actionString.startsWith(ActionPrefix.setScoreBonesWhen)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setScoreBonesWhen);
            ShowBones value = ShowBones.valueOf(arg);
            model.getScore().setShowWhenSelected(value);

        } else if (actionString.startsWith(ActionPrefix.setShadowMode)) {
            arg = MyString.remainder(actionString, ActionPrefix.setShadowMode);
            RenderQueue.ShadowMode value = RenderQueue.ShadowMode.valueOf(arg);
            target.setShadowMode(value);

        } else if (actionString.startsWith(ActionPrefix.setTweenRotations)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setTweenRotations);
            TweenRotations value = TweenRotations.valueOf(arg);
            model.getTweenTransforms().setTweenRotations(value);

        } else if (actionString.startsWith(ActionPrefix.setTweenScales)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setTweenScales);
            TweenVectors value = TweenVectors.valueOf(arg);
            model.getTweenTransforms().setTweenScales(value);

        } else if (actionString.startsWith(ActionPrefix.setTweenTranslations)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setTweenTranslations);
            TweenVectors value = TweenVectors.valueOf(arg);
            model.getTweenTransforms().setTweenTranslations(value);

        } else if (actionString.startsWith(ActionPrefix.setUserData)) {
            arg = MyString.remainder(actionString, ActionPrefix.setUserData);
            target.setUserData(arg);

        } else {
            handled = false;
        }

        return handled;
    }
}
