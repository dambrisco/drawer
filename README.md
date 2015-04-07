# drawer
This is an rather simplistic implementation of an Android drawer widget which provides a handle.

It supports click, drag, and fling actions out of the box.

# Attributes
* `drawerHandle` : `drawable`
* `drawerOpen` : `text|drawable`
* `drawerClosed` : `text|drawable`
* `drawerOpenColor` : `color`
* `drawerClosedColor` : `color`
* `drawerPeek` : `dimension`
* `drawerHandleHeight` : `dimension`
* `drawerHandleWidth` : `dimension`
* `drawerHandleId` : `id`
* `drawerHandleTextSize` : `dimension`
* `drawerInitialState` : `enum`
 * `closed`
 * `open`
* `drawerSlideDuration` : `int (milliseconds)`

# How to use
* Include the library in your project
* Use in XML or programmatically - Drawer extends LinearLayout so it can be used just as any other ViewGroup
* Fill in attributes - no guarantees on what happens if some are missing although theoretically it'll work
