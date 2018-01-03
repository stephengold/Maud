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
import jme3utilities.MyString;
import maud.Maud;
import maud.model.option.ViewMode;

/**
 * View menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ViewMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ViewMenus.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private ViewMenus() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Handle a "select menuItem" action from the View menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean menuView(String remainder) {
        boolean handled = true;
        String modePrefix = "Mode" + EditorMenus.menuPathSeparator;
        String scenesPrefix = "Scene options" + EditorMenus.menuPathSeparator;
        String scoresPrefix = "Score options" + EditorMenus.menuPathSeparator;
        if (remainder.startsWith(modePrefix)) {
            String arg = MyString.remainder(remainder, modePrefix);
            menuViewMode(arg);
            handled = true;

        } else if (remainder.startsWith(scenesPrefix)) {
            String arg = MyString.remainder(remainder, scenesPrefix);
            handled = menuSceneView(arg);

        } else if (remainder.startsWith(scoresPrefix)) {
            String arg = MyString.remainder(remainder, scoresPrefix);
            handled = menuScoreView(arg);

        } else {
            switch (remainder) {
                case "Mode":
                    EnumMenus.selectViewMode();
                    break;
                case "Scene options":
                    ShowMenus.sceneViewOptions();
                    break;
                case "Score options":
                    ShowMenus.scoreViewOptions();
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Handle a "select menuItem" action from the "View -> Scene options" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuSceneView(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "Axes":
                Maud.gui.tools.select("axes");
                break;
            case "Bounds":
                Maud.gui.tools.select("bounds");
                break;
            case "Camera":
                Maud.gui.tools.select("camera");
                break;
            case "Cursor":
                Maud.gui.tools.select("cursor");
                break;
            case "Mode":
                EnumMenus.selectViewMode();
                break;
            case "Platform":
                Maud.gui.tools.select("platform");
                break;
            case "Render":
                Maud.gui.tools.select("render");
                break;
            case "Skeleton":
                Maud.gui.tools.select("skeleton");
                break;
            case "Skeleton color":
                Maud.gui.tools.select("skeletonColor");
                break;
            case "Sky":
                Maud.gui.tools.select("sky");
                break;
            case "Vertex":
                Maud.gui.tools.select("sceneVertex");
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "View -> Score options" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuScoreView(String remainder) {
        boolean handled = true;
        switch (remainder) {
            case "Background":
                Maud.gui.tools.select("background");
                break;
            case "Tool":
                Maud.gui.tools.select("score");
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "View -> Mode" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static void menuViewMode(String remainder) {
        assert remainder != null;

        ViewMode viewMode = ViewMode.valueOf(remainder);
        Maud.getModel().getMisc().setViewMode(viewMode);
    }
}
