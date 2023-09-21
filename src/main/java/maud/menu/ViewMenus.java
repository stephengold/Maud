/*
 Copyright (c) 2017-2022, Stephen Gold
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
import maud.tool.EditorTools;
import maud.view.ViewType;
import maud.view.scene.SceneView;

/**
 * View menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class ViewMenus {
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
        if (viewMode == ViewMode.Scene
                || viewMode == ViewMode.Hybrid) {
            builder.addSubmenu("Scene options");
        }

        if (viewMode == ViewMode.Score || viewMode == ViewMode.Hybrid) {
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
        builder.add("Clear rotation");
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
                EditorTools.select("axes");
                break;
            case "Background":
                EditorTools.select("background");
                break;
            case "Bounds":
                EditorTools.select("bounds");
                break;
            case "Camera":
                EditorTools.select("camera");
                break;
            case "Clear rotation":
                SceneView view = Maud.getModel().getTarget().getSceneView();
                view.getTransform().setYAngle(0f);
                break;
            case "Cursor":
                EditorTools.select("cursor");
                break;
            case "Lighting":
                EditorTools.select("sceneLighting");
                break;
            case "Physics":
                EditorTools.select("physics");
                break;
            case "Platform":
                EditorTools.select("platform");
                break;
            case "Render":
                EditorTools.select("render");
                break;
            case "Skeleton":
                EditorTools.select("skeleton");
                break;
            case "Sky":
                EditorTools.select("sky");
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
                EditorTools.select("background");
                break;
            case "Tool":
                EditorTools.select("score");
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
     */
    private static void menuViewMode(String remainder) {
        assert remainder != null;

        ViewMode viewMode = ViewMode.valueOf(remainder);
        Maud.getModel().getMisc().selectViewMode(viewMode);
    }
}
