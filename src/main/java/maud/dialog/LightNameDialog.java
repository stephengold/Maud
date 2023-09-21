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
package maud.dialog;

import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.dialog.TextEntryDialog;
import maud.Maud;
import maud.model.cgm.SelectedLight;

/**
 * Controller for a text-entry dialog box used to name a new light.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class LightNameDialog extends TextEntryDialog {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(LightNameDialog.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller with the specified commit description.
     *
     * @param description commit-button text (not null, not empty)
     */
    LightNameDialog(String description) {
        super(description);
    }
    // *************************************************************************
    // TextEntryDialog methods

    /**
     * Determine the feedback message for the specified input text.
     *
     * @param proposedName the input text (not null)
     * @return the message (not null)
     */
    @Override
    protected String feedback(String proposedName) {
        String msg = "";
        if (isReserved(proposedName)) {
            msg = String.format("%s is a reserved name",
                    MyString.quote(proposedName));
        } else if (isUsed(proposedName)) {
            msg = String.format("%s is already in use",
                    MyString.quote(proposedName));
        }

        return msg;
    }
    // *************************************************************************
    // private methods

    /**
     * Test whether the specified light name is reserved.
     *
     * @param name which name to test (not null)
     * @return true if reserved, otherwise false
     */
    private static boolean isReserved(String name) {
        boolean result = name.equals(SelectedLight.noLight);
        return result;
    }

    /**
     * Test whether the specified light name is used in the target C-G model.
     *
     * @param name which name to test (not null)
     * @return true if used, otherwise false
     */
    private static boolean isUsed(String name) {
        assert name != null;
        boolean result = Maud.getModel().getTarget().hasLight(name);
        return result;
    }
}
