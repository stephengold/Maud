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
package maud.view;

import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import maud.model.cgm.Cgm;

/**
 * Interface to an MVC view in Maud's edit screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public interface EditorView {
    /**
     * Consider selecting each axis in this view.
     *
     * @param bestSelection best selection found so far (not null, modified)
     */
    void considerAxes(Selection bestSelection);

    /**
     * Consider selecting each bone in this view.
     *
     * @param bestSelection best selection found so far (not null, modified)
     */
    void considerBones(Selection bestSelection);

    /**
     * Consider selecting each boundary of this view.
     *
     * @param bestSelection best selection found so far (not null, modified)
     */
    void considerBoundaries(Selection bestSelection);

    /**
     * Consider selecting each gnomon in this view.
     *
     * @param bestSelection best selection found so far (not null, modified)
     */
    void considerGnomons(Selection bestSelection);

    /**
     * Consider selecting each keyframe in this view.
     *
     * @param bestSelection best selection found so far (not null, modified)
     */
    void considerKeyframes(Selection bestSelection);

    /**
     * Consider selecting each visualized track in this view.
     *
     * @param selection best selection found so far (not null, modified)
     */
    void considerTracks(Selection selection);

    /**
     * Consider selecting each vertex in this view.
     *
     * @param bestSelection best selection found so far (not null, modified)
     */
    void considerVertices(Selection bestSelection);

    /**
     * Access the camera used to render this view.
     *
     * @return a pre-existing instance, or null if not rendered
     */
    Camera getCamera();

    /**
     * Read what type of view this is.
     *
     * @return an enum value (not null)
     */
    ViewType getType();

    /**
     * Access the view port used to render this view.
     *
     * @return the pre-existing view port
     */
    ViewPort getViewPort();

    /**
     * Update this view prior to rendering. (Invoked once per frame on each
     * instance.)
     *
     * @param renderCgm which C-G model to render
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    void update(Cgm renderCgm, float tpf);

    /**
     * Attempt to warp a cursor to the screen coordinates of the mouse pointer.
     */
    void warpCursor();
}
