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

import com.jme3.scene.VertexBuffer;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.SizeValue;
import de.lessvoid.nifty.tools.SizeValueType;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.WindowController;
import jme3utilities.wes.TweenRotations;
import jme3utilities.wes.TweenVectors;
import maud.Maud;
import maud.menu.EnumMenus;
import maud.menu.ShowMenus;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.cgm.SelectedVertex;
import maud.model.option.ViewMode;
import maud.model.option.scene.TriangleMode;

/**
 * Process actions that start with the word "select" and a letter in the t-z
 * range.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SelectTZAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectTZAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SelectTZAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "select" and a letter
     * in the t-z range.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        switch (actionString) {
            case Action.selectTriangleMode:
                EnumMenus.selectTriangleMode();
                break;

            case Action.selectTweenRotations:
                EnumMenus.selectTweenRotations();
                break;

            case Action.selectTweenScales:
                EnumMenus.selectTweenScales();
                break;

            case Action.selectTweenTranslations:
                EnumMenus.selectTweenTranslations();
                break;

            case Action.selectUserKey:
                ShowMenus.selectUserKey();
                break;

            case Action.selectVertex:
                ShowMenus.selectVertex();
                break;

            case Action.selectVertexBuffer:
                EnumMenus.selectVertexBuffer();
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
     * in the t-z range -- 2nd part: test for prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean testForPrefixes(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        Cgm target = model.getTarget();
        String arg;

        if (actionString.startsWith(ActionPrefix.selectTriangleMode)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectTriangleMode);
            TriangleMode mode = TriangleMode.valueOf(arg);
            model.getScene().getRender().setTriangleMode(mode);

        } else if (actionString.startsWith(ActionPrefix.selectTweenRotations)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectTweenRotations);
            TweenRotations value = TweenRotations.valueOf(arg);
            model.getTweenTransforms().setTweenRotations(value);

        } else if (actionString.startsWith(ActionPrefix.selectTweenScales)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectTweenScales);
            TweenVectors value = TweenVectors.valueOf(arg);
            model.getTweenTransforms().setTweenScales(value);

        } else if (actionString.startsWith(
                ActionPrefix.selectTweenTranslations)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectTweenTranslations);
            TweenVectors value = TweenVectors.valueOf(arg);
            model.getTweenTransforms().setTweenTranslations(value);

        } else if (actionString.startsWith(ActionPrefix.selectUserKey)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectUserKey);
            target.getUserData().selectKey(arg);

        } else if (actionString.startsWith(ActionPrefix.selectVertex)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectVertex);
            int index = Integer.parseInt(arg);
            int indexBase = Maud.getModel().getMisc().getIndexBase();
            target.getVertex().select(index - indexBase);

        } else if (actionString.startsWith(ActionPrefix.selectVertexBone)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectVertexBone);
            int weightIndex = Integer.parseInt(arg);
            SelectedVertex vertex = target.getVertex();
            int[] boneIndices = vertex.boneIndices(null);
            int boneIndex = boneIndices[weightIndex];
            target.getBone().select(boneIndex);

        } else if (actionString.startsWith(ActionPrefix.selectVertexBuffer)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectVertexBuffer);
            VertexBuffer.Type buffer = VertexBuffer.Type.valueOf(arg);
            model.getMisc().selectVertexBuffer(buffer);

        } else if (actionString.startsWith(ActionPrefix.selectViewMode)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectViewMode);
            ViewMode newSetting = ViewMode.valueOf(arg);
            model.getMisc().selectViewMode(newSetting);

        } else {
            handled = false;
        }

        if (!handled && actionString.startsWith(ActionPrefix.selectTool)) {
            String toolName = MyString.remainder(actionString,
                    ActionPrefix.selectTool);
            WindowController tool = Maud.gui.getTool(toolName);
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

                WindowController tool = Maud.gui.getTool(toolName);
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
