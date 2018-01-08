/*
 Copyright (c) 2017-2018, Stephen Gold
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

import com.jme3.shadow.EdgeFilteringMode;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.Maud;
import maud.MaudUtil;
import maud.action.ActionPrefix;
import maud.model.cgm.Cgm;

/**
 * Display options applicable to scene views in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SceneOptions implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SceneOptions.class.getName());
    // *************************************************************************
    // fields

    /**
     * configuration of coordinate axes visualization(s)
     */
    private AxesOptions axes = new AxesOptions();
    /**
     * true if physics objects are visualized, otherwise false
     */
    private boolean physicsRendered = true;
    /**
     * shadows (true &rarr; rendered, false &rarr; not rendered)
     */
    private boolean shadowsRendered = true;
    /**
     * sky background (true &rarr; rendered, false &rarr; not rendered)
     */
    private boolean skyRendered = true;
    /**
     * configuration of the bounds visualization(s)
     */
    private BoundsOptions bounds = new BoundsOptions();
    /**
     * configuration of the camera(s)
     */
    private CameraOptions camera = new CameraOptions();
    /**
     * configuration of the 3-D cursor(s)
     */
    private DddCursorOptions cursor = new DddCursorOptions();
    /**
     * edge filtering mode for shadows
     */
    private EdgeFilteringMode edgeFilter = EdgeFilteringMode.Bilinear;
    /**
     * diameter of platform(s) (in world units, &gt;0)
     */
    private float platformDiameter = 2f;
    /**
     * number of shadow-map splits (&gt;0)
     */
    private int numSplits = 3;
    /**
     * width (and height) of shadow maps (pixels per side, &gt;0)
     */
    private int shadowMapSize = 4_096;
    /**
     * type of platform(s) (not null)
     */
    private PlatformType platformType = PlatformType.Square;
    /**
     * configuration of the skeleton visualization(s)
     */
    private SkeletonOptions skeleton = new SkeletonOptions();
    /**
     * CG-model triangle rendering option
     */
    private TriangleMode triangleMode = TriangleMode.PerMaterial;
    /**
     * configuration of the vertex visualization(s)
     */
    private VertexOptions vertex = new VertexOptions();
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
     * Access the configuration of coordinate axes visualization(s).
     *
     * @return the pre-existing instance (not null)
     */
    public AxesOptions getAxes() {
        assert axes != null;
        return axes;
    }

    /**
     * Access the configuration of bounds visualization(s).
     *
     * @return the pre-existing instance (not null)
     */
    public BoundsOptions getBounds() {
        assert bounds != null;
        return bounds;
    }

    /**
     * Access the configuration of the camera(s).
     *
     * @return the pre-existing instance (not null)
     */
    public CameraOptions getCamera() {
        assert camera != null;
        return camera;
    }

    /**
     * Access the configuration of the 3-D cursor(s).
     *
     * @return the pre-existing instance (not null)
     */
    public DddCursorOptions getCursor() {
        assert cursor != null;
        return cursor;
    }

    /**
     * Read the edge filtering mode for shadows.
     *
     * @return an enum value (not null)
     */
    public EdgeFilteringMode getEdgeFilter() {
        assert edgeFilter != null;
        return edgeFilter;
    }

    /**
     * Read the number of shadow-map splits.
     *
     * @return count (&gt;0)
     */
    public int getNumSplits() {
        assert numSplits > 0 : numSplits;
        return numSplits;
    }

    /**
     * Read the diameter of the platform(s).
     *
     * @return diameter (in world units, &gt;0)
     */
    public float getPlatformDiameter() {
        assert platformDiameter > 0f : platformDiameter;
        return platformDiameter;
    }

    /**
     * Read the type of the platform(s).
     *
     * @return an enum value (not null)
     */
    public PlatformType getPlatformType() {
        assert platformType != null;
        return platformType;
    }

    /**
     * Read the width (and height) of shadow maps.
     *
     * @return pixels per side (&gt;0)
     */
    public int getShadowMapSize() {
        assert shadowMapSize > 0 : shadowMapSize;
        return shadowMapSize;
    }

    /**
     * Access the configuration of the skeleton visualization(s).
     *
     * @return the pre-existing instance (not null)
     */
    public SkeletonOptions getSkeleton() {
        assert skeleton != null;
        return skeleton;
    }

    /**
     * Read the CG-model triangle rendering mode.
     *
     * @return an enum value (not null)
     */
    public TriangleMode getTriangleMode() {
        assert triangleMode != null;
        return triangleMode;
    }

    /**
     * Access the configuration of the vertex visualization(s).
     *
     * @return the pre-existing instance (not null)
     */
    public VertexOptions getVertex() {
        assert vertex != null;
        return vertex;
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
     * Test whether the sky background is rendered.
     *
     * @return true if rendered, otherwise false
     */
    public boolean isSkyRendered() {
        return skyRendered;
    }

    /**
     * Alter the edge filtering mode for shadows.
     *
     * @param newSetting (not null)
     */
    public void setEdgeFilter(EdgeFilteringMode newSetting) {
        Validate.nonNull(newSetting, "new setting");
        edgeFilter = newSetting;
    }

    /**
     * Alter the number of shadow-map splits.
     *
     * @param newNumSplits new size (in pixels, &gt;0)
     */
    public void setNumSplits(int newNumSplits) {
        Validate.inRange(newNumSplits, "new size", 1, Integer.MAX_VALUE);
        numSplits = newNumSplits;
    }

    /**
     * Alter whether physics objects are visualized.
     *
     * @param newSetting true to visualize, false to hide
     */
    public void setPhysicsRendered(boolean newSetting) {
        physicsRendered = newSetting;
    }

    /**
     * Alter the diameter of the platform(s).
     *
     * @param diameter (in world units, &gt;0)
     */
    public void setPlatformDiameter(float diameter) {
        Validate.positive(diameter, "diameter");
        platformDiameter = diameter;
    }

    /**
     * Alter the type of the platform(s).
     *
     * @param newType an enum value (not null)
     */
    public void setPlatformType(PlatformType newType) {
        Validate.nonNull(newType, "new type");
        platformType = newType;
    }

    /**
     * Alter the width (and height) of shadow maps.
     *
     * @param newSize new size (in pixels, &gt;0)
     */
    public void setShadowsMapSize(int newSize) {
        Validate.inRange(newSize, "new size", 1, Integer.MAX_VALUE);
        shadowMapSize = newSize;
    }

    /**
     * Alter whether shadows are rendered.
     *
     * @param newState true &rarr; rendered, false &rarr; not rendered
     */
    public void setShadowsRendered(boolean newState) {
        shadowsRendered = newState;
    }

    /**
     * Alter the rendering of the sky background.
     *
     * @param newState true &rarr; rendered, false &rarr; not rendered
     */
    public void setSkyRendered(boolean newState) {
        skyRendered = newState;
    }

    /**
     * Alter how CG-model triangles are rendered.
     *
     * @param newSetting an enum value (not null)
     */
    public void setTriangleMode(TriangleMode newSetting) {
        Validate.nonNull(newSetting, "new setting");

        triangleMode = newSetting; // TODO check for change

        Cgm target = Maud.getModel().getTarget();
        target.updateSceneWireframe();

        Cgm source = Maud.getModel().getSource();
        if (source.isLoaded()) {
            source.updateSceneWireframe();
        }
    }

    /**
     * Write the options to a script using the specified writer.
     *
     * @param writer (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    public void writeToScript(Writer writer) throws IOException {
        Validate.nonNull(writer, "writer");

        camera.writeToScript(writer);
        skeleton.writeToScript(writer);

        String action = ActionPrefix.setPhysicsRendered
                + Boolean.toString(physicsRendered);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setShadowsRendered
                + Boolean.toString(shadowsRendered);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.selectEdgeFilter + edgeFilter.toString();
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setNumSplits + Integer.toString(numSplits);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setMapSize + Integer.toString(shadowMapSize);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.setSkyRendered + Boolean.toString(skyRendered);
        MaudUtil.writePerformAction(writer, action);

        action = ActionPrefix.selectTriangleMode + triangleMode.toString();
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
    public SceneOptions clone() throws CloneNotSupportedException {
        SceneOptions clone = (SceneOptions) super.clone();
        axes = axes.clone();
        bounds = bounds.clone();
        camera = camera.clone();
        cursor = cursor.clone();
        skeleton = skeleton.clone();
        vertex = vertex.clone();

        return clone;
    }
}
