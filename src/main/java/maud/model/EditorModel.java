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
 * An MVC-model state for the editor screen in the Maud application. Includes
 * all state that's checkpointed.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditorModel {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            EditorModel.class.getName());
    // *************************************************************************
    // fields

    /**
     * known asset folders
     */
    final public AssetFolders folders;
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
     * load slot for the (editable) target CG model
     */
    final public EditableCgm target;
    /**
     * the loaded skeleton map
     */
    final public EditableMap map;
    /**
     * load slot for the (read-only) source CG model
     */
    final public LoadedCgm source;
    /**
     * miscellaneous details
     */
    final public MiscStatus misc;
    /**
     * options for "scene" views
     */
    final public SceneOptions scene;
    /**
     * options for "score" views
     */
    final public ScoreOptions score;
    /**
     * status of the skeleton visualization(s)
     */
    final public SkeletonStatus skeleton;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an MVC model with the default settings.
     */
    public EditorModel() {
        folders = new AssetFolders();
        axes = new AxesStatus();
        bounds = new BoundsStatus();
        camera = new CameraStatus();
        cursor = new CursorStatus();
        target = new EditableCgm();
        map = new EditableMap();
        source = new LoadedCgm();
        misc = new MiscStatus();
        scene = new SceneOptions();
        score = new ScoreOptions();
        skeleton = new SkeletonStatus();
    }

    /**
     * Instantiate an MVC model with settings copied from another MVC-model
     * instance.
     *
     * @param other (not null)
     */
    public EditorModel(EditorModel other) {
        try {
            folders = other.folders.clone();
            axes = other.axes.clone();
            bounds = other.bounds.clone();
            camera = other.camera.clone();
            cursor = other.cursor.clone();
            target = other.target.clone();
            map = other.map.clone();
            source = other.source.clone();
            misc = other.misc.clone();
            scene = other.scene.clone();
            score = other.score.clone();
            skeleton = other.skeleton.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException();
        }
    }
}
