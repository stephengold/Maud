/*
 Copyright (c) 2017-2019, Stephen Gold
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
import maud.view.ViewType;

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
     * Build a View menu.
     *
     * @param builder the menu builder to use (not null, modified)
     */
    static void buildViewMenu(MenuBuilder builder) {
        builder.addSubmenu("Select mode");

        ViewMode viewMode = Maud.getModel().getMisc().viewMode();
        if (viewMode.equals(ViewMode.Scene)
                || viewMode.equals(ViewMode.Hybrid)) {
            builder.addSubmenu("Scene options");
        }

        if (viewMode.equals(ViewMode.Score)
                || viewMode.equals(ViewMode.Hybrid)) {
            builder.addSubmenu("Score options");
        }
    }

    /**
     * Handle a "select menuItem" action from the View menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean menuView(String remainder) {
        boolean handled = true;
        String modePrefix = "Select mode" + EditorMenus.menuPathSeparator;
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
                case "Select mode":
                    EnumMenus.selectViewMode();
                    break;
                case "Scene options":
                    sceneViewOptions();
                    break;
                case "Score options":
                    scoreViewOptions();
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Display an options menu for the view where the mouse is located.
     */
    public static void pick() {
        ViewType type = Maud.gui.mouseViewType();
        if (type == ViewType.Scene) {
            sceneViewOptions();
        } else if (type == ViewType.Score) {
            scoreViewOptions();
        }
    }

    /**
     * Display a "View -&gt; Scene options" menu.
     */
    public static void sceneViewOptions() {
        MenuBuilder builder = new MenuBuilder();

        builder.addTool("Axes");
        builder.addTool("Background");
        builder.addTool("Bounds");
        builder.addTool("Camera");
        builder.addTool("Cursor");
        builder.addTool("Lighting");
        builder.addTool("Physics");
        builder.addTool("Platform");
        builder.addTool("Render");
        builder.addTool("Skeleton");
        builder.addTool("Sky");

        builder.show("select menuItem View -> Scene options -> ");
    }

    /**
     * Display a "View -&gt; Score options" menu.
     */
    public static void scoreViewOptions() {
        MenuBuilder builder = new MenuBuilder();

        builder.addTool("Tool");
        builder.addTool("Background");

        builder.show("select menuItem View -> Score options -> ");
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
            case "Background":
                Maud.gui.tools.select("background");
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
            case "Lighting":
                Maud.gui.tools.select("sceneLighting");
                break;
            case "Physics":
                Maud.gui.tools.select("physics");
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
            case "Sky":
                Maud.gui.tools.select("sky");
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
     * Handle a "select menuItem" action from the "View -> Select mode" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static void menuViewMode(String remainder) {
        assert remainder != null;

        ViewMode viewMode = ViewMode.valueOf(remainder);
        Maud.getModel().getMisc().selectViewMode(viewMode);
    }
}
