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





import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

/**
 *
 * @author gherardo varando gherardo.varando@gmail.com
 */
public class ObjCounter_Options implements PlugIn{
	public void run(String arg) {

		int dotSize=(int) Prefs.get("ObjCounter-Options_dotSize.double", 5);
		int fontSize=(int) Prefs.get("ObjCounter-Options_fontSize.double", 10);
		boolean showNb=Prefs.get("ObjCounter-Options_showNb.boolean", true);
		boolean whiteNb=Prefs.get("ObjCOunter-Options_whiteNb.boolean", true);


		GenericDialog gd=new GenericDialog("ObjCOunter options");
		gd.addMessage("");
		gd.addMessage("Style parameters:");
		gd.addNumericField("Dots_size", dotSize, 0);
		gd.addNumericField("Font_size", fontSize, 0);
		gd.addCheckbox("Show_numbers", showNb);
		gd.addCheckbox("White_numbers", whiteNb);
		gd.showDialog();

		if (gd.wasCanceled()) return;

		Prefs.set("ObjCounter-Options_dotSize.double", (int) gd.getNextNumber());
		Prefs.set("ObjCounter-Options_fontSize.double", (int) gd.getNextNumber());
		Prefs.set("ObjCounter-Options_showNb.boolean", gd.getNextBoolean());
		Prefs.set("ObjCounter-Options_whiteNb.boolean", gd.getNextBoolean());
	}
}