
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
import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.util.Tools;

import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Vector;

/**
 *
 * @author Gherardo Varando, gherardo.varando@gmail.com
 * @version 0.0.1 19/10/2016
 */
public class _ObjCounter implements PlugIn, AdjustmentListener, FocusListener {

	private static final String PARAM_SILENT = "silent";
	private static final String PARAM_FAST = "fast";
	private static final String PARAM_THRESHOLD = "threshold";
	private static final String PARAM_SLICE = "slice";
	private static final String PARAM_MIN = "min";
	private static final String PARAM_MAX = "max";
	private static final String PARAM_FRACTION = "fraction";
	private static final String PARAM_TOLERANCE = "tolerance";
	private static final String PARAM_SHOW_CENTROIDS = "centroids";
	private static final String PARAM_SHOW_COM = "com";
	private static final String PARAM_COMPUTE_POINTS_MAP = "points_map";
	private static final String PARAM_SHOW_OBJECTS = "objects";
	private static final String PARAM_VALIDATE = "validate";
	private static final String PARAM_OUTPUT_OBJECTS = "output_objects";
	private static final String PARAM_OUTPUT_POINTS = "output_points";

	private ImagePlus currentImage;
	private ImageProcessor ip;
	private int width, height, nbSlices, length, tolerance;
	private double min, max;
	private int threshold, slice, minSize, maxSize, dotSize, fontSize;
	private boolean showObjects=false, showCentroids=false, showCOM=false, showNb=false, whiteNb=false, fast=false;
	private Vector sliders, values;
	private double fraction = 0.5;
	private boolean validate = false;
	private boolean showPointsMap = false;
	public void run(String arg) {
		
		if (IJ.versionLessThan("1.39i"))
			return;
		
        boolean isSilent = false;
		String macroOptions = Macro.getOptions();
		try{
			isSilent = Boolean.parseBoolean(Macro.getValue(macroOptions, PARAM_SILENT, "false"));
		}catch(Exception ex){
			isSilent = false;
		}
		
		currentImage = WindowManager.getCurrentImage();

		if (currentImage == null) {
			if (!isSilent)
				IJ.error("You need to open an image first.");
			return;
		}

		if (currentImage.getBitDepth() > 16) {
			if (!isSilent)
				IJ.error("ObjectCounter only works on 8-bits or 16-bits image.");
			return;
		}

		width = currentImage.getWidth();
		height = currentImage.getHeight();
		nbSlices = currentImage.getStackSize();
		length = height * width * nbSlices;

		min = Math.pow(2, currentImage.getBitDepth());
		max = 0;

		for (int i = 1; i <= nbSlices; i++) {
			currentImage.setSlice(i);
			ip = currentImage.getProcessor();
			min = Math.min(min, currentImage.getStatistics().min);
			max = Math.max(max, currentImage.getStatistics().max);
		}

		currentImage.setSlice((int) nbSlices / 2);
		currentImage.resetDisplayRange();
		threshold = ip.getAutoThreshold();
		ip.setThreshold(threshold, max, ImageProcessor.RED_LUT);
		currentImage.updateAndDraw();

		minSize = (int) Prefs.get("ObjCounter-Options_minSize.integer", 0);
		maxSize = (int) Prefs.get("ObjCounter-Options_maxSize.integer", length);
		showObjects = Prefs.get("ObjCounter-Options_showObjects.boolean", false);
		showCentroids = Prefs.get("ObjCounter-Options_showCentroids.boolean", false);
		showCOM = Prefs.get("ObjCounter-Options_showCOM.boolean", false);
		showPointsMap = Prefs.get("ObjCounter-Options_showPointsMap.boolean", false);
		fast = Prefs.get("ObjCounter-Options_fast.boolean", false);
		validate = Prefs.get("ObjCounter-Options_validate.boolean", false);
		fast = Prefs.get("ObjCounter-Options_fast.boolean", false);
		tolerance = (int) Prefs.get("ObjCounter-Options_tolerance.integer", 0);
		fraction = Prefs.get("ObjCounter-Options_fraction.double", 0.5);
		
	    String outputPathObjects = null;
		String outputPathPoints = null;
		if (macroOptions != null) {
			try {
				threshold = Integer.parseInt(Macro.getValue(macroOptions, PARAM_THRESHOLD, String.valueOf(threshold)));
				slice = Integer.parseInt(Macro.getValue(macroOptions, PARAM_SLICE, String.valueOf(nbSlices / 2)));
				fast = Boolean.parseBoolean(Macro.getValue(macroOptions, PARAM_FAST, String.valueOf(fast)));
				minSize = Integer.parseInt(Macro.getValue(macroOptions, PARAM_MIN, String.valueOf(minSize)));
				maxSize = Integer.parseInt(Macro.getValue(macroOptions, PARAM_MAX, String.valueOf(maxSize) ));
				fraction = Double.parseDouble(Macro.getValue(macroOptions, PARAM_FRACTION, String.valueOf(fraction)));
				tolerance = Integer.parseInt(Macro.getValue(macroOptions, PARAM_TOLERANCE, String.valueOf(tolerance)));
				showCentroids = Boolean.parseBoolean(
						Macro.getValue(macroOptions, PARAM_SHOW_CENTROIDS, String.valueOf(showCentroids)));
				showCOM = Boolean.parseBoolean(Macro.getValue(macroOptions, PARAM_SHOW_COM, String.valueOf(showCOM)));
				showPointsMap = Boolean
						.parseBoolean(Macro.getValue(macroOptions, PARAM_COMPUTE_POINTS_MAP, String.valueOf(showPointsMap)));
				showObjects = Boolean.parseBoolean(
						Macro.getValue(macroOptions, PARAM_SHOW_OBJECTS, String.valueOf(showObjects)));
				validate = Boolean.parseBoolean(Macro.getValue(macroOptions, PARAM_VALIDATE, String.valueOf(validate)));
				outputPathObjects = Macro.getValue(macroOptions, PARAM_OUTPUT_OBJECTS, null);
				outputPathPoints = Macro.getValue(macroOptions, PARAM_OUTPUT_POINTS, null);
			} catch (Exception ex) {
				if (!isSilent)
					IJ.error("param format is incorrect.");
				return;
			}
		} else {
			GenericDialog gd = new GenericDialog("ObjCounter v0.0.1");
			gd.addSlider("Threshold", min, max, threshold);
			gd.addSlider("Slice", 1, nbSlices, nbSlices / 2);
			sliders = gd.getSliders();
			
			((Scrollbar) sliders.elementAt(0)).addAdjustmentListener(this);
			((Scrollbar) sliders.elementAt(1)).addAdjustmentListener(this);
			values = gd.getNumericFields();
			((TextField) values.elementAt(0)).addFocusListener(this);
			((TextField) values.elementAt(1)).addFocusListener(this);
			gd.addCheckbox("Simple 3d propagation", fast);
			gd.addMessage("Size filter: ");
			gd.addNumericField("Min.", minSize, 0);
			gd.addNumericField("Max.", maxSize, 0);
			gd.addMessage("Connection parameters:");
			gd.addNumericField("Fraction for connections ", fraction, 3);
			gd.addNumericField("Tolerance for propagation", tolerance, 0);
			gd.addMessage("");
			gd.addMessage("What to show: ");
			gd.addCheckbox("Objects", showObjects);
			gd.addCheckbox("Centroids", showCentroids);
			gd.addCheckbox("Centres of masses", showCOM);
			gd.addCheckbox("Point map", showPointsMap);
			gd.addCheckbox("Validation with overlay", false);
			gd.showDialog();

			if (gd.wasCanceled()) {
				ip.resetThreshold();
				currentImage.updateAndDraw();
				return;
			}

			threshold = (int) gd.getNextNumber();
			slice = (int) gd.getNextNumber();
			fast = gd.getNextBoolean();
			minSize = (int) gd.getNextNumber();
			maxSize = (int) gd.getNextNumber();
			fraction = (double) gd.getNextNumber();
			tolerance = (int) gd.getNextNumber();
			showObjects = gd.getNextBoolean();
			showCentroids = gd.getNextBoolean();
			showCOM = gd.getNextBoolean();
			showPointsMap = gd.getNextBoolean();
			validate = gd.getNextBoolean();
			
			Prefs.set("ObjCounter-Options_showObjects.boolean", showObjects);
			Prefs.set("ObjCounter-Options_showCentroids.boolean", showCentroids);
			Prefs.set("ObjCounter-Options_showCOM.boolean", showCOM);
			Prefs.set("ObjCounter-Options_validate.boolean", validate);
			Prefs.set("ObjCounter-Options_showPointsMap.boolean", showPointsMap);
			
			Prefs.set("ObjCounter-Options_fast.boolean", fast);
			Prefs.set("ObjCounter-Options_minSize.integer", minSize);
			Prefs.set("ObjCounter-Options_maxSize.integer", maxSize);
			Prefs.set("ObjCounter-Options_fraction.double", fraction);
			Prefs.set("ObjCounter-Options_tolerance.integer", tolerance);

		}

		
		ip.resetThreshold();
		currentImage.updateAndDraw();

		if (!isSilent)
			IJ.log("STARTING");
		long start = System.currentTimeMillis();
		ConnectObjects OC = new ConnectObjects(currentImage, threshold, slice, minSize, maxSize, fraction, tolerance,
				fast, isSilent);

		dotSize = (int) Prefs.get("ObjCounter-Options_dotSize.double", 5);
		fontSize = (int) Prefs.get("ObjCounter-Options_fontSize.double", 10);
		showNb = Prefs.get("ObjCounter-Options_showNb.boolean", true);
		whiteNb = Prefs.get("ObjCounter-Options_whiteNb.boolean", true);

		if (outputPathPoints != null) {
				OC.writeCSV(outputPathPoints);
		}
		if (showObjects || outputPathObjects != null) {
			ImagePlus objectMap = OC.getObjMap(showNb, fontSize);
			IJ.run(objectMap, "glasbey inverted", null);
			if (outputPathObjects != null) {
				IJ.save(objectMap, outputPathObjects);
			} else {
				objectMap.show();
			}
		}
		if (showCentroids) {
			OC.getCentroidMap(showNb, whiteNb, dotSize, fontSize).show();
			IJ.run("glasbey inverted");
			OC.getResultsTable().show("Centroids");
		}
		if (showCOM) {
			OC.getCentreOfMassMap(showNb, whiteNb, dotSize, fontSize).show();
			IJ.run("glasbey inverted");
		}
		if (showPointsMap) {
			OC.computePointsMap().show();
		}

		if (validate) {
			OC.getValidation();
		}

		// compute all the time
		long elapsedTimeMillis = System.currentTimeMillis() - start;
		float elapsedTimeSec = elapsedTimeMillis / 1000F;

		if (!isSilent)
			IJ.log("DONE in :" + elapsedTimeSec + " seconds");
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		updateImg();
	}

	public void focusLost(FocusEvent e) {
		if (e.getSource().equals(values.elementAt(0))) {
			int val = (int) Tools.parseDouble(((TextField) values.elementAt(0)).getText());
			val = (int) Math.min(max, Math.max(min, val));
			((TextField) values.elementAt(0)).setText("" + val);
		}

		if (e.getSource().equals(values.elementAt(1))) {
			int val = (int) Tools.parseDouble(((TextField) values.elementAt(1)).getText());
			val = (int) Math.min(max, Math.max(min, val));
			((TextField) values.elementAt(1)).setText("" + val);
		}

		updateImg();
	}

	public void focusGained(FocusEvent e) {
	}

	private void updateImg() {
		threshold = ((Scrollbar) sliders.elementAt(0)).getValue();
		currentImage.setSlice(((Scrollbar) sliders.elementAt(1)).getValue());
		currentImage.resetDisplayRange();
		ip.setThreshold(threshold, max, ImageProcessor.RED_LUT);
	}
}
