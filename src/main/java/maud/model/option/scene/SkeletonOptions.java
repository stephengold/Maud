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
package maud.model.option.scene;

import com.jme3.math.ColorRGBA;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.model.ShowBones;

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
     * color for skeleton links
     */
    private ColorRGBA linkColor = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * color for heads of bones with tracks
     */
    private ColorRGBA trackedColor = new ColorRGBA(0f, 1f, 0f, 1f);
    /**
     * color for heads of bones without tracks
     */
    private ColorRGBA tracklessColor = new ColorRGBA(1f, 0f, 0f, 1f);
    /**
     * line width for skeleton links (in pixels, &ge;0, 0&rarr;hidden)
     */
    private float lineWidth = 1f;
    /**
     * point size for bone heads (in pixels, &ge;0, 0&rarr;hidden)
     */
    private float pointSize = 5f;
    /**
     * which kinds of bones to visualize (not null)
     */
    private ShowBones showBones = ShowBones.Influencers;
    // *************************************************************************
    // new methods exposed

    /**
     * Which kinds of bones to visualize. TODO sort methods
     *
     * @return enum (not null)
     */
    public ShowBones getShowBones() {
        assert showBones != null;
        return showBones;
    }

    /**
     * Copy the color for the skeleton links.
     *
     * @param storeResult (modified if not null)
     * @return color (either storeResult or a new instance)
     */
    public ColorRGBA copyLinkColor(ColorRGBA storeResult) {
        if (storeResult == null) {
            storeResult = new ColorRGBA();
        }
        storeResult.set(linkColor);

        return storeResult;
    }

    /**
     * Copy the color for the heads of bones with tracks.
     *
     * @param storeResult (modified if not null)
     * @return color (either storeResult or a new instance)
     */
    public ColorRGBA copyTrackedColor(ColorRGBA storeResult) {
        if (storeResult == null) {
            storeResult = new ColorRGBA();
        }
        storeResult.set(trackedColor);

        return storeResult;
    }

    /**
     * Copy the color for the heads of bones without tracks.
     *
     * @param storeResult (modified if not null)
     * @return color (either storeResult or a new instance)
     */
    public ColorRGBA copyTracklessColor(ColorRGBA storeResult) {
        if (storeResult == null) {
            storeResult = new ColorRGBA();
        }
        storeResult.set(tracklessColor);

        return storeResult;
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
     * Alter which bones are visualized. TODO sort methods
     *
     * @param newSetting enum (not null)
     */
    public void setShowBones(ShowBones newSetting) {
        Validate.nonNull(newSetting, "new setting");
        showBones = newSetting;
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
     * Alter the color of the skeleton links.
     *
     * @param newColor (not null, unaffected)
     */
    public void setLinkColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "color");
        linkColor.set(newColor);
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
     * Alter the color of the heads of bones with tracks.
     *
     * @param newColor (not null, unaffected)
     */
    public void setTrackedColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "color");
        trackedColor.set(newColor);
    }

    /**
     * Alter the color of the heads of bones without tracks.
     *
     * @param newColor (not null, unaffected)
     */
    public void setTracklessColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "color");
        tracklessColor.set(newColor);
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
        clone.linkColor = linkColor.clone();
        clone.trackedColor = trackedColor.clone();
        clone.tracklessColor = tracklessColor.clone();

        return clone;
    }
}
