/**
 * ConnectObjects
 * 
 * Author: Gherardo Varando (2015), gherardo.varando@gmail.com
 * 
 * 
 * Based on Counter3D class by Fabrice P. Cordelieres,
 * The algorithm are completely changed from Counter3D, the present 
 * implementation no longer use Object3D class.
 * Some methods as offset, imgArrayModifier are taken from Counter3D.
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
import ij.ImageStack;
import ij.gui.NewImage;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;

import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author Gherardo Varando, gherardo.varando@gmail.com
 * @version 0.0.1 19/10/2016
 *
 */
public class ConnectObjects {
	boolean corners=false;
	double fraction;
	int[] splits;
	boolean fast;
	boolean propagateSlice;
	int thr=0;
	boolean[] isSurf;
	int width=1, height=1, nbSlices=1, length=1, depth=8;
	Calibration cal;
	String title="img";
	int curIndx;
	int minSize, maxSize, nbObj=0;
	int[] imgArray, objID, tempArray;
	float[][] centreOfMass, centroid;
	double large=1000;
	Overlay overlay;
	int toll=0;
	boolean excludeOnEdges=false;
	//Path pathIm;
	

	Map<Integer, Integer> IDcount,objAnchor, IDonEdge;
	Map<Integer, double[]> IDrange;

	boolean objectLinked=false, sliceProcessed=false, getCentreOfMass=false, getCentroid=false;
	double pi=Math.PI;
	String method="Spherical";
	boolean autoWindow=false;
 
	
	/**
	 * Constructor
	 * 
	 * @param image  the image to work with 
	 * @param thr    the threshold values
	 * @param min    the minimum size of objects
	 * @param max    the maximum size of objects
	 * @param fraction the threshold value for connecting objects through slices
	 * @param toll     the tollerance in the propagation of the tags
	 * @param fast     use the fast version?
	 * @param excludeOnEdges  exclude objects touching edges?
	 */
	public ConnectObjects(ImagePlus image, int thr, int min, int max, double fraction, int toll, boolean fast, boolean excludeOnEdges) {
		width=image.getWidth();
		height=image.getHeight();
		nbSlices=image.getNSlices();
		length=width*height*nbSlices;
		depth=image.getBitDepth();
		title=image.getTitle();
		cal=image.getCalibration();
		this.fast=fast;
		if (fast){this.propagateSlice=true;}
		else { this.propagateSlice=false;}
		this.thr=thr;
		IJ.log("thr"+thr);
		this.toll=toll;
		minSize=min;
		maxSize=max;
		this.fraction=fraction;
		this.excludeOnEdges=excludeOnEdges;
		if (depth!=8 && depth!=16) throw new IllegalArgumentException("ConnectObjects expects 8-bits and 16-bits images only");

		nbObj=length;       
		imgArray=new int[length];
		imgArrayModifier(image);
		objID=new int[length];
		tempArray=new int[length];
		IDcount=new HashMap<Integer,Integer>();
		IDonEdge=new HashMap<Integer,Integer>();
		IDcount.put(0, length);
		IDrange=new HashMap<Integer,double[]>();

		IDrange.put(0, new double[]{0,0,0,0,0});
		objAnchor= new HashMap<Integer,Integer>();

		overlay=image.getOverlay();
		//pathIm=Paths.get(IJ.getDirectory("current"));

	}

