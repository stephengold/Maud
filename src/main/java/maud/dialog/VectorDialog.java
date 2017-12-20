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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import jme3utilities.nifty.dialog.DialogController;

/**
 * Controller for a text-entry dialog box used to input an boolean value.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class VectorDialog implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(VectorDialog.class.getName());
    /**
     * pattern for matching a vector element
     */
    final private static Pattern elementPattern
            = Pattern.compile("([^)(,\\s]+)");
    /**
     * pattern for matching the word null
     */
    final private static Pattern nullPattern
            = Pattern.compile("\\s*null\\s*");
    // *************************************************************************
    // fields

    /**
     * if true, "null" is an allowed value, otherwise it is disallowed
     */
    final private boolean allowNull;
    /**
     * number of elements in the vector (&ge;2, &le;4)
     */
    final private int numElements;
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
     * @param numElements number of elements in the vector (&ge;2, &le;4)
     * @param allowNull if true, "null" will be an allowed value
     */
    public VectorDialog(String description, int numElements,
            boolean allowNull) {
        Validate.nonEmpty(description, "description");
        Validate.inRange(numElements, "number of elements", 2, 4);

        commitDescription = description;
        this.numElements = numElements;
        this.allowNull = allowNull;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Parse the specified text to obtain a vector.
     *
     * @param text (not null, not empty)
     * @return a new vector (not null)
     */
    public static Object parseVector(String text) {
        Validate.nonEmpty(text, "text");

        String lcText = text.toLowerCase(Locale.ROOT);
        Matcher matcher = elementPattern.matcher(lcText);
        List<Float> elements = new ArrayList<>(4);
        while (matcher.find()) {
            String group = matcher.group(1);
            float element = Float.parseFloat(group);
            elements.add(element);
        }
        Object result;
        int numElements = elements.size();
        float x = elements.get(MyVector3f.xAxis);
        float y = elements.get(MyVector3f.yAxis);
        float z, w;
        switch (numElements) {
            case 2:
                result = new Vector2f(x, y);
                break;

            case 3:
                z = elements.get(MyVector3f.zAxis);
                result = new Vector3f(x, y, z);
                break;

            case 4:
                z = elements.get(MyVector3f.zAxis);
                w = elements.get(3);
                result = new Vector4f(x, y, z, w);
                break;

            default:
                throw new IllegalArgumentException();
        }

        return result;
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
        String lcText = text.toLowerCase(Locale.ROOT);
        boolean allowCommit = true;

        Matcher matcher = elementPattern.matcher(lcText);
        int elementCount = 0;
        while (allowCommit && matcher.find()) {
            String element = matcher.group(1);
            try {
                float inputValue = Float.parseFloat(element);
                if (Float.isNaN(inputValue)) {
                    allowCommit = false;
                }
                ++elementCount;
            } catch (NumberFormatException e) {
                allowCommit = false;
            }
        }
        if (elementCount != numElements) {
            allowCommit = false;
        }

        if (!allowCommit && allowNull && matchesNull(lcText)) {
            allowCommit = true;
        }

        return allowCommit;
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

        if (allowCommit(dialogElement)) {
            commitLabel = commitDescription;
        } else {
            feedbackMessage
                    = String.format("must be a %d-element vector", numElements);
            if (allowNull) {
                feedbackMessage += " or null";
            }
        }

        Button commitButton
                = dialogElement.findNiftyControl("#commit", Button.class);
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
        TextField textField
                = dialogElement.findNiftyControl("#textfield", TextField.class);
        String text = textField.getRealText();

        assert text != null;
        return text;
    }

    /**
     * Test whether the specified string matches nullPattern.
     *
     * @param lcText text string (converted to lower case)
     * @return true for match, otherwise false
     */
    private boolean matchesNull(String lcText) {
        Matcher matcher = nullPattern.matcher(lcText);
        boolean result = matcher.matches();

        return result;
    }
}
