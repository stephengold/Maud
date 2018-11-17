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

import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.menu.AnimationMenus;
import maud.menu.CgmMenus;
import maud.menu.EditorMenus;
import maud.model.EditorModel;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.LoadedCgm;

/**
 * Process actions that start with the word "load".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class LoadAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(LoadAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private LoadAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "load".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        LoadedCgm source = Maud.getModel().getSource();
        LoadedCgm target = Maud.getModel().getTarget();
        switch (actionString) {
            case Action.loadAnimation:
                AnimationMenus.loadAnimation(target);
                break;

            case Action.loadCgm:
                CgmMenus.loadCgm();
                break;

            case Action.loadMapAsset:
                EditorMenus.loadMapAsset();
                break;

            case Action.loadRetargetedPose:
                target.getAnimation().loadRetargetedPose();
                break;

            case Action.loadSourceAnimation:
                AnimationMenus.loadAnimation(source);
                break;

            case Action.loadSourceCgm:
                CgmMenus.loadSourceCgm();
                break;

            case Action.loadTexture:
                EditorMenus.loadTexture();
                break;

            default:
                handled = testForPrefixes(actionString);
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an ongoing action that starts with the word "load" -- 2nd part:
     * test for prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean testForPrefixes(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        LoadedCgm source = model.getSource();
        EditableCgm target = model.getTarget();

        String args, name, spec;
        if (actionString.startsWith(ActionPrefix.loadAnimation)) {
            name = MyString.remainder(actionString, ActionPrefix.loadAnimation);
            AnimationMenus.loadAnimation(name, target);

        } else if (actionString.startsWith(ActionPrefix.loadCgmAsset)) {
            args = MyString.remainder(actionString, ActionPrefix.loadCgmAsset);
            Maud.gui.buildMenus.loadCgmAsset(args, target);

        } else if (actionString.startsWith(ActionPrefix.loadCgmLocator)) {
            spec = MyString.remainder(actionString,
                    ActionPrefix.loadCgmLocator);
            Maud.gui.buildMenus.loadCgmLocator(spec, target);

        } else if (actionString.startsWith(ActionPrefix.loadCgmNamed)) {
            name = MyString.remainder(actionString, ActionPrefix.loadCgmNamed);
            target.loadNamed(name);

        } else if (actionString.startsWith(ActionPrefix.loadMapAsset)) {
            args = MyString.remainder(actionString, ActionPrefix.loadMapAsset);
            Maud.gui.buildMenus.loadMapAsset(args);

        } else if (actionString.startsWith(ActionPrefix.loadMapLocator)) {
            spec = MyString.remainder(actionString,
                    ActionPrefix.loadMapLocator);
            Maud.gui.buildMenus.loadMapLocator(spec);

        } else if (actionString.startsWith(ActionPrefix.loadMapNamed)) {
            name = MyString.remainder(actionString,
                    ActionPrefix.loadMapNamed);
            model.getMap().loadNamed(name);

        } else if (actionString.startsWith(ActionPrefix.loadSourceAnimation)) {
            name = MyString.remainder(actionString,
                    ActionPrefix.loadSourceAnimation);
            AnimationMenus.loadAnimation(name, source);

        } else if (actionString.startsWith(ActionPrefix.loadSourceCgmAsset)) {
            args = MyString.remainder(actionString,
                    ActionPrefix.loadSourceCgmAsset);
            Maud.gui.buildMenus.loadCgmAsset(args, source);

        } else if (actionString.startsWith(ActionPrefix.loadSourceCgmLocator)) {
            spec = MyString.remainder(actionString,
                    ActionPrefix.loadSourceCgmLocator);
            Maud.gui.buildMenus.loadCgmLocator(spec, source);

        } else if (actionString.startsWith(ActionPrefix.loadSourceCgmNamed)) {
            name = MyString.remainder(actionString,
                    ActionPrefix.loadSourceCgmNamed);
            source.loadNamed(name);

        } else if (actionString.startsWith(ActionPrefix.loadTextureAsset)) {
            args = MyString.remainder(actionString,
                    ActionPrefix.loadTextureAsset);
            Maud.gui.buildMenus.loadTextureAsset(args);

        } else if (actionString.startsWith(ActionPrefix.loadTextureLocator)) {
            spec = MyString.remainder(actionString,
                    ActionPrefix.loadTextureLocator);
            Maud.gui.buildMenus.loadTextureLocator(spec);

        } else {
            handled = false;
        }

        return handled;
    }
}
