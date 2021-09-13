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
package maud.model.cgm;

import com.jme3.scene.Spatial;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;

/**
 * Information about a particular Spatial, for use in a DialogController.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SpatialItem implements Comparable<SpatialItem> {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SpatialItem.class.getName());
    // *************************************************************************
    // fields

    /**
     * Spatial in the MVC model
     */
    final private Spatial spatial;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new item.
     */
    SpatialItem(Spatial spatial) {
        assert spatial != null;
        this.spatial = spatial;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the Spatial itself.
     *
     * @return the pre-existing instance (not null)
     */
    Spatial getSpatial() {
        return spatial;
    }
    // *************************************************************************
    // Comparable methods

    /**
     * Compare to another SpatialItem, based on the spatial's name.
     *
     * @param otherItem (not null)
     * @return 0 if this equals otherItem; negative if this comes before
     * otherItem; positive if this comes after otherItem
     */
    @Override
    public int compareTo(SpatialItem otherItem) {
        int result;

        String name = spatial.getName();
        String otherName = otherItem.getSpatial().getName();
        if (name == null) {
            if (otherName == null) {
                result = 0;
            } else {
                result = -1;
            }
        } else {
            result = name.compareTo(otherName);
        }

        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Test for exact equivalence with another Object.
     *
     * @param otherObject the object to compare to (may be null, unaffected)
     * @return true if the objects are equivalent, otherwise false
     */
    @Override
    public boolean equals(Object otherObject) {
        boolean result;
        if (otherObject == this) {
            result = true;
        } else if (otherObject != null
                && otherObject.getClass() == getClass()) {
            SpatialItem otherItem = (SpatialItem) otherObject;
            result = (otherItem.getSpatial() == spatial);
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Generate the hash code for this item.
     *
     * @return the value to use for hashing
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(spatial);

        return hash;
    }

    /**
     * Represent the item as a text string.
     *
     * @return a descriptive string of text (not null, not empty)
     */
    @Override
    public String toString() {
        String typeText = spatial.getClass().getSimpleName();
        String quotedName = MyString.quote(spatial.getName());
        Cgm cgm = Maud.getModel().getTarget();
        List<Integer> treePosition = cgm.findSpatial(spatial);
        String result
                = String.format("%s %s %s", quotedName, typeText, treePosition);

        return result;
    }
}
