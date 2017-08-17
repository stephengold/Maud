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
package maud.model;

import java.util.List;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;

/**
 * The MVC model of the selected user data in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class UserData implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            UserData.class.getName());
    // *************************************************************************
    // fields

    /**
     * editable CG model containing the spatial, if any (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private EditableCgm editableCgm;
    /**
     * loaded CG model containing the spatial (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private LoadedCgm cgm = null;
    /**
     * key of the selected user data, or null if none selected
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
            keyList = cgm.spatial.listUserKeys();
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
     * Select the user data with the specified key.
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
        List<String> keyList = cgm.spatial.listUserKeys();
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
        List<String> keyList = cgm.spatial.listUserKeys();
        int numKeys = keyList.size();
        int index = keyList.indexOf(selectedKey);
        int nextIndex = MyMath.modulo(index - 1, numKeys);
        String previousName = keyList.get(nextIndex);
        selectKey(previousName);
    }

    /**
     * Alter which CG model contains the data.
     *
     * @param newLoaded (not null)
     */
    void setCgm(LoadedCgm newLoaded) {
        assert newLoaded != null;

        cgm = newLoaded;
        if (newLoaded instanceof EditableCgm) {
            editableCgm = (EditableCgm) newLoaded;
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
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public UserData clone() throws CloneNotSupportedException {
        UserData clone = (UserData) super.clone();
        return clone;
    }
}
