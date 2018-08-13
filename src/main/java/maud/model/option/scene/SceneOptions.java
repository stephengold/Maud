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

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.MaudUtil;
import maud.action.ActionPrefix;
import maud.model.WhichCgm;

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
     * options for the coordinate axes visualization(s)
     */
    private AxesOptions axesOptions = new AxesOptions();
    /**
     * options for the bounds visualization(s)
     */
    private BoundsOptions boundsOptions = new BoundsOptions();
    /**
     * options for the camera(s)
     */
    private CameraOptions cameraOptions = new CameraOptions();
    /**
     * options for the 3-D cursor(s)
     */
    private DddCursorOptions cursorOptions = new DddCursorOptions();
    /**
     * diameter of source-CGM platform (in world units, &gt;0)
     */
    private float sourcePlatformDiameter = 2f;
    /**
     * diameter of target-CGM platform (in world units, &gt;0)
     */
    private float targetPlatformDiameter = 2f;
    /**
     * options for lights with no sky simulation
     */
    private LightsOptions lightsOptions = new LightsOptions();
    /**
     * type of platform(s) (not null)
     */
    private PlatformType platformType = PlatformType.Square;
    /**
     * options for rendering the scene(s)
     */
    private RenderOptions renderOptions = new RenderOptions();
    /**
     * options for the skeleton visualization(s)
     */
    private SkeletonOptions skeletonOptions = new SkeletonOptions();
    /**
     * options for the vertex visualization(s)
     */
    private VertexOptions vertexOptions = new VertexOptions();
    // *************************************************************************
    // new methods exposed

    /**
     * Access the options for coordinate axes visualization(s).
     *
     * @return the pre-existing instance (not null)
     */
    public AxesOptions getAxes() {
        assert axesOptions != null;
        return axesOptions;
    }

    /**
     * Access the options for bounds visualization(s).
     *
     * @return the pre-existing instance (not null)
     */
    public BoundsOptions getBounds() {
        assert boundsOptions != null;
        return boundsOptions;
    }

    /**
     * Access the options for the camera(s).
     *
     * @return the pre-existing instance (not null)
     */
    public CameraOptions getCamera() {
        assert cameraOptions != null;
        return cameraOptions;
    }

    /**
     * Access the options for the 3-D cursor(s).
     *
     * @return the pre-existing instance (not null)
     */
    public DddCursorOptions getCursor() {
        assert cursorOptions != null;
        return cursorOptions;
    }

    /**
     * Access the options for lights with no sky simulation.
     *
     * @return the pre-existing instance (not null)
     */
    public LightsOptions getLights() {
        assert lightsOptions != null;
        return lightsOptions;
    }

    /**
     * Read the diameter of a platform.
     *
     * @param whichCgm (not null, unaffected)
     * @return diameter (in world units, &gt;0)
     */
    public float getPlatformDiameter(WhichCgm whichCgm) {
        float diameter;
        switch (whichCgm) {
            case Source:
                diameter = sourcePlatformDiameter;
                break;

            case Target:
                diameter = targetPlatformDiameter;
                break;

            default:
                throw new IllegalArgumentException();
        }

        assert diameter > 0f : diameter;
        return diameter;
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
     * Access the options for rendering the scene(s).
     *
     * @return the pre-existing instance (not null)
     */
    public RenderOptions getRender() {
        assert renderOptions != null;
        return renderOptions;
    }

    /**
     * Access the options for the skeleton visualization(s).
     *
     * @return the pre-existing instance (not null)
     */
    public SkeletonOptions getSkeleton() {
        assert skeletonOptions != null;
        return skeletonOptions;
    }

    /**
     * Access the options for the vertex visualization(s).
     *
     * @return the pre-existing instance (not null)
     */
    public VertexOptions getVertex() {
        assert vertexOptions != null;
        return vertexOptions;
    }

    /**
     * Alter the diameter of a platform.
     *
     * @param whichCgm (not null)
     * @param newDiameter (in world units, &gt;0)
     */
    public void setPlatformDiameter(WhichCgm whichCgm, float newDiameter) {
        Validate.positive(newDiameter, "new diameter");
        switch (whichCgm) {
            case Source:
                sourcePlatformDiameter = newDiameter;
                break;

            case Target:
                targetPlatformDiameter = newDiameter;
                break;

            default:
                throw new IllegalArgumentException();
        }
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
     * Write the options to a script using the specified writer.
     *
     * @param writer (not null)
     * @throws java.io.IOException if an I/O error occurs while writing
     */
    public void writeToScript(Writer writer) throws IOException {
        Validate.nonNull(writer, "writer");

        axesOptions.writeToScript(writer);
        boundsOptions.writeToScript(writer);
        cameraOptions.writeToScript(writer);
        cursorOptions.writeToScript(writer);

        // CGM load overrides platform diameter, so no point in writing it here
        String action = ActionPrefix.selectPlatformType
                + platformType.toString();
        MaudUtil.writePerformAction(writer, action);

        lightsOptions.writeToScript(writer);
        renderOptions.writeToScript(writer);
        skeletonOptions.writeToScript(writer);
        vertexOptions.writeToScript(writer);
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
        axesOptions = axesOptions.clone();
        boundsOptions = boundsOptions.clone();
        cameraOptions = cameraOptions.clone();
        cursorOptions = cursorOptions.clone();
        lightsOptions = lightsOptions.clone();
        renderOptions = renderOptions.clone();
        skeletonOptions = skeletonOptions.clone();
        vertexOptions = vertexOptions.clone();

        return clone;
    }
}
