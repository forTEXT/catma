/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.util;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A converter for different RGB color formats.
 * 
 * @author marco.petris@web.de
 */
public class ColorConverter {
	
	/**
	 * Fills up the hexString with a leading zero.
	 * @param hexString
	 * @return a filled up hex string
	 */
	private static String fillUp(String hexString) {
		if (hexString.length() < 2) {
			return "0"+hexString;
		}
		
		return hexString;
	}
	
	/**
	 * @param colorInteger a string representation of an integer representing an RGB color
	 * consisting of the red component in bits 16-23, the green component in bits 8-15, 
	 * and the blue component in bits 0-7
	 * @return a corresponding hex string consisting of three hex values (without a leading #)
	 */
	public static String toHex(String colorInteger) {
		return toHex(Integer.valueOf(colorInteger));
	}

	/**
	 * @param rgb an array with three values for red, green and blue values
	 * @return a corresponding hex string consisting of three hex values (without a leading #)
	 */
	public static String toHex(int[] rgb) {
		return fillUp(Integer.toHexString(rgb[0]).toUpperCase()) 
				+ fillUp(Integer.toHexString(rgb[1]).toUpperCase()) 
				+ fillUp(Integer.toHexString(rgb[2]).toUpperCase());
	}
	
	/**
	 * @param rgb an integer representing an RGB color
	 * consisting of the red component in bits 16-23, the green component in bits 8-15, 
	 * and the blue component in bits 0-7
	 * @return a corresponding hex string consisting of three hex values (without a leading #)
	 */
	public static String toHex(int rgb) {
		int red = getRed(rgb);
		int green = getGreen(rgb);
		int blue = getBlue(rgb);
		return fillUp(Integer.toHexString(red).toUpperCase()) 
				+ fillUp(Integer.toHexString(green).toUpperCase()) 
				+ fillUp(Integer.toHexString(blue).toUpperCase());
	}
	
	private static int getRed(int rgb) {
		return (rgb >> 16) & 0xFF;
	}
	
	private static int getGreen(int rgb) {
		return (rgb >> 8) & 0xFF;
	}
	
	private static int getBlue(int rgb) {
		return (rgb >> 0) & 0xFF;
	}
	
	/**
	 * @param hex a string consisting of three hex values (6 characters in total, without a leading #)
	 * @return a string representation of an integer representing an RGB color
	 * consisting of the red component in bits 16-23, the green component in bits 8-15, 
	 * and the blue component in bits 0-7
	 */
	public static String toRGBIntAsString(String hex) {
		return String.valueOf(toRGBInt(hex));
	}
	
	/**
	 * @param hex a string consisting of three hex values (6 characters in total, without a leading #)
	 * @return an integer representing an RGB color
	 * consisting of the red component in bits 16-23, the green component in bits 8-15, 
	 * and the blue component in bits 0-7
	 */
	public static int toRGBInt(String hex) {
		if (hex.length() < 6) {
			throw new IllegalArgumentException("cannot convert " + hex);
		}
		String redString = hex.substring(0,2);
		String greenString = hex.substring(2,4);
		String blueString = hex.substring(4,6);
		
		int red = Integer.valueOf(redString, 16);
		int green = Integer.valueOf(greenString, 16);
		int blue = Integer.valueOf(blueString, 16);
		
		return ((255 & 0xFF) << 24) |
                ((red & 0xFF) << 16) |
                ((green & 0xFF) << 8)  |
                ((blue & 0xFF) << 0);
	}

	/**
	 * @return a random hex color consisting of three hex values (without a leading #)
	 */
	public static String randomHex() {
		return toHex(getRandomColor());
	}
	
	/**
	 * @return an array with three integers for red, green and blue representing a non dark color 
	 */
	public static int[] getRandomNonDarkColor() {
		int r = getRandomNonDarkInt();
		int g = getRandomNonDarkInt();
		int b = getRandomNonDarkInt();
		return new int[] {r, g, b};
	}
	
	private static int getRandomNonDarkInt() {
		Random r = new Random();
		
		int i = r.nextInt(255);
		while (i<20) {
			i = r.nextInt(255);
		}
		return i;
	}
	
	private static int getRandomInt() {
		return new Random().nextInt(255);
	}
	
	/**
	 * @return a random color as an array of three integers for red, green and blue
	 */
	public static int[] getRandomColor() {
		int r = getRandomInt();
		int g = getRandomInt();
		int b = getRandomInt();
		return new int[] {r, g, b};
	}

	public static boolean isLightColor(String colorInteger) {
		int rgb = Integer.valueOf(colorInteger);
		int r = getRed(rgb);
		int g = getGreen(rgb);
		int b = getBlue(rgb);
		return isLightColor(r, g, b);
	}
	
	public static boolean isLightColor(int r, int g, int b) {
		// copied from https://codepen.io/WebSeed/pen/pvgqEq (Will Bamford)
		double perceptiveLuminance = 1 - (0.299 * r + 0.587 * g + 0.114 * b) / 255;
		return (perceptiveLuminance < 0.5);
	}
	
	public static Set<String> getColorNames() {
		String colorNames = 
			"Amaranth,Amber,Amethyst,Apricot,Aquamarine,Azure,"
			+ "Baby blue,Beige,Black,Blue,Blue-green,Blue-violet,Blush,Bronze,Brown,Burgundy,Byzantium,"
			+ "Carmine,Cerise,Cerulean,Champagne,Chocolate,Cobalt blue,Coffee,Copper,Coral,Crimson,Cyan,"
			+ "Desert sand,"
			+ "Electric blue,Emerald,Erin,Gold,"
			+ "Gray,Green,"
			+ "Harlequin,"
			+ "Indigo,Ivory,"
			+ "Jade,Jungle green,Lavender,"
			+ "Lemon,Lilac,Lime,"
			+ "Magenta,Magenta rose,Maroon,Mauve,"
			+ "Navy blue,"
			+ "Ochre,Olive,Orange,Orange-red,Orchid,"
			+ "Peach,Pear,Periwinkle,Persian blue,Pink,Plum,Prussian blue,Puce,Purple,"
			+ "Raspberry,Red,Red-violet,Rose,Ruby,"
			+ "Salmon,Sangria,Sapphire,Scarlet,Silver,Slate gray,Spring bud,Spring green,"
			+ "Tan,Taupe,Teal,Turquoise,"
			+ "Ultramarine,Violet,Viridian,"
			+ "White,"
			+ "Yellow";
		Set<String> colorNameSet = new HashSet<String>();
		for (String color : colorNames.split(Pattern.quote(","))) {
			colorNameSet.add(color);
		}
		return colorNameSet;
	}

}
