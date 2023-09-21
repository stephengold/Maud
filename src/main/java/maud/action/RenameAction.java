/*
 Copyright (c) 2018-2022, Stephen Gold
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
import maud.model.cgm.EditableCgm;

/**
 * Process actions that start with the word "rename".
 *
 * @author Stephen Gold sgold@sonic.net
 */
final class RenameAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(RenameAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private RenameAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "rename".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        switch (actionString) {
            case Action.renameAnimation:
                EditorDialogs.renameAnimation();
                break;

            case Action.renameBone:
                EditorDialogs.renameBone();
                break;

            case Action.renameLight:
                EditorDialogs.renameLight();
                break;

            case Action.renameMaterial:
                EditorDialogs.renameMaterial();
                break;

            case Action.renameOverride:
                EditorDialogs.renameOverride();
                break;

            case Action.renameSpatial:
                EditorDialogs.renameSpatial();
                break;

            case Action.renameUserKey:
                EditorDialogs.renameUserKey();
                break;

            default:
                handled = testForPrefixes(actionString);
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an ongoing action that starts with the word "rename" -- 2nd part:
     * test for prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean testForPrefixes(String actionString) {
        boolean handled = true;

        String newName;
        EditableCgm target = Maud.getModel().getTarget();
        if (actionString.startsWith(ActionPrefix.renameAnimation)) {
            newName = MyString.remainder(actionString,
                    ActionPrefix.renameAnimation);
            target.getAnimation().rename(newName);

        } else if (actionString.startsWith(ActionPrefix.renameBone)) {
            newName = MyString.remainder(actionString, ActionPrefix.renameBone);
            target.renameBone(newName);

        } else if (actionString.startsWith(ActionPrefix.renameLight)) {
            newName = MyString.remainder(actionString,
                    ActionPrefix.renameLight);
            target.getLight().rename(newName);

        } else if (actionString.startsWith(
                ActionPrefix.renameMatchingTextures)) {
            String args = MyString.remainder(actionString,
                    ActionPrefix.renameMatchingTextures);
            String[] argArray = args.split(";");
            if (argArray.length == 2) {
                CharSequence match = argArray[0];
                CharSequence replacement = argArray[1];
                target.getTexture().replaceMatchingTextures(match, replacement);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.renameMaterial)) {
            newName = MyString.remainder(actionString,
                    ActionPrefix.renameMaterial);
            target.renameMaterial(newName);

        } else if (actionString.startsWith(ActionPrefix.renameOverride)) {
            newName = MyString.remainder(actionString,
                    ActionPrefix.renameOverride);
            target.getOverride().rename(newName);

        } else if (actionString.startsWith(ActionPrefix.renameSpatial)) {
            newName = MyString.remainder(actionString,
                    ActionPrefix.renameSpatial);
            target.renameSpatial(newName);

        } else if (actionString.startsWith(ActionPrefix.renameUserKey)) {
            newName = MyString.remainder(actionString,
                    ActionPrefix.renameUserKey);
            target.getUserData().renameKey(newName);

        } else {
            handled = false;
        }

        return handled;
    }
}
