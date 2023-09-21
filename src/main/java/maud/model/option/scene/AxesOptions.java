/*
 Copyright (c) 2017-2021, Stephen Gold
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

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.Maud;
import maud.MaudUtil;
import maud.action.ActionPrefix;
import maud.model.EditState;

/**
 * Options for visible coordinate axes in scene views.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AxesOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AxesOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * effect produced by dragging an axis tip
     */
    private AxesDragEffect dragEffect = AxesDragEffect.Rotate;
    /**
     * which set of axes is visualized
     */
    private AxesSubject subject = AxesSubject.Bone;
    /**
     * flag to enable the depth test for visibility of the axes
     */
    private boolean depthTestFlag = false;
    /**
     * line width for the arrows (in pixels, &ge;1) or 0 for solid arrows
     */
    private float lineWidth = 0f;
    // *************************************************************************
    // new methods exposed

    /**
     * Generate a description of the options, for display in the status bar.
     *
     * @return textual description (not null, length&lt;50)
     */
    public String describe() {
        StringBuilder builder = new StringBuilder(50);
        if (subject == AxesSubject.None) {
            builder.append("axes disabled");
        } else {
            builder.append(dragEffect);
            builder.append(" ");
            builder.append(subject);
        }

        assert builder.length() < 50 : builder.length();
        return builder.toString();
    }

    /**
     * Read the depth-test flag.
     *
     * @return true to enable test, otherwise false
     */
    public boolean getDepthTestFlag() {
        return depthTestFlag;
    }

    /**
     * Read the drag effect.
     *
     * @return an enum value (not null)
     */
    public AxesDragEffect getDragEffect() {
        assert dragEffect != null;
        return dragEffect;
    }

    /**
     * Read the width of each line.
     *
     * @return width (in pixels, &ge;1) or 0 for solid arrows
     */
    public float getLineWidth() {
        assert lineWidth >= 0f : lineWidth;
        return lineWidth;
    }

    /**
     * Read the current visualization subject.
     *
     * @return an enum value (not null)
     */
    public AxesSubject getSubject() {
        assert subject != null;
        return subject;
    }

    /**
     * Alter the depth-test flag.
     *
     * @param newState true &rarr; enable depth test, false &rarr; no depth test
     */
    public void setDepthTestFlag(boolean newState) {
        if (depthTestFlag != newState) {
            depthTestFlag = newState;
            EditState.optionSetEdited("axes depth test=" + newState);
        }
    }

    /**
     * Alter the drag effect.
     *
     * @param newEffect an enum value (not null)
     */
    public void setDragEffect(AxesDragEffect newEffect) {
        Validate.nonNull(newEffect, "new effect");

        if (dragEffect != newEffect) {
            dragEffect = newEffect;
            EditState.optionSetEdited("axes drag effect=" + newEffect);
        }
    }

    /**
     * Alter the line width.
     *
     * @param width line width for axes (in pixels, &ge;1) or 0 for solid arrows
     */
    public void setLineWidth(float width) {
        Validate.nonNegative(width, "width");

        if (lineWidth != width) {
            lineWidth = width;
            EditState editState = Maud.getModel().getOptionsEditState();
            editState.setEditedAxesLineWidth();
        }
    }

    /**
     * Alter the visualization subject.
     *
     * @param newSubject an enum value (not null)
     */
    public void setSubject(AxesSubject newSubject) {
        Validate.nonNull(newSubject, "new subject");

        if (subject != newSubject) {
            subject = newSubject;
            EditState.optionSetEdited("axes subject=" + newSubject);
        }
    }

    /**
     * Write the options to a script using the specified writer.
     *
     * @param writer (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    void writeToScript(Writer writer) throws IOException {
        assert writer != null;

        String action = ActionPrefix.selectAxesDragEffect
                + dragEffect.toString();
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.selectAxesSubject + subject.toString();
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.sfAxesDepthTest + Boolean.toString(depthTestFlag);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setAxesLineWidth + Float.toString(lineWidth);
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
    public AxesOptions clone() throws CloneNotSupportedException {
        AxesOptions clone = (AxesOptions) super.clone();
        return clone;
    }
}
