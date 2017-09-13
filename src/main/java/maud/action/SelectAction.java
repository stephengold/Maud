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
import maud.model.Cgm;
import maud.model.EditorModel;
import maud.model.option.OrbitCenter;
import maud.view.SceneDrag;
import maud.view.ScoreDrag;

/**
 * Process an action string that begins with "select".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SelectAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SelectAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SelectAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action string that begin with "select".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;
        EditorModel model = Maud.getModel();
        Cgm target = model.getTarget();
        switch (actionString) {
            case Action.selectAnimControl:
                Maud.gui.showMenus.selectAnimControl(target);
                break;
            case Action.selectBone:
                Maud.gui.buildMenus.selectBone();
                break;
            case Action.selectBoneChild:
                Maud.gui.boneMenus.selectBoneChild();
                break;
            case Action.selectBoneParent:
                target.getBone().selectParent();
                break;
            case Action.selectJoint:
                Maud.gui.showMenus.selectJoint(target);
                break;
            case Action.selectKeyframeFirst:
                target.getTrack().selectFirstKeyframe();
                break;
            case Action.selectKeyframeLast:
                target.getTrack().selectLastKeyframe();
                break;
            case Action.selectKeyframeNearest:
                target.getTrack().selectNearestKeyframe();
                break;
            case Action.selectKeyframeNext:
                target.getTrack().selectNextKeyframe();
                break;
            case Action.selectKeyframePrevious:
                target.getTrack().selectPreviousKeyframe();
                break;
            case Action.selectMapSourceBone:
                model.getMap().selectFromSource();
                break;
            case Action.selectMapTargetBone:
                model.getMap().selectFromTarget();
                break;
            case Action.selectOrbitCenter:
                Maud.gui.showMenus.selectOrbitCenter();
                break;
            case Action.selectPhysics:
                Maud.gui.showMenus.selectPhysics(target);
                break;
            case Action.selectPhysicsShape:
                long shapeId = target.getPhysics().getShapeId();
                if (shapeId != -1) {
                    target.getShape().select(shapeId);
                    Maud.gui.tools.select("shape");
                }
                break;
            case Action.selectScreenBone:
                Maud.gui.selectBone();
                break;
            case Action.selectScreenGnomon:
                Maud.gui.selectGnomon();
                break;
            case Action.selectScreenKeyframe:
                Maud.gui.selectKeyframe();
                break;
            case Action.selectScreenVertex:
                Maud.gui.selectVertex();
                break;
            case Action.selectScreenXY:
                Maud.gui.selectXY();
                break;
            case Action.selectSgc:
                Maud.gui.showMenus.selectSgc();
                break;
            case Action.selectSgcObject:
                String physicsName = target.getSgc().objectName();
                if (!physicsName.isEmpty()) {
                    target.getPhysics().select(physicsName);
                    Maud.gui.tools.select("physics");
                }
                break;
            case Action.selectShape:
                Maud.gui.showMenus.selectShape(target);
                break;
            case Action.selectShapeChild:
                Maud.gui.menus.selectShapeChild();

                break;
            case Action.selectSourceAnimControl:
                Maud.gui.showMenus.selectAnimControl(model.getSource());
                break;
            case Action.selectSourceBone:
                Maud.gui.buildMenus.selectSourceBone();
                break;
            case Action.selectSpatialChild:
                Maud.gui.showMenus.selectSpatialChild("");
                break;
            case Action.selectSpatialParent:
                target.getSpatial().selectParent();
                break;
            case Action.selectUserKey:
                Maud.gui.showMenus.selectUserKey();
                break;
            case Action.selectVertex:
                Maud.gui.showMenus.selectVertex();
                break;
            default:
                handled = processPrefixes(actionString);
        }

        return handled;
    }

    /**
     * Process a non-ongoing action string that begin with "select".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean processNotOngoing(String actionString) {
        boolean handled = true;
        switch (actionString) {
            case Action.selectScreenBone:
            case Action.selectScreenKeyframe:
            case Action.selectScreenVertex:
                break;

            case Action.selectScreenGnomon:
                ScoreDrag.setDraggingGnomon(null);
                break;

            case Action.selectScreenXY:
                SceneDrag.clearDragAxis();
                ScoreDrag.setDraggingGnomon(null);
                break;
            default:
                handled = false;
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an action that starts with "select" -- 2nd part: test prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean processPrefixes(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        Cgm target = model.getTarget();
        String arg;
        if (actionString.startsWith(ActionPrefix.selectAnimControl)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectAnimControl);
            target.getAnimControl().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectBone)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectBone);
            Maud.gui.boneMenus.selectBone(arg);

        } else if (actionString.startsWith(ActionPrefix.selectBoneChild)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectBoneChild);
            Maud.gui.showMenus.selectBoneChild(arg);

        } else if (actionString.startsWith(ActionPrefix.selectGeometry)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectGeometry);
            Maud.gui.menus.selectSpatial(arg, false);

        } else if (actionString.startsWith(ActionPrefix.selectJoint)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectJoint);
            long id = Long.parseLong(arg, 16);
            target.getJoint().select(id);

        } else if (actionString.startsWith(ActionPrefix.selectOrbitCenter)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectOrbitCenter);
            OrbitCenter oc = OrbitCenter.parse(arg);
            model.getScene().getCamera().setMode(oc);

        } else if (actionString.startsWith(ActionPrefix.selectPhysics)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectPhysics);
            target.getPhysics().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectSgc)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectSgc);
            target.getSgc().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectShape)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectShape);
            long id = Long.parseLong(arg, 16);
            target.getShape().select(id);

        } else if (actionString.startsWith(
                ActionPrefix.selectSourceAnimControl)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectSourceAnimControl);
            model.getSource().getAnimControl().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectSourceBone)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectSourceBone);
            Maud.gui.boneMenus.selectSourceBone(arg);

        } else if (actionString.startsWith(ActionPrefix.selectSpatialChild)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectSpatialChild);
            Maud.gui.selectSpatialChild(arg);

        } else if (actionString.startsWith(ActionPrefix.selectSpatial)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectSpatial);
            Maud.gui.menus.selectSpatial(arg, true);

        } else if (actionString.startsWith(ActionPrefix.selectUserKey)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectUserKey);
            target.getUserData().selectKey(arg);

        } else if (actionString.startsWith(ActionPrefix.selectVertex)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectVertex);
            int index = Integer.parseInt(arg);
            int indexBase = Maud.getModel().getMisc().getIndexBase();
            target.getVertex().select(index - indexBase);

        } else {
            handled = false;
        }

        if (!handled && actionString.startsWith(ActionPrefix.selectMenuItem)) {
            String menuPath = MyString.remainder(actionString,
                    ActionPrefix.selectMenuItem);
            handled = Maud.gui.menus.selectMenuItem(menuPath);
        }
        if (!handled && actionString.startsWith(ActionPrefix.selectTool)) {
            String toolName = MyString.remainder(actionString,
                    ActionPrefix.selectTool);
            handled = Maud.gui.selectTool(toolName);
        }

        return handled;
    }
}