	/**
	 * Propagate the object tags   
	 * @param i, index of pixel to propagate
	 * @param value, tags value to propagate
	 * @param sliceConnections, if true, the propagation will continue on different slices (up and down).
	 * @param strength, strength value to decide if propagate
	 */
	private void propagate(int i, int value, boolean sliceConnections,int strenght, int maximum){
		if (i<0 || i>=length){
			return;
		}
		if (objID[i]!=value && imgArray[i]>0 && strenght+toll>=imgArray[i] && maximum>=tempArray[i]  ){  // 
			tempArray[i]=Math.max(maximum,imgArray[i]);
			strenght = imgArray[i];
			maximum = Math.max(imgArray[i], maximum);
			IDcount.put(objID[i],IDcount.get(objID[i])-1);
			objID[i]=value;
			IDcount.put(value,IDcount.get(value)+1);
			IDrange.put(value,new double[]{Math.min(IDrange.get(value)[0],imgArray[i]),Math.max(IDrange.get(value)[1],imgArray[i]),IDrange.get(value)[2]+imgArray[i],IDrange.get(value)[3]+Math.pow(imgArray[i],2),IDrange.get(value)[4]+1 } );
			if (((i+1) % width )!= 0 ){
				// propagate to the right 	 
				propagate(i+1,value, sliceConnections, strenght, maximum);

				if (corners){
					if ( ((i+1-width)/(width*height)) == (i / (width*height)) ){
						// propagate to the down-right
						propagate(i+1-width,value, sliceConnections, strenght, maximum);

					}
					if ( (  ((i+1+width)/(width*height)) ) == ( i / (width*height))  ){
						// propagate to the up-right
						propagate(i+1+width,value, sliceConnections, strenght, maximum);
					}
				}
			}
			if ((i % width )!= 0){
				// propagate to the left 

				propagate(i-1,value, sliceConnections, strenght, maximum);

				if (corners){
					if ( ( (i-1-width)/(width*height)) == ( i / (width*height))  ){
						propagate(i-1-width,value, sliceConnections, strenght, maximum);

					}
					if ( ( (i-1+width)/(width*height)) == ( i / (width*height)) ){ 
						propagate(i-1+width,value, sliceConnections, strenght, maximum);

					}
				}
			}

			if ( ((i+width)/(width*height)) == ( i / (width*height)) ){ 
				// propagate up
				propagate(i+width,value, sliceConnections, strenght, maximum);

			}
			if ( ( (i-width)/(width*height) ) == (  i / (width*height) )  ){ 
				//propagate down
				propagate(i-width,value, sliceConnections, strenght, maximum);
			}

			if (sliceConnections){
				//propagate one slice up
				propagate(i+width*height,value, sliceConnections, strenght, maximum);
				// propagate one slice down
				propagate(i-width*height,value, sliceConnections, strenght, maximum);
			}
		}
	}

	
	/**
	 * Fast version of propagation, propagates the tags also in the 3d dimension (through slices) 
	 * @param i index of the pixel to propagate
	 * @param value value of the tag to propagate
	 * @param strenght strenght of the propagations (usually previous intensity)
	 */
	private void propagateFast(int i, int value,int strenght){
		if (i<0 || i>=length){
			return;
		}
		if (objID[i]!=value && imgArray[i]>0 && strenght+toll>=imgArray[i] && strenght+toll>=tempArray[i]  ){
			tempArray[i]=strenght+toll;
			strenght=imgArray[i];
			IDcount.put(objID[i],IDcount.get(objID[i])-1);
			objID[i]=value;
			IDcount.put(value,IDcount.get(value)+1);
			if (((i+1) % width )!= 0 ){

				// propagate to the right 	 

				propagateFast(i+1,value, strenght);

				if ( ((i+1-width)/(width*height)) == (i / (width*height)) ){
					// propagate to the down-right
					propagateFast(i+1-width,value, strenght);

				}
				if ( (  ((i+1+width)/(width*height)) ) == ( i / (width*height))  ){
					// propagate to the up-right
					propagateFast(i+1+width,value,  strenght);
				}
			}
			if ((i % width )!= 0){
				// propagate to the left 

				propagateFast(i-1,value,  strenght);


				if ( ( (i-1-width)/(width*height)) == ( i / (width*height))  ){
					propagateFast(i-1-width,value,  strenght);

				}
				if ( ( (i-1+width)/(width*height)) == ( i / (width*height)) ){ 
					propagateFast(i-1+width,value, strenght);

				}
			}

			if ( ((i+width)/(width*height)) == ( i / (width*height)) ){ 
				// propagate up
				propagateFast(i+width,value,  strenght);

			}
			if ( ( (i-width)/(width*height) ) == (  i / (width*height) )  ){ 
				//propagate down
				propagateFast(i-width,value,  strenght);
			}
			//propagate one slice up
			propagateFast(i+width*height,value, strenght);
			// propagate one slice down
			propagateFast(i-width*height,value, strenght);

		}
	}


	
	/**
	 * similar to propagate fast but less safe, just use for change objIDs of already segmented objects
	 * @param i index of the pixel to propagate
	 * @param newvalue new value of the tag to propagate
	 * @param oldvalue old value to substitute
	 */
	private void iterativeSubstitution(int i, int newvalue, int oldvalue){
		if (i<0 || i>=length){
			return;
		}
		if (oldvalue==newvalue){return;}
		if (objID[i]==oldvalue && imgArray[i]>0 ){
			// no need for checking on borders because we suppose the ID we are going to substitute are correct
			IDcount.put(oldvalue,IDcount.get(oldvalue)-1);
			objID[i]=newvalue;
			IDcount.put(newvalue,IDcount.get(newvalue)+1);
			iterativeSubstitution(i+1,newvalue,oldvalue);
			iterativeSubstitution(i-1,newvalue,oldvalue); 		 
			iterativeSubstitution(i+width,newvalue,oldvalue);  		 
			iterativeSubstitution(i-width,newvalue,oldvalue);  		 
			iterativeSubstitution(i+1-width,newvalue,oldvalue);   		 
			iterativeSubstitution(i+1+width,newvalue,oldvalue);
			iterativeSubstitution(i-1-width,newvalue,oldvalue);
			iterativeSubstitution(i-1+width,newvalue,oldvalue);
			iterativeSubstitution(i+width*height,newvalue,oldvalue);
			iterativeSubstitution(i-width*height,newvalue,oldvalue);
		}
	}



