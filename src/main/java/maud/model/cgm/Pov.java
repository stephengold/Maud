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
package maud.model.cgm;

/**
 * Interface to a camera's MVC model in Maud's edit screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public interface Pov {
    /**
     * Zoom the camera and/or move it forward/backward when the scroll wheel is
     * turned.
     *
     * @param amount scroll wheel notches
     */
    void moveBackward(float amount);

    /**
     * Move the camera left/right when the mouse is dragged from left/right.
     *
     * @param amount drag component
     */
    void moveLeft(float amount);

    /**
     * Move the camera up/down when the mouse is dragged up/down.
     *
     * @param amount drag component
     */
    void moveUp(float amount);

    /**
     * Alter which CG model uses this POV. (Invoked only during initialization
     * and cloning.)
     *
     * @param newCgm (not null)
     */
    void setCgm(Cgm newCgm);

    /**
     * Update the camera used to render this POV.
     */
    void updateCamera();
}
