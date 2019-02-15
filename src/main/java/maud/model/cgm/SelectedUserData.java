/*
 Copyright (c) 2017-2019, Stephen Gold
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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Spatial;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.dialog.VectorDialog;
import maud.model.History;

/**
 * The MVC model of the selected user datum in a loaded C-G model.
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
     * C-G model containing the datum (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * editable C-G model, if any, containing the datum (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm;
    /**
     * key of the selected datum in the selected spatial, or null if none
     * selected
     */
    private String selectedKey = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Delete (and deselect) the selected datum.
     */
    public void delete() {
        if (isSelected()) {
            Spatial selectedSpatial = cgm.getSpatial().find();

            History.autoAdd();
            selectedSpatial.setUserData(selectedKey, null);
            String description
                    = String.format("delete user data %s",
                            MyString.quote(selectedKey));
            editableCgm.getEditState().setEdited(description);
            selectedKey = null;
        }
    }

    /**
     * Describe the datum's type.
     *
     * @return a textual description (not null, not empty) or "" if none
     * selected
     */
    public String describeType() {
        String result = "";
        if (isSelected()) {
            Object value = getValue();
            result = value.getClass().getSimpleName();
        }

        return result;
    }

    /**
     * Deselect the selected datum, if any.
     */
    public void deselect() {
        selectedKey = null;
    }

    /**
     * Find the index of the selected datum (in key lexical order).
     *
     * @return index, or -1 if none selected
     */
    public int findKeyIndex() {
        int index;
        if (selectedKey == null) {
            index = -1;
        } else {
            List<String> keyList = cgm.getSpatial().listUserKeys();
            index = keyList.indexOf(selectedKey);
        }

        return index;
    }

    /**
     * Access the selected datum's value.
     *
     * @return the pre-existing instance, or null if none selected
     */
    public Object getValue() {
        Spatial selectedSpatial = cgm.getSpatial().find();
        Object result = selectedSpatial.getUserData(selectedKey);

        return result;
    }

    /**
     * Test whether a datum is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        boolean result;
        if (selectedKey == null) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Read the selected datum's key.
     *
     * @return a key, or null if none selected
     */
    public String key() {
        return selectedKey;
    }

    /**
     * Change the selected datum's key.
     *
     * @param newKey new key name (not null)
     */
    public void renameKey(String newKey) {
        Validate.nonNull(newKey, "new key");

        Spatial spatial = cgm.getSpatial().find();
        String oldKey = selectedKey;
        Object value = getValue();

        History.autoAdd();
        spatial.setUserData(oldKey, null);
        spatial.setUserData(newKey, value);

        String description = String.format("rename user-data key %s to %s",
                MyString.quote(oldKey), MyString.quote(newKey));
        editableCgm.getEditState().setEdited(description);

        selectKey(newKey);
    }

    /**
     * Select the datum with the specified key.
     *
     * @param key a key, or null to deselect
     */
    public void selectKey(String key) {
        selectedKey = key;
    }

    /**
     * Select the next datum (in key lexical order).
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
     * Select the previous datum (in key lexical order).
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
     * Alter which C-G model contains the datum. (Invoked only during
     * initialization and cloning.)
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

    /**
     * Alter the value of the selected datum.
     *
     * @param valueString string representation of the new value (not null)
     */
    public void setValue(String valueString) {
        Validate.nonNull(valueString, "value string");

        Object value = getValue();
        Spatial spatial = cgm.getSpatial().find();
        String key = key();

        History.autoAdd();
        if (value instanceof Boolean) {
            boolean newValue = Boolean.parseBoolean(valueString);
            spatial.setUserData(key, newValue);

        } else if (value instanceof Float) {
            float newValue = Float.parseFloat(valueString);
            spatial.setUserData(key, newValue);

        } else if (value instanceof Integer) {
            int newValue = Integer.parseInt(valueString);
            spatial.setUserData(key, newValue);

        } else if (value instanceof Long) {
            long newValue = Long.parseLong(valueString);
            spatial.setUserData(key, newValue);

        } else if (value instanceof String) {
            spatial.setUserData(key, valueString);

        } else if (value instanceof Vector2f
                || value instanceof Vector3f
                || value instanceof Vector4f) {
            Object newValue = VectorDialog.parseVector(valueString);
            spatial.setUserData(key, newValue);

        } else {        // TODO bone value
            throw new IllegalStateException();
        }

        String description = String.format("alter value of user datum %s",
                MyString.quote(key));
        editableCgm.getEditState().setEdited(description);
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
