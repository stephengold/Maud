/*
 Copyright (c) 2018, Stephen Gold
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
import maud.model.cgm.EditableCgm;

/**
 * Process actions that start with the word "delete".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class DeleteAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DeleteAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private DeleteAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "delete".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        EditableCgm target = Maud.getModel().getTarget();
        switch (actionString) {
            case Action.deleteAnimation:
                target.getAnimation().delete();
                break;

            case Action.deleteBuffer:
                target.deleteBuffer();
                break;

            case Action.deleteLight:
                target.getLight().delete();
                break;

            case Action.deleteMapping:
                Maud.getModel().getMap().deleteBoneMapping();
                break;

            case Action.deleteMatParam:
                target.getMatParam().delete();
                break;

            case Action.deleteOverride:
                target.getOverride().delete();
                break;

            case Action.deleteSgc:
                target.getSgc().delete();
                break;

            case Action.deleteSingleKeyframe:
                target.getTrack().deleteSelectedKeyframe();
                break;

            case Action.deleteTrackRotations:
                target.getTrack().deleteRotations();
                break;

            case Action.deleteTrackScales:
                target.getTrack().deleteScales();
                break;

            case Action.deleteTrackTranslations:
                target.getTrack().deleteTranslations();
                break;

            case Action.deleteUserKey:
                target.getUserData().delete();
                break;

            default:
                handled = testForPrefixes(actionString);
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an ongoing action that starts with the word "delete" -- 2nd part:
     * test for prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean testForPrefixes(String actionString) {
        boolean handled = true;

        EditableCgm target = Maud.getModel().getTarget();
        if (actionString.startsWith(ActionPrefix.deleteAssetLocationSpec)) {
            String spec = MyString.remainder(actionString,
                    ActionPrefix.deleteAssetLocationSpec);
            Maud.getModel().getLocations().remove(spec);
            handled = true;

        } else if (actionString.startsWith(ActionPrefix.deleteNextKeyframes)) {
            String arg = MyString.remainder(actionString,
                    ActionPrefix.deleteNextKeyframes);
            int number = Integer.parseInt(arg);
            target.getTrack().deleteNextKeyframes(number);

        } else if (actionString.startsWith(
                ActionPrefix.deletePreviousKeyframes)) {
            String arg = MyString.remainder(actionString,
                    ActionPrefix.deletePreviousKeyframes);
            int number = Integer.parseInt(arg);
            target.getTrack().deletePreviousKeyframes(number);

        } else {
            handled = false;
        }

        return handled;
    }
}
