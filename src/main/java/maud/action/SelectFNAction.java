/*
 Copyright (c) 2017-2019, Stephen Gold
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

import com.jme3.bullet.joints.JointEnd;
import com.jme3.material.RenderState;
import com.jme3.scene.Mesh;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.ParseUtil;
import maud.menu.EditorMenus;
import maud.menu.EnumMenus;
import maud.menu.PhysicsMenus;
import maud.menu.ShowMenus;
import maud.menu.SpatialMenus;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.option.LoadBvhAxisOrder;
import maud.model.option.scene.MovementMode;

/**
 * Process actions that start with the word "select" and a letter in the f-n
 * range.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SelectFNAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectFNAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SelectFNAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "select" and a letter
     * in the f-n range.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        Cgm target = model.getTarget();
        switch (actionString) {
            case Action.selectFaceCull:
                EnumMenus.selectFaceCull();
                break;

            case Action.selectJoint:
                PhysicsMenus.selectJoint(target);
                break;

            case Action.selectKeyframeFirst:
                target.getFrame().selectFirst();
                break;

            case Action.selectKeyframeLast:
                target.getFrame().selectLast();
                break;

            case Action.selectKeyframeNearest:
                target.getFrame().selectNearest();
                break;

            case Action.selectKeyframeNext:
                target.getFrame().selectNext();
                break;

            case Action.selectKeyframePrevious:
                target.getFrame().selectPrevious();
                break;

            case Action.selectLight:
                ShowMenus.selectLight();
                break;

            case Action.selectLightOwner:
                target.getSpatial().selectLightOwner();
                break;

            case Action.selectLink:
                PhysicsMenus.selectLink();
                break;

            case Action.selectLinkChild:
                PhysicsMenus.selectLinkChild();
                break;

            case Action.selectLinkedBone:
                String name = target.getLink().boneName();
                target.getBone().select(name);
                Maud.gui.tools.select("bone");
                break;

            case Action.selectLinkedJoint:
                target.getLink().selectJoint();
                Maud.gui.tools.select("joint");
                break;

            case Action.selectLinkedObject:
                target.getLink().selectObject();
                Maud.gui.tools.select("object");
                break;

            case Action.selectLinkParent:
                String name2 = target.getLink().nameParent();
                target.getLink().select(name2);
                break;

            case Action.selectLinkToolAxis:
                PhysicsMenus.selectLinkToolAxis();
                break;

            case Action.selectLoadBvhAxisOrder:
                EnumMenus.selectLoadBvhAxisOrder();
                break;

            case Action.selectMapSourceBone:
                model.getMap().selectFromSource();
                break;

            case Action.selectMapTargetBone:
                model.getMap().selectFromTarget();
                break;

            case Action.selectMaterialEditMenu:
                SpatialMenus.editMaterial();
                break;

            case Action.selectMatParam:
                ShowMenus.selectMatParam("");
                break;

            case Action.selectMeshMode:
                EnumMenus.selectMeshMode();
                break;

            default:
                handled = testForPrefixes(actionString);
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an ongoing action that starts with the word "select" and a letter
     * in the f-n range -- 2nd part: test for prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean testForPrefixes(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        String arg;
        if (actionString.startsWith(ActionPrefix.selectFaceCull)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectFaceCull);
            RenderState.FaceCullMode newMode
                    = RenderState.FaceCullMode.valueOf(arg);
            target.setFaceCullMode(newMode);

        } else if (actionString.startsWith(ActionPrefix.selectIndexBase)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectIndexBase);
            int base = Integer.parseInt(arg);
            model.getMisc().setIndexBase(base);

        } else if (actionString.startsWith(ActionPrefix.selectJoint)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectJoint);
            target.getJoint().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectJointBody)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectJointBody);
            JointEnd end = JointEnd.valueOf(arg);
            String bodyName = target.getJoint().endName(end);
            target.getObject().select(bodyName);
            Maud.gui.tools.select("object");

        } else if (actionString.startsWith(ActionPrefix.selectKeyframe)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectKeyframe);
            int index = Integer.parseInt(arg);
            int indexBase = Maud.getModel().getMisc().indexBase();
            target.getFrame().select(index - indexBase);

        } else if (actionString.startsWith(ActionPrefix.selectLight)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectLight);
            target.getLight().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectLink)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectLink);
            target.getLink().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectLinkChild)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectLinkChild);
            PhysicsMenus.selectLinkChild(arg);

        } else if (actionString.startsWith(ActionPrefix.selectLinkToolAxis)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectLinkToolAxis);
            int axisIndex = ParseUtil.axisIndex(arg);
            model.getMisc().selectLinkToolAxis(axisIndex);

        } else if (actionString.startsWith(
                ActionPrefix.selectLoadBvhAxisOrder)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectLoadBvhAxisOrder);
            LoadBvhAxisOrder axisOrder = LoadBvhAxisOrder.valueOf(arg);
            model.getMisc().selectLoadBvhAxisOrder(axisOrder);

        } else if (actionString.startsWith(ActionPrefix.selectMatParam)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectMatParam);
            ShowMenus.selectMatParam(arg);

        } else if (actionString.startsWith(ActionPrefix.selectMenuItem)) {
            String menuPath = MyString.remainder(actionString,
                    ActionPrefix.selectMenuItem);
            handled = EditorMenus.selectMenuItem(menuPath);

        } else if (actionString.startsWith(ActionPrefix.selectMeshMode)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectMeshMode);
            Mesh.Mode mode = Mesh.Mode.valueOf(arg);
            target.setMeshMode(mode);

        } else if (actionString.startsWith(ActionPrefix.selectMovement)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectMovement);
            MovementMode mode = MovementMode.valueOf(arg);
            model.getScene().getCamera().setMode(mode);

        } else {
            handled = false;
        }

        return handled;
    }
}
