/*
 Copyright (c) 2017-2022, Stephen Gold
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

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.shadow.EdgeFilteringMode;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.menu.AnimationMenus;
import maud.menu.BoneMenus;
import maud.menu.EnumMenus;
import maud.menu.MeshMenus;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.option.Background;
import maud.model.option.scene.AxesDragEffect;
import maud.model.option.scene.AxesSubject;

/**
 * Process actions that start with the word "select" and a letter in the a-e
 * range.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final class SelectAEAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectAEAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SelectAEAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "select" and a letter
     * in the a-e range.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        Cgm target = model.getTarget();
        switch (actionString) {
            case Action.selectAnimationEditMenu:
                if (target.getAnimation().isReal()) {
                    AnimationMenus.editAnimation();
                }
                break;

            case Action.selectAnimControl:
                AnimationMenus.selectAnimControl(target);
                break;

            case Action.selectAxesDragEffect:
                EnumMenus.selectAxesDragEffect();
                break;

            case Action.selectAxesSubject:
                EnumMenus.selectAxesSubject();
                break;

            case Action.selectBackground:
                EnumMenus.selectBackground();
                break;

            case Action.selectBatchHint:
                EnumMenus.selectBatchHint();
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

            case Action.selectBoneTrack:
                target.getBone().selectTrack();
                break;

            case Action.selectBuffer:
                MeshMenus.selectBuffer("");
                break;

            case Action.selectBufferUsage:
                EnumMenus.selectBufferUsage();
                break;

            case Action.selectCullHint:
                EnumMenus.selectCullHint();
                break;

            case Action.selectEdgeFilter:
                EnumMenus.selectEdgeFilter();
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
     * in the a-e range -- 2nd part: test for prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean testForPrefixes(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        String arg;
        if (actionString.startsWith(ActionPrefix.selectAnimControl)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectAnimControl);
            target.getAnimControl().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectAxesDragEffect)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectAxesDragEffect);
            AxesDragEffect value = AxesDragEffect.valueOf(arg);
            model.getScene().getAxes().setDragEffect(value);

        } else if (actionString.startsWith(ActionPrefix.selectAxesSubject)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectAxesSubject);
            AxesSubject value = AxesSubject.valueOf(arg);
            model.getScene().getAxes().setSubject(value);

        } else if (actionString.startsWith(ActionPrefix.selectBackground)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectBackground);
            Background value = Background.valueOf(arg);
            model.getMisc().selectBackground(value);

        } else if (actionString.startsWith(ActionPrefix.selectBatchHint)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectBatchHint);
            Spatial.BatchHint value = Spatial.BatchHint.valueOf(arg);
            target.setBatchHint(value);

        } else if (actionString.startsWith(ActionPrefix.selectBone)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectBone);
            BoneMenus.selectBone(arg);

        } else if (actionString.startsWith(ActionPrefix.selectBoneChild)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectBoneChild);
            BoneMenus.selectBoneChild(arg);

        } else if (actionString.startsWith(ActionPrefix.selectBoneIndex)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectBoneIndex);
            int index = Integer.parseInt(arg);
            int indexBase = Maud.getModel().getMisc().indexBase();
            target.getBone().select(index - indexBase);

        } else if (actionString.startsWith(ActionPrefix.selectBuffer)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectBuffer);
            MeshMenus.selectBuffer(arg);

        } else if (actionString.startsWith(ActionPrefix.selectBufferUsage)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectBufferUsage);
            VertexBuffer.Usage usage = VertexBuffer.Usage.valueOf(arg);
            target.getBuffer().setUsage(usage);

        } else if (actionString.startsWith(ActionPrefix.selectCullHint)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectCullHint);
            Spatial.CullHint value = Spatial.CullHint.valueOf(arg);
            target.setCullHint(value);

        } else if (actionString.startsWith(ActionPrefix.selectCursorColor)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectCursorColor);
            int index = Integer.parseInt(arg);
            model.getMisc().setColorIndex(index);

        } else if (actionString.startsWith(ActionPrefix.selectEdgeFilter)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectEdgeFilter);
            EdgeFilteringMode newMode = EdgeFilteringMode.valueOf(arg);
            model.getScene().getRender().setEdgeFilter(newMode);

        } else if (actionString.startsWith(ActionPrefix.selectExtremeVertex)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectExtremeVertex);
            String[] args = arg.split(" ");
            if (args.length == 3) {
                float x = Float.parseFloat(args[0]);
                float y = Float.parseFloat(args[1]);
                float z = Float.parseFloat(args[2]);
                Vector3f direction = new Vector3f(x, y, z);
                model.getTarget().getVertex().selectExtreme(direction);
            } else {
                handled = false;
            }

        } else {
            handled = false;
        }

        return handled;
    }
}
