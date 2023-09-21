/*
 Copyright (c) 2017-2023, Stephen Gold
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
package maud.model.option;

/**
 * Enumerate bone selection options for visualizers.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum ShowBones {
    // *************************************************************************
    // values

    /**
     * visualize all bones in the selected skeleton
     */
    All,
    /**
     * visualize the selected bone and all its ancestors
     */
    Ancestry,
    /**
     * visualize the selected bone, all its ancestors, plus any immediate
     * children
     */
    Family,
    /**
     * visualize only bones that influence mesh vertices
     */
    Influencers,
    /**
     * visualize only leaf bones
     */
    Leaves,
    /**
     * visualize only bones with mappings in the loaded skeleton map
     */
    Mapped,
    /**
     * visualize no bones
     */
    None,
    /**
     * visualize only root bones
     */
    Roots,
    /**
     * visualize only the selected bone
     */
    Selected,
    /**
     * visualize the selected bone and all its descendants
     */
    Subtree,
    /**
     * visualize only bones with tracks in the loaded animation
     */
    Tracked,
    /**
     * visualize only bones without mappings in the loaded skeleton map
     */
    Unmapped
}
