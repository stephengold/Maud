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
package maud.dialog;

import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jme3utilities.Validate;
import jme3utilities.nifty.dialog.TextEntryDialog;

/**
 * Controller for a text-entry dialog box used to input display dimensions.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DimensionsDialog extends TextEntryDialog {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DimensionsDialog.class.getName());
    /**
     * pattern for matching a display dimensions
     */
    final private static Pattern dimensionsPattern
            = Pattern.compile("^\\s*(\\d+)\\s*[x,]\\s*(\\d+)\\s*");
    // *************************************************************************
    // fields

    /**
     * maximum value for display height (&ge;minHeight)
     */
    final private int maxHeight;
    /**
     * maximum value for display width (&ge;minWidth)
     */
    final private int maxWidth;
    /**
     * minimum value for display height (&le;maxHeight)
     */
    final private int minHeight;
    /**
     * minimum value for display width (&le;maxWidth)
     */
    final private int minWidth;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller.
     *
     * @param description commit-button text (not null, not empty)
     * @param minWidth minimum value for display width (&le;maxWidth)
     * @param minHeight minimum value for display height (&le;maxHeight)
     * @param maxWidth maximum value for display width (&ge;minWidth)
     * @param maxHeight maximum value for display height (&ge;minHeight)
     */
    public DimensionsDialog(String description, int minWidth, int minHeight,
            int maxWidth, int maxHeight) {
        super(description);

        assert minWidth <= maxWidth;
        assert minHeight <= maxHeight;

        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Parse the specified text to obtain dimensions.
     *
     * @param text the input text (not null, not empty)
     * @return a new array containing the width and height, or null for a syntax
     * error
     */
    public static int[] parseDimensions(String text) {
        Validate.nonEmpty(text, "text");

        String lcText = text.toLowerCase(Locale.ROOT);
        Matcher matcher = dimensionsPattern.matcher(lcText);
        int[] result = null;
        if (matcher.find()) {
            result = new int[2];
            String group1 = matcher.group(1);
            result[0] = Integer.parseInt(group1);
            String group2 = matcher.group(2);
            result[1] = Integer.parseInt(group2);
        }

        return result;
    }
    // *************************************************************************
    // TextEntryDialog methods

    /**
     * Determine the feedback message for the specified input text.
     *
     * @param text the input text (not null)
     * @return the message (not null)
     */
    @Override
    protected String feedback(String text) {
        String msg = "";
        Matcher matcher = dimensionsPattern.matcher(text);
        if (matcher.find()) {
            String group1 = matcher.group(1);
            int width = Integer.parseInt(group1);
            if (width < minWidth) {
                msg = String.format("width must not be < %d", minWidth);
            } else if (width > maxWidth) {
                msg = String.format("width must not be > %d", maxWidth);
            } else {
                String group2 = matcher.group(2);
                int height = Integer.parseInt(group2);
                if (height < minHeight) {
                    msg = String.format("height must not be < %d", minHeight);
                } else if (height > maxHeight) {
                    msg = String.format("height must not be > %d", maxHeight);
                }
            }
        } else {
            msg = "improper format for display dimensions";
        }

        return msg;
    }
}
