
# ObjCounter plugin

### author: Gherardo Varando, gherardo.varando@gmail.com

#### What it is?

ObjCounter is an ImageJ plugin to segment connected objects in stack of iamges. The plugin detects the objects, and it can create stacks with the labelled objects, stacks with centroid or center of masses of the detected objects, and it can exports the results to csv format. Moreover if an overlay (with single dots labelling every objects) is present than it is possible to evaluate the segmentation.

#### Installation and dependencies

ImageJ is required (see http://fiji.sc/, to install FIJI a "batteries-included" distribution of ImageJ). 

If you have installed the ImageJ [updater](https://imagej.net/Updater) you just have to enable the Computational Intelligence [Update sites](https://imagej.net/Updater#Adding_update_sites), you can find it named [Cig-upm](http://sites.imagej.net/Cig-upm/).
After the update sites is enabled you can easily find ObjCounter among the installable plugins.

Otherwise you can download the .jar from the releases page and copy it into the plugins directory of your ImageJ installation.

#### Acknowledgemnts

ObjCounter is based partly on _3D_Object_Counter code by Fabrice P. Cordelières.
The algorithm to connect the objects is completely different to achieve better performance.
Moreover the logic is different, the connection is done in every slice, then based on the intersection between objects in
different slices the objects are linked or not (modifying the fraction parameter change the tolerance to connections, e.g. with fraction=0 all the possible connections will be made).

#### License

 This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
