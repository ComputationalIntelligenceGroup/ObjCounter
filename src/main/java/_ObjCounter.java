/**
 * _ObjCounter.java
 *
 *
 * Copyright (C) 2015 Gherardo Varando
 *  
 * This plugin is based on _3D_Object_Counter code by Fabrice P. Cordelières
 * 
 * License:
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


import Utilities.ConnectObjects;
import Utilities.holes_detection;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.util.Tools;

import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Vector;




/**
 *
 * @author Gherardo Varando, gherardo.varando@gmail.com
 * @version 0.0.1 19/10/2016
 */
public class _ObjCounter implements PlugIn, AdjustmentListener, FocusListener {
	ImagePlus imp;
	ImageProcessor ip;
	int width, height, nbSlices, length, toll;
	double min, max;
	String title, redirectTo;
	int thr, minSize, maxSize, dotSize, fontSize;
	boolean excludeOnEdges, showObj, showSurf, showCentro, showCOM, showNb, whiteNb, newRT, showStat, showMaskedImg, closeImg, showSummary, redirect, showDensity, showPoints;
	Vector sliders, values;
	double fraction=0.5;
	boolean validate=false;
	boolean computePointsMap=false;
	boolean export2CSV=false;
	public void run(String arg) {
		if (IJ.versionLessThan("1.39i")) return;

		imp=WindowManager.getCurrentImage();

		if (imp==null){
			IJ.error("You need to open an image first.");
			return;
		}

		if (imp.getBitDepth()>8){
			IJ.error("ObjectCounter only works on 8-bits image.");
			return;
		}

		width=imp.getWidth();
		height=imp.getHeight();
		nbSlices=imp.getStackSize();
		length=height*width*nbSlices;
		title=imp.getTitle();

		min=Math.pow(2, imp.getBitDepth());
		max=0;

		for (int i=1; i<=nbSlices; i++){
			imp.setSlice(i);
			ip=imp.getProcessor();
			min=Math.min(min, imp.getStatistics().min);
			max=Math.max(max, imp.getStatistics().max);
		}

		imp.setSlice((int)nbSlices/2);
		imp.resetDisplayRange();
		thr=ip.getAutoThreshold();
		ip.setThreshold(thr, max,ImageProcessor.RED_LUT);
		imp.updateAndDraw();


		minSize=(int) Prefs.get("3D-OC_minSize.double", 10);
		maxSize=length;
		excludeOnEdges=Prefs.get("3D-OC_excludeOnEdges.boolean", true);
		showObj=Prefs.get("3D-OC_showObj.boolean", true);
		showSurf=Prefs.get("3D-OC_showSurf.boolean", true);
		showCentro=Prefs.get("3D-OC_showCentro.boolean", true);
		showCOM=Prefs.get("3D-OC_showCOM.boolean", true);
		showStat=Prefs.get("3D-OC_showStat.boolean", true);
		showSummary=Prefs.get("3D-OC_summary.boolean", true);

		showMaskedImg=Prefs.get("3D-OC-Options_showMaskedImg.boolean", true);
		closeImg=Prefs.get("3D-OC-Options_closeImg.boolean", false);


		redirectTo=Prefs.get("3D-OC-Options_redirectTo.string", "none");
		redirect=!this.redirectTo.equals("none") && WindowManager.getImage(this.redirectTo)!=null;

		if (redirect){
			ImagePlus imgRedir=WindowManager.getImage(this.redirectTo);
			if (!(imgRedir.getWidth()==this.width && imgRedir.getHeight()==this.height && imgRedir.getNSlices()==this.nbSlices) || imgRedir.getBitDepth()>16){
				redirect=false;
				showMaskedImg=false;
				IJ.log("Redirection canceled: images should have the same size and a depth of 8- or 16-bits.");
			}
			if (imgRedir.getTitle().equals(this.title)){
				redirect=false;
				showMaskedImg=false;
				IJ.log("Redirection canceled: both images have the same title.");
			}
		}

		if (!redirect){
			Prefs.set("3D-OC-Options_redirectTo.string", "none");
			Prefs.set("3D-OC-Options_showMaskedImg.boolean", false);
		}


		GenericDialog gd=new GenericDialog("ObjCounter v0.0.1 (beta)");

		gd.addSlider("Threshold", min, max, thr);
		gd.addSlider("Slice", 1, nbSlices, nbSlices/2);

		sliders=gd.getSliders();
		((Scrollbar)sliders.elementAt(0)).addAdjustmentListener(this);
		((Scrollbar)sliders.elementAt(1)).addAdjustmentListener(this);
		values = gd.getNumericFields();
		((TextField)values.elementAt(0)).addFocusListener(this);
		((TextField)values.elementAt(1)).addFocusListener(this);

		gd.addCheckbox("fast algorithm", false);
		gd.addMessage("Size filter: ");
		gd.addNumericField("Min.",minSize, 0);
		gd.addNumericField("Max.", maxSize, 0);
		gd.addNumericField("Fraction for connections ", 0.5, 3);
		gd.addNumericField("tollerance for propagation", 0, 0);
		gd.addCheckbox("Exclude_objects_on_edges", excludeOnEdges);
		gd.addMessage("Maps to show: ");
		gd.addCheckbox("Objects", showObj);
		gd.addCheckbox("Centroids", showCentro);
		gd.addCheckbox("Centres_of_masses", showCOM);
		gd.addCheckbox("Point map", false);
		gd.addCheckbox("Export_points to CSV", false);
		gd.addCheckbox("Export_results to CSV", false);
		gd.addCheckbox("validation with overlay", false);



		if (redirect) gd.addMessage("\nRedirection:\nImage used as a mask: "+this.title+"\nMeasures will be done on: "+this.redirectTo+(showMaskedImg?"\nMasked image will be shown":"")+".");
		if (closeImg) gd.addMessage("\nCaution:\nImage(s) will be closed during the processing\n(see 3D-OC options to change this setting).");

		gd.showDialog();


		if (gd.wasCanceled()){
			ip.resetThreshold();
			imp.updateAndDraw();
			return;
		}


		thr = (int) gd.getNextNumber();
		int slice= (int) gd.getNextNumber();
		boolean fast = gd.getNextBoolean();
		minSize=(int) gd.getNextNumber();
		maxSize=(int) gd.getNextNumber();
		fraction= (double) gd.getNextNumber();
		toll= (int) gd.getNextNumber();
		excludeOnEdges=gd.getNextBoolean();
		showObj=gd.getNextBoolean();
		showCentro=gd.getNextBoolean();
		showCOM=gd.getNextBoolean();
		computePointsMap=gd.getNextBoolean();
		export2CSV=gd.getNextBoolean();
		boolean exportResults=gd.getNextBoolean();
		validate=gd.getNextBoolean();



		Prefs.set("3D-OC_minSize.double", minSize);
		Prefs.set("3D-OC_excludeOnEdges.boolean", excludeOnEdges);
		Prefs.set("3D-OC_showObj.boolean", showObj);
		Prefs.set("3D-OC_showCentro.boolean", showCentro);
		Prefs.set("3D-OC_showCOM.boolean", showCOM);
		Prefs.set("Obj_Count_fraction.double", fraction);
		Prefs.set("Obj_Count_toll.int", toll);
		if (!redirect) Prefs.set("3D-OC-Options_redirectTo.string", "none");

		ip.resetThreshold();
		imp.updateAndDraw();


		IJ.log("STARTING");
		long start = System.currentTimeMillis();
		ConnectObjects OC=new ConnectObjects(imp, thr, minSize, maxSize, fraction, toll, fast, excludeOnEdges);




		dotSize=(int) Prefs.get("3D-OC-Options_dotSize.double", 5);
		fontSize=(int) Prefs.get("3D-OC-Options_fontSize.double", 10);
		showNb=Prefs.get("3D-OC-Options_showNb.boolean", true);
		whiteNb=Prefs.get("3D-OC-Options_whiteNb.boolean", true);

		if (showObj){OC.getObjMap(showNb, fontSize).show(); IJ.run("3-3-2 RGB");}
		if (showCentro){OC.getCentroidMap(showNb, whiteNb, dotSize, fontSize).show(); IJ.run("3-3-2 RGB");}
		if (showCOM){OC.getCentreOfMassMap(showNb, whiteNb, dotSize, fontSize).show(); IJ.run("3-3-2 RGB");}
		if (computePointsMap) {OC.computePointsMap().show();}
		if (export2CSV) {
			OC.writeCSV("",IJ.getDirectory("current")+"points/");
			String pathSave=IJ.getDirectory("current") + "objects/objects_"+title ;
			ImagePlus obj= OC.getObjMap(showNb, fontSize);
			IJ.save( obj , pathSave );
		}
		if (validate) {OC.getValidation();}


		if (exportResults){
			holes_detection hc=new holes_detection(title);
			hc.detect();

			Calibration cal=imp.getCalibration();
			double voxelVolume=cal.getX(1)*cal.getY(1)*cal.getZ(1);

			int[] numbPoint=OC.getNumberOfPoint();
			double volume=width*height*nbSlices*voxelVolume;
			double density=numbPoint[0]/volume;
			double densS=numbPoint[1]/volume;
			double densH=numbPoint[0]/(volume-hc.getHolesVolume());
			double densSH=numbPoint[1]/(volume-hc.getHolesVolume());

			String fileName="";
			try {
				if (fileName==""){
					fileName=title+"_results.csv";
				}
				Path fileCSV = Files.createFile(Paths.get(IJ.getDirectory("current"),fileName)) ;
				String line1="stackName, numberObject, numberObjectSter,volume, volumeHoles, denisty, density_Holes, density_Ster, density_Ster_Holes"  + System.lineSeparator() ;
				String line2=title+","+OC.getNumberOfPoint()[0]+","+OC.getNumberOfPoint()[1]+ ","+volume+ ","+ hc.getHolesVolume()+ "," +density+","+densH+","+densS+","+densSH + System.lineSeparator() ;
				Files.write(fileCSV,line1.getBytes(),StandardOpenOption.APPEND);	
				Files.write(fileCSV,line2.getBytes(),StandardOpenOption.APPEND);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//compute all the time 
		long elapsedTimeMillis = System.currentTimeMillis()-start;
		float elapsedTimeSec = elapsedTimeMillis/1000F;

		IJ.log("DONE in :"+ elapsedTimeSec + " seconds");
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		updateImg();
	}

	public void focusLost(FocusEvent e) {
		if (e.getSource().equals(values.elementAt(0))){
			int val=(int) Tools.parseDouble(((TextField)values.elementAt(0)).getText());
			val=(int) Math.min(max, Math.max(min, val));
			((TextField)values.elementAt(0)).setText(""+val);
		}

		if (e.getSource().equals(values.elementAt(1))){
			int val=(int) Tools.parseDouble(((TextField)values.elementAt(1)).getText());
			val=(int) Math.min(max, Math.max(min, val));
			((TextField)values.elementAt(1)).setText(""+val);
		}

		updateImg();
	}

	public void focusGained(FocusEvent e) {
	}

	private void updateImg(){
		thr=((Scrollbar)sliders.elementAt(0)).getValue();
		imp.setSlice(((Scrollbar)sliders.elementAt(1)).getValue());
		imp.resetDisplayRange();
		ip.setThreshold(thr, max, ImageProcessor.RED_LUT);
	}



}
