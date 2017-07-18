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
import jme3utilities.Validate;
import jme3utilities.nifty.DialogController;

/**
 * Controller for a text-entry dialog box used to input a long integer value.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LongDialog implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            LongDialog.class.getName());
    // *************************************************************************
    // fields

    /**
     * maximum value to commit
     */
    final private long maxValue;
    /**
     * minimum value to commit
     */
    final private long minValue;
    /**
     * description of the commit action (not null, not empty, should fit the
     * button -- about 8 or 9 characters)
     */
    final private String commitDescription;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller.
     *
     * @param description commit description (not null, not empty)
     * @param min minimum value (&lt;max)
     * @param max minimum value (&gt;min)
     */
    public LongDialog(String description, long min, long max) {
        Validate.nonEmpty(description, "description");
        assert min < max : max;

        commitDescription = description;
        minValue = min;
        maxValue = max;
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
        Validate.nonNull(dialogElement, "dialog element");

        String text = getText(dialogElement);
        boolean result;
        try {
            long inputValue = Long.parseLong(text);
            if (inputValue < minValue || inputValue > maxValue) {
                result = false;
            } else {
                result = true;
            }
        } catch (NumberFormatException e) {
            result = false;
        }

        return result;
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
        Validate.nonNull(dialogElement, "dialog element");

        String commitLabel = "";
        String feedbackMessage = "";

        String text = getText(dialogElement);
        try {
            long inputValue = Long.parseLong(text);
            if (inputValue < minValue) {
                feedbackMessage = String.format("must not be <%ld", minValue);
            } else if (inputValue > maxValue) {
                feedbackMessage = String.format("must not be >%ld", maxValue);
            } else {
                commitLabel = commitDescription;
            }
        } catch (NumberFormatException e) {
            feedbackMessage = "must be a number";
        }

        Button commitButton = dialogElement.findNiftyControl("#commit",
                Button.class);
        commitButton.setText(commitLabel);

        Element feedbackElement = dialogElement.findElementById("#feedback");
        TextRenderer renderer = feedbackElement.getRenderer(TextRenderer.class);
        renderer.setText(feedbackMessage);
    }
    // *************************************************************************
    // private methods

    /**
     * Read the text field.
     *
     * @param dialogElement (not null)
     * @return a text string (not null)
     */
    private String getText(Element dialogElement) {
        TextField textField = dialogElement.findNiftyControl("#textfield",
                TextField.class);
        String text = textField.getRealText();

        assert text != null;
        return text;
    }
}
