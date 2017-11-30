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
package maud.dialog;

import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.nifty.dialog.DialogController;
import maud.model.cgm.TrackItem;

/**
 * Controller for a multi-select dialog box used to select animation tracks for
 * mixing.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class MixDialog implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MixDialog.class.getName());
    // *************************************************************************
    // fields

    /**
     * list of animation tracks to select from
     */
    final private List<TrackItem> items;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller with the specified list of items.
     *
     * @param itemList (not null, not empty)
     */
    MixDialog(List<TrackItem> itemList) {
        assert itemList != null;
        assert !itemList.isEmpty();

        items = itemList;
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
        boolean result;
        if (anySelected(dialogElement) && !anyConflicts(dialogElement)) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Callback to update the dialog box prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param dialogElement (not null)
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(Element dialogElement, float elapsedTime) {
        String commitLabel, feedbackMessage;
        if (!anySelected(dialogElement)) {
            commitLabel = "";
            feedbackMessage = "no tracks selected";
        } else if (anyConflicts(dialogElement)) {
            commitLabel = "";
            feedbackMessage = "target conflict";
        } else {
            commitLabel = "Mix tracks";
            feedbackMessage = "";
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
     * Test whether any selected tracks share the same target.
     *
     * @param dialogElement (not null)
     * @return true if any share targets, otherwise false
     */
    private boolean anyConflicts(Element dialogElement) {
        assert dialogElement != null;

        List<Integer> indices = getSelectedIndices(dialogElement);
        int numSelected = indices.size();
        for (int i = 0; i < numSelected; i++) {
            int iIndex = indices.get(i);
            TrackItem iItem = items.get(iIndex);
            for (int j = i + 1; j < numSelected; j++) {
                int jIndex = indices.get(j);
                TrackItem jItem = items.get(jIndex);
                if (iItem.hasSameTargetAs(jItem)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Test whether any tracks are selected.
     *
     * @param dialogElement (not null)
     * @return true if any selected, otherwise false
     */
    private boolean anySelected(Element dialogElement) {
        assert dialogElement != null;

        List<Integer> indices = getSelectedIndices(dialogElement);
        if (indices.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Read the selected indices.
     *
     * @param dialogElement (not null)
     * @return a text string (not null)
     */
    private List<Integer> getSelectedIndices(Element dialogElement) {
        assert dialogElement != null;

        ListBox listBox = dialogElement.findNiftyControl("#box", ListBox.class);
        @SuppressWarnings("unchecked")
        List<Integer> result = listBox.getSelectedIndices();

        return result;
    }
}
