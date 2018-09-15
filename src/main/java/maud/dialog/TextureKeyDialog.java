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

import com.jme3.asset.TextureKey;
import com.jme3.texture.Texture;
import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.dialog.DialogController;
import maud.DescribeUtil;

/**
 * Controller for a dialog box used to edit a TextureKey.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TextureKeyDialog implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TextureKeyDialog.class.getName());
    /**
     * pattern to match a tag in parentheses at the end of a string
     */
    final private static Pattern tagPattern = Pattern.compile(" \\((\\w+)\\)$");
    // *************************************************************************
    // fields

    /**
     * if true, "null" is an allowed value, otherwise it is disallowed
     */
    final private boolean allowNull; // TODO need a way to specify null
    /**
     * asset path to be used in the result (not null)
     */
    private String assetPath = "";
    /**
     * type to be used in the result (not null)
     */
    private Texture.Type typeHint = Texture.Type.TwoDimensional;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller.
     *
     * @param textureKey the key to edit (may be null, unaffected)
     */
    public TextureKeyDialog(TextureKey textureKey, boolean allowNull) {
        this.allowNull = allowNull;
        if (textureKey != null) {
            String name = textureKey.getName();
            setAssetPath(name);

            Texture.Type hint = textureKey.getTextureTypeHint();
            setTypeHint(hint);
        }
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the type hint to be used in the result.
     *
     * @return the texture type (not null)
     */
    public Texture.Type getTypeHint() {
        assert typeHint != null;
        return typeHint;
    }

    /**
     * Parse the specified text to obtain a texture key.
     *
     * @param text the input text (not null, not empty)
     * @return a new texture key (not null)
     */
    public static TextureKey parseTextureKey(String text) {
        Validate.nonEmpty(text, "text");

        boolean flipY = false;
        boolean generateMips = false;
        int anisotropy = 0;
        Texture.Type typeHint = Texture.Type.TwoDimensional;
        String name;

        while (true) {
            Matcher matcher = tagPattern.matcher(text);
            if (matcher.find()) {
                int startPos = matcher.start();
                String tag = matcher.group(1);
                if (tag.startsWith("Anisotropy")) {
                    String numberText = MyString.remainder(tag, "Anisotropy");
                    anisotropy = Integer.parseInt(numberText);
                } else {
                    switch (tag) {
                        case "3D":
                            typeHint = Texture.Type.ThreeDimensional;
                            break;
                        case "Array":
                            typeHint = Texture.Type.TwoDimensionalArray;
                            break;
                        case "Cube":
                            typeHint = Texture.Type.CubeMap;
                            break;
                        case "Flipped":
                            flipY = true;
                            break;
                        case "Mipmapped":
                            generateMips = true;
                            break;
                        default:
                            throw new IllegalArgumentException(tag);
                    }
                }
                text = text.substring(0, startPos);
            } else {
                name = text;
                break;
            }
        }

        TextureKey result = new TextureKey(name, flipY);
        result.setAnisotropy(anisotropy);
        result.setGenerateMips(generateMips);
        result.setTextureTypeHint(typeHint);

        return result;
    }

    /**
     * Alter the asset path to be used in the result.
     *
     * @param assetPath the desired asset path (not null)
     */
    final public void setAssetPath(String assetPath) {
        Validate.nonNull(assetPath, "asset path");
        this.assetPath = assetPath;
    }

    /**
     * Alter the type hint to be used in the result.
     *
     * @param newType the desired texture type (not null)
     */
    final public void setTypeHint(Texture.Type newType) {
        Validate.nonNull(newType, "new type");
        typeHint = newType;
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
        // The anisotropy must be a non-negative decimal integer.
        TextField textField = dialogElement.findNiftyControl("#anisotropy",
                TextField.class);
        String input = textField.getRealText();
        boolean allow;
        try {
            int anisotropy = Integer.parseInt(input);
            allow = (anisotropy >= 0);
        } catch (NumberFormatException e) {
            allow = false;
        }

        if (allow) {
            // The asset path must not be empty.
            allow = !assetPath.isEmpty();
        }

        return allow;
    }

    /**
     * Construct the action-string suffix for a commit.
     *
     * @param dialogElement (not null)
     * @return the suffix (not null)
     */
    @Override
    public String commitSuffix(Element dialogElement) {
        CheckBox flipBox
                = dialogElement.findNiftyControl("#flip", CheckBox.class);
        boolean flipY = flipBox.isChecked();

        CheckBox mipMapBox
                = dialogElement.findNiftyControl("#mipmap", CheckBox.class);
        boolean generateMips = mipMapBox.isChecked();

        TextField anisotropyField = dialogElement.findNiftyControl(
                "#anisotropy", TextField.class);
        String anisotropyText = anisotropyField.getRealText();
        int anisotropy = Integer.parseInt(anisotropyText);

        TextureKey textureKey = new TextureKey(assetPath, flipY);
        textureKey.setAnisotropy(anisotropy);
        textureKey.setGenerateMips(generateMips);
        textureKey.setTextureTypeHint(typeHint);
        String result = textureKey.toString();

        return result;
    }

    /**
     * Update the dialog box prior to rendering. (Invoked once per frame.)
     *
     * @param dialogElement (not null)
     * @param ignored time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(Element dialogElement, float ignored) {
        Button pathButton
                = dialogElement.findNiftyControl("#path", Button.class);
        pathButton.setText(assetPath);

        Button typeButton
                = dialogElement.findNiftyControl("#type", Button.class);
        String typeString = DescribeUtil.type(typeHint);
        typeButton.setText(typeString);

        Button commitButton
                = dialogElement.findNiftyControl("#commit", Button.class);
        if (allowCommit(dialogElement)) {
            commitButton.getElement().show();
        } else {
            commitButton.getElement().hide();
        }
    }
}
