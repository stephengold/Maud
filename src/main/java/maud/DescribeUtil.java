/*
 Copyright (c) 2018-2022, Stephen Gold
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

import com.jme3.animation.Bone;
import com.jme3.asset.TextureKey;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureCubeMap;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Utility methods to describe values of various sorts. All methods should be
 * static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class DescribeUtil {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DescribeUtil.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private DescribeUtil() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Format an index value for the current index base.
     *
     * @param index zero-base index value (&ge;0)
     * @return a formatted text string (not null, not empty)
     */
    public static String index(int index) {
        Validate.nonNegative(index, "index");

        String result;
        int indexBase = Maud.getModel().getMisc().indexBase();
        if (indexBase == 0) {
            result = String.format("[%d]", index);
        } else if (indexBase == 1) {
            result = String.format("#%d", index + 1);
        } else {
            throw new IllegalStateException("indexBase = " + indexBase);
        }

        assert result != null;
        assert !result.isEmpty();
        return result;
    }

    /**
     * Format an index value for the current index base and number of values.
     *
     * @param i zero-base index value (&ge;0)
     * @param numValues number of legal values (&ge;1)
     * @return a formatted text string (not null, not empty)
     */
    public static String index(int i, int numValues) {
        Validate.nonNegative(i, "index");
        Validate.positive(numValues, "number of values");

        String indexDesc = index(i);
        String result = String.format("%s of %d", indexDesc, numValues);

        assert result != null;
        assert !result.isEmpty();
        return result;
    }

    /**
     * Describe a texture key.
     *
     * @param textureKey (not null, unaffected)
     * @return a textual description (not null, not empty)
     */
    public static String key(TextureKey textureKey) {
        String result = textureKey.toString();

        int anisotropy = textureKey.getAnisotropy();
        if (anisotropy != 0) {
            result += String.format(" (Anisotropy%d)", anisotropy);
        }

        assert result != null;
        assert !result.isEmpty();
        return result;
    }

    /**
     * Describe a material-parameter value.
     *
     * @param value (may be null, unaffected)
     * @return a textual description (not null, not empty)
     */
    public static String matParam(Object value) {
        String description;

        if (value == null || value instanceof String) {
            String string = (String) value;
            description = MyString.quote(string);

        } else if (value instanceof Bone) {
            Bone bone = (Bone) value;
            description = bone.getName();

        } else if (value instanceof Matrix3f) {
            Matrix3f m = (Matrix3f) value;
            StringBuilder builder = new StringBuilder(100);
            Vector3f row = new Vector3f();
            for (int i = 0; i < 3; i++) {
                m.getRow(i, row);
                builder.append(row);
            }
            description = builder.toString();

        } else if (value instanceof Matrix3f[]) {
            Matrix3f[] ma = (Matrix3f[]) value;
            description = String.format("length=%d", ma.length);

        } else if (value instanceof Matrix4f) {
            Matrix4f m = (Matrix4f) value;
            description = String.format("(%f %f %f %f)(%f %f %f %f)"
                    + "(%f %f %f %f)(%f %f %f %f)",
                    m.m00, m.m01, m.m02, m.m03,
                    m.m10, m.m11, m.m12, m.m13,
                    m.m20, m.m21, m.m22, m.m23,
                    m.m30, m.m31, m.m32, m.m33);

        } else if (value instanceof Matrix4f[]) {
            Matrix4f[] ma = (Matrix4f[]) value;
            description = String.format("length=%d", ma.length);

        } else if (value instanceof Texture) {
            Texture texture = (Texture) value;
            description = texture(texture);

        } else {
            description = value.toString();
        }

        return description;
    }

    /**
     * Describe a texture.
     *
     * @param texture the input texture (may be null, unaffected)
     * @return a textual description (not null, not empty)
     */
    public static String texture(Texture texture) {
        String result = "null";

        if (texture != null) {
            TextureKey textureKey = (TextureKey) texture.getKey();
            if (textureKey == null) {
                result = "(no key)";
            } else {
                result = key(textureKey);
            }

            Texture.MagFilter mag = texture.getMagFilter();
            result += " mag:" + mag.toString();
            Texture.MinFilter min = texture.getMinFilter();
            result += " min:" + min.toString();
            if (texture instanceof Texture3D
                    || texture instanceof TextureCubeMap) {
                Texture.WrapMode rWrap = texture.getWrap(Texture.WrapAxis.R);
                result += " r:" + rWrap.toString();
            }
            Texture.WrapMode sWrap = texture.getWrap(Texture.WrapAxis.S);
            result += " s:" + sWrap.toString();
            Texture.WrapMode tWrap = texture.getWrap(Texture.WrapAxis.T);
            result += " t:" + tWrap.toString();
        }

        return result;
    }

    /**
     * Describe a texture type, using the same names as
     * {@link com.jme3.asset.TextureKey#toString()}.
     *
     * @param type (not null)
     * @return a textual description (not null, not empty)
     */
    public static String type(Texture.Type type) {
        String description;
        switch (type) {
            case TwoDimensional:
                description = "2D";
                break;
            case ThreeDimensional:
                description = "3D";
                break;
            case TwoDimensionalArray:
                description = "Array";
                break;
            case CubeMap:
                description = "Cube";
                break;
            default:
                throw new IllegalArgumentException("type = " + type);
        }

        return description;
    }
}
