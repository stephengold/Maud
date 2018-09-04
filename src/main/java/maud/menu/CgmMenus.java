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
package maud.menu;

import java.util.logging.Logger;
import maud.Maud;
import maud.dialog.EditorDialogs;

/**
 * Menus in Maud's editor screen that deal with computer-graphics models.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class CgmMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(CgmMenus.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private CgmMenus() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Handle a "select menuItem" action from the CGM menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean menuCgm(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "History":
                Maud.gui.tools.select("history");
                break;

            case "Load":
                Maud.gui.buildMenus.loadCgm();
                break;

            case "Load source model":
                Maud.gui.buildMenus.loadSourceCgm();
                break;

            case "Merge source model":
                Maud.getModel().getTarget().getSpatial().attachClone();
                break;

            case "Save":
                EditorDialogs.saveCgm();
                break;

            case "Tool":
                Maud.gui.tools.select("cgm");
                break;

            case "Unload source model":
                Maud.getModel().getSource().unload();
                break;

            default:
                handled = false;
        }

        return handled;
    }
}
