/*
 Copyright (c) 2021, Stephen Gold
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

import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.dialog.DialogController;
import maud.model.cgm.GeometryItem;

/**
 * Controller for a multi-select dialog box used to select geometries for
 * merging.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class MergeDialog implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MergeDialog.class.getName());
    // *************************************************************************
    // fields

    /**
     * geometry items to select from
     */
    final private List<GeometryItem> allItems;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller with the specified list of items.
     *
     * @param itemList (not null, not empty)
     */
    MergeDialog(List<GeometryItem> itemList) {
        assert itemList != null;
        assert !itemList.isEmpty();

        allItems = itemList;
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

        boolean result;
        List<Integer> indices = getSelectedIndices(dialogElement);
        if (indices.size() < 2) {
            result = false;
        } else {
            String feedbackMessage = mergeFeedback(indices);
            result = feedbackMessage.isEmpty();
        }

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
        ListBox listBox = dialogElement.findNiftyControl("#box", ListBox.class);
        List indices = listBox.getSelectedIndices();
        String suffix = MyString.join(",", indices);

        return suffix;
    }

    /**
     * Update this dialog box prior to rendering. (Invoked once per frame.)
     *
     * @param dialogElement (not null)
     * @param ignored time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(Element dialogElement, float ignored) {
        String commitLabel, feedbackMessage;

        List<Integer> indices = getSelectedIndices(dialogElement);
        if (indices.isEmpty()) {
            commitLabel = "";
            feedbackMessage = "no geometries selected";
        } else if (indices.size() < 2) {
            commitLabel = "";
            feedbackMessage = "not enough geometries selected";
        } else {
            feedbackMessage = mergeFeedback(indices);
            if (feedbackMessage.isEmpty()) {
                commitLabel = "Merge";
            } else {
                commitLabel = "";
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
     * Read the selected indices.
     *
     * @param dialogElement (not null)
     * @return a new list of indices
     */
    private static List<Integer> getSelectedIndices(Element dialogElement) {
        assert dialogElement != null;

        ListBox listBox = dialogElement.findNiftyControl("#box", ListBox.class);
        @SuppressWarnings("unchecked")
        List<Integer> result = listBox.getSelectedIndices();

        return result;
    }

    /**
     * Determine whether all the selected geometries are bare (no S-G controls,
     * no user data, no local lights, and no M-P overrides) and mutually
     * compatible (same hints, same material, same transform, etcetera).
     *
     * @param indices the selected indices (not null)
     * @return "" if compatible, otherwise a non-null, non-empty message
     */
    private String mergeFeedback(List<Integer> indices) {
        int numSelected = indices.size();

        for (int i = 0; i < numSelected; i++) {
            int iIndex = indices.get(i);
            GeometryItem iItem = allItems.get(iIndex);
            String result = iItem.mergeFeedback();
            if (!result.isEmpty()) {
                return result;
            }

            for (int j = i + 1; j < numSelected; j++) {
                int jIndex = indices.get(j);
                GeometryItem jItem = allItems.get(jIndex);
                result = iItem.mergeFeedback(jItem);
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }

        return "";
    }
}
