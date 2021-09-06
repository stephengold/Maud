/*
 * Copyright (c) 2009-2017 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.scene.plugins.bvh;

import com.jme3.animation.Animation;
import com.jme3.animation.Skeleton;
import java.util.logging.Logger;

/**
 * The content of a BVH asset.
 *
 * @author Nehon
 */
public class BVHAnimData {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            BVHAnimData.class.getName());
    // *************************************************************************
    // fields

    /**
     * the animation
     */
    private Animation animation;
    /**
     * the time per frame (in seconds, &gt;0)
     */
    private float timePerFrame;
    /**
     * the skeleton
     */
    private Skeleton skeleton;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an instance.
     *
     * @param skeleton the skeleton
     * @param anim the animation
     * @param timePerFrame (in seconds, &gt;0)
     */
    public BVHAnimData(Skeleton skeleton, Animation anim, float timePerFrame) {
        this.skeleton = skeleton;
        this.animation = anim;
        this.timePerFrame = timePerFrame;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the animation.
     *
     * @return the pre-existing instance
     */
    public Animation getAnimation() {
        return animation;
    }

    /**
     * Access the skeleton.
     *
     * @return the pre-existing instance
     */
    public Skeleton getSkeleton() {
        return skeleton;
    }
}