	/**
	 * iteratively expand object on not-tagged pixels that have been set free (because too small)
	 * @param i index of the pixel
	 * @param value tag of the object to expand
	 */
	private void iterativeExpand(int i, int value){
		if (i<0 || i>=length){
			return;
		}
		if ( (tempArray[i]==value || objID[i]==0) && imgArray[i]>0 ){
			tempArray[i]=-1;
			objID[i]=value;

			if (((i+1) % width )!= 0 ){

				// propagate to the right 	 

				iterativeExpand(i+1,value);
				if (corners){
					if ( ((i+1-width)/(width*height)) == (i / (width*height)) ){
						// propagate to the down-right
						iterativeExpand(i+1-width,value);

					}
					if ( (  ((i+1+width)/(width*height)) ) == ( i / (width*height))  ){
						// propagate to the up-right
						iterativeExpand(i+1+width,value);
					}
				}
			}
			if ((i % width )!= 0){
				// propagate to the left 

				iterativeExpand(i-1,value);

				if (corners){
					if ( ( (i-1-width)/(width*height)) == ( i / (width*height))  ){
						iterativeExpand(i-1-width,value);

					}
					if ( ( (i-1+width)/(width*height)) == ( i / (width*height)) ){ 
						iterativeExpand(i-1+width,value);

					}
				}
			}

			if ( ((i+width)/(width*height)) == ( i / (width*height)) ){ 
				// propagate up
				iterativeExpand(i+width,value);

			}
			if ( ( (i-width)/(width*height) ) == (  i / (width*height) )  ){ 
				//propagate down
				iterativeExpand(i-width,value);
			}
		}

	}


	/**
	 *fast process, using propagation in all direction 
	 */
	private void processSlicesFast(){
		int id=0;
		for (int i=0; i<length; i++){
			if (imgArray[i]>0){
				if (objID[i]==0){
					id++;
					// I remember one anchor point of the object, it is sufficient since the objects are always connected
					objAnchor.put(id,i);
					IDcount.put(id, 0);
					// from the anchor point I start propagating the id
					propagateFast(i,id,imgArray[i]);
				}
			}
			else{
				objID[i]=0;
			}
		}
		nbObj=id;
		sliceProcessed=true;
	}


