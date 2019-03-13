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

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.shader.VarType;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.math.MyColor;
import jme3utilities.math.MyVector3f;
import maud.Maud;
import maud.MaudUtil;
import maud.dialog.EditorDialogs;
import maud.menu.MeshMenus;
import maud.model.EditorModel;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedMatParam;
import maud.model.option.Background;

/**
 * Process actions that start with the word "set" and a letter in the a-n range.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SetANAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SetANAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SetANAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "set" and a letter in
     * the a-n range.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        switch (actionString) {
            case Action.setAnisotropy:
                EditorDialogs.setAnisotropy();
                break;

            case Action.setBufferInstanceSpan:
                EditorDialogs.setBufferInstanceSpan();
                break;

            case Action.setBufferLimit:
                EditorDialogs.setBufferLimit();
                break;

            case Action.setBufferStride:
                EditorDialogs.setBufferStride();
                break;

            case Action.setDumpIndentSpaces:
                EditorDialogs.setDumpIndentSpaces();
                break;

            case Action.setDumpMaxChildren:
                EditorDialogs.setDumpMaxChildren();
                break;

            case Action.setLightDirCardinal:
                target.getLight().cardinalizeDirection();
                break;

            case Action.setLightDirReverse:
                target.getLight().reverseDirection();
                break;

            case Action.setLinkMass:
                EditorDialogs.setLinkMass();
                break;

            case Action.setMatParamValue:
                SelectedMatParam matParam = target.getMatParam();
                if (matParam.isSelected()) {
                    String parameterName = matParam.getName();
                    VarType varType = matParam.getVarType();
                    Object oldValue = matParam.getValue();
                    boolean allowNull = false;
                    EditorDialogs.setMatParamValue(parameterName, varType,
                            oldValue, allowNull, ActionPrefix.setMatParamValue);
                }
                break;

            case Action.setMeshWeights:
                MeshMenus.setMeshWeights();
                break;

            default:
                handled = testForPrefixes(actionString);
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an ongoing action that starts with the word "set" and a letter in
     * the a-n range -- 2nd part: test for prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean testForPrefixes(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        String arg;
        if (actionString.startsWith(ActionPrefix.set3DCursorColor)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.set3DCursorColor);
            String[] args = arg.split(" ");
            if (args.length >= 2) {
                int index = Integer.parseInt(args[0]);
                String colorText = MyString.remainder(arg, args[0] + " ");
                ColorRGBA color = MyColor.parse(colorText);
                model.getScene().getCursor().setColor(index, color);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.set3DCursorCycleTime)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.set3DCursorCycleTime);
            float cycleTime = Float.valueOf(arg);
            model.getScene().getCursor().setCycleTime(cycleTime);

        } else if (actionString.startsWith(ActionPrefix.set3DCursorSize)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.set3DCursorSize);
            float size = Float.valueOf(arg);
            model.getScene().getCursor().setSize(size);

        } else if (actionString.startsWith(ActionPrefix.setAmbientLevel)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setAmbientLevel);
            float level = Float.valueOf(arg);
            model.getScene().getLights().setAmbientLevel(level);

        } else if (actionString.startsWith(ActionPrefix.setAnisotropy)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setAnisotropy);
            int anisotropy = Integer.parseInt(arg);
            target.getTexture().setAnisotropy(anisotropy);

        } else if (actionString.startsWith(ActionPrefix.setAxesLineWidth)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setAxesLineWidth);
            float width = Float.valueOf(arg);
            model.getScene().getAxes().setLineWidth(width);

        } else if (actionString.startsWith(ActionPrefix.setBackgroundColor)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setBackgroundColor);
            String[] args = arg.split(" ");
            if (args.length >= 2) {
                Background which = Background.valueOf(args[0]);
                String colorText = MyString.remainder(arg, args[0] + " ");
                ColorRGBA color = MyColor.parse(colorText);
                model.setBackgroundColor(which, color);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.setBoundsColor)) {
            arg = MyString.remainder(actionString, ActionPrefix.setBoundsColor);
            ColorRGBA color = MyColor.parse(arg);
            model.getScene().getBounds().setColor(color);

        } else if (actionString.startsWith(ActionPrefix.setBoundsLineWidth)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setBoundsLineWidth);
            float width = Float.valueOf(arg);
            model.getScene().getBounds().setLineWidth(width);

        } else if (actionString.startsWith(
                ActionPrefix.setBufferInstanceSpan)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setBufferInstanceSpan);
            int span = Integer.parseInt(arg);
            target.getBuffer().setInstanceSpan(span);

        } else if (actionString.startsWith(ActionPrefix.setBufferLimit)) {
            arg = MyString.remainder(actionString, ActionPrefix.setBufferLimit);
            int limit = Integer.parseInt(arg);
            target.getBuffer().setLimit(limit);

        } else if (actionString.startsWith(ActionPrefix.setBufferStride)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setBufferStride);
            int stride = Integer.parseInt(arg);
            target.getBuffer().setStride(stride);

        } else if (actionString.startsWith(ActionPrefix.setCloudiness)) {
            arg = MyString.remainder(actionString, ActionPrefix.setCloudiness);
            float fraction = Float.parseFloat(arg);
            model.getScene().getRender().setCloudiness(fraction);

        } else if (actionString.startsWith(ActionPrefix.setDumpIndentSpaces)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setDumpIndentSpaces);
            int numSpaces = Integer.parseInt(arg);
            String indent = MaudUtil.repeat(" ", numSpaces);
            model.getDumper().setIndentIncrement(indent);

        } else if (actionString.startsWith(ActionPrefix.setDumpMaxChildren)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setDumpMaxChildren);
            int maxCount = Integer.parseInt(arg);
            model.getDumper().setMaxChildren(maxCount);

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

        } else if (actionString.startsWith(ActionPrefix.setFrameTime)) {
            arg = MyString.remainder(actionString, ActionPrefix.setFrameTime);
            float value = Float.parseFloat(arg);
            target.getFrame().setTime(value);

        } else if (actionString.startsWith(ActionPrefix.setHour)) {
            arg = MyString.remainder(actionString, ActionPrefix.setHour);
            float hour = Float.valueOf(arg);
            model.getScene().getRender().setHour(hour);

        } else if (actionString.startsWith(ActionPrefix.setLinkMass)) {
            arg = MyString.remainder(actionString, ActionPrefix.setLinkMass);
            float mass = Float.valueOf(arg);
            target.setLinkMass(mass);

        } else if (actionString.startsWith(ActionPrefix.setMainDirection)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setMainDirection);
            Vector3f direction = MyVector3f.parse(arg);
            model.getScene().getLights().setDirection(direction);

        } else if (actionString.startsWith(ActionPrefix.setMainLevel)) {
            arg = MyString.remainder(actionString, ActionPrefix.setMainLevel);
            float level = Float.valueOf(arg);
            model.getScene().getLights().setMainLevel(level);

        } else if (actionString.startsWith(ActionPrefix.setMapSize)) {
            arg = MyString.remainder(actionString, ActionPrefix.setMapSize);
            int mapSize = Integer.parseInt(arg);
            model.getScene().getRender().setShadowMapSize(mapSize);

        } else if (actionString.startsWith(ActionPrefix.setMatParamValue)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setMatParamValue);
            target.setMatParamValue(arg);

        } else if (actionString.startsWith(ActionPrefix.setMeshWeights)) {
            arg = MyString.remainder(actionString, ActionPrefix.setMeshWeights);
            int mnwpv = Integer.parseInt(arg);
            target.setMeshWeights(mnwpv);

        } else if (actionString.startsWith(ActionPrefix.setNumSplits)) {
            arg = MyString.remainder(actionString, ActionPrefix.setNumSplits);
            int numSplits = Integer.parseInt(arg);
            model.getScene().getRender().setNumSplits(numSplits);

        } else {
            handled = false;
        }

        return handled;
    }
}
