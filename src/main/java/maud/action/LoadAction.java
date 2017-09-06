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
import maud.model.EditableCgm;
import maud.model.EditorModel;
import maud.model.LoadedCgm;

/**
 * Process an action string that begins with "new".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class LoadAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            LoadAction.class.getName());
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
     * Process an action string that begin with "load".
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
                Maud.gui.menus.loadAnimation(target);
                break;
            case Action.loadCgm:
                Maud.gui.buildMenus.loadCgm();
                break;
            case Action.loadMapAsset:
                Maud.gui.buildMenus.loadMapAsset();
                break;
            case Action.loadRetargetedPose:
                target.getAnimation().loadRetargetedPose();
                break;
            case Action.loadSourceAnimation:
                Maud.gui.menus.loadAnimation(source);
                break;
            case Action.loadSourceCgm:
                Maud.gui.buildMenus.loadSourceCgm();
                break;
            default:
                handled = processPrefixes(actionString);
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an action that starts with "load" -- 2nd part: test prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean processPrefixes(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        LoadedCgm source = model.getSource();
        EditableCgm target = model.getTarget();
        String args, name, path;
        if (actionString.startsWith(ActionPrefix.loadAnimation)) {
            args = MyString.remainder(actionString, ActionPrefix.loadAnimation);
            Maud.gui.menus.loadAnimation(args, target);

        } else if (actionString.startsWith(ActionPrefix.loadCgmAsset)) {
            args = MyString.remainder(actionString, ActionPrefix.loadCgmAsset);
            Maud.gui.buildMenus.loadCgmAsset(args, target);

        } else if (actionString.startsWith(ActionPrefix.loadCgmLocator)) {
            path = MyString.remainder(actionString,
                    ActionPrefix.loadCgmLocator);
            Maud.gui.buildMenus.loadCgmLocator(path, target);

        } else if (actionString.startsWith(ActionPrefix.loadCgmNamed)) {
            name = MyString.remainder(actionString, ActionPrefix.loadCgmNamed);
            target.loadNamed(name);

        } else if (actionString.startsWith(ActionPrefix.loadMapAsset)) {
            path = MyString.remainder(actionString, ActionPrefix.loadMapAsset);
            Maud.gui.buildMenus.loadMapAsset(path);

        } else if (actionString.startsWith(ActionPrefix.loadMapLocator)) {
            path = MyString.remainder(actionString,
                    ActionPrefix.loadMapLocator);
            Maud.gui.buildMenus.loadMapLocator(path);

        } else if (actionString.startsWith(ActionPrefix.loadMapNamed)) {
            name = MyString.remainder(actionString,
                    ActionPrefix.loadMapNamed);
            model.getMap().loadNamed(name);

        } else if (actionString.startsWith(ActionPrefix.loadSourceAnimation)) {
            args = MyString.remainder(actionString,
                    ActionPrefix.loadSourceAnimation);
            Maud.gui.menus.loadAnimation(args, source);

        } else if (actionString.startsWith(ActionPrefix.loadSourceCgmAsset)) {
            args = MyString.remainder(actionString,
                    ActionPrefix.loadSourceCgmAsset);
            Maud.gui.buildMenus.loadCgmAsset(args, source);

        } else if (actionString.startsWith(ActionPrefix.loadSourceCgmLocator)) {
            path = MyString.remainder(actionString,
                    ActionPrefix.loadSourceCgmLocator);
            Maud.gui.buildMenus.loadCgmLocator(path, source);

        } else if (actionString.startsWith(ActionPrefix.loadSourceCgmNamed)) {
            name = MyString.remainder(actionString,
                    ActionPrefix.loadSourceCgmNamed);
            source.loadNamed(name);

        } else {
            handled = false;
        }

        return handled;
    }
}