	/**
	 * Process every slice independently
	 */
	private void processSlices(){
		int id=0;
		for (int i=0; i<length; i++){
			if (imgArray[i]>0){
				if (objID[i]==0){
					id++;
					// I remember one anchor point of the object, it is sufficient since the objects are always connected
					objAnchor.put(id,i);
					IDcount.put(id, 0);
					IDrange.put(id, new double[]{255,0,0,0,0});
					// from the anchor point I start propagating the id
					propagate(i,id, propagateSlice,imgArray[i], imgArray[i]);
				}
			}
			else{
				objID[i]=0;
			}
		}
		nbObj=id;
		sliceProcessed=true;
	}

	/**
	 * Computes pixel similarity using something like Bhattacharyya coefficient/angle
	 * @param i index of the first pixel
	 * @param j index of the second pixel
	 * @return
	 */
	private double pixelSimilarity(int i, int j){
		double a,b;
		double[] range=IDrange.get(tempArray[i]);
		a= (imgArray[i])/range[2]; //probability
		range=IDrange.get(tempArray[j]);
		b= (imgArray[j])/range[2]; //probability
		return(Math.sqrt(a*b));  //using something like Bhattacharyya coefficient/angle
	}





	/**
	 *  Connect objects in different/adjacent slices
	 */
	private void connectSlices(){	   
		IJ.log("preliminary i found "+nbObj+" objects, now i will try to connect them");
		Map<Integer,Map<Integer,Double>> intersections= new HashMap<Integer, Map<Integer,Double>>();
		tempArray = objID.clone();
		Map<Integer, Integer> oldCount=new HashMap<Integer,Integer>();
		oldCount.putAll(IDcount);
		Map<Integer,Double> temp;
		int m=width*height;
		for (int i=0; i<length; i++){
			if ((objID[i]!=0)){
				if (i+m<length){
					if (objID[i+m]!=0){
						if (intersections.containsKey(tempArray[i])){
							if (intersections.get(tempArray[i]).containsKey(tempArray[i+m])){
								intersections.get(tempArray[i]).put(tempArray[i+m], intersections.get(tempArray[i]).get(tempArray[i+m])+pixelSimilarity(i, i+m));
							}
							else intersections.get(tempArray[i]).put(tempArray[i+m], pixelSimilarity(i, i+m));
						}
						else {
							temp= new HashMap<Integer, Double>(1);
							temp.put(tempArray[i+m], pixelSimilarity(i, i+m));
							intersections.put(tempArray[i],temp);
						}
						if ( (intersections.get(tempArray[i]).get(tempArray[i+m]))>fraction) { //*oldCount.get(oldID[i]) 
							iterativeSubstitution(i+m,objID[i],objID[i+m]);   
						}
					}
				}
				if (i-m>=0){
					if (objID[i-m]!=0){
						if ( (intersections.get(tempArray[i-m]).get(tempArray[i]))>fraction ){ //*oldCount.get(oldID[i])
							iterativeSubstitution(i-m,objID[i],objID[i-m]); 
						}
					}
				}
			}
		}
	}  


	/**
	 * Process the slice (fast or not) and then clean objects tag and remove small objects
	 */
	private void linkObjects() {

		if (!fast){
			if (!sliceProcessed) processSlices();
			connectSlices();
		}
		else{
			if (!sliceProcessed) processSlicesFast();
		}


		//delete small object and put consecutive objID  	
		int newCurrID=0;
		for (int i=1; i<=nbObj; i++){
			if (IDcount.get(i)!=0 && IDcount.get(i)>=minSize && IDcount.get(i)<=maxSize  ){
				newCurrID++;   
				iterativeSubstitution(objAnchor.get(i),newCurrID,i);
				objAnchor.put(newCurrID,objAnchor.get(i)); //substitute the anchor points for the given object
			}else{
				if ((IDcount.get(i)!=0)){
					iterativeSubstitution(objAnchor.get(i),0,i);
					objAnchor.put(i,-1);
				}   	
			}
			IJ.showStatus("checking size of objects");
		}



		//join small objects if possible by expanding the remaining objects on the free space left by deleted small objects
		tempArray = objID.clone();
		for (int i=0; i<length; i++){
			if (objID[i]>0){
				iterativeExpand(i, objID[i]);
			}
			IJ.showStatus("joining small objects");
		}
		nbObj=newCurrID;
		IJ.log("I found "+nbObj+" objects");
		objectLinked=true;


		if (this.excludeOnEdges){
			deleteOnEdges();	
		}

		markOnEdges();

	}

