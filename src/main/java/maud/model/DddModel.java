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
    final public AxesStatus axes = new AxesStatus();
    /**
     * status of the camera
     */
    final public CameraStatus camera = new CameraStatus();
    /**
     * status of the 3D cursor
     */
    final public CursorStatus cursor = new CursorStatus();
    /**
     * which animation/pose is loaded
     */
    final public LoadedAnimation animation = new LoadedAnimation();
    /**
     * the loaded CG model
     */
    final public LoadedCGModel cgm = new LoadedCGModel();
    /**
     * bone transforms of the displayed pose
     */
    final public Pose pose = new Pose();
    /**
     * which bone is selected
     */
    final public SelectedBone bone = new SelectedBone();
    /**
     * which spatial is selected
     */
    final public SelectedSpatial spatial = new SelectedSpatial();
}
