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
package maud;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import maud.model.DddModel;

/**
 * A checkpoint in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class Checkpoint {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Checkpoint.class.getName());
    // *************************************************************************
    // fields

    /**
     * the date and time of creation
     */
    final private Date timestamp;
    /**
     * a copy of the MVC model at time of creation
     */
    final private DddModel model;
    /**
     * list of events since the previous checkpoint
     */
    final private List<String> eventDescriptions = new ArrayList<>(20);
    /**
     * a copy of the view's CG model at time of creation
     */
    final private ViewCGModel viewCgm;
    // *************************************************************************
    // constructors

    /**
     * Create a new checkpoint based on the application's live state.
     *
     * @param descriptions events since the previous checkpoint (not null,
     * unaffected)
     */
    Checkpoint(List<String> descriptions) {
        timestamp = new Date();
        model = new DddModel(Maud.model);
        eventDescriptions.addAll(descriptions);
        viewCgm = Maud.viewState.createCopy();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the timestamp of this checkpoint.
     *
     * @return a new instance
     */
    Date copyTimestamp() {
        Date result = (Date) timestamp.clone();
        return result;
    }

    /**
     * List all events since the previous checkpoint.
     *
     * @return a new list of descriptions
     */
    List<String> listEvents() {
        int numEvents = eventDescriptions.size();
        List<String> result = new ArrayList<>(numEvents);
        result.addAll(eventDescriptions);

        return result;
    }

    /**
     * Copy this checkpoint to the application's live state.
     */
    void restore() {
        Maud.model = new DddModel(model);
        Maud.viewState.restore(viewCgm);
    }
}
