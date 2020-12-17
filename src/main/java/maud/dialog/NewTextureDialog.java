/*
 Copyright (c) 2020, Stephen Gold
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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.dialog.DialogController;
import jme3utilities.ui.Locators;
import maud.Maud;

/**
 * Controller for a text-and-check dialog box used to select a path for a new
 * texture asset.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class NewTextureDialog implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(NewTextureDialog.class.getName());
    // *************************************************************************
    // fields

    /**
     * list of accepted extensions (not null, no elements null)
     */
    final private List<String> extensions = new ArrayList<>(9);
    /**
     * cache information about which paths exist (avoid HTTP response code 429)
     */
    final private Map<String, Boolean> pathCache = new TreeMap<>();
    /**
     * description of the commit action (not null, not empty, should fit the
     * button -- about 8 or 9 characters)
     */
    final private String buttonText;
    /**
     * text to disply beside the checkbox (not null, not empty)
     */
    final private String checkboxLabel;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller with the specified labels and list of
     * extensions.
     *
     * @param buttonText commit-button text (not null, not empty)
     * @param checkboxLabel checkbox-label text (not null, not empty)
     * @param extList list of accepted extensions (not null, unaffected)
     */
    NewTextureDialog(String buttonText, String checkboxLabel,
            Collection<String> extList) {
        Validate.nonEmpty(buttonText, "button text");
        Validate.nonEmpty(checkboxLabel, "checkbox label");

        this.buttonText = buttonText;
        this.checkboxLabel = "  " + checkboxLabel;
        extensions.addAll(extList);
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

        String text = text(dialogElement);
        String feedback = feedback(text);
        boolean result = feedback.isEmpty();

        return result;
    }

    /**
     * Construct the action-string suffix for a commit.
     *
     * @param dialogElement (not null)
     * @return the suffix (not null)
     */
    @Override
    public String commitSuffix(Element dialogElement) {
        Validate.nonNull(dialogElement, "dialog element");

        boolean isChecked = isChecked(dialogElement);
        String text = text(dialogElement);
        String result = Boolean.toString(isChecked) + " " + text;

        return result;
    }

    /**
     * Update this dialog box prior to rendering. (Invoked once per frame.)
     *
     * @param dialogElement (not null)
     * @param unused time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(Element dialogElement, float unused) {
        Validate.nonNull(dialogElement, "dialog element");

        String text = text(dialogElement);
        String feedback = feedback(text);
        Button commitButton
                = dialogElement.findNiftyControl("#commit", Button.class);

        Element element = dialogElement.findElementById("#feedback");
        TextRenderer renderer = element.getRenderer(TextRenderer.class);
        renderer.setText(feedback);

        element = dialogElement.findElementById("#checklabel");
        renderer = element.getRenderer(TextRenderer.class);
        renderer.setText(checkboxLabel);

        if (feedback.isEmpty()) {
            commitButton.setText(buttonText);
            commitButton.getElement().show();

        } else {
            commitButton.setText("");
            commitButton.getElement().hide();
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Test whether an asset exists at the specified path.
     *
     * @param assetPath (not null)
     * @return true if one exists, otherwise false
     */
    private boolean exists(String assetPath) {
        assert assetPath != null;

        if (pathCache.containsKey(assetPath)) {
            return pathCache.get(assetPath);
        }

        AssetKey key = new AssetKey(assetPath);
        AssetManager manager = Locators.getAssetManager();
        /*
         * Search in all known asset locations.
         */
        Locators.save();
        Locators.unregisterAll();
        Locators.registerDefault();
        List<String> specList = Maud.getModel().getLocations().listAll();
        Locators.register(specList);
        /*
         * Temporarily hush AssetManager warnings about missing resources.
         */
        Logger amLogger = Logger.getLogger(AssetManager.class.getName());
        Level savedLevel = amLogger.getLevel();
        amLogger.setLevel(Level.SEVERE);
        AssetInfo info = manager.locateAsset(key);
        amLogger.setLevel(savedLevel);

        Locators.restore();

        boolean result;
        if (info == null) {
            result = false;
        } else {
            result = true;
        }
        pathCache.put(assetPath, result);

        return result;
    }

    /**
     * Determine the feedback message for the specified input text.
     *
     * @param assetPath the input text (not null)
     * @return the message (not null)
     */
    private String feedback(String assetPath) {
        String msg = "";
        if (hasExtension(assetPath)) {
            if (exists(assetPath)) {
                msg = String.format("path already in use");
            }
        } else {
            String list = MyString.join("/", extensions);
            msg = String.format("needs extension: %s", MyString.quote(list));
        }

        return msg;
    }

    /**
     * Test whether the specified asset path has one of the required extensions.
     *
     * @param assetPath path to test (not null)
     * @return true if it end with an extension on the list, otherwise false
     */
    private boolean hasExtension(String assetPath) {
        assert assetPath != null;

        boolean match = false;
        for (String ext : extensions) {
            if (assetPath.endsWith(ext)) {
                match = true;
                break;
            }
        }

        return match;
    }

    /**
     * Read the checkbox status.
     *
     * @param dialogElement (not null)
     * @return a text string (not null)
     */
    private boolean isChecked(Element dialogElement) {
        CheckBox checkBox = dialogElement.findNiftyControl("#dialogcheck",
                CheckBox.class);
        boolean result = checkBox.isChecked();
        return result;
    }

    /**
     * Read the text field.
     *
     * @param dialogElement (not null)
     * @return a text string (not null)
     */
    private String text(Element dialogElement) {
        TextField textField
                = dialogElement.findNiftyControl("#textfield", TextField.class);
        String text = textField.getRealText();

        assert text != null;
        return text;
    }
}
