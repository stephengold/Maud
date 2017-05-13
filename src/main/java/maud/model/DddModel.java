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
            LoadedCGModel.class.getName());
    // *************************************************************************
    // fields

    /**
     * status of the visible coordinate axes
     */
    final public AxesStatus axes;
    /**
     * status of the camera
     */
    final public CameraStatus camera;
    /**
     * status of the 3D cursor
     */
    final public CursorStatus cursor;
    /**
     * which animation/pose is loaded
     */
    final public LoadedAnimation animation;
    /**
     * which CG model is loaded (set by
     * {@link maud.Maud#guiInitializeApplication()})
     */
    public LoadedCGModel cgm = null;
    /**
     * miscellaneous details
     */
    final public MiscStatus misc;
    /**
     * bone transforms of the displayed pose
     */
    final public Pose pose;
    /**
     * parameters for re-targeting animations (set by
     * {@link maud.Maud#guiInitializeApplication()})
     */
    public RetargetParameters retarget = null;
    /**
     * which bone is selected
     */
    final public SelectedBone bone;
    /**
     * which spatial is selected
     */
    final public SelectedSpatial spatial;
    /**
     * status of the skeleton visualization
     */
    final public SkeletonStatus skeleton;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a model with the default settings.
     */
    public DddModel() {
        axes = new AxesStatus();
        camera = new CameraStatus();
        cursor = new CursorStatus();
        animation = new LoadedAnimation();
        /* cgm field will be set later */
        misc = new MiscStatus();
        pose = new Pose();
        /* retarget field will be set later */
        bone = new SelectedBone();
        spatial = new SelectedSpatial();
        skeleton = new SkeletonStatus();
    }

    /**
     * Instantiate a model with settings copied from another model.
     *
     * @param source (not null)
     */
    public DddModel(DddModel source) {
        try {
            axes = (AxesStatus) source.axes.clone();
            camera = (CameraStatus) source.camera.clone();
            cursor = (CursorStatus) source.cursor.clone();
            animation = (LoadedAnimation) source.animation.clone();
            cgm = (LoadedCGModel) source.cgm.clone();
            misc = (MiscStatus) source.misc.clone();
            pose = (Pose) source.pose.clone();
            bone = (SelectedBone) source.bone.clone();
            retarget = (RetargetParameters) source.retarget.clone();
            spatial = (SelectedSpatial) source.spatial.clone();
            skeleton = (SkeletonStatus) source.skeleton.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException();
        }
    }
}
