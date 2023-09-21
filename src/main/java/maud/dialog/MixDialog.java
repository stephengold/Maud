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
package maud.dialog;

import java.util.List;
import java.util.logging.Logger;
import jme3utilities.nifty.dialog.MultiSelectDialog;
import maud.model.cgm.TrackItem;

/**
 * Controller for a multi-select dialog box used to select animation tracks for
 * mixing.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class MixDialog extends MultiSelectDialog<TrackItem> {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MixDialog.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller with the specified list of items.
     *
     * @param itemList (not null, not empty, unaffected)
     */
    MixDialog(List<TrackItem> itemList) {
        super("Next", itemList);
    }
    // *************************************************************************
    // MultiSelectDialog methods

    /**
     * Determine the feedback message for the specified list of indices.
     *
     * @param indexList the indices of all selected items (not null, unaffected)
     * @return the message (not null)
     */
    @Override
    public String feedback(List<Integer> indexList) {
        String result;

        if (indexList.isEmpty()) {
            result = "no tracks selected";
        } else if (anyConflicts(indexList)) {
            result = "target conflict";
        } else {
            result = "";
        }

        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Test whether any selected tracks share the same target.
     *
     * @param indexList the indices of all selected items (not null, unaffected)
     * @return true if any share targets, otherwise false
     */
    private boolean anyConflicts(List<Integer> indexList) {
        int numSelected = indexList.size();
        for (int i = 0; i < numSelected; i++) {
            int iIndex = indexList.get(i);
            TrackItem iItem = getItem(iIndex);
            for (int j = i + 1; j < numSelected; j++) {
                int jIndex = indexList.get(j);
                TrackItem jItem = getItem(jIndex);
                if (iItem.hasSameTargetAs(jItem)) {
                    return true;
                }
            }
        }

        return false;
    }
}