	/**
	 *  Deletes objects on edges
	 */
	public void deleteOnEdges(){
		//excludeOnEdges

		for (int i=0; i<length; i++){
			if (objID[i]>0 && (i<width*height | (i % width )== 0 | ((i-width)/(width*height)) != ( i / (width*height)) ) ){
				iterativeSubstitution(objAnchor.get(objID[i]),0,objID[i]);
			}
			IJ.showStatus("deleting objects on Edges");
		}

	}

	/**
	 * Marks objects on edges
	 */
	public void markOnEdges(){
		for (int i=1; i<=nbObj; i++){
			IDonEdge.put(i, 0);
		}
		for (int i=0; i<length; i++){
			if (objID[i]>0 && (i<width*height | (i % width )== 0 | ((i-width)/(width*height)) != ( i / (width*height)) ) ){
				IDonEdge.put(objID[i], 1);
			}
			IJ.showStatus("marking objects on Edges");
		}

	}



	/**
	 * Returns the objects map.
	 * @param drawNb should be true if numbers have to be drawn at each coordinate stored in cenArray (boolean).
	 * @param fontSize font size of the numbers to be shown (integer).* @return an ImagePlus containing all found objects, each one carrying pixel value equal to its ID.
	 */
	public ImagePlus getObjMap(boolean drawNb, int fontSize){
		if (!objectLinked) linkObjects();
		if (!getCentroid) populateCentroid();
		return buildImg(objID, coord2imgArray(centroid)  , "Objects map of "+title, false, drawNb, true, 0, fontSize);
	}

	/**
	 * Returns the objects map.
	 *
	 * @return an ImagePlus containing all found objects, each one carrying pixel value equal to its ID.
	 */
	public ImagePlus getObjMap(){
		if (!objectLinked) linkObjects();
		return buildImg(objID, null, "Objects map of "+title, false, false, true, 0, 0);
	}

	/**
	 * Returns the objects map as a 1D integer array.
	 *
	 * @return an ImagePlus containing all found objects, each one carrying pixel value equal to its ID.
	 */
	public int[] getObjMapAsArray(){
		if (!objectLinked) linkObjects();
		return objID;
	}

	/** Generates and fills the "centreOfMass" array.
	 */
	private void populateCentreOfMass(){
		int indx=0;
		if (!objectLinked) linkObjects();
		centreOfMass=new float[nbObj][3];
		float[] totalMass = new float[nbObj];
		for (int z=1; z<=nbSlices; z++){
			for (int x=0; x< width; x++){
				for (int y=0; y< height; y++){
					indx=offset(x,y,z);
					if (objID[indx]>0 && objID[indx]<=nbObj){

						centreOfMass[objID[indx]-1][0]+= imgArray[indx] * x  ;
						centreOfMass[objID[indx]-1][1]+= imgArray[indx] * y  ;
						centreOfMass[objID[indx]-1][2]+= imgArray[indx] * z  ;
						totalMass[objID[indx]-1]+=imgArray[indx];
					}
				}
			}
		}
		for (int i=0; i<nbObj; i++){
			centreOfMass[i][0]=centreOfMass[i][0]/totalMass[i];
			centreOfMass[i][1]=centreOfMass[i][1]/totalMass[i];
			centreOfMass[i][2]=centreOfMass[i][2]/totalMass[i];
		}
		getCentreOfMass=true;
	}

	/**
	 * Returns the centres of masses' list.
	 *
	 * @return the coordinates of all found centres of masses as a dual float array ([ID][0:x, 1:y, 2:z]).
	 */
	public float[][] getCentreOfMassList(){
		if (!getCentreOfMass) populateCentreOfMass();
		return centreOfMass;
	}

