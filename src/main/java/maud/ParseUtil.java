/*
 Copyright (c) 2017-2019, Stephen Gold
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
package maud;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.MatParam;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector4f;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.dialog.VectorDialog;
import jme3utilities.ui.Locators;

/**
 * Parsing methods for the Maud application. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ParseUtil {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ParseUtil.class.getName());
    /**
     * pattern for matching the word "null"
     */
    final private static Pattern nullPattern = Pattern.compile("\\s*null\\s*");
    /**
     * pattern to match a tag in parentheses at the end of a string
     */
    final private static Pattern tagPattern = Pattern.compile(" \\((\\w+)\\)$");
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private ParseUtil() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Parse a material parameter from the specified text string.
     *
     * @param oldParameter old parameter (not null, unaffected)
     * @param textString input text (not null, not empty)
     * @return a new object or null
     */
    public static Object parseMatParam(MatParam oldParameter,
            String textString) {
        Validate.nonNull(oldParameter, "old parameter");
        Validate.nonEmpty(textString, "text string");

        String lcText = textString.toLowerCase(Locale.ROOT);
        Matcher matcher = nullPattern.matcher(lcText);
        Object result = null;
        if (!matcher.matches()) {
            VarType varType = oldParameter.getVarType();
            switch (varType) {
                case Boolean:
                    result = Boolean.parseBoolean(lcText);
                    break;

                case Float:
                    result = Float.parseFloat(lcText);
                    break;

                case Int:
                    result = Integer.parseInt(lcText);
                    break;

                case Texture2D:
                case Texture3D:
                case TextureArray:
                case TextureCubeMap:
                    TextureKey key = parseTextureKey(textString);
                    AssetManager assetManager = Locators.getAssetManager();
                    try {
                        result = assetManager.loadTexture(key);
                    } catch (RuntimeException exception) {
                        exception.printStackTrace();
                    }
                    break;

                case Vector2:
                case Vector3:
                    result = VectorDialog.parseVector(lcText);
                    break;

                case Vector4:
                    result = VectorDialog.parseVector(lcText);
                    Vector4f v = (Vector4f) result;
                    Object oldValue = oldParameter.getValue();
                    if (oldValue instanceof Quaternion) {
                        result = new Quaternion(v.x, v.y, v.z, v.w);
                    } else if (!(oldValue instanceof Vector4f)) {
                        /*
                         * best guess for oldValue == null
                         * If we guess wrong, there's a delayed cast exception.
                         */
                        result = new ColorRGBA(v.x, v.y, v.z, v.w);
                    }
                    break;

                default:
                    /* TODO handle FloatArray, IntArray, Matrix3, Matrix3Array,
                     * Matrix4, Matrix4Array, TextureBuffer,
                     * Vector2Array, Vector3Array, Vector4Array */
                    throw new IllegalArgumentException();
            }
        }

        return result;
    }

    /**
     * Parse the specified text to obtain a TextureKey.
     *
     * @param text the input text (not null, not empty)
     * @return a new TextureKey (not null)
     * @see maud.DescribeUtil#key(com.jme3.asset.TextureKey)
     */
    public static TextureKey parseTextureKey(String text) {
        Validate.nonEmpty(text, "text");

        boolean flipY = false;
        boolean generateMips = false;
        int anisotropy = 0;
        Texture.Type typeHint = Texture.Type.TwoDimensional;
        String remainingText = text;

        while (true) {
            Matcher matcher = tagPattern.matcher(remainingText);
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
                remainingText = remainingText.substring(0, startPos);
            } else {
                break;
            }
        }

        TextureKey result = new TextureKey(remainingText, flipY);
        result.setAnisotropy(anisotropy);
        result.setGenerateMips(generateMips);
        result.setTextureTypeHint(typeHint);

        return result;
    }
}
