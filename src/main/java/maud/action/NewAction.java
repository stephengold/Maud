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

import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.dialog.EditorDialogs;
import maud.model.SelectedTrack;

/**
 * Process an action string that begins with "new".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class NewAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            NewAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private NewAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an action string that begin with "new".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;
        switch (actionString) {
            case "new animation fromPose":
                EditorDialogs.newAnimationFromPose();
                break;
            case "new checkpoint":
                Maud.gui.addCheckpoint("user interface");
                break;
            case "new mapping":
                Maud.getModel().getMap().mapBones();
                break;
            case "new singleKeyframe": // insert OR replace
                SelectedTrack track = Maud.getModel().getTarget().getTrack();
                if (track.isTrackSelected()) {
                    int frameIndex = track.findKeyframeIndex();
                    if (frameIndex == -1) {
                        track.insertKeyframe();
                    } else {
                        track.replaceKeyframe();
                    }
                }
                break;
            case "new userKey":
                Maud.gui.showMenus.selectUserDataType();
                break;
            default:
                handled = processPrefixes(actionString);
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an action that starts with "new" -- 2nd part: test prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean processPrefixes(String actionString) {
        boolean handled = false;
        if (actionString.startsWith(ActionPrefix.newAssetLocation)) {
            String path = MyString.remainder(actionString,
                    ActionPrefix.newAssetLocation);
            Maud.gui.buildMenus.newAssetLocation(path);
            handled = true;

        } else if (actionString.startsWith(ActionPrefix.newAnimationFromPose)) {
            String name = MyString.remainder(actionString,
                    ActionPrefix.newAnimationFromPose);
            Maud.getModel().getTarget().getAnimation().poseAndLoad(name);
            handled = true;

        } else if (actionString.startsWith(ActionPrefix.newUserKey)) {
            String args;
            args = MyString.remainder(actionString, ActionPrefix.newUserKey);
            if (args.contains(" ")) {
                String type = args.split(" ")[0];
                String key = MyString.remainder(args, type + " ");
                Maud.getModel().getTarget().addUserKey(type, key);
            } else {
                EditorDialogs.newUserKey(actionString + " ");
            }
            handled = true;
        }

        return handled;
    }
}