	/**
	 * Returns the centres of masses' map.
	 * @param drawNb should be true if numbers have to be drawn at each coordinate stored in cenArray (boolean).
	 * @param whiteNb should be true if numbers have to appear white  (boolean).
	 * @param dotSize size of the dots to be drawn (integer).
	 * @param fontSize font size of the numbers to be shown (integer).* @return an ImagePlus containing all centres of masses, each one carrying pixel value equal to its ID.
	 */
	public ImagePlus getCentreOfMassMap(boolean drawNb, boolean whiteNb, int dotSize, int fontSize){
		if (!getCentreOfMass) populateCentreOfMass();
		int[] array=coord2imgArray(centreOfMass);
		return buildImg(array, array, "Centres of mass map of "+title, true, drawNb, whiteNb, dotSize, fontSize);
	}

	/**
	 * Returns the centres of masses' map.
	 *
	 * @return an ImagePlus containing all centres of masses, each one carrying pixel value equal to its ID.
	 */
	public ImagePlus getCentreOfMassMap(){
		if (!getCentreOfMass) populateCentreOfMass();
		int[] array=coord2imgArray(centreOfMass);
		return buildImg(array, null, "Centres of mass map of "+title, true, false, false, 5, 0);
	}

	/** Generates and fills the "centroid" array.
	 */
	private void populateCentroid(){
		int indx=0;
		if (!objectLinked) linkObjects();
		centroid=new float[nbObj][3];
		float[] totalMass = new float[nbObj];
		for (int z=1; z<=nbSlices; z++){
			for (int x=0; x< width; x++){
				for (int y=0; y< height; y++){
					indx=offset(x,y,z);
					if (objID[indx]>0 && objID[indx]<=nbObj){
						centroid[objID[indx]-1][0]+= x  ;
						centroid[objID[indx]-1][1]+= y  ;
						centroid[objID[indx]-1][2]+= z  ;
						totalMass[objID[indx]-1]+=1;
					}
				}
			}
		}
		for (int i=0; i<nbObj; i++){
			centroid[i][0]=centroid[i][0]/totalMass[i];
			centroid[i][1]=centroid[i][1]/totalMass[i];
			centroid[i][2]=centroid[i][2]/totalMass[i];
		}
		getCentroid=true;
	}

	/**
	 * Returns the centroids' list.
	 *
	 * @return the coordinates of all found centroids as a dual float array ([ID][0:x, 1:y, 2:z]).
	 */
	public float[][] getCentroidList(){
		if (!getCentroid) populateCentroid();
		return centroid;
	}

	/**
	 * Returns the centroids' map.
	 * @param drawNb should be true if numbers have to be drawn at each coordinate stored in cenArray (boolean).
	 * @param whiteNb should be true if numbers have to appear white  (boolean).
	 * @param dotSize size of the dots to be drawn (integer).
	 * @param fontSize font size of the numbers to be shown (integer).* @return an ImagePlus containing all centroids, each one carrying pixel value equal to its ID.
	 */
	public ImagePlus getCentroidMap(boolean drawNb, boolean whiteNb, int dotSize, int fontSize){
		if (!getCentroid) populateCentroid();
		int[] array=coord2imgArray(centroid);
		return buildImg(array, array, "Centroids map of "+title, true, drawNb, whiteNb, dotSize, fontSize);
	}

	/**
	 * Returns the centroids' map.
	 *
	 * @return an ImagePlus containing all centroids, each one carrying pixel value equal to its ID.
	 */
	public ImagePlus getCentroidMap(){
		if (!getCentroid) populateCentroid();
		int[] array=coord2imgArray(centroid);
		return buildImg(array, null, "Centroids map of "+title, true, false, false, 5, 0);
	}





	/** Transforms a coordinates array ([ID][0:x, 1:y, 3:z]) to a linear array containing all pixels one next to the other.
	 *
	 *@return the linear array as an integer array.
	 */
	private int[] coord2imgArray(float[][] coord){
		int[] array=new int[length];
		for (int i=0; i<coord.length; i++)array[offset((int) coord[i][0], (int) coord[i][1], (int) coord[i][2])]=i+1;
		return array;
	}

