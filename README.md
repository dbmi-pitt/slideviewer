# slideviewer
 SlideViwer is a high level java API that describes the behaviour of a generic digital slide image viewer.

This allows one to create applications such as SlideTutor ITS that are agnostic to vendor sepcific details of the viewer implementation.

To incorporate a specific viewer, a software developer can create a wrapper for vendor specific java viewer application that simply implements SlideViewer API.

As of this writing, SlideViewer has an implementation of Xippix ImagePump viewer that can load old Interscope images, as well as our own in-house developed QuickView which can connect to Aperio Image Server, CMU OpenSlide server, Hamamatsu NanoZoomer Server and Zeiss Tile Server. The link below is the demonstration of SlideViewer API. 
