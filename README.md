<img height="150" src="https://i.imgur.com/beTKjwO.png">

Maud is an editor for the animated 3-D models used with the
[jMonkeyEngine (jME)][jme] game engine.

Anticipated uses:

 + develop animations from motion-capture data
 + copy/retarget animations between models
 + convert models in other formats to native J3O format
 + troubleshoot issues with models (or with the model-asset pipeline)

Summary of features:

 + load models from local filesystems, JAR/ZIP archives, or HTTP servers
 + merge models
 + import models from [Blender][]/[glTF][]/[Ogre][]/[Wavefront][obj]/[Xbuf][] and save to native J3O format
 + import animations from [Biovision Hierarchy (BVH)](#bvh) files
 + visualize animations, axes, bones, bounding boxes, lights,
   mesh vertices, physics objects, and skeletons
 + browse animations, bones, keyframes, lights, material parameters,
   material-parameter overrides, mesh vertices, physics objects,
   physics shapes, scene-graph controls, spatials, tracks, and user data
 + play animations forward/backward at various speeds and pause them
 + create new animations from poses or by altering/mixing existing animations
 + retarget bone animations from one model to another using skeleton maps
 + create new attachments nodes, lights, physics controls, scene-graph controls,
   scene-graph nodes, and user data
 + insert keyframes into animations and bone tracks
 + rename animations, bones, lights, spatials, and user data
 + change animation speeds/durations
 + behead/mix/truncate animations
 + reduce/resample/wrap animations and tracks
 + translate animations for support or traction
 + delete animations, keyframes, lights, material parameters,
   material-parameter overrides, scene-graph controls, spatials, tracks,
   and user data
 + modify bone/spatial transforms (translation, rotation, and scale)
 + modify batch hints, cull hints, lights, material parameters,
   material-parameter overrides, render-queue buckets, shadow modes, and user data
 + review an unlimited edit history and undo/redo edits
 + customizable mouse-button assignments and keyboard shortcuts
 + complete Java source code provided under a [BSD 3-Clause License][bsd3]

Maud was designed for a desktop environment with:

 + a wheel mouse and
 + a display at least 640 pixels wide and 480 pixels tall.

Status as of February 2019: seeking more alpha testers.

## Contents of this document

 + [How to download and run a pre-built release of Maud](#prebuilt)
 + [How to build Maud from source](#build)
 + [Using Maud](#use)
 + [3-D Models](#cgms)
 + [Bones](#bones)
 + [Animations](#animations)
 + [The displayed pose](#pose)
 + [The skeleton map](#map)
 + [The edit history](#history)
 + [Scripting](#scripting)
 + [Command-line arguments](#args)
 + [External links](#links)
 + [Wish list](#wishlist)
 + [Conventions](#conventions)
 + [History](#history)
 + [Acknowledgments](#acks)

<a name="prebuilt"/>

## How to download and run a pre-built release of Maud

1. Find the desired release in the webpage at
   https://github.com/stephengold/Maud/releases
2. Download the ZIP file for your platform.
   (For a 64-bit Windows platform, download "Maud-Windows-x64.zip".)
3. Extract the contents of the ZIP file.
4. Open/run the Maud executable/application in the extracted folder/directory.
   (In Windows, open "Maud.exe" in the "Maud" folder.)

<a name="build"/>

## How to build Maud from source

Maud currently targets Version 3.2.2 of jME.  You are welcome to
use jME without installing its Integrated Development Environment (IDE),
but I use the IDE, so I tend to assume you will too.

### In the IDE

#### IDE setup

If you already have the IDE installed, skip to step 6.

The hardware and software requirements of the IDE are documented at
https://jmonkeyengine.github.io/wiki/jme3/requirements.html

 1. Download a jMonkeyEngine v3.2.1-stable-sdk3 installer
    from https://github.com/jMonkeyEngine/sdk/releases
 2. Install the Software Development Kit (SDK), which includes:
    + the engine,
    + the IDE, which is based on [NetBeans][],
    + various IDE plugins, and
    + the [Blender 3D][blender] application.
 3. When you execute the installer, it prompts you to
    specify a folder for storing projects:
    + Fill in the "Folder name" text box.
    + Click on the "Set Project Folder" button.
 4. Open the IDE.
 5. The first time you open the IDE, you should update
    all the pre-installed plugins:
    + Menu bar -> "Tools" -> "Plugins" to open the "Plugins" dialog.
    + Click on the "Update" button to open the "Plugin Installer" wizard.
    + Click on the "Next >" button.
    + After the plugins have downloaded, click "Finish".
    + The IDE will restart.
 6. In order to open the Maud project in the IDE (or in NetBeans),
    you will need to install the "Gradle Support" plugin:
    + Menu bar -> "Tools" -> "Plugins" to open the "Plugins" dialog.
    + Click on the "Available Plugins" tab.
    + Check the box next to "Gradle Support" in the "Gradle" category.
     If this plugin isn't shown in the IDE's "Plugins" tool,
     you can download it from
     [GitHub](https://github.com/kelemen/netbeans-gradle-project/releases).
    + Click on the "Install" button to open the the "Plugin Installer" wizard.
    + Click on the "Next >" button.
    + Check the box next to
     "I accept the terms in all the license agreements."
    + Click on the "Install" button.
    + When the "Verify Certificate" dialog appears,
     click on the "Continue" button.
    + Click on the "Finish" button.
    + The IDE will restart.

#### Source files

Clone the Maud repository using Git:

 1. Open the "Clone Repository" wizard in the IDE, either:
     + Menu bar -> "Team" -> "Git" -> "Clone..." or
     + Menu bar -> "Team" -> "Remote" -> "Clone..."
 2. For "Repository URL:" specify
    `https://github.com/stephengold/Maud.git`
 3. Clear the "User:" and "Password:" text boxes.
 4. For "Clone into:" specify a writable folder (on a local filesystem)
    that doesn't already contain "Maud".
 5. Click on the "Next >" button.
 6. Make sure the "master" remote branch is checked.
 7. Click on the "Next >" button again.
 8. Make sure the Checkout Branch is set to "master".
 9. Make sure the "Scan for NetBeans Projects after Clone" box is checked.
10. Click on the "Finish" button.
11. When the "Clone Completed" dialog appears, click on the "Open Project..."
    button.

#### Build the project

 1. In the "Projects" window of the IDE,
    right-click on the "Maud" project to select it.
 2. Select "Build".

### In a command-line environment

 1. Set your `JAVA_HOME` environment variable.
 2. Set your working directory to a writable directory (on a local filesystem)
    that doesn't already contain "Maud".
 3. `git clone https://github.com/stephengold/Maud.git`
 4. `cd Maud`
 5. Ensure that the `gradlew` script is executable: `chmod +x gradlew`
 6. `./gradlew build`

<a name="use"/>

## Using Maud

### Overview of the user interface

Maud's user interface consists of 4 screens:
"Start", "Editor", "Display Settings", and "Bind".
The Start Screen loads first. It merely displays the Maud logo while
initialization completes.  It should automatically transition to the Editor
Screen after a few seconds.

The Editor Screen is Maud's main screen, where 3-D models are visualized, browsed,
and edited.  There's a menu bar across the top and a message bar across the bottom.
The rest of its user interface
is split into overlapping sub-windows called "tools".

![screenshot](https://i.imgur.com/1GRhpOB.png "Editor Screen with 3 tools visible and a menu active")

#### Tools

At last count, Maud had 50 tools.
Each tool can be independently hidden or made visible.

Selecting a tool makes it visible and moves it to the top layer for convenient
use, but you can use a tool without it being on top.
The controls in a tool work even when partly obscured by other tools.
You can move a tool by dragging its title bar with the left mouse button (LMB).

If a tool gets in your way, you can hide it by clicking on
the X in its upper right corner with the LMB.
Hiding a tool won't affect anything else, so it's always a safe move.

#### Menus

Clicking on the menu bar with the LMB activates the corresponding menu.
When a menu is active, the remainder of the screen darkens,
and the mouse cursor becomes a right-pointing magenta arrow.
You can then select a menu item by clicking on it with the LMB or RMB.

Some menu items activate submenus.  Others display icons to help describe
what they do:

 + a wrench icon to select a tool
 + a dialog-box icon to open a modal dialog box
 + a bone icon to select a bone
 + a pencil icon to immediately edit the loaded map or target model
 + and so forth.

Menus are context-sensitive, so for instance you'll see many more options
in the Animation menu when a real animation is loaded than when the model is
in its bind pose.

The first 10 items in each menu are numbered.
For instance, the first item in the menu bar is
labeled "1] View".  The numbers indicate keyboard shortcuts for
navigating menus.  When no popups are open,
you can select the View Menu by pressing
the "1" key on the main keyboard (but NOT the "1" key on the numeric keypad).

#### Keyboard shortcuts

Keyboard shortcuts in the Editor Screen include:

 + "." to pause/restart the loaded animation(s)
 + "Esc" to exit from the active menu (or if there's no active menu, to exit from Maud)
 + "H" to toggle the visibility of the menu bar and the status bar
 + "X" to create a checkpoint
 + "Z" to undo to the previous checkpoint
 + "Y" to redo to the next checkpoint
 + "F1" to switch to the Bind screen

Mouse-button assignments and keyboard shortcuts can be customized using the
Bind Screen (or by editing the "Interface/bindings/editor.properties" asset
prior to startup)
in which case shortcuts described in this document might not work.

### Views and models in the Editor Screen

At startup, the Editor Screen displays a "scene" view of a single 3-D model:
Jaime, from the jme3-testdata library, with no tools selected.

![screenshot](https://i.imgur.com/vaKixFI.png "Editor Screen at startup")

Startup actions can be customized using the "Settings -> Update startup script"
menu item (or by editing the "Scripts/startup.js" asset prior to startup)
in which case Maud's initial state might differ from that described here.

A scene view consists of a 3-D render of a loaded model, possibly with a
background, a 3-D cursor, a supporting platform, and/or overlaid visualizations.
Visualizations can include axes, a bounding box, a mesh vertex, physics objects, and/or a
skeleton.  If you load and play an animation in a scene view, you'll see the
model move, rather like it would in a game.

When the mouse cursor is in a scene view, you can use the "A" and "D" keys
to rotate the model left and right.  (These keys don't alter the model itself,
only its orientation in the view.)  You can also press:

 + "B" to select the bone closest to the mouse pointer
 + "E" to deselect the selected bone
 + "Q" to open the "View -> Scene options" menu
 + "V" to select the mesh vertex closest to the mouse pointer
 + "W" to deselect the selected mesh vertex

The Editor Screen can also display "score" views of loaded animations.
A score view is a schematic diagram, like a musical score, with bones arranged
vertically and time (indicated by a gnomon) progressing from left to right.
When the mouse cursor is in a score view, the following keyboard
shortcuts may prove helpful:

 + "B" to select the bone track closest to the mouse pointer
 + "G" to grab the gnomon and (while key is held down) drag it with the mouse pointer
 + "K" to select the keyframe closest to the mouse pointer
 + "Q" to open the "View -> Score options" menu

While Maud can only *edit* one model at a time, the Editor Screen can be split
to *display* 2 different models.
(This is useful when merging models or
retargeting animations from one model to another.)
The model being edited is called the "target" model.
The other model is called the "source" model.

The Editor Screen can also be split to display 2 views of the same model.

When the editor screen is split, you can adjust the horizontal position of the
boundary by dragging it with the RMB.

### Views modes of the Editor Screen

The Editor Screen operates in 3 "view modes":
"Scene Mode", "Score Mode", and "Hybrid Mode".
You can change the view mode using the "View -> Select mode" submenu or
use the backtick ("`") keyboard shortcut to cycle through these modes.

<table>
    <tr>
        <th></th>
        <th>With no source model loaded:</th>
        <th>With a source model loaded:</th>
    </tr>
    <tr>
        <td>In Scene Mode...</td>
        <td style="border: 1px solid black;">A full-width scene view of the target model</td>
        <td style="border: 1px solid black;">A split display with a scene view of the source model on the left
            and a scene view of the target model on the right</td>
    </tr>
    <tr>
        <td>In Score Mode...</td>
        <td style="border: 1px solid black;">A full-width score view of the target model</td>
        <td style="border: 1px solid black;">A split display with a score view of the source model on the left
            and a score view of the target model on the right</td>
    </tr>
    <tr>
        <td>In Hybrid Mode...</td>
        <td colspan="2" style="border: 1px solid black;">A split display with a score view of the target model on the left
            and a scene view of the target model on the right</td>
    </tr>
</table>

### Maud's cameras

Each view has its own camera.
Generally, the mouse wheel and middle mouse button (MMB) control Maud's cameras.
Rotate the mouse wheel to move a camera forward or backward.
In scene views, drag with MMB to turn the camera.
In score views, drag up/down with MMB to scroll the view down/up.

#### Scene-view cameras

Beyond that, it gets complicated, since scene-view cameras operate in
2 "movement modes" and 2 "projection modes".
The "Camera Tool" (selected using "View -> Scene options -> Camera")
is used to select these modes.

"Orbit Mode" is a scene-view camera's default movement mode.
In orbit mode, the camera orbits a central point.
In orbit mode, turning the camera also changes its location, making it easy to
view models from many directions.
By the default, the central point is the "3-D cursor" -- a user-selected
location, typically visible as a small, black/yellow, 6-pointed star
at the center of the view.

Move the 3-D cursor to a new location by clicking LMB on an object in the view:
either the model or the platform.
The 3-D cursor doesn't attach to other objects, so moving or altering objects
in the scene won't affect the location of the 3-D cursor.
The "Cursor Tool" (selected using "View -> Scene options -> Cursor")
can alter the appearance of the 3-D cursor.
The "Camera Tool" (selected using "View -> Scene options -> Camera")
can be used to redefine what the camera orbits.

"Fly Mode" is a scene-view camera's alternative movement mode.
In fly mode, the camera disregards the central point.
This is useful for close-up viewing of places the 3-D cursor can't easily reach,
such as the interior of a model.

"Perspective Mode" is a scene-view camera's default projection mode,
and "Parallel Mode" is the alternative.
In Parallel Mode, the mouse wheel
alters the scale of the projection without actually moving the camera.

Standard keyboard shortcuts affecting the scene-view cameras:

 + "Numpad-0" to toggle between movement modes
 + "Numpad-1" to move (or rotate) a camera to a horizontal view
 + "Numpad-5" to toggle between projection modes

#### Score-view cameras

Score views don't have anything analogous to the 3-D cursor, but you can
move the score-view camera up/down by clicking LMB where you want it to look.

<a name="cgms"/>

## 3-D models

As mentioned above, Maud always has a (target) model loaded,
and an additional model (the source model) can also be loaded.
The source model can't be modified; only the target can be modified.

The "Model Tool" (selected using "Models -> Tool") displays basic
information about the target model.

### Loading (or importing) models from assets

Models are loaded from assets, which may be located in the
Java classpath (built into Maud) or in a local filesystem or on a web server.

Before loading a model from a local filesystem, you must specify
a where in the filesystem the model's assets are located:
select "Settings -> Add asset location",
then navigate to the asset folder (typically it contains a "Model" subfolder),
and select "! add this folder".
Or if the assets are in a JAR/ZIP file, simply select that file.

To load a model as the target model, select the "Models -> Load" menu item,
then select an asset location, then navigate to the model file.
To load a model as the source model, select "Models -> Source model -> Load",
then select an asset location, then navigate to the model file.

Maud can of course load models in jME's native binary format.
To be recognized, such models must have filenames ending in ".j3o".

Maud can also import models in other formats:

 + [Blender 3D][blender] (filename must end in ".blend")
 + [glTF][] (filename must end in ".glb" or ".gltf")
 + [Ogre][] DotScene (filename must end in ".scene")
 + [Ogre][] XML (filename must end in ".mesh.xml")
 + [Wavefront OBJ][obj] (filename must end in ".obj")
 + [Xbuf][] (filename must end in ".xbuf")

In addition, Biovision Hierarchy animations can be imported as models.
To be recognized, such animations must have filenames ending in ".bvh".
The imported model will consist of a single scene-graph node without any geometries.

### Saving the target model

To write the target model to a file in jME's native binary format,
open the save dialog: "Models -> Save".
Modify the base file path as desired, and click on the "Save" button.
The suffix ".j3o" is automatically appended to the base file path.
Note that materials and textures are not saved.

By default, models loaded from the classpath or from an archive will be written to a
"Written Assets" folder under Maud's working folder.
When loading assets, Maud treats this folder as if it overrides the classpath.

<a name="bones"/>

## Bones

In jME, "bones" are named parts of a 3-D model that can influence
vertices in the model's meshes.
A single vertex can be influenced by up to 4 bones.
A bone can also influence other bones, called its "children".
A bone with no children is a "leaf" bone.
A bone with no parent is a "root" bone.

Each bone has a "head" around which it pivots.
However a jME bone need not have a well-defined "tail", a length,
nor even a direction.

In a scene view, Maud visualizes each bone as a round dot
(red or green by default)
connected to its children by lines (white by default).
(You can customize these colors using the "Skeleton Tool":
"View -> Scene options -> Skeleton".)

In a score view, each bone is represented by horizontal "staff".
If space permits, the staff includes a rectangular name label on the left.
If the bone is tracked (more about that later) the staff also includes
up to 10 stacked "sparklines" bracketed by a pair of "finials".
The sparklines, rendered in 4 colors, represent animation data,
and the finials help identify which sparkline is which.

![screenshot](https://i.imgur.com/nnXqZAI.png "a detailed staff in a score view")

Before editing a bone in Maud, you must "select" it.
In a scene view, the selected bone (if any) is typically indicated by
3 arrows, denoting the axes of its local coordinate axes.

The "Bone Tool" (selected using "Bone -> Tool") controls and describes
the target model's selected bone.

### Selecting bones

Maud provides many ways to select a bone.

The "Bone -> Select" submenu enables you to select a bone in the
target model by name:

 + from among all bones in the selected skeleton, or
 + from among the root bones in the selected skeleton, or
 + from among all bones with attachments nodes, or
 + from among all bones with tracks in the loaded animation, or
 + from among the children of the selected bone.

It also enables you to navigate the bone hierarchy "By parent"
or cycle through bones in numeric order ("Previous" and "Next").

The Bone Tool ("Bone -> Tool") provides a more convenient interface to these same
selection options.

The quickest way to select a bone is to click the right mouse
button (RMB) on or near it in a scene view.
This works for both the source model and the target model.
However, since bones can appear close together in scene views,
and since the RMB is also used to select objects other than bones,
use this technique with caution.

<a name="animations"/>

## Animations

In jME, "animations" are usually found as part of a 3-D model.
Each animation has a name and a duration.

Maud treats the model's bind pose like a zero-duration
animation for many purposes.

A real animation is stored in an `AnimControl` and composed of "tracks", usually bone tracks.
Each bone track transforms a single bone.
An animation need not include a track for every bone.
Maud refers to bones that have tracks in the loaded animation
as "tracked bones".

Each track is composed of series of "keyframes", starting at time zero.

The (Target) Animation Tool (selected using "Animations -> Tool")
controls the target model's loaded animation.
There's also a Source Animation Tool ("Animations -> Source tool")
to control the source model's loaded animation.

### Loading animations

In Maud, "loading" an animation means selecting it
for visualization, playback, and/or editing.

To load an animation for the target model,
select "Animations -> Load" and then the name of the animation.
To load an animation for the source model,
select "Animations -> Load source" and then the name of the animation.

### Playing, pausing, and pinning animations

Once an animation is loaded, Maud can play it forward or backward at speeds
of up to 2x.
Each animation tool has a slider to control the speed and direction
of playback.

Each animation tool also has a button to pause/resume the loaded animation.
As mentioned above, you can also use the "." keyboard shortcut
to pause/resume loaded animations.

By default, Maud "loops" to the start (or end) of the loaded animation
when playback reaches the animation's end (or start).
Using the animation tools, you can instruct Maud to pause and/or reverse
direction ("pong") instead.

Using the animation tools, you can also "pin" a loaded animation.
Pinning an animation forces all its root bones to the model origin
for display (scene-view) purposes.

<a name="pose"/>

## The displayed pose

While an animation is playing on the target model, Maud typically refreshes
the model's bone transforms on every frame.
Once the animation is paused, however, the target model's bone transforms
(called the "displayed pose") can be modified independently
of the loaded animation.

Use the "Bone-Translation Tool" ("Bone -> Translate") to modify the local
translation of the selected bone in the displayed pose.
Use the "Bone-Scale Tool" ("Bone -> Scale") to scale
the selected bone in the displayed pose.
And use the "Bone-Rotation Tool" ("Bone -> Rotate") to rotate
the selected bone in the displayed pose.

In scene views, you can also rotate the selected bone by grabbing
any of the bone's 3 axis tips with the RMB and dragging with the mouse.
The axis tips are constrained to an invisible sphere surrounding the
selected bone's head, so for many mouse-pointer screen locations,
2 axis directions are possible.
While you're dragging a bone axis, Maud remembers whether the axis
inclines toward the camera or away from it.
You can toggle this inclination using the "S" shortcut key.

The displayed pose has many uses.  For instance:
To save a pose (creating a new, zero-length animation from it)
select "Animations -> Add new -> Pose".
To insert keyframes (and replace any pre-existing keyframes)
at the current time in the loaded animation,
select "Animations -> Edit -> Insert keyframes".
To insert a keyframe (or replace the pre-existing keyframe)
at the current time in the selected bone track,
select "Keyframe -> Insert from pose".
To alter all keyframes in the selected bone
track, use the "Set all to pose" buttons in the "Keyframe Tool"
("Keyframe -> Tool").

You can also use the displayed pose like a paste buffer, to copy bone transforms
from one time to another.
For this to work, you must "freeze" the pose so Maud won't
overwrite it as soon as the animation time changes.
To freeze the pose, either tick the "freeze" check box in the Animation Tool
or use the "F" keyboard shortcut.
Then go to the animation time when you want to paste and select
"Keyframe -> Insert from pose" (to paste to the selected bone track)
or "Animations -> Edit -> Insert keyframes" (to paste to all bone tracks in the loaded animation).
Remember to unfreeze the pose afterward!

<a name="map"/>

## The skeleton map

Once an animation is created for one model, it's a simple matter to
copy it to another model, provided both models have identical
skeletons.

To retarget animations between models with different skeletons, Maud
requires a "skeleton map" to match up bones in the source model with
corresponding ones in the target model.
(Corresponding bones may have different names and/or indices.)

Furthermore, since jME bones don't have defined tails,
it's often necessary to adjust bone orientations when retargeting
animations between models.
To automate these adjustments, each bone mapping in a jME skeleton
map includes a "twist" rotation value.

Maud can load skeleton maps from assets, edit
them, save them, and use them to retarget animations.
A few sample skeleton maps are build into Maud, such as "SinbadToJaime" which
maps Sinbad's skeleton to that of Jaime.

Use the "Mapping Tool" ("Map -> Tool") to load and edit skeleton maps.
To do anything with a skeleton map, you must have a source model loaded.
To add a mapping between 2 bones, select the source bone in the source
model and the target bone in the target model, then click LMB on
the "Map" button in the lower left or use the equals ("=") keyboard shortcut.

As long as the 2 selected bones map to each other in the loaded map,
the corresponding bone mapping stays selected.
You can unmap the selected bone mapping (with the "Unmap" button) or
adjust its twist value with the "Twist Tool" ("Map -> Twist tool").

When editing a skeleton map, it's helpful to see
its effect on the target model in real time.
To do this, click LMB on the "Show retargeted pose" button in the Mapping Tool or
select "Animations -> Load -> ( retargeted pose )".
With this pseudo-animation loaded:

 + changing the source pose automatically updates the target pose,
 + changing the skeleton map automatically updates the target pose,
 + selecting a mapped source bone also selects the corresponding target bone,
 + selecting a mapped target bone also selects the corresponding source bone, and
 + dragging an axis of the target bone automatically updates the twist value of the selected bone mapping.

To save the loaded skeleton map, select "Map -> Save".

When you're ready to retarget animations between models,
use the "Retarget Tool" ("Animations -> Add new -> Retarget source animation").

<a name="history"/>

## The edit history

Like any serious editor, Maud provides an "undo" capability.

Maud's undo capability is based on checkpointing the editor's state.
Tool visibility and positioning are not checkpointed, nor are shortcut
key bindings, but nearly everything else is, including selections, view mode,
view options, and loaded models, maps, and animations.

By default, Maud creates a checkpoint before editing or unloading any map or model.
You can also create a checkpoint manually using the "X" keyboard shortcut.
To undo changes since the most recent checkpoint, use the "Z" keyboard shortcut.

To safeguard work since the last checkpoint,
the "Z" key often creates a new checkpoint as well.
To redo each change you've undone, use the "Y" keyboard shortcut.

Maud places no limit on the number of checkpoints you can create/undo/redo.
To help you visualize and navigate your edit history,
Maud provides a "History Tool" (selected using "History -> Tool").

The edit history occupies heap memory.
To release this memory, you can delete the history using "History -> Clear".

<a name="scripting"/>

## Scripting

Maud has a built-in JavaScript scripting capability based
on the [Nashorn][] engine.

During startup, Maud looks for a "/Scripts/startup.js" asset.
The built-in startup script (on the classpath) simply loads the Jaime model.
However, if Maud finds a custom script in the "Written Assets" folder,
it executes the custom script instead of the built-in one.

To auto-generate a custom startup script,
select "Settings -> Update startup script".
The generated script will initialize:

 + asset locations
 + global options such as view mode, boundary position, menu-bar visibility,
   performance debug mode, base index, and tweening techniques
 + most scene-view and score-view options
 + the screen positions of any selected tools

Note that customized hotkeys and display settings
are not included in the startup script.
Maud has other mechanisms to make them persist.

To remove a custom startup script,
select "Settings -> Revert startup script to default".

<a name="args"/>

## Command-line arguments

The following arguments can be specified on the command line (or in the Gradle
build script) to configure Maud prior to executing its startup script:

 + `--forceDialog` (or `-f`)
  to show the JME3 "Display Settings" dialog during startup.
  This dialog can be used to edit display properties (such as resolution)
  in case they become corrupted.  If this argument is not
  specified, Maud shows the dialog only if it believes
  (based on JME3 AppSettings) that this is the first time it has run.
 + `--skipStartup` (or `-s`)
  to bypass Maud's startup script.  If this argument is not specified, Maud
  looks for a startup script and attempts to execute it before displaying the
  Editor Screen.

<a name="links"/>

## External links

YouTube videos about Maud:

  + January 2018 demo (pick-and-drag editing):
    https://www.youtube.com/watch?v=OLLl7xiuCw0 (8:43)
  + December 2017 demo (importing animated models via glTF):
    https://www.youtube.com/watch?v=tv7QgAtwDCA (7:47)
  + September 2017 spatial animation clip:
    https://www.youtube.com/watch?v=EDtiYu-u_Ls (0:12)
  + September 2017 demo part 2:
    https://www.youtube.com/watch?v=2kmxOzDCl_8 (28:22)
  + September 2017 demo part 1:
    https://www.youtube.com/watch?v=4UwxbsOewow (15:02)
  + June 2017 retargeted animation clip:
    https://www.youtube.com/watch?v=yRjh1rAsipI (0:09)
  + May 2017 demo (out-of-date!):
    https://www.youtube.com/watch?v=fSjsbyBWlPk (9:13)

<a name="bvh"/>

BVH resources:

  + BVH format description:
    http://research.cs.wisc.edu/graphics/Courses/cs-838-1999/Jeff/BVH.html
  + motion-capture data from CMU's Graphics Lab, converted to BVH by Bruce Hahn:
    https://sites.google.com/a/cgspeed.com/cgspeed/motion-capture
  + free motion-capture data from Ohio State University's ACCAD:
    https://accad.osu.edu/research/motion-lab/system-data

[bican]: https://github.com/canbican "Can Bican"
[blender]: https://docs.blender.org "Blender Project"
[bsd3]: https://opensource.org/licenses/BSD-3-Clause "3-Clause BSD License"
[bullet]: https://pybullet.org/wordpress "Bullet Real-Time Physics Simulation"
[bvhretarget]: https://github.com/Nehon/bvhretarget "BVH Retarget Project"
[chrome]: https://www.google.com/chrome "Chrome"
[elements]: http://www.adobe.com/products/photoshop-elements.html "Photoshop Elements"
[findbugs]: http://findbugs.sourceforge.net "FindBugs Project"
[git]: https://git-scm.com "Git"
[github]: https://github.com "GitHub"
[gltf]: https://www.khronos.org/gltf "glTF Project"
[gradle]: https://gradle.org "Gradle Project"
[jfrog]: https://www.jfrog.com "JFrog"
[jme]: http://jmonkeyengine.org  "jMonkeyEngine Project"
[jme-ttf]: http://1337atr.weebly.com/jttf.html "jME-TTF Rendering System"
[makehuman]: http://www.makehumancommunity.org/ "MakeHuman Community"
[markdown]: https://daringfireball.net/projects/markdown "Markdown Project"
[minie]: https://github.com/stephengold/Minie "Minie Project"
[nashorn]: https://docs.oracle.com/javase/8/docs/technotes/guides/scripting/nashorn "Nashorn User Guide"
[netbeans]: https://netbeans.org "NetBeans Project"
[nifty]: http://nifty-gui.github.io/nifty-gui "Nifty GUI Project"
[obj]: http://www.fileformat.info/format/wavefrontobj/egff.htm "Wavefront OBJ File Format"
[obs]: https://obsproject.com "Open Broadcaster Software Project"
[ogre]: http://www.ogre3d.org "Ogre Project"
[profont]: http://tobiasjung.name/profont "ProFont Project"
[retarget]: https://github.com/Nehon/bvhretarget "BVH Retarget Project"
[rwce]: http://www.rw-designer.com/cursor-maker "RealWorld Cursor Editor"
[utilities]: https://github.com/stephengold/jme3-utilities "jME3 Utilities Project"
[vegdahl]: http://www.cessen.com "Nathan Vegdahl"
[winmerge]: http://winmerge.org "WinMerge Project"
[xbuf]: https://github.com/xbuf/xbuf "Xbuf Project"

<a name="wishlist"/>

## Wish list

Maud is incomplete.
The following features are on my "to do" list, in no particular order:

 + advance/delay keyframe(s)
 + better support for physics controls/joints/objects/shapes
 + smoother camera motion in score views
 + select bone mappings that don't correspond to the loaded models
 + export a model to [OBJ][] format
 + export an animation to [BVH](#bvh) format
 + localization
 + more scene-view options for platform
 + tool tips
 + mirror an animation/pose
 + joint-angle limits for models

<a name="conventions"/>

## Conventions

Maud's source code is compatible with JDK 7.

World coordinate system: the Y axis points upward (toward the zenith).
Prior to loading a model that uses the Z-up convention, select the "+Z up"
load orientation in the Settings Tool ("Settings" -> "Tool").

The first keyframe in each track must be at time=0.

<a name="history"/>

## History

Since April 2017, the Maud project has been hosted at
https://github.com/stephengold/Maud

Maud began as a demo application for the jme3-utilities-debug library,
part of the jme3-utilities project at
https://github.com/stephengold/jme3-utilities

Maud incorporates code from [jMonkeyEngine][jme] and the [BVH Retarget Project][bvhretarget].

<a name="acks"/>

## Acknowledgments

Like most projects, Maud builds on the work of those who went before.
I therefore acknowledge the following artists and software developers:

+ Rémy Bouquet (aka "nehon") for creating the Jaime model and the [BVH Retarget
  Project][retarget] and also for many helpful insights
+ Paul Speed (aka "pspeed") for many helpful insights
+ David Bernard (aka "david_bernard_31") for creating [Xbuf][]
+ Adam T. Ryder (aka "tryder") for creating [jME-TTF][]
+ Tobias Jung for creating [ProFont][]
+ [Nathan Vegdahl][vegdahl], for creating the Puppet model
+ [Can Bican][bican], for creating the Jprefctl preferences editor
+ Zi Ye, for creating the Sinbad model
+ the brave souls who volunteered to be alpha testers for Maud, including:
    + "Jesterrrrrr"
    + Moff Kalast
    + Adam (aka "safari")
+ plus the creators of (and contributors to) the following software:
    + [Adobe Photoshop Elements][elements]
    + the [Blender][] 3-D animation suite
    + the [Bullet][] real-time physics library
    + the [FindBugs][] source-code analyzer
    + the [Git][] revision-control system and GitK commit viewer
    + the [Google Chrome web browser][chrome]
    + the [Gradle][] build tool
    + the Java compiler, standard doclet, and runtime environment
    + [jMonkeyEngine][jme] and the jME3 Software Development Kit
    + LWJGL, the Lightweight Java Game Library
    + the [MakeHuman][] Community
    + the [Markdown][] document conversion tool
    + Microsoft Windows
    + the [NetBeans][] integrated development environment
    + the [Nifty][] graphical user interface library
    + [Open Broadcaster Software Studio][obs]
    + the PMD source-code analyzer
    + the [RealWorld Cursor Editor][rwce]
    + the [WinMerge][] differencing and merging tool

I am grateful to [JFrog][] and [Github][], for providing free hosting for the
Maud Project and many other open-source projects.

I'm also grateful to my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know so I can
correct the situation: sgold@sonic.net
