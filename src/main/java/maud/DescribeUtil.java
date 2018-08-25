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
package maud;

import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Utility methods to describe values of various sorts. All methods should be
 * static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DescribeUtil {
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
     * Describe a pair of display dimensions.
     *
     * @param width width in pixels (&gt;0)
     * @param height height in pixels (&gt;0)
     * @return a textual description (not null, not empty)
     */
    public static String displayDimensions(int width, int height) {
        Validate.positive(width, "width");
        Validate.positive(height, "height");

        String description = String.format("%d x %d", width, height);
        return description;
    }

    /**
     * Format an index value for the current index base.
     *
     * @param index zero-base index value (&ge;0)
     * @return a formatted text string (not null, not empty)
     */
    public static String index(int index) {
        Validate.nonNegative(index, "index");

        String result;
        int indexBase = Maud.getModel().getMisc().getIndexBase();
        if (indexBase == 0) {
            result = String.format("[%d]", index);
        } else if (indexBase == 1) {
            result = String.format("#%d", index + 1);
        } else {
            throw new IllegalStateException();
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
     * Describe an MSAA sampling factor.
     *
     * @param factor samples per pixel (&ge;0, &le;16)
     * @return a textual description (not null, not empty)
     */
    public static String msaaFactor(int factor) {
        String description;
        if (factor <= 1) {
            description = "disabled";
        } else {
            description = String.format("%dx", factor);
        }

        return description;
    }
}
