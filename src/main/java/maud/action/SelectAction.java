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

import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.SizeValue;
import de.lessvoid.nifty.tools.SizeValueType;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.menu.AnimationMenus;
import maud.menu.BoneMenus;
import maud.menu.EditorMenus;
import maud.menu.PhysicsMenus;
import maud.menu.ShowMenus;
import maud.menu.SpatialMenus;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedSgc;
import maud.model.option.RigidBodyParameter;
import maud.model.option.ShapeParameter;
import maud.model.option.scene.OrbitCenter;
import maud.view.SceneDrag;
import maud.view.ScoreDrag;

/**
 * Process an action string that begins with the word "select".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SelectAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectAction.class.getName());
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
                AnimationMenus.selectAnimControl(target);
                break;

            case Action.selectBone:
                BoneMenus.selectBone();
                break;

            case Action.selectBoneChild:
                BoneMenus.selectBoneChild();
                break;

            case Action.selectBoneParent:
                target.getBone().selectParent();
                break;

            case Action.selectJoint:
                PhysicsMenus.selectJoint(target);
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
                ShowMenus.selectOrbitCenter();
                break;

            case Action.selectPhysics:
                PhysicsMenus.selectObject(target);
                break;

            case Action.selectPhysicsRbp:
                PhysicsMenus.selectRbp();
                break;

            case Action.selectPhysicsShape:
                long shapeId = target.getObject().getShapeId();
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
                ShowMenus.selectSgc();
                break;

            case Action.selectSgcObject:
                SelectedSgc sgc = target.getSgc();
                String objectName = sgc.physicsObjectName();
                if (!objectName.isEmpty() && sgc.isEnabled()) {
                    target.getObject().select(objectName);
                    Maud.gui.tools.select("object");
                }
                break;

            case Action.selectSgcSpatial:
                target.getSpatial().selectControlled();
                break;

            case Action.selectShape:
                PhysicsMenus.selectShape(target);
                break;

            case Action.selectShapeChild:
                PhysicsMenus.selectShapeChild();
                break;

            case Action.selectShapeParm:
                PhysicsMenus.selectShapeParameter();
                break;

            case Action.selectShapeUser:
                PhysicsMenus.selectShapeUser();
                break;

            case Action.selectSourceAnimControl:
                AnimationMenus.selectAnimControl(model.getSource());
                break;

            case Action.selectSourceBone:
                BoneMenus.selectSourceBone();
                break;

            case Action.selectSpatialChild:
                ShowMenus.selectSpatialChild("");
                break;

            case Action.selectSpatialParent:
                target.getSpatial().selectParent();
                break;

            case Action.selectUserKey:
                ShowMenus.selectUserKey();
                break;

            case Action.selectVertex:
                ShowMenus.selectVertex();
                break;

            default:
                handled = testForPrefixes(actionString);
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
                SceneDrag.clear();
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
     * Process an action that starts with "select" -- 2nd part: test for
     * prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean testForPrefixes(String actionString) {
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
            BoneMenus.selectBone(arg);

        } else if (actionString.startsWith(ActionPrefix.selectBoneChild)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectBoneChild);
            ShowMenus.selectBoneChild(arg);

        } else if (actionString.startsWith(ActionPrefix.selectGeometry)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectGeometry);
            SpatialMenus.selectSpatial(arg, false);

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
            target.getObject().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectPhysicsRbp)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectPhysicsRbp);
            RigidBodyParameter rbp = RigidBodyParameter.valueOf(arg);
            model.getMisc().setRbp(rbp);

        } else if (actionString.startsWith(ActionPrefix.selectSgc)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectSgc);
            target.getSgc().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectShape)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectShape);
            int colonPosition = arg.indexOf(":");
            if (colonPosition == -1) {
                handled = false;
            } else {
                String hexId = arg.substring(colonPosition + 1);
                long id = Long.parseLong(hexId, 16);
                target.getShape().select(id);
            }

        } else if (actionString.startsWith(ActionPrefix.selectShapeParm)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectShapeParm);
            ShapeParameter parameter = ShapeParameter.valueOf(arg);
            model.getMisc().setShapeParameter(parameter);

        } else if (actionString.startsWith(
                ActionPrefix.selectSourceAnimControl)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectSourceAnimControl);
            model.getSource().getAnimControl().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectSourceBone)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectSourceBone);
            BoneMenus.selectSourceBone(arg);

        } else if (actionString.startsWith(ActionPrefix.selectSpatialChild)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectSpatialChild);
            Maud.gui.selectSpatialChild(arg);

        } else if (actionString.startsWith(ActionPrefix.selectSpatial)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectSpatial);
            SpatialMenus.selectSpatial(arg, true);

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
            handled = EditorMenus.selectMenuItem(menuPath);
        }
        if (!handled && actionString.startsWith(ActionPrefix.selectTool)) {
            String toolName = MyString.remainder(actionString,
                    ActionPrefix.selectTool);
            WindowController tool = Maud.gui.tools.getTool(toolName);
            if (tool != null) {
                tool.select();
                handled = true;
            }
        }
        if (!handled && actionString.startsWith(ActionPrefix.selectToolAt)) {
            String argList = MyString.remainder(actionString,
                    ActionPrefix.selectToolAt);
            String[] args = argList.split(" ");
            if (args.length == 3) {
                String toolName = args[0];
                int x = Integer.parseInt(args[1]);
                int y = Integer.parseInt(args[2]);

                WindowController tool = Maud.gui.tools.getTool(toolName);
                if (tool != null) {
                    tool.select();
                    Element element = tool.getElement();
                    SizeValue newX = new SizeValue(x, SizeValueType.Pixel);
                    element.setConstraintX(newX);
                    SizeValue newY = new SizeValue(y, SizeValueType.Pixel);
                    element.setConstraintY(newY);
                    Screen screen = Maud.gui.getScreen();
                    screen.layoutLayers();
                    handled = true;
                }
            }
        }

        return handled;
    }
}
