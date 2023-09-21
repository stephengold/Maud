/*
 Copyright (c) 2018-2021, Stephen Gold
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
import com.jme3.shadow.EdgeFilteringMode;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.Maud;
import maud.MaudUtil;
import maud.action.ActionPrefix;
import maud.model.EditState;
import maud.model.cgm.Cgm;
import maud.model.option.Background;

/**
 * Rendering options for scene views.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class RenderOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(RenderOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * true if physics objects are visualized, otherwise false
     */
    private boolean physicsRendered = true;
    /**
     * shadows (true &rarr; rendered, false &rarr; not rendered)
     */
    private boolean shadowsRendered = true;
    /**
     * sky simulation (true &rarr; simulated, false &rarr; not simulated)
     */
    private boolean skySimulated = false;
    /**
     * background color for the source C-G model with no sky simulation
     */
    private ColorRGBA sourceBackground = new ColorRGBA(0.22f, 0.22f, 0.22f, 1f);
    /**
     * background color for the target C-G model with no sky simulation
     */
    private ColorRGBA targetBackground = new ColorRGBA(0.33f, 0.33f, 0.33f, 1f);
    /**
     * edge filtering mode for shadows
     */
    private EdgeFilteringMode edgeFilter = EdgeFilteringMode.PCFPOISSON;
    /**
     * opacity of cloud layers (&ge;0, &le;1)
     */
    private float cloudiness = 0.5f;
    /**
     * hours since midnight, solar time (&ge;0, &le;24)
     */
    private float hour = 11f;
    /**
     * number of shadow-map splits (&ge;1, &le;4)
     */
    private int numSplits = 3;
    /**
     * width (and height) of shadow maps (pixels per side, &gt;0)
     */
    private int shadowMapSize = 8_192;
    /**
     * CG-model triangle rendering option
     */
    private TriangleMode triangleMode = TriangleMode.PerMaterial;
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether shadows are rendered.
     *
     * @return true if rendered, otherwise false
     */
    public boolean areShadowsRendered() {
        return shadowsRendered;
    }

    /**
     * Read the cloudiness for sky simulation.
     *
     * @return layer opacity (&ge;0, &le;1)
     */
    public float cloudiness() {
        assert cloudiness >= 0f;
        assert cloudiness <= 1f;

        return cloudiness;
    }

    /**
     * Read the edge-filtering mode for shadows.
     *
     * @return an enum value (not null)
     */
    public EdgeFilteringMode edgeFilter() {
        assert edgeFilter != null;
        return edgeFilter;
    }

    /**
     * Read the time of day for sky simulation.
     *
     * @return hours since midnight, solar time (&ge;0, &le;24)
     */
    public float hour() {
        assert hour >= 0f;
        assert hour <= 24f;

        return hour;
    }

    /**
     * Test whether physics objects are visualized.
     *
     * @return true if visualized, otherwise false
     */
    public boolean isPhysicsRendered() {
        return physicsRendered;
    }

    /**
     * Test whether skies are simulated.
     *
     * @return true if simulated, otherwise false (meaning that lighting and
     * backgrounds can be controlled directly)
     */
    public boolean isSkySimulated() {
        return skySimulated;
    }

    /**
     * Read the number of shadow-map splits.
     *
     * @return number (&ge;1, &le;4)
     */
    public int numSplits() {
        assert numSplits >= 1 : numSplits;
        assert numSplits <= 4 : numSplits;

        return numSplits;
    }

    /**
     * Alter the cloudiness of the sky.
     *
     * @param newOpacity (&ge;0, &le;1)
     */
    public void setCloudiness(float newOpacity) {
        Validate.fraction(newOpacity, "new opacity");

        if (cloudiness != newOpacity) {
            cloudiness = newOpacity;

            EditState editState = Maud.getModel().getOptionsEditState();
            editState.setEditedSkyCloudiness();
        }
    }

    /**
     * Alter the edge filtering mode for shadows.
     *
     * @param newSetting (not null)
     */
    public void setEdgeFilter(EdgeFilteringMode newSetting) {
        Validate.nonNull(newSetting, "new setting");

        if (edgeFilter != newSetting) {
            edgeFilter = newSetting;
            EditState.optionSetEdited("edge filter=" + newSetting);
        }
    }

    /**
     * Alter the time of day.
     *
     * @param newHour hours since midnight, solar time (&ge;0, &le;24)
     */
    public void setHour(float newHour) {
        Validate.inRange(newHour, "new hour", 0f, 24f);

        if (hour != newHour) {
            hour = newHour;

            EditState editState = Maud.getModel().getOptionsEditState();
            editState.setEditedSkyHour();
        }
    }

    /**
     * Alter the number of shadow-map splits.
     *
     * @param newNumSplits new number (&ge;1, &le;4)
     */
    public void setNumSplits(int newNumSplits) {
        Validate.inRange(newNumSplits, "new number of splits", 1, 4);

        if (numSplits != newNumSplits) {
            numSplits = newNumSplits;
            EditState.optionSetEdited("shadow splits=" + newNumSplits);
        }
    }

    /**
     * Alter whether physics objects are visualized.
     *
     * @param newSetting true to visualize, false to hide
     */
    public void setPhysicsRendered(boolean newSetting) {
        if (physicsRendered != newSetting) {
            physicsRendered = newSetting;
            EditState.optionSetEdited("physics debug=" + newSetting);
        }
    }

    /**
     * Alter the width (and height) of shadow maps.
     *
     * @param newSize new size (in pixels, &gt;0)
     */
    public void setShadowMapSize(int newSize) {
        Validate.inRange(newSize, "new size", 1, Integer.MAX_VALUE);

        if (shadowMapSize != newSize) {
            shadowMapSize = newSize;
            EditState.optionSetEdited("shadow map size=" + newSize);
        }
    }

    /**
     * Alter whether shadows are rendered.
     *
     * @param newState true &rarr; rendered, false &rarr; not rendered
     */
    public void setShadowsRendered(boolean newState) {
        if (shadowsRendered != newState) {
            shadowsRendered = newState;
            EditState.optionSetEdited("shadows=" + newState);
        }
    }

    /**
     * Alter whether skies are simulated.
     *
     * @param newState true &rarr; simulated, false &rarr; not simulated
     */
    public void setSkySimulated(boolean newState) {
        if (skySimulated != newState) {
            skySimulated = newState;
            EditState.optionSetEdited("sky=" + newState);
        }
    }

    /**
     * Alter the background color for a source C-G model with no sky simulation.
     *
     * @param newColor (not null, unaffected)
     */
    public void setSourceBackgroundColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");

        if (!sourceBackground.equals(newColor)) {
            sourceBackground.set(newColor);

            EditState editState = Maud.getModel().getOptionsEditState();
            Background background = Background.SourceScenesWithNoSky;
            editState.setEditedBackgroundColor(background);
        }
    }

    /**
     * Alter the background color for a target C-G model with no sky simulation.
     *
     * @param newColor (not null, unaffected)
     */
    public void setTargetBackgroundColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "new color");

        if (!targetBackground.equals(newColor)) {
            targetBackground.set(newColor);

            EditState editState = Maud.getModel().getOptionsEditState();
            Background background = Background.TargetScenesWithNoSky;
            editState.setEditedBackgroundColor(background);
        }
    }

    /**
     * Alter how CG-model triangles are rendered.
     *
     * @param newSetting an enum value (not null)
     */
    public void setTriangleMode(TriangleMode newSetting) {
        Validate.nonNull(newSetting, "new setting");

        if (triangleMode != newSetting) {
            triangleMode = newSetting;

            Cgm target = Maud.getModel().getTarget();
            target.updateSceneWireframe();

            Cgm source = Maud.getModel().getSource();
            if (source.isLoaded()) {
                source.updateSceneWireframe();
            }

            EditState.optionSetEdited("triangle mode=" + newSetting);
        }
    }

    /**
     * Read the width (and height) of shadow maps.
     *
     * @return pixels per side (&gt;0)
     */
    public int shadowMapSize() {
        assert shadowMapSize > 0 : shadowMapSize;
        return shadowMapSize;
    }

    /**
     * Copy the background color for a source C-G model with no sky simulation.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the background color (either storeResult or a new instance, not
     * null)
     */
    public ColorRGBA sourceBackgroundColor(ColorRGBA storeResult) {
        if (storeResult == null) {
            return sourceBackground.clone();
        } else {
            return storeResult.set(sourceBackground);
        }
    }

    /**
     * Copy the background color for a target C-G model with no sky simulation.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the background color (either storeResult or a new instance, not
     * null)
     */
    public ColorRGBA targetBackgroundColor(ColorRGBA storeResult) {
        if (storeResult == null) {
            return targetBackground.clone();
        } else {
            return storeResult.set(targetBackground);
        }
    }

    /**
     * Toggle whether physics objects are visualized.
     */
    public void togglePhysicsRendered() {
        setPhysicsRendered(!physicsRendered);
    }

    /**
     * Read the CG-model triangle rendering mode.
     *
     * @return an enum value (not null)
     */
    public TriangleMode triangleMode() {
        assert triangleMode != null;
        return triangleMode;
    }

    /**
     * Write the options to a script using the specified writer.
     *
     * @param writer (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    void writeToScript(Writer writer) throws IOException {
        assert writer != null;

        String action = ActionPrefix.sfPhysicsRendered
                + Boolean.toString(physicsRendered);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.sfShadowsRendered
                + Boolean.toString(shadowsRendered);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.sfSkySimulated + Boolean.toString(skySimulated);
        MaudUtil.writePerformAction(writer, action);

        action = String.format("%s%s %s", ActionPrefix.setBackgroundColor,
                Background.SourceScenesWithNoSky, sourceBackground);
        MaudUtil.writePerformAction(writer, action);

        action = String.format("%s%s %s", ActionPrefix.setBackgroundColor,
                Background.TargetScenesWithNoSky, targetBackground);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.selectEdgeFilter + edgeFilter.toString();
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setCloudiness + Float.toString(cloudiness);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setHour + Float.toString(hour);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setNumSplits + Integer.toString(numSplits);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setMapSize + Integer.toString(shadowMapSize);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.selectTriangleMode + triangleMode.toString();
        MaudUtil.writePerformAction(writer, action);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public RenderOptions clone() throws CloneNotSupportedException {
        RenderOptions clone = (RenderOptions) super.clone();
        clone.sourceBackground = sourceBackground.clone();
        clone.targetBackground = targetBackground.clone();

        return clone;
    }
}
