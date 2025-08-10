<img height="150" src="https://i.imgur.com/beTKjwO.png" alt="Maud logo">

Maud is an editor for the animated 3-D models used with
[the jMonkeyEngine (JME) game engine][jme].

Anticipated uses:

 + customize models created by others
 + develop animations from motion-capture data
 + copy/retarget animations between models
 + convert models in other formats to native J3O format
 + troubleshoot issues with models (or with the model-asset pipeline)

Summary of features:

 + load models from local filesystems, JAR/ZIP archives, or HTTP servers
 + import models from [Blender]/[glTF]/[IQE]/[Ogre]/[Wavefront][obj]/[Xbuf] and save to native J3O format
 + import animations from [Biovision Hierarchy (BVH)](#bvh) files
 + visualize animations, axes, bones, bounding boxes, lights,
   mesh vertices, physics objects, and skeletons
 + merge models or geometries
 + split geometries
 + apply transforms to meshes or child spatials
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
 + review the edit history and undo/redo edits
 + customizable mouse-button assignments and keyboard shortcuts

Java source code is provided under
[a 3-clause BSD license][license].

Maud was designed for a desktop environment with:

 + a wheel mouse and
 + a display at least 640 pixels wide and 720 pixels tall.


<a name="toc"></a>

## Contents of this document

+ [How to download and run a pre-built release of Maud](#prebuilt)
+ [How to build and run Maud from source](#build)
+ [Using Maud](#use)
+ [3-D Models](#cgms)
+ [Bones](#bones)
+ [Animations](#animations)
+ [The displayed pose](#pose)
+ [The skeleton map](#map)
+ [The edit history](#edithistory)
+ [Scripting](#scripting)
+ [Command-line arguments](#args)
+ [External links](#links)
+ [Wish list](#wishlist)
+ [Conventions](#conventions)
+ [History](#history)
+ [Acknowledgments](#acks)


<a name="prebuilt"></a>

## How to download and run a pre-built release of Maud

1. Install a 64-bit [Java], if you don't already have one.
   (Maud no longer supports 32-bit Java.)
2. Point the `JAVA_HOME` environment variable to your Java installation.
   (In other words, set it to the path of a directory/folder
   containing a "bin" that contains a Java executable.
   The path might be something like "C:\Program Files\Java\jre1.8.0_301"
   or "/usr/lib/jvm/java-11-openjdk-amd64/" .)
  + using Bash or Zsh: `export JAVA_HOME="` *path to installation* `"`
  + using [Fish]: `set -g JAVA_HOME "` *path to installation* `"`
  + using Windows Command Prompt: `set JAVA_HOME="` *path to installation* `"`
  + using PowerShell: `$env:JAVA_HOME = '` *path to installation* `'`
3. Install the latest Maud release from GitHub:
  + Browse to [the latest release](https://github.com/stephengold/Maud/releases/latest)
  + Follow the "Maud.zip" link.
  + Save the ZIP file.
  + Extract the contents of the saved ZIP file.
4. `cd` to the extracted "Maud" directory/folder that contains "bin" and "lib".
5. Run the Maud startup script:
  + using Bash or Fish or Zsh: `./bin/Maud`
  + using Windows Command Prompt: `./bin/Maud.bat`
  + using PowerShell: `.\bin\Maud.bat`

[Jump to the table of contents](#toc)


<a name="build"></a>

## How to build and run Maud from source

1. Install a 64-bit [Java Development Kit (JDK)][adoptium],
   version 17 or higher,
   if you don't already have one.
2. Point the `JAVA_HOME` environment variable to your JDK installation:
   (In other words, set it to the path of a directory/folder
   containing a "bin" that contains a Java executable.
   That path might look something like
   "C:\Program Files\Eclipse Adoptium\jdk-17.0.3.7-hotspot"
   or "/usr/lib/jvm/java-17-openjdk-amd64/" or
   "/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home" .)
  + using Bash or Zsh: `export JAVA_HOME="` *path to installation* `"`
  + using [Fish]: `set -g JAVA_HOME "` *path to installation* `"`
  + using Windows Command Prompt: `set JAVA_HOME="` *path to installation* `"`
  + using PowerShell: `$env:JAVA_HOME = '` *path to installation* `'`
3. Download and extract the Maud source code from GitHub:
  + using Git:
    + `git clone https://github.com/stephengold/Maud.git`
    + `cd Maud`
    + `git checkout -b latest 1.0.0-beta2`
  + using a web browser:
    + browse to [the latest release](https://github.com/stephengold/Maud/releases/latest)
    + follow the "Source code (zip)" link
    + save the ZIP file
    + extract the contents of the saved ZIP file
    + `cd` to the extracted directory/folder
4. Run the [Gradle] wrapper:
  + using Bash or Fish or PowerShell or Zsh: `./gradlew build`
  + using Windows Command Prompt: `.\gradlew build`

After a successful build,
archives for distribution will be found in `build/distributions`.

You can run the local build using the Gradle wrapper:
+ using Bash or Fish or Zsh or PowerShell: `./gradlew run`
+ using Windows Command Prompt: `.\gradlew run`

You can restore the project to a pristine state:
+ using Bash or Fish or Zsh or PowerShell: `./gradlew clean cleanSandbox`
+ using Windows Command Prompt: `.\gradlew clean cleanSandbox`

[Jump to the table of contents](#toc)


<a name="use"></a>

## Using Maud

### Overview of the user interface

Maud's user interface consists of 4 screens:
"Start", "Editor", "Display Settings", and "Bind".
The Start Screen loads first. It merely displays the Maud logo while
initialization completes.  It should automatically transition to the Editor
Screen after a few seconds.

The Editor Screen is Maud's main screen, where 3-D models are visualized, browsed,
and edited.  There's a menu bar across the top and a message bar across the bottom.
Most of its user interface
is split into overlapping sub-windows called "tools".

![screenshot](https://i.imgur.com/1GRhpOB.png "Editor Screen with 3 tools visible and a menu active")

#### Tools

At last count, Maud had 52 tools.
Each tool can be independently hidden or made visible.

Selecting a tool makes it visible and moves it to the top layer for convenient
use, but you can use a tool without it being on top.
The controls in a tool work even when partly obscured by other tools.
You can move a tool by dragging its title bar with the left mouse button (LMB).

If a tool gets in your way, you can hide it by clicking on
the X in its upper right corner with the LMB.
Hiding a tool won't affect anything else, so it's always a safe move.

#### Menus

Clicking a button in the menu bar with the LMB activates the corresponding menu.
Menus are modal: activating a menu disables all other user interfaces in Maud.
When a menu is active, the remainder of the screen darkens,
and the mouse cursor becomes a right-pointing magenta arrow.
You can then select a menu item by clicking on it with the LMB or RMB.

Many menu items display icons to help indicate what they do:

 + a right-pointing black arrow to activate a submenu
 + a wrench icon to select a tool
 + a dialog-box icon to open a dialog box or go to a different screen
 + a pencil icon to immediately edit the loaded skeleton map or target model
 + a bone icon to select a bone
 + an ogre's head to immediately load an [Ogre] model
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

 + "." or "Pause" to pause/restart the loaded animation(s)
 + "Esc" to exit from the active menu (or if there's no active menu, to exit from Maud)
 + "H" to toggle the visibility of the menu bar and the status bar
 + "X" to create a checkpoint
 + "Z" to undo to the previous checkpoint
 + "Y" to redo to the next checkpoint
 + "F1" to switch to the Bind Screen

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

[Jump to the table of contents](#toc)

<a name="cgms"></a>

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
 + [glTF] (filename must end in ".glb" or ".gltf")
 + [Inter-Quake Export][iqe] (filename must end in ".iqe")
 + [Ogre] DotScene (filename must end in ".scene")
 + [Ogre] XML (filename must end in ".mesh.xml")
 + [Wavefront OBJ][obj] (filename must end in ".obj")
 + [Xbuf] (filename must end in ".xbuf")

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

[Jump to the table of contents](#toc)

<a name="bones"></a>

## Bones

In jME, "bones" are named parts of a 3-D model that can influence
vertices in the model's meshes.
A single vertex can be influenced by up to 4 bones.
A bone can also influence other bones, called its "children".
A bone with no children is a "leaf" bone.
A bone with no parent is a "root" bone.

Each bone has a "head" around which it pivots.
However, a jME bone need not have a well-defined "tail", a length,
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

[Jump to the table of contents](#toc)

<a name="animations"></a>

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

[Jump to the table of contents](#toc)

<a name="pose"></a>

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

[Jump to the table of contents](#toc)

<a name="map"></a>

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

[Jump to the table of contents](#toc)

<a name="edithistory"></a>

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

To help you visualize and navigate your edit history,
Maud provides a "History Tool" (selected using "History -> Tool").

The edit history occupies heap memory.
To release this memory, you can delete the history using "History -> Clear".

Maud places a limit on the number of checkpoints you can have at any one time.
The default limit is 8.
You can alter this limit using the History Tool.

[Jump to the table of contents](#toc)

<a name="scripting"></a>

## Scripting

Maud has a built-in JavaScript scripting capability based
on the [Nashorn] engine.

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

[Jump to the table of contents](#toc)

<a name="args"></a>

## Command-line arguments

The following arguments can be specified on the command line (or in the Gradle
build script) to configure Maud prior to executing its startup script:

 + `--forceDialog` (or `-f`)
  to show the JME3 "Display Settings" dialog during startup.
  This dialog can be used to edit display properties (such as resolution)
  in case they become corrupted.  If this argument is not
  specified, Maud shows the dialog only if it believes
  (based on JME3 AppSettings) that this is the first time it has run.
 + `--openGL3` (or `-3`)
  to force LWJGL to use the core OpenGL3.2 renderer.
  Forcing this renderer enables Maud to run on systems with
  non-proprietary GPU drivers.
 + `--skipStartup` (or `-s`)
  to bypass the startup script.  If this argument is not specified, Maud
  looks for a startup script and attempts to execute it before displaying the
  Editor Screen.
 + `--verbose` (or `-v`)
  to generate verbose log output.
  This output may assist in diagnosing certain issues.

[Jump to the table of contents](#toc)

<a name="links"></a>

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

<a name="bvh"></a>

BVH resources:

+ BVH format description:
  http://research.cs.wisc.edu/graphics/Courses/cs-838-1999/Jeff/BVH.html
+ motion-capture data from CMU's Graphics Lab, converted to BVH by Bruce Hahn:
  https://sites.google.com/a/cgspeed.com/cgspeed/motion-capture
+ free motion-capture data from Ohio State University's ACCAD:
  https://accad.osu.edu/research/motion-lab/system-data

[Jump to the table of contents](#toc)


<a name="wishlist"></a>

## Wish list

Maud is incomplete.
The following features are on my "to do" list, in no particular order:

 + advance/delay keyframe(s)
 + better support for physics controls/joints/objects/shapes
 + smoother camera motion in score views
 + select bone mappings that don't correspond to the loaded models
 + export a model to [OBJ] format
 + export an animation to [BVH](#bvh) format
 + localization
 + more scene-view options for platform
 + tool tips
 + mirror an animation/pose
 + joint-angle limits for models

[Jump to the table of contents](#toc)

<a name="conventions"></a>

## Conventions

Maud's source code is compatible with JDK 7.

World coordinate system: the Y axis points upward (toward the zenith).
Prior to loading a model that uses the Z-up convention, select the "+Z up"
load orientation in the Settings Tool ("Settings" -> "Tool").

The first keyframe in each track must be at time=0.

[Jump to the table of contents](#toc)


<a name="history"></a>

## History

Since April 2017, the Maud project has been hosted at
https://github.com/stephengold/Maud

Maud began as a demo application for the jme3-utilities-debug library,
part of the jme3-utilities project at
https://github.com/stephengold/jme3-utilities

Maud incorporates code from [jMonkeyEngine][jme] and the [BVH Retarget Project][bvhretarget].

[Jump to the table of contents](#toc)


<a name="acks"></a>

## Acknowledgments

Like most projects, Maud builds on the work of many who
have gone before.  I therefore acknowledge the following
artists and software developers:

+ Rémy Bouquet (aka "nehon") for creating the Jaime model and the [BVH Retarget
  Project][retarget] and also for many helpful insights
+ Paul Speed (aka "pspeed") for many helpful insights
+ David Bernard (aka "david_bernard_31") for creating [Xbuf]
+ Adam T. Ryder (aka "tryder") for creating [jME-TTF]
+ Tobias Jung for creating [ProFont]
+ [Nathan Vegdahl][vegdahl], for creating the Puppet model
+ [Can Bican][bican], for creating the Jprefctl preferences editor
+ Zi Ye, for creating the Sinbad model
+ Alweth on hub.jmonkeyengine.org forums
  for providing `IQELoader` for use, free of charge
+ the brave souls who volunteered to be alpha testers for Maud, including:
    + "Jesterrrrrr"
    + Moff Kalast
    + Adam (aka "safari")
    + Ryan (aka "yaRnMcDonuts")
    + "raistm"
    + Remy Van Doosselaer (aka "remy_vd")
    + "Ali_RS"
    + "Toboi"
+ plus the creators of (and contributors to) the following software:
    + [Adobe Photoshop Elements][elements]
    + the [Blender] 3-D animation suite
    + the [Bullet] real-time physics library
    + the [Checkstyle] tool
    + the [FindBugs] source-code analyzer
    + the [Firefox] and [Chrome] web browsers
    + the [Git] revision-control system and GitK commit viewer
    + the [GitKraken] client
    + the [Gradle] build tool
    + the [IntelliJ IDEA][idea] and [NetBeans] integrated development environments
    + the [Java] compiler, standard doclet, and virtual machine
    + [jMonkeyEngine][jme] and the jME3 Software Development Kit
    + the [Linux Mint][mint] operating system
    + LWJGL, the Lightweight Java Game Library
    + the [MakeHuman] 3-D character creation tool
    + the [Markdown] document-conversion tool
    + Microsoft Windows
    + the [Nifty] graphical user-interface library
    + [Open Broadcaster Software Studio][obs]
    + the PMD source-code analyzer
    + [ProFont], the programmers' font
    + the [RealWorld Cursor Editor][rwce]
    + the [WinMerge] differencing and merging tool

I am grateful to [GitHub], [JFrog], and [Imgur]
for providing free hosting for this project
and many other open-source projects.

I'm also grateful to my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know, so I can
correct the situation: sgold@sonic.net

[Jump to the table of contents](#toc)


[adoptium]: https://adoptium.net/releases.html "Adoptium Project"
[ant]: https://ant.apache.org "Apache Ant Project"
[bican]: https://github.com/canbican "Can Bican"
[blender]: https://docs.blender.org "Blender Project"
[bullet]: https://pybullet.org/wordpress "Bullet Real-Time Physics Simulation"
[bvhretarget]: https://github.com/Nehon/bvhretarget "BVH Retarget Project"
[checkstyle]: https://checkstyle.org "Checkstyle"
[chrome]: https://www.google.com/chrome "Chrome"
[elements]: https://www.adobe.com/products/photoshop-elements.html "Photoshop Elements"
[findbugs]: http://findbugs.sourceforge.net "FindBugs Project"
[firefox]: https://www.mozilla.org/en-US/firefox "Firefox"
[fish]: https://fishshell.com/ "Fish command-line shell"
[git]: https://git-scm.com "Git"
[github]: https://github.com "GitHub"
[gitkraken]: https://www.gitkraken.com "GitKraken client"
[gltf]: https://www.khronos.org/gltf "glTF Project"
[gradle]: https://gradle.org "Gradle Project"
[heart]: https://github.com/stephengold/Heart "Heart Project"
[idea]: https://www.jetbrains.com/idea/ "IntelliJ IDEA"
[imgur]: https://imgur.com/ "Imgur"
[iqe]: http://sauerbraten.org/iqm "Inter-Quake Model Formats"
[java]: https://en.wikipedia.org/wiki/Java_(programming_language) "Java programming language"
[jfrog]: https://www.jfrog.com "JFrog"
[jme]: https://jmonkeyengine.org "jMonkeyEngine Project"
[jme-ttf]: https://1337atr.weebly.com/jttf.html "jME-TTF Rendering System"
[license]: https://github.com/stephengold/Maud/blob/master/LICENSE "Maud license"
[makehuman]: http://www.makehumancommunity.org/ "MakeHuman Community"
[markdown]: https://daringfireball.net/projects/markdown "Markdown Project"
[minie]: https://github.com/stephengold/Minie "Minie Project"
[mint]: https://linuxmint.com "Linux Mint Project"
[nashorn]: https://docs.oracle.com/javase/8/docs/technotes/guides/scripting/nashorn "Nashorn User Guide"
[netbeans]: https://netbeans.org "NetBeans Project"
[nifty]: https://nifty-gui.github.io/nifty-gui "Nifty GUI Project"
[obj]: https://www.fileformat.info/format/wavefrontobj/egff.htm "Wavefront OBJ File Format"
[obs]: https://obsproject.com "Open Broadcaster Software Project"
[ogre]: https://www.ogre3d.org "Ogre Project"
[openJDK]: https://openjdk.java.net "OpenJDK Project"
[profont]: https://tobiasjung.name/profont "ProFont Project"
[retarget]: https://github.com/Nehon/bvhretarget "BVH Retarget Project"
[rwce]: http://www.rw-designer.com/cursor-maker "RealWorld Cursor Editor"
[skycontrol]: https://github.com/stephengold/SkyControl "SkyControl Project"
[utilities]: https://github.com/stephengold/jme3-utilities "Jme3-utilities Project"
[vegdahl]: https://www.cessen.com "Nathan Vegdahl"
[wes]: https://github.com/stephengold/Wes "Wes Project"
[winmerge]: https://winmerge.org "WinMerge Project"
[xbuf]: https://github.com/xbuf/xbuf "Xbuf Project"
[youtube]: https://www.youtube.com/ "YouTube"
