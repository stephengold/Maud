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
package maud.model.option.scene;

import com.jme3.math.ColorRGBA;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.MaudUtil;
import maud.action.ActionPrefix;
import maud.model.option.ShowBones;

/**
 * Options for skeleton visualizations in scene views.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SkeletonOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SkeletonOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * color for the heads of unmapped bones without tracks in the loaded
     * animation
     */
    private ColorRGBA defaultColor = new ColorRGBA(1f, 0f, 0f, 1f);
    /**
     * color for the links between child bones and their parents
     */
    private ColorRGBA linkColor = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * color for the heads of mapped bones in retargeted pose
     */
    private ColorRGBA mappedColor = new ColorRGBA(0f, 1f, 0f, 1f);
    /**
     * color for the heads of bones that have tracks in the loaded animation
     */
    private ColorRGBA trackedColor = new ColorRGBA(0f, 1f, 0f, 1f);
    /**
     * line width for skeleton links (in pixels, &ge;0, 0&rarr;hidden)
     */
    private float lineWidth = 1f;
    /**
     * point size for bone heads (in pixels, &ge;0, 0&rarr;hidden)
     */
    private float pointSize = 10f;
    /**
     * which kinds of bones to visualize (not null)
     */
    private ShowBones showBones = ShowBones.Influencers;
    /**
     * which color to view/edit in SkeletonTool (not null)
     */
    private SkeletonColors editColor = SkeletonColors.Links;
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the color for the specified use.
     *
     * @param use which color to copy (not null)
     * @param storeResult (modified if not null)
     * @return color (either storeResult or a new instance)
     */
    public ColorRGBA copyColor(SkeletonColors use, ColorRGBA storeResult) {
        Validate.nonNull(use, "use");
        if (storeResult == null) {
            storeResult = new ColorRGBA();
        }

        switch (use) {
            case IdleBones:
                storeResult.set(defaultColor);
                break;

            case Links:
                storeResult.set(linkColor);
                break;

            case MappedBones:
                storeResult.set(mappedColor);
                break;

            case TrackedBones:
                storeResult.set(trackedColor);
                break;

            default:
                throw new IllegalArgumentException();
        }

        return storeResult;
    }

    /**
     * Read which color to view/edit in SkeletonTool.
     *
     * @return an enum value (not null)
     */
    public SkeletonColors getEditColor() {
        assert editColor != null;
        return editColor;
    }

    /**
     * Read the width of the link lines.
     *
     * @return width (in pixels, &ge;0)
     */
    public float getLineWidth() {
        assert lineWidth >= 0f : lineWidth;
        return lineWidth;
    }

    /**
     * Read the size of the bone-head points.
     *
     * @return size (in pixels, &ge;0)
     */
    public float getPointSize() {
        assert pointSize >= 0f : pointSize;
        return pointSize;
    }

    /**
     * Read which kinds of bones to visualize.
     *
     * @return an enum value (not null)
     */
    public ShowBones getShowBones() {
        assert showBones != null;
        return showBones;
    }

    /**
     * Select which color to view/edit in SkeletonTool.
     *
     * @param newEditColor (not null)
     */
    public void selectEditColor(SkeletonColors newEditColor) {
        Validate.nonNull(newEditColor, "new edit color");
        editColor = newEditColor;
    }

    /**
     * Alter the color for the specified use.
     *
     * @param use which color to alter (not null)
     * @param newColor (not null, unaffected)
     */
    public void setColor(SkeletonColors use, ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");

        switch (use) {
            case IdleBones:
                defaultColor.set(newColor);
                break;

            case Links:
                linkColor.set(newColor);
                break;

            case MappedBones:
                mappedColor.set(newColor);
                break;

            case TrackedBones:
                trackedColor.set(newColor);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Alter the line width of skeleton links.
     *
     * @param width line width (in pixels, &ge;0, 0&rarr;hidden)
     */
    public void setLineWidth(float width) {
        Validate.inRange(width, "line width", 0f, Float.MAX_VALUE);
        lineWidth = width;
    }

    /**
     * Alter the point size of the bone heads.
     *
     * @param size point size (in pixels, &ge;0, 0&rarr;hidden)
     */
    public void setPointSize(float size) {
        Validate.inRange(size, "point size", 0f, Float.MAX_VALUE);
        pointSize = size;
    }

    /**
     * Alter which bones are visualized.
     *
     * @param newSetting an enum value (not null)
     */
    public void setShowBones(ShowBones newSetting) {
        Validate.nonNull(newSetting, "new setting");
        showBones = newSetting;
    }

    /**
     * Write the options to a script using the specified writer.
     *
     * @param writer (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    void writeToScript(Writer writer) throws IOException {
        Validate.nonNull(writer, "writer");

        ColorRGBA color = new ColorRGBA();
        String action;
        for (SkeletonColors use : SkeletonColors.values()) {
            copyColor(use, color);
            action = String.format("%s%s %s", ActionPrefix.setSkeletonColor,
                    use, color);
            MaudUtil.writePerformAction(writer, action);
        }

        action = ActionPrefix.setSkeletonLineWidth + Float.toString(lineWidth);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setSkeletonPointSize + Float.toString(pointSize);
        MaudUtil.writePerformAction(writer, action);

        // CGM load overrides showBones, so no point in writing it here
        action = ActionPrefix.selectSkeletonColor + editColor.toString();
        MaudUtil.writePerformAction(writer, action);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public SkeletonOptions clone() throws CloneNotSupportedException {
        SkeletonOptions clone = (SkeletonOptions) super.clone();
        clone.defaultColor = defaultColor.clone();
        clone.linkColor = linkColor.clone();
        clone.mappedColor = mappedColor.clone();
        clone.trackedColor = trackedColor.clone();

        return clone;
    }
}
