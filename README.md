
# ObjCounter plugin

###author: Gherardo Varando, gherardo.varando@gmail.com

This is an imageJ plugin based on _3D_Object_Counter code by Fabrice P. Cordelières.
The algorithm to connect the objects is completely different to achieve better performance.
Moreover the logic is different, the connection is done in every slice, then based on the intersection between objects in 
different slices the objects are linked or not (modifying the fraction parameter change the tolerance to connections, e.g. with fraction=0 all the possible connections will be made).

The _3D_OC_Options class is the same as used in _3D_Object_Counter plugin, and should not conflict.

To install the plugin just copy the .jar file in the plugin directory.