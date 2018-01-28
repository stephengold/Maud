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
import maud.Maud;
import maud.model.EditorModel;
import maud.model.History;
import maud.model.cgm.EditableCgm;

/**
 * Process actions that start with the word "next".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class NextAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(NextAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private NextAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "next".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        switch (actionString) {
            case Action.nextAnimation:
                target.getAnimation().loadNext();
                break;

            case Action.nextAnimControl:
                target.getAnimControl().selectNext();
                break;

            case Action.nextBone:
                target.getBone().selectNext();
                break;

            case Action.nextCheckpoint:
                History.redo();
                break;

            case Action.nextJoint:
                target.getJoint().selectNext();
                break;

            case Action.nextLight:
                target.getLight().selectNext();
                break;

            case Action.nextMapping:
                model.getMap().selectNext();
                break;

            case Action.nextMatParam:
                target.getMatParam().selectNextName();
                break;

            case Action.nextOverride:
                target.getOverride().selectNextName();
                break;

            case Action.nextPerformanceMode:
                model.getMisc().selectNextPerformanceMode();
                break;

            case Action.nextPhysics:
                target.getObject().selectNext();
                break;

            case Action.nextSgc:
                target.getSgc().selectNext();
                break;

            case Action.nextShape:
                target.getShape().selectNext();
                break;

            case Action.nextSourceAnimation:
                model.getSource().getAnimation().loadNext();
                break;

            case Action.nextSourceAnimControl:
                model.getSource().getAnimControl().selectNext();
                break;

            case Action.nextUserData:
                target.getUserData().selectNextKey();
                break;

            case Action.nextVertex:
                target.getVertex().selectNext();
                break;

            case Action.nextViewMode:
                model.getMisc().selectNextViewMode();
                break;

            default:
                handled = false;
        }

        return handled;
    }
}
