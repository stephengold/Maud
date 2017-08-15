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

import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.TweenRotations;
import maud.TweenVectors;

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
    final private static Logger logger = Logger.getLogger(
            SetAction.class.getName());
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
     * Process an action string that begin with "select".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;
        switch (actionString) {
            case "set batchHint":
                Maud.gui.buildMenus.setBatchHint();
                break;
            case "set cullHint":
                Maud.gui.buildMenus.setCullHint();
                break;
            case "set queueBucket":
                Maud.gui.buildMenus.setQueueBucket();
                break;
            case "set shadowMode":
                Maud.gui.buildMenus.setShadowMode();
                break;
            case "set track rotation all":
                Maud.getModel().target.track.setTrackRotationAll();
                break;
            case "set track scale all":
                Maud.getModel().target.track.setTrackScaleAll();
                break;
            case "set track translation all":
                Maud.getModel().target.track.setTrackTranslationAll();
                break;
            case "set tweenRotations":
                Maud.gui.buildMenus.setTweenRotations();
                break;
            case "set tweenScales":
                Maud.gui.buildMenus.setTweenScales();
                break;
            case "set tweenTranslations":
                Maud.gui.buildMenus.setTweenTranslations();
                break;
            case "set twist cardinal":
                Maud.getModel().getMap().cardinalizeTwist();
                break;
            case "set twist snapX":
                Maud.getModel().getMap().snapTwist(0);
                break;
            case "set twist snapY":
                Maud.getModel().getMap().snapTwist(1);
                break;
            case "set twist snapZ":
                Maud.getModel().getMap().snapTwist(2);
                break;
            case "set userData":
                Maud.gui.dialogs.setUserData();
                break;
            default:
                handled = processPrefixes(actionString);
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an action that starts with "set" -- 2nd part: test prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean processPrefixes(String actionString) {
        boolean handled = true;
        String arg;
        if (actionString.startsWith(ActionPrefix.setDuration)) {
            arg = MyString.remainder(actionString, ActionPrefix.setDuration);
            float value = Float.parseFloat(arg);
            Maud.getModel().target.animation.setDuration(value);
        } else if (actionString.startsWith(ActionPrefix.setBatchHint)) {
            arg = MyString.remainder(actionString, ActionPrefix.setBatchHint);
            Spatial.BatchHint value = Spatial.BatchHint.valueOf(arg);
            Maud.getModel().target.setBatchHint(value);
        } else if (actionString.startsWith(ActionPrefix.setCullHint)) {
            arg = MyString.remainder(actionString, ActionPrefix.setCullHint);
            Spatial.CullHint value = Spatial.CullHint.valueOf(arg);
            Maud.getModel().target.setCullHint(value);
        } else if (actionString.startsWith(ActionPrefix.setQueueBucket)) {
            arg = MyString.remainder(actionString, ActionPrefix.setQueueBucket);
            RenderQueue.Bucket value = RenderQueue.Bucket.valueOf(arg);
            Maud.getModel().target.setQueueBucket(value);
        } else if (actionString.startsWith(ActionPrefix.setShadowMode)) {
            arg = MyString.remainder(actionString, ActionPrefix.setShadowMode);
            RenderQueue.ShadowMode value = RenderQueue.ShadowMode.valueOf(arg);
            Maud.getModel().target.setShadowMode(value);
        } else if (actionString.startsWith(ActionPrefix.setTweenRotations)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setTweenRotations);
            TweenRotations value = TweenRotations.valueOf(arg);
            Maud.getModel().misc.setTweenRotations(value);
        } else if (actionString.startsWith(ActionPrefix.setTweenScales)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setTweenScales);
            TweenVectors value = TweenVectors.valueOf(arg);
            Maud.getModel().misc.setTweenScales(value);
        } else if (actionString.startsWith(ActionPrefix.setTweenTranslations)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setTweenTranslations);
            TweenVectors value = TweenVectors.valueOf(arg);
            Maud.getModel().misc.setTweenTranslations(value);
        } else if (actionString.startsWith(ActionPrefix.setUserData)) {
            arg = MyString.remainder(actionString, ActionPrefix.setUserData);
            Maud.getModel().target.setUserData(arg);
        } else {
            handled = false;
        }

        return handled;
    }
}