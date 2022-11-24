/*
 Copyright (c) 2020-2022, Stephen Gold
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

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimTrack;
import com.jme3.animation.Animation;
import com.jme3.animation.Track;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A temporary list of tracks, used while constructing a new AnimClip/Animation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final class TmpTracks {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TmpTracks.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private TmpTracks() {
    }
    // *************************************************************************
    // fields

    /**
     * list of tracks
     */
    final private static List<Object> trackList = new ArrayList<>(64);
    // *************************************************************************
    // new methods exposed

    /**
     * Add an AnimTrack or Track to the list.
     *
     * @param track the track to add (not null)
     */
    static void add(Object track) {
        assert track != null;
        assert track instanceof AnimTrack || track instanceof Track :
                track.getClass().getSimpleName();

        trackList.add(track);
    }

    /**
     * Add all temporary tracks to the specified Animation or AnimClip.
     *
     * @param anim the Animation or AnimClip to modify (not null, empty)
     */
    static void addAllToAnim(Object anim) {
        if (anim instanceof Animation) {
            for (Object tmpTrack : trackList) {
                Track track = (Track) tmpTrack;
                ((Animation) anim).addTrack(track);
            }

        } else {
            int numTracks = trackList.size();
            AnimTrack<?>[] tracks = new AnimTrack[numTracks];
            for (int i = 0; i < numTracks; ++i) {
                tracks[i] = (AnimTrack<?>) trackList.get(i);
            }
            ((AnimClip) anim).setTracks(tracks);
        }

        clear(); // just to be safe
    }

    /**
     * Reset the list of temporary tracks.
     */
    static void clear() {
        trackList.clear();
    }
}
