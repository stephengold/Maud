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
package maud.view;

import java.util.logging.Logger;
import maud.Maud;
import maud.model.Cgm;

/**
 * Drag state for score views. Currently the only score objects that can be
 * dragged are gnomons.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScoreDrag {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ScoreDrag.class.getName());
    // *************************************************************************
    // fields

    /**
     * which gnomon is being dragged ("none", "source", or "target")
     */
    private static String dragGnomon = "none";
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private ScoreDrag() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the CG model whose gnomon is being dragged.
     *
     * @return a CG model, or null
     */
    public static Cgm getDraggingGnomonCgm() {
        Cgm result;
        switch (dragGnomon) {
            case "none":
                result = null;
                break;
            case "source":
                result = Maud.getModel().getSource();
                break;
            case "target":
                result = Maud.getModel().getTarget();
                break;
            default:
                throw new IllegalStateException();
        }
        return result;
    }

    /**
     * Alter which gnomon is being dragged.
     *
     * @param cgm a CG model, or null
     */
    public static void setDraggingGnomon(Cgm cgm) {
        if (cgm == Maud.getModel().getTarget()) {
            dragGnomon = "target";
        } else if (cgm == Maud.getModel().getSource()) {
            dragGnomon = "source";
        } else if (cgm == null) {
            dragGnomon = "none";
        } else {
            throw new IllegalArgumentException();
        }
    }
}
