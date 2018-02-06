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

import com.jme3.light.Light;
import com.jme3.shader.VarType;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.PhysicsUtil;
import maud.dialog.EditorDialogs;
import maud.menu.AnimationMenus;
import maud.menu.EnumMenus;
import maud.menu.ShowMenus;
import maud.model.EditorModel;
import maud.model.History;
import maud.model.WhichCgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedAnimControl;
import maud.model.cgm.SelectedTrack;
import maud.model.cgm.UserDataType;
import maud.tool.HistoryTool;

/**
 * Process actions that start with the word "new".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class NewAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(NewAction.class.getName());
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
     * Process an ongoing action that starts with the word "new".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        switch (actionString) {
            case Action.newAnimation:
                AnimationMenus.addNewAnimation();
                break;

            case Action.newAnimationFromPose:
                EditorDialogs.newAnimationFromPose();
                break;

            case Action.newAnimationFromRetarget:
                EditorDialogs.newAnimationFromRetarget();
                break;

            case Action.newCheckpoint:
                History.addCheckpoint();
                HistoryTool historyTool
                        = (HistoryTool) Maud.gui.getTool("history");
                historyTool.setAutoScroll();
                break;

            case Action.newLight:
                EnumMenus.addNewLight();
                break;

            case Action.newMapping:
                model.getMap().mapBones();
                break;

            case Action.newOverride:
                EnumMenus.selectOverrideType();
                break;

            case Action.newSgc:
                ShowMenus.addNewSgc();
                break;

            case Action.newSingleKeyframe: // insert OR replace
                SelectedTrack track = model.getTarget().getTrack();
                if (track.isTrackSelected()) {
                    int frameIndex = track.findKeyframeIndex();
                    if (frameIndex == -1) {
                        track.insertKeyframe();
                    } else {
                        track.replaceKeyframe();
                    }
                }
                break;

            case Action.newUserKey:
                EnumMenus.selectUserDataType();
                break;

            default:
                handled = testForPrefixes(actionString);
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an ongoing action that starts with the word "new" -- 2nd part:
     * test for prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean testForPrefixes(String actionString) {
        boolean handled = true;

        EditableCgm target = Maud.getModel().getTarget();
        if (actionString.startsWith(ActionPrefix.newAssetLocation)) {
            String path = MyString.remainder(actionString,
                    ActionPrefix.newAssetLocation);
            Maud.gui.buildMenus.newAssetLocation(path);

        } else if (actionString.startsWith(ActionPrefix.newAssetLocationSpec)) {
            String spec = MyString.remainder(actionString,
                    ActionPrefix.newAssetLocationSpec);
            Maud.getModel().getLocations().addSpec(spec);

        } else if (actionString.startsWith(
                ActionPrefix.newAnimationFromChain)) {
            String argList = MyString.remainder(actionString,
                    ActionPrefix.newAnimationFromChain);
            String[] args = argList.split(" ");
            if (args.length > 2) {
                SelectedAnimControl sac = target.getAnimControl();
                WhichCgm which1 = WhichCgm.valueOf(args[0]);
                WhichCgm which2 = WhichCgm.valueOf(args[1]);
                String prefix = args[0] + " " + args[1] + " ";
                String newAnimationName = MyString.remainder(argList, prefix);
                sac.chain(which1, which2, newAnimationName);
                target.getAnimation().load(newAnimationName);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.newAnimationFromCopy)) {
            String newName = MyString.remainder(actionString,
                    ActionPrefix.newAnimationFromCopy);
            target.getAnimControl().newFromCopy(newName);
            target.getAnimation().load(newName);

        } else if (actionString.startsWith(ActionPrefix.newAnimationFromMix)) {
            String args = MyString.remainder(actionString,
                    ActionPrefix.newAnimationFromMix);
            if (args.contains(" ")) {
                String indices = args.split(" ")[0];
                String newAnimationName
                        = MyString.remainder(args, indices + " ");
                target.getAnimControl().mix(indices, newAnimationName);
                target.getAnimation().load(newAnimationName);
            } else {
                EditorDialogs.newAnimationFromMix(actionString + " ");
            }

        } else if (actionString.startsWith(ActionPrefix.newAnimationFromPose)) {
            String newAnimationName = MyString.remainder(actionString,
                    ActionPrefix.newAnimationFromPose);
            target.getAnimControl().newFromPose(newAnimationName);
            target.getAnimation().load(newAnimationName);

        } else if (actionString.startsWith(
                ActionPrefix.newAnimationFromRetarget)) {
            String newAnimationName = MyString.remainder(actionString,
                    ActionPrefix.newAnimationFromRetarget);
            target.getAnimControl().retarget(newAnimationName);
            target.getAnimation().load(newAnimationName);

        } else if (actionString.startsWith(ActionPrefix.newGhostControl)) {
            String shapeName = MyString.remainder(actionString,
                    ActionPrefix.newGhostControl);
            PhysicsUtil.ShapeType shapeType
                    = PhysicsUtil.ShapeType.valueOf(shapeName);
            target.getSpatial().addGhostControl(shapeType);

        } else if (actionString.startsWith(ActionPrefix.newLight)) {
            String args = MyString.remainder(actionString,
                    ActionPrefix.newLight);
            if (args.contains(" ")) {
                String typeName = args.split(" ")[0];
                Light.Type type = Light.Type.valueOf(typeName);
                String lightName = MyString.remainder(args, typeName + " ");
                target.getSpatial().addLight(type, lightName);
            } else {
                EditorDialogs.newLight(actionString + " ");
            }

        } else if (actionString.startsWith(ActionPrefix.newOverride)) {
            String args = MyString.remainder(actionString,
                    ActionPrefix.newOverride);
            if (args.contains(" ")) {
                String typeName = args.split(" ")[0];
                VarType type = VarType.valueOf(typeName);
                String parameterName = MyString.remainder(args, typeName + " ");
                target.addOverride(type, parameterName);
            } else {
                EditorDialogs.newOverride(actionString + " ");
            }

        } else if (actionString.startsWith(ActionPrefix.newRbc)) {
            String shapeName
                    = MyString.remainder(actionString, ActionPrefix.newRbc);
            PhysicsUtil.ShapeType shapeType
                    = PhysicsUtil.ShapeType.valueOf(shapeName);
            target.getSpatial().addRigidBodyControl(shapeType);

        } else if (actionString.startsWith(ActionPrefix.newUserKey)) {
            String args
                    = MyString.remainder(actionString, ActionPrefix.newUserKey);
            if (args.contains(" ")) {
                String typeName = args.split(" ")[0];
                UserDataType type = UserDataType.valueOf(typeName);
                String key = MyString.remainder(args, typeName + " ");
                target.addUserKey(type, key);
            } else {
                EditorDialogs.newUserKey(actionString + " ");
            }

        } else {
            handled = false;
        }

        return handled;
    }
}
