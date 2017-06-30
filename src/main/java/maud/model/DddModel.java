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

import java.util.logging.Logger;

/**
 * The MVC model for the "3D View" screen in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DddModel {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            LoadedCgm.class.getName());
    // *************************************************************************
    // fields

    /**
     * status of the visible coordinate axes
     */
    final public AxesStatus axes;
    /**
     * status of the bounds visualization(s)
     */
    final public BoundsStatus bounds;
    /**
     * status of the camera
     */
    final public CameraStatus camera;
    /**
     * status of the 3D cursor
     */
    final public CursorStatus cursor;
    /**
     * the target CG model
     */
    final public EditableCgm target;
    /**
     * the loaded skeleton mapping
     */
    final public EditableMapping mapping;
    /**
     * the (read-only) source CG model
     */
    final public LoadedCgm source;
    /**
     * miscellaneous details
     */
    final public MiscStatus misc;
    /**
     * status of the skeleton visualization(s)
     */
    final public SkeletonStatus skeleton;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an MVC model with the default settings.
     */
    public DddModel() {
        axes = new AxesStatus();
        bounds = new BoundsStatus();
        camera = new CameraStatus();
        cursor = new CursorStatus();
        target = new EditableCgm();
        mapping = new EditableMapping();
        source = new LoadedCgm();
        misc = new MiscStatus();
        skeleton = new SkeletonStatus();
    }

    /**
     * Instantiate an MVC model with settings copied from another model
     * instance.
     *
     * @param other (not null)
     */
    public DddModel(DddModel other) {
        try {
            axes = other.axes.clone();
            bounds = other.bounds.clone();
            camera = other.camera.clone();
            cursor = other.cursor.clone();
            target = other.target.clone();
            mapping = other.mapping.clone();
            source = other.source.clone();
            misc = other.misc.clone();
            skeleton = other.skeleton.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException();
        }
    }
}
