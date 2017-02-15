
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
import ij.Macro;
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

	private static final String PARAM_SILENT = "silent";
	private static final String PARAM_FAST = "fast";
	private static final String PARAM_THRESHOLD = "threshold";
	private static final String PARAM_SLICE = "slice";
	private static final String PARAM_MIN = "min";
	private static final String PARAM_MAX = "max";
	private static final String PARAM_FRACTION = "fraction";
	private static final String PARAM_TOLERANCE = "tolerance";
	private static final String PARAM_SHOW_CENTROIDS = "show_centroids";
	private static final String PARAM_SHOW_COM = "show_com";
	private static final String PARAM_COMPUTE_POINTS_MAP = "compute_points_map";
	private static final String PARAM_EXPORT_OBJECTS = "export_objects";
	private static final String PARAM_EXPORT_POINTS = "export_points";
	private static final String PARAM_EXPORT_RESULTS = "export_results";
	private static final String PARAM_VALIDATE = "validate";
	private static final String PARAM_OUTPUT_OBJECTS = "output_objects";
	private static final String PARAM_OUTPUT_POINTS = "output_points";

	private ImagePlus currentImage;
	private ImageProcessor ip;
	private int width, height, nbSlices, length, tolerance;
	private double min, max;
	private String title, redirectTo;
	private int threshold, slice, minSize, maxSize, dotSize, fontSize;
	private boolean excludeOnEdges, exportObjects, showCentroids, showCOM, showNb, whiteNb, redirect, fast,
			exportResults;
	private Vector sliders, values;
	private double fraction = 0.5;
	private boolean validate = false;
	private boolean computePointsMap = false;
	private boolean exportPoints = false;

	public void run(String arg) {
		if (IJ.versionLessThan("1.39i"))
			return;

		String macroOptions = Macro.getOptions();
		boolean isSilent = Boolean.parseBoolean(Macro.getValue(macroOptions, PARAM_SILENT, "false"));

		currentImage = WindowManager.getCurrentImage();

		if (currentImage == null) {
			IJ.error("You need to open an image first.");
			return;
		}

		if (currentImage.getBitDepth() > 16) {
			IJ.error("ObjectCounter only works on 8-bits or 16-bits image.");
			return;
		}

		width = currentImage.getWidth();
		height = currentImage.getHeight();
		nbSlices = currentImage.getStackSize();
		length = height * width * nbSlices;
		title = currentImage.getTitle();

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

		minSize = (int) Prefs.get("3D-OC_minSize.double", 10);
		maxSize = length;
		excludeOnEdges = Prefs.get("3D-OC_excludeOnEdges.boolean", true);
		exportObjects = Prefs.get("3D-OC_showObj.boolean", true);
		showCentroids = Prefs.get("3D-OC_showCentro.boolean", true);
		showCOM = Prefs.get("3D-OC_showCOM.boolean", true);

		redirectTo = Prefs.get("3D-OC-Options_redirectTo.string", "none");
		redirect = !this.redirectTo.equals("none") && WindowManager.getImage(this.redirectTo) != null;

		if (redirect) {
			ImagePlus imgRedir = WindowManager.getImage(this.redirectTo);
			if (!(imgRedir.getWidth() == this.width && imgRedir.getHeight() == this.height
					&& imgRedir.getNSlices() == this.nbSlices) || imgRedir.getBitDepth() > 16) {
				redirect = false;
				IJ.log("Redirection canceled: images should have the same size and a depth of 8- or 16-bits.");
			}
			if (imgRedir.getTitle().equals(this.title)) {
				redirect = false;
				IJ.log("Redirection canceled: both images have the same title.");
			}
		}

		if (!redirect) {
			Prefs.set("3D-OC-Options_redirectTo.string", "none");
			Prefs.set("3D-OC-Options_showMaskedImg.boolean", false);
		}

		if (isSilent) {
			try {
				threshold = Integer.parseInt(Macro.getValue(macroOptions, PARAM_THRESHOLD, String.valueOf(threshold)));
				slice = Integer.parseInt(Macro.getValue(macroOptions, PARAM_SLICE, String.valueOf(nbSlices / 2)));
				fast = Boolean.parseBoolean(Macro.getValue(macroOptions, PARAM_FAST, "false"));
				minSize = Integer.parseInt(Macro.getValue(macroOptions, PARAM_MIN, "0"));
				maxSize = Integer.parseInt(Macro.getValue(macroOptions, PARAM_MAX, "0"));
				fraction = Double.parseDouble(Macro.getValue(macroOptions, PARAM_FRACTION, "0.5"));
				tolerance = Integer.parseInt(Macro.getValue(macroOptions, PARAM_TOLERANCE, "0"));
				showCentroids = Boolean.parseBoolean(
						Macro.getValue(macroOptions, PARAM_SHOW_CENTROIDS, String.valueOf(showCentroids)));
				showCOM = Boolean.parseBoolean(Macro.getValue(macroOptions, PARAM_SHOW_COM, String.valueOf(showCOM)));
				computePointsMap = Boolean
						.parseBoolean(Macro.getValue(macroOptions, PARAM_COMPUTE_POINTS_MAP, "false"));
				exportObjects = Boolean.parseBoolean(
						Macro.getValue(macroOptions, PARAM_EXPORT_OBJECTS, String.valueOf(exportObjects)));
				exportPoints = Boolean.parseBoolean(Macro.getValue(macroOptions, PARAM_EXPORT_POINTS, "false"));
				exportResults = Boolean.parseBoolean(Macro.getValue(macroOptions, PARAM_EXPORT_RESULTS, "false"));
				validate = Boolean.parseBoolean(Macro.getValue(macroOptions, PARAM_VALIDATE, "false"));
			} catch (Exception ex) {
				IJ.error("Any param format is incorrect.");
				return;
			}
		} else {
			GenericDialog gd = new GenericDialog("ObjCounter v0.0.1 (beta)");

			gd.addSlider("Threshold", min, max, threshold);
			gd.addSlider("Slice", 1, nbSlices, nbSlices / 2);

			sliders = gd.getSliders();
			((Scrollbar) sliders.elementAt(0)).addAdjustmentListener(this);
			((Scrollbar) sliders.elementAt(1)).addAdjustmentListener(this);
			values = gd.getNumericFields();
			((TextField) values.elementAt(0)).addFocusListener(this);
			((TextField) values.elementAt(1)).addFocusListener(this);

			gd.addCheckbox("fast algorithm", false);
			gd.addMessage("Size filter: ");
			gd.addNumericField("Min.", minSize, 0);
			gd.addNumericField("Max.", maxSize, 0);
			gd.addNumericField("Fraction for connections ", 0.5, 3);
			gd.addNumericField("tollerance for propagation", 0, 0);
			gd.addCheckbox("Exclude_objects_on_edges", excludeOnEdges);
			gd.addMessage("Maps to show: ");
			gd.addCheckbox("Objects", exportObjects);
			gd.addCheckbox("Centroids", showCentroids);
			gd.addCheckbox("Centres_of_masses", showCOM);
			gd.addCheckbox("Point map", false);
			gd.addCheckbox("Export_points to CSV", false);
			gd.addCheckbox("Export_results to CSV", false);
			gd.addCheckbox("validation with overlay", false);

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
			excludeOnEdges = gd.getNextBoolean();
			exportObjects = gd.getNextBoolean();
			showCentroids = gd.getNextBoolean();
			showCOM = gd.getNextBoolean();
			computePointsMap = gd.getNextBoolean();
			exportPoints = gd.getNextBoolean();
			exportResults = gd.getNextBoolean();
			validate = gd.getNextBoolean();
		}

		Prefs.set("3D-OC_minSize.double", minSize);
		Prefs.set("3D-OC_excludeOnEdges.boolean", excludeOnEdges);
		Prefs.set("3D-OC_showObj.boolean", exportObjects);
		Prefs.set("3D-OC_showCentro.boolean", showCentroids);
		Prefs.set("3D-OC_showCOM.boolean", showCOM);
		Prefs.set("Obj_Count_fraction.double", fraction);
		Prefs.set("Obj_Count_toll.int", tolerance);
		if (!redirect)
			Prefs.set("3D-OC-Options_redirectTo.string", "none");

		ip.resetThreshold();
		currentImage.updateAndDraw();

		IJ.log("STARTING");
		long start = System.currentTimeMillis();
		ConnectObjects OC = new ConnectObjects(currentImage, threshold, slice, minSize, maxSize, fraction, tolerance,
				fast, excludeOnEdges);

		dotSize = (int) Prefs.get("3D-OC-Options_dotSize.double", 5);
		fontSize = (int) Prefs.get("3D-OC-Options_fontSize.double", 10);
		showNb = Prefs.get("3D-OC-Options_showNb.boolean", true);
		whiteNb = Prefs.get("3D-OC-Options_whiteNb.boolean", true);

		if (exportPoints) {
			String outputPath = Macro.getValue(macroOptions, PARAM_OUTPUT_POINTS, null);
			if (outputPath != null) {
				OC.writeCSV(outputPath);
			} else {
				OC.getResultsTable().show("Centroids");
			}
		}
		if (exportObjects) {
			ImagePlus objectMap = OC.getObjMap(showNb, fontSize);
			IJ.run(objectMap, "3-3-2 RGB", null);
			String outputPath = Macro.getValue(macroOptions, PARAM_OUTPUT_OBJECTS, null);
			if (outputPath != null) {
				IJ.save(objectMap, outputPath);
			} else {
				objectMap.show();
			}
		}
		if (showCentroids) {
			OC.getCentroidMap(showNb, whiteNb, dotSize, fontSize).show();
			IJ.run("3-3-2 RGB");
		}
		if (showCOM) {
			OC.getCentreOfMassMap(showNb, whiteNb, dotSize, fontSize).show();
			IJ.run("3-3-2 RGB");
		}
		if (computePointsMap) {
			OC.computePointsMap().show();
		}

		if (validate) {
			OC.getValidation();
		}

		if (exportResults) {
			holes_detection hc = new holes_detection(title);
			hc.detect();

			Calibration cal = currentImage.getCalibration();
			double voxelVolume = cal.getX(1) * cal.getY(1) * cal.getZ(1);

			int[] numbPoint = OC.getNumberOfPoint();
			double volume = width * height * nbSlices * voxelVolume;
			double density = numbPoint[0] / volume;
			double densS = numbPoint[1] / volume;
			double densH = numbPoint[0] / (volume - hc.getHolesVolume());
			double densSH = numbPoint[1] / (volume - hc.getHolesVolume());

			String fileName = "";
			try {
				if (fileName == "") {
					fileName = title + "_results.csv";
				}
				Path fileCSV = Files.createFile(Paths.get(IJ.getDirectory("current"), fileName));
				String line1 = "stackName, numberObject, numberObjectSter,volume, volumeHoles, denisty, density_Holes, density_Ster, density_Ster_Holes"
						+ System.lineSeparator();
				String line2 = title + "," + OC.getNumberOfPoint()[0] + "," + OC.getNumberOfPoint()[1] + "," + volume
						+ "," + hc.getHolesVolume() + "," + density + "," + densH + "," + densS + "," + densSH
						+ System.lineSeparator();
				Files.write(fileCSV, line1.getBytes(), StandardOpenOption.APPEND);
				Files.write(fileCSV, line2.getBytes(), StandardOpenOption.APPEND);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// compute all the time
		long elapsedTimeMillis = System.currentTimeMillis() - start;
		float elapsedTimeSec = elapsedTimeMillis / 1000F;

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
