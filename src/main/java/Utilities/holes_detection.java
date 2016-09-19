/**
 * holes_detection
 * 
 * Author: Gherardo Varando (2016), gherardo.varando@gmail.com
 * 
 * 
 * 
 *  License:
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package Utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;

/**
 * @author gherardo varando
 * @version 0.0.1 19/10/2016
 *
 */
public class holes_detection {
    int width=1, height=1, nbSlices=1, length=1, depth=8;
    Calibration cal;
    String title="img";
    ImagePlus image;
    int[] imgArray;
	public holes_detection(String title){
		 
		this.image=IJ.openImage(IJ.getDirectory("current")+"holes"+title);
		  
		
	    width=image.getWidth();
	    height=image.getHeight();
	    nbSlices=image.getNSlices();
	    length=width*height*nbSlices;
	    depth=image.getBitDepth();
	    title=image.getTitle();
	    cal=image.getCalibration();
	    imgArray=new int[length];
	}
	
	public void detect(){
		
		int index=0;
        for (int i=1; i<=nbSlices; i++){
            image.setSlice(i);
            for (int j=0; j<height; j++){
                for (int k=0; k<width; k++){
                    imgArray[index]=image.getProcessor().getPixel(k, j);
                    index++;
                }
            }
        }
	}
	
	public double getHolesVolume(){
		double voxelVolume=cal.getX(1)*cal.getY(1)*cal.getZ(1);
		double volume=0;
		for (int i=0; i<length; i++){
			volume=volume+imgArray[i];
		}
		return (volume*voxelVolume);
	}
}
