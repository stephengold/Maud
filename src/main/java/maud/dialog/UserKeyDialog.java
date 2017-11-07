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
package maud.dialog;

import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.dialog.DialogController;
import maud.Maud;

/**
 * Controller for a text-entry dialog box used to name a new user key.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class UserKeyDialog implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(UserKeyDialog.class.getName());
    // *************************************************************************
    // fields

    /**
     * description of the commit action (not null, not empty, should fit the
     * button -- about 8 or 9 characters)
     */
    final private String commitDescription;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller with the specified commit description.
     *
     * @param description (not null, not empty)
     */
    UserKeyDialog(String description) {
        assert description != null;
        assert !description.isEmpty();

        commitDescription = description;
    }
    // *************************************************************************
    // DialogController methods

    /**
     * Test whether "commit" actions are allowed.
     *
     * @param dialogElement (not null)
     * @return true if allowed, otherwise false
     */
    @Override
    public boolean allowCommit(Element dialogElement) {
        String proposedKey = getKey(dialogElement);
        if (isReserved(proposedKey) || existsInSpatial(proposedKey)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Callback to update the dialog box prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param dialogElement (not null)
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(Element dialogElement, float elapsedTime) {
        String commitLabel, feedbackMessage;
        String proposedKey = getKey(dialogElement);
        if (isReserved(proposedKey)) {
            commitLabel = "";
            feedbackMessage = String.format("%s is a reserved key",
                    MyString.quote(proposedKey));
        } else if (existsInSpatial(proposedKey)) {
            commitLabel = "";
            feedbackMessage = String.format("%s is already in use",
                    MyString.quote(proposedKey));
        } else {
            commitLabel = commitDescription;
            feedbackMessage = "";
        }

        Button commitButton;
        commitButton = dialogElement.findNiftyControl("#commit", Button.class);
        commitButton.setText(commitLabel);

        Element feedbackElement = dialogElement.findElementById("#feedback");
        TextRenderer renderer = feedbackElement.getRenderer(TextRenderer.class);
        renderer.setText(feedbackMessage);
    }
    // *************************************************************************
    // private methods

    /**
     * Test whether the specified key exists in the selected spatial.
     *
     * @param key the input key (not null)
     * @return true if used, otherwise false
     */
    private static boolean existsInSpatial(String key) {
        assert key != null;

        if (Maud.getModel().getTarget().getSpatial().hasUserKey(key)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Read the text field.
     *
     * @param dialogElement (not null)
     * @return a text string (not null)
     */
    private String getKey(Element dialogElement) {
        assert dialogElement != null;

        TextField textField = dialogElement.findNiftyControl("#textfield",
                TextField.class);
        String text = textField.getRealText();

        assert text != null;
        return text;
    }

    /**
     * Test whether the specified user key is reserved.
     *
     * @param key the input key (not null)
     * @return true if reserved, otherwise false
     */
    private static boolean isReserved(String key) {
        boolean result;
        if (key.isEmpty()) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }
}