	/** Set to zero pixels below the threshold in the "imgArray" arrays.
	 */
	private void imgArrayModifier(ImagePlus img){
		int index=0;
		for (int i=1; i<=nbSlices; i++){
			img.setSlice(i);
			for (int j=0; j<height; j++){
				for (int k=0; k<width; k++){
					imgArray[index]=img.getProcessor().getPixel(k, j);
					if (imgArray[index]<thr){
						imgArray[index]=0;
						nbObj--;
					}
					index++;
				}
			}
		}
		if (nbObj<=0){
			//IJ.error("No object found");
			return;
		}
	}

	/** Set to zero pixels below the threshold in the "imgArray" arrays.
	 */
	private void imgArrayModifier(){
		int index=0;
		for (int i=1; i<=nbSlices; i++){
			for (int j=0; j<height; j++){
				for (int k=0; k<width; k++){
					if (imgArray[index]<thr){
						imgArray[index]=0;
						nbObj--;
					}
					index++;
				}
			}
		}
		if (nbObj<=0){
			IJ.error("No object found");
			return;
		}
	}







	/** Returns the index where to find the informations corresponding to pixel (x, y, z).
	 * @param x coordinate of the pixel.
	 * @param y coordinate of the pixel.
	 * @param z coordinate of the pixel.
	 * @return the index where to find the informations corresponding to pixel (x, y, z).
	 */
	private int offset(int m,int n,int o){
		if (m+n*width+(o-1)*width*height>=width*height*nbSlices){
			return width*height*nbSlices-1;
		}else{
			if (m+n*width+(o-1)*width*height<0){
				return 0;
			}else{
				return m+n*width+(o-1)*width*height;
			}
		}
	}






	/** Generates the ImagePlus based on Counter3D object width, height and number of slices, the input array and title.
	 * @param imgArray containing the pixels intensities (integer array).
	 * @param cenArray containing the coordinates of pixels where the labels should be put (integer array).
	 * @param title to attribute to the ImagePlus (string).
	 * @param drawDots should be true if dots should be drawn instead of a single pixel for each coordinate of imgArray (boolean).
	 * @param drawNb should be true if numbers have to be drawn at each coordinate stored in cenArray (boolean).
	 * @param whiteNb should be true if numbers have to appear white  (boolean).
	 * @param dotSize size of the dots to be drawn (integer).
	 * @param fontSize font size of the numbers to be shown (integer).
	 */
	private ImagePlus buildImg(int[] imgArray, int[] cenArray, String title, boolean drawDots, boolean drawNb, boolean whiteNb, int dotSize, int fontSize){
		int index=0;
		int imgDepth=16;
		float min=imgArray[0];
		float max=imgArray[0];

		for (int i=0; i<imgArray.length; i++){
			int currVal=imgArray[i];
			min=Math.min(min, currVal);
			max=Math.max(max, currVal);
		}

		if (max<256) imgDepth=8;
		ImagePlus img=NewImage.createImage(title, width, height, nbSlices, imgDepth, 1);

		for (int z=1; z<=nbSlices; z++){
			IJ.showStatus("Creating the image...");
			img.setSlice(z);
			ImageProcessor ip=img.getProcessor();
			for (int y=0; y<height; y++){
				for (int x=0; x<width; x++){
					int currVal=imgArray[index];
					if (currVal!=0){
						ip.setValue(currVal);
						if (drawDots){
							ip.setLineWidth(dotSize);
							ip.drawDot(x, y);
						}else{
							ip.putPixel(x, y, currVal);
						}
					}
					index++;
				}
			}
		}
		IJ.showStatus("");

		index=0;
		if (drawNb && cenArray!=null){
			for (int z=1; z<=nbSlices; z++){
				IJ.showStatus("Numbering objects...");
				img.setSlice(z);
				ImageProcessor ip=img.getProcessor();
				ip.setValue(Math.pow(2, imgDepth));
				ip.setFont(new Font("Arial", Font.PLAIN, fontSize));
				for (int y=0; y<height; y++){
					for (int x=0; x<width; x++){
						int currVal=cenArray[index];
						if (currVal!=0){
							if (!whiteNb) ip.setValue(currVal);
							ip.drawString(""+currVal, x, y);
						}
						index++;
					}
				}
			}
		}
		IJ.showStatus("");

		img.setCalibration(cal);
		img.setDisplayRange(min, max);
		return img;
	}

