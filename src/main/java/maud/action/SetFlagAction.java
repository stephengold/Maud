/*
 Copyright (c) 2018-2019, Stephen Gold
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
import jme3utilities.minie.DumpFlags;
import jme3utilities.minie.PhysicsDumper;
import maud.Maud;
import maud.model.EditorModel;
import maud.model.option.scene.SceneOptions;

/**
 * Process actions that start with the word "setFlag".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SetFlagAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SetFlagAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SetFlagAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "setFlag".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        EditorModel model = Maud.getModel();
        PhysicsDumper dumper = model.getDumper();
        SceneOptions scene = model.getScene();

        String[] words = actionString.split(" ");
        assert words.length == 3 : words.length;
        assert words[0].equals("setFlag") : words[0];
        String valueString = words[2];
        boolean newValue = Boolean.parseBoolean(valueString);

        boolean handled = true;
        String prefix = String.format("%s %s ", words[0], words[1]);
        switch (prefix) {
            case ActionPrefix.sf3DCursorVisible:
                scene.getCursor().setVisible(newValue);
                break;

            case ActionPrefix.sfAxesDepthTest:
                scene.getAxes().setDepthTestFlag(newValue);
                break;

            case ActionPrefix.sfBoundsDepthTest:
                scene.getBounds().setDepthTestFlag(newValue);
                break;

            case ActionPrefix.sfDiagnose:
                model.getMisc().setDiagnoseLoads(newValue);
                break;

            case ActionPrefix.sfDumpBuckets:
                dumper.setDumpBucket(newValue);
                break;

            case ActionPrefix.sfDumpCullHints:
                dumper.setDumpCull(newValue);
                break;

            case ActionPrefix.sfDumpJib:
                dumper.setEnabled(DumpFlags.JointsInBodies, newValue);
                break;

            case ActionPrefix.sfDumpJis:
                dumper.setEnabled(DumpFlags.JointsInSpaces, newValue);
                break;

            case ActionPrefix.sfDumpMatParams:
                dumper.setDumpMatParam(newValue);
                break;

            case ActionPrefix.sfDumpMpo:
                dumper.setDumpOverride(newValue);
                break;

            case ActionPrefix.sfDumpShadows:
                dumper.setDumpShadow(newValue);
                break;

            case ActionPrefix.sfDumpTforms:
                dumper.setDumpTransform(newValue);
                break;

            case ActionPrefix.sfDumpUserData:
                dumper.setDumpUser(newValue);
                break;

            case ActionPrefix.sfLoadZUp:
                model.getMisc().setLoadZup(newValue);
                break;

            case ActionPrefix.sfMenuBarVisible:
                model.getMisc().setMenuBarVisible(newValue);
                break;

            case ActionPrefix.sfPhysicsRendered:
                scene.getRender().setPhysicsRendered(newValue);
                break;

            case ActionPrefix.sfShadowsRendered:
                scene.getRender().setShadowsRendered(newValue);
                break;

            case ActionPrefix.sfShowRotations:
                model.getScore().setShowRotations(newValue);
                break;

            case ActionPrefix.sfShowScales:
                model.getScore().setShowScales(newValue);
                break;

            case ActionPrefix.sfShowTranslations:
                model.getScore().setShowTranslations(newValue);
                break;

            case ActionPrefix.sfSkySimulated:
                scene.getRender().setSkySimulated(newValue);
                break;

            default:
                handled = false;
        }

        return handled;
    }
}
