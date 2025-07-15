/*
 Copyright (c) 2020-2025 Stephen Gold
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
package maud.tool;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.Tool;
import maud.Maud;
import maud.model.cgm.SelectedSpatial;

/**
 * The controller for the "Spatial-Bounds" tool in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SpatialBoundsTool extends Tool {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SpatialBoundsTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized tool.
     *
     * @param screenController the controller of the screen that will contain
     * the tool (not null)
     */
    SpatialBoundsTool(GuiScreenController screenController) {
        super(screenController, "spatialBounds");
    }
    // *************************************************************************
    // Tool methods

    /**
     * Update this tool prior to rendering. (Invoked once per frame while this
     * tool is displayed.)
     */
    @Override
    protected void toolUpdate() {
        updateName();
        updateType();

        SelectedSpatial selected = Maud.getModel().getTarget().getSpatial();
        BoundingVolume meshBound = selected.copyBound(true);
        updateBound("spatialBoundM", meshBound);

        BoundingVolume worldBound = selected.copyBound(false);
        updateBound("spatialBoundW", worldBound);
    }
    // *************************************************************************
    // private methods

    /**
     * Clear the specified row.
     *
     * @param idPrefix the ID prefix of the row to clear (not null)
     */
    private void clearRow(String idPrefix) {
        setStatusText(idPrefix + "X", "");
        setStatusText(idPrefix + "Y", "");
        setStatusText(idPrefix + "Z", "");
    }

    /**
     * Fill the specified row with the specified data.
     *
     * @param idPrefix which row to fill (not null)
     * @param vector the ID prefix of the row to fill (not null, unaffected)
     */
    private void fillRow(String idPrefix, Vector3f vector) {
        setStatusText(idPrefix + "X", " " + Float.toString(vector.x));
        setStatusText(idPrefix + "Y", " " + Float.toString(vector.y));
        setStatusText(idPrefix + "Z", " " + Float.toString(vector.z));
    }

    /**
     * Refresh the rows to reflect the specified bounding volume.
     *
     * @param idPrefix the ID prefix of all 4 rows (not null)
     * @param volume the bounding volume to use (may be null)
     */
    private void updateBound(String idPrefix, BoundingVolume volume) {
        if (volume == null) {
            for (int axisIndex = 0; axisIndex < 3; ++axisIndex) {
                clearRow(idPrefix + "C");
                clearRow(idPrefix + "R");
                clearRow(idPrefix + "Min");
                clearRow(idPrefix + "Max");
            }

        } else {
            Vector3f radii;
            if (volume instanceof BoundingSphere) {
                BoundingSphere sphere = (BoundingSphere) volume;
                float radius = sphere.getRadius();
                radii = new Vector3f(radius, radius, radius);
            } else {
                BoundingBox box = (BoundingBox) volume;
                radii = box.getExtent(null);
            }
            fillRow(idPrefix + "R", radii);

            Vector3f center = volume.getCenter(); // alias
            fillRow(idPrefix + "C", center);

            Vector3f minima = center.subtract(radii);
            fillRow(idPrefix + "Min", minima);

            Vector3f maxima = center.add(radii);
            fillRow(idPrefix + "Max", maxima);
        }
    }

    /**
     * Update the display of the spatial's name.
     */
    private void updateName() {
        SelectedSpatial selected = Maud.getModel().getTarget().getSpatial();
        String name = selected.getName();
        String description = MyString.quote(name);
        setStatusText("spatialName3", " " + description);
    }

    /**
     * Update the spatial's bound-type status and toggle button.
     */
    private void updateType() {
        SelectedSpatial selected = Maud.getModel().getTarget().getSpatial();
        BoundingVolume.Type type = selected.getWorldBoundType();
        String typeText = "null";
        if (type != null) {
            typeText = type.toString();
        }
        setStatusText("spatialBound", " " + typeText);

        String toggleText = "";
        if (selected.isGeometry()) {
            toggleText = "Toggle";
        }
        setButtonText("spatialBoundType", toggleText);
    }
}
