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

import java.util.logging.Logger;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.History;
import maud.model.cgm.EditableCgm;

/**
 * Process actions that start with the word "previous".
 *
 * @author Stephen Gold sgold@sonic.net
 */
final class PreviousAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PreviousAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private PreviousAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "previous".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        switch (actionString) {
            case Action.previousAnimation:
                target.getAnimation().loadPrevious();
                break;

            case Action.previousAnimControl:
                target.getAnimControl().selectPrevious();
                break;

            case Action.previousBone:
                target.getBone().selectPrevious();
                break;

            case Action.previousBuffer:
                target.getBuffer().selectPrevious();
                break;

            case Action.previousCheckpoint:
                History.undo();
                break;

            case Action.previousGeometry:
                target.getSpatial().selectPreviousGeometry();
                break;

            case Action.previousJoint:
                target.getJoint().selectPrevious();
                break;

            case Action.previousLight:
                target.getLight().selectPrevious();
                break;

            case Action.previousLink:
                target.getLink().selectPrevious();
                break;

            case Action.previousMapping:
                model.getMap().selectPrevious();
                break;

            case Action.previousMatParam:
                target.getMatParam().selectPreviousName();
                break;

            case Action.previousOverride:
                target.getOverride().selectPreviousName();
                break;

            case Action.previousPco:
                target.getPco().selectPrevious();
                break;

            case Action.previousSgc:
                target.getSgc().selectPrevious();
                break;

            case Action.previousShape:
                target.getShape().selectPrevious();
                break;

            case Action.previousSourceAnimation:
                model.getSource().getAnimation().loadPrevious();
                break;

            case Action.previousSourceAnimControl:
                model.getSource().getAnimControl().selectPrevious();
                break;

            case Action.previousTexture:
                target.getTexture().selectPrevious();
                break;

            case Action.previousTrack:
                target.getTrack().selectPrevious();
                break;

            case Action.previousUserData:
                target.getUserData().selectPreviousKey();
                break;

            case Action.previousVertex:
                target.getVertex().selectPrevious();
                break;

            default:
                handled = false;
        }

        return handled;
    }
}
