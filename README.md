
# ObjCounter plugin

###author: Gherardo Varando, gherardo.varando@gmail.com

####What it is?

ObjCounter is an ImageJ plugin to segment connected objects in stack of iamges. The plugin detects the objects, and it can create stacks with the labelled objects, stacks with centroid or center of masses of the detected objects, and it can exports the results to csv format. Moreover if an overlay (with single dots labelling every objects) is present than it is possible to evaluate the segmentation.

ObjCounter is based partly on _3D_Object_Counter code by Fabrice P. Cordelières.
The algorithm to connect the objects is completely different to achieve better performance.
Moreover the logic is different, the connection is done in every slice, then based on the intersection between objects in 
different slices the objects are linked or not (modifying the fraction parameter change the tolerance to connections, e.g. with fraction=0 all the possible connections will be made).

The _3D_OC_Options class is the same as used in _3D_Object_Counter plugin, and should not conflict.


#### Installation and dependencies

ObjCounter is an ImageJ plugin, thus ImageJ must be installed (see http://fiji.sc/ to install FIJI a "batteries-included" distribution of ImageJ).

To install the plugin just copy the .JAR file into the plugin directory of your ImageJ installation.

