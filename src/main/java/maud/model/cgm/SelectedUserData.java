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
package maud.model.cgm;

import java.util.List;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;

/**
 * The MVC model of the selected user data a loaded C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedUserData implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedUserData.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the user data (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * editable C-G model, if any, containing the user data (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm;
    /**
     * key of the selected user data in the selected spatial, or null if none
     * selected
     */
    private String selectedKey = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Delete (and deselect) the selected user data.
     */
    public void delete() {
        if (selectedKey != null) {
            editableCgm.deleteUserData();
            selectedKey = null;
        }
    }

    /**
     * Find the index of the selected user data (in key lexical order).
     *
     * @return index, or -1 if none selected
     */
    public int findKeyIndex() {
        int index;
        if (selectedKey == null) {
            index = -1;
        } else {
            List<String> keyList;
            keyList = cgm.getSpatial().listUserKeys();
            index = keyList.indexOf(selectedKey);
        }

        return index;
    }

    /**
     * Read the selected user data's key.
     *
     * @return a key, or null if none selected
     */
    public String getKey() {
        return selectedKey;
    }

    /**
     * Select the user data with the specified key. TODO add deselectKey()
     * method
     *
     * @param key a key, or null to deselect
     */
    public void selectKey(String key) {
        selectedKey = key;
    }

    /**
     * Select the next user data (in key lexical order).
     */
    public void selectNextKey() {
        List<String> keyList = cgm.getSpatial().listUserKeys();
        int numKeys = keyList.size();
        int index = keyList.indexOf(selectedKey);
        int nextIndex = MyMath.modulo(index + 1, numKeys);
        String nextName = keyList.get(nextIndex);
        selectKey(nextName);
    }

    /**
     * Select the previous user data (in key lexical order).
     */
    public void selectPreviousKey() {
        List<String> keyList = cgm.getSpatial().listUserKeys();
        int numKeys = keyList.size();
        int index = keyList.indexOf(selectedKey);
        int nextIndex = MyMath.modulo(index - 1, numKeys);
        String previousName = keyList.get(nextIndex);
        selectKey(previousName);
    }

    /**
     * Alter which C-G model contains the data.
     *
     * @param newCgm (not null, alias created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getUserData() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
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
    public SelectedUserData clone() throws CloneNotSupportedException {
        SelectedUserData clone = (SelectedUserData) super.clone();
        return clone;
    }
}