	/**
	 *  Validate the segmentations with ROI
	 */
	public void getValidation(){
		if (!getCentroid) populateCentroid();
		if (overlay==null){
			IJ.log("no overlay in the image, impossible to validate");
			return;
		}
		Map<Integer,Roi> polys =new HashMap<Integer,Roi>();
		int nbRoi=0;
		int currentID;
		int indx=0;
		int position;
		int num=0;

		Roi[] roi=overlay.toArray();
		for (int i=0; i<roi.length;i++){
			if (roi[i].getType()==10){
				position =offset((int) roi[i].getXBase(),(int) roi[i].getYBase(),roi[i].getPosition()); 
				currentID=objID[position];
				num++;
				if (currentID>0){
					indx++;
					iterativeSubstitution(position, 0, currentID);
				}
			}
			else{
				nbRoi++;
				polys.put(nbRoi,roi[i]);
			}
		}
		int[] inside=new int[nbRoi];
		int totInside=0;
		for (int i=0; i<nbObj; i++){
			for (int j=1; j<=nbRoi; j++){
				if (polys.get(j).getFloatPolygon().contains(centroid[i][0],centroid[i][1])){ 
					inside[j-1]++;
					totInside++;
					break;
				}
			}

		}
		for (int j=1; j<=nbRoi; j++){
			if (inside[j-1]>0){
				IJ.log("Roi "+polys.get(j).getName()+" contains "+inside[j-1]+" detected objects");
			}
		}
		IJ.log("ROIs contains "+num+" overlay points ");
		IJ.log(" " + indx+" object  of "+num+" has been corectly associated");
		IJ.log(" " + totInside+" detected objects with centroid  inside the ROI(s)");
		double sensitivity=((double) indx)/num;
		double precision=((double)indx)/totInside;
		double f1Score=((double) 2*indx)/(num+totInside);
		IJ.log("Recall = "+ sensitivity);
		IJ.log("Precision = "+precision);
		IJ.log("F1 score = "+ f1Score );
		IJ.log("relative naive error = " +(((double) totInside-num))/num);
	}

	/**
	 * @return Points map of centroid
	 */
	public ImagePlus computePointsMap(){
		if (!getCentroid) populateCentroid();
		int depthbit;
		if (nbSlices<255){
			depthbit=8;
		}
		else{
			depthbit=32;
		}
		ImagePlus img=NewImage.createImage("PointsStack_"+title, width, height, nbSlices, depthbit, 1);
		ImageStack stack= img.getStack();
		ImageProcessor proc;
		for (int i=0; i<nbObj; i++){
			proc=stack.getProcessor((int) centroid[i][2]);
			proc.putPixelValue((int) centroid[i][0], (int) centroid[i][1], proc.getPixel((int) centroid[i][0],(int) centroid[i][1]) +1);
		}

		return img;
	}

	/**
	 * @return the density Array 
	 */
	public float[] densityMapArray(){
		float[] densityArray = new float[width*length];

		return densityArray;
	}

	/**
	 *  write csv with centroid
	 * @param fileName file name 
	 * @param path path of where write the file
	 */
	public ResultsTable getResultsTable(){
		if (!getCentroid) populateCentroid();
	    ResultsTable rt = new ResultsTable();
	    rt.setPrecision(5);
		for (int i=0; i<nbObj; i++){
			rt.setValue(1, i, centroid[i][0]);
			rt.setValue(2, i, centroid[i][1]);
			rt.setValue(3, i, centroid[i][2]);
			rt.setValue(4, i, IDonEdge.get(i+1));
		}
        return rt;
	}


	/**
	 * @return the total number of objects
	 */
	public int[] getNumberOfPoint(){
		int[] result=new int[2];
		for (int i=0;i<nbObj; i++){
			result[0]=result[0]+1;
			if (IDonEdge.get(i+1)==0){
				result[1]=result[1]+1;
			}
		}


		return(result);


	}





}
