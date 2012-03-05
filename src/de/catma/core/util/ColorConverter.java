package de.catma.core.util;

public class ColorConverter {
	
	private int red;
	private int green;
	private int blue;
	
	public ColorConverter(int rgb) {
		red = (rgb >> 16) & 0xFF;
		green = (rgb >> 8) & 0xFF;
		blue = (rgb >> 0) & 0xFF;
	}
	
	public ColorConverter(String color) {
		this(Integer.valueOf(color));
	}

	public String toHex() {
		return fillUp(Integer.toHexString(red).toUpperCase()) 
				+ fillUp(Integer.toHexString(green).toUpperCase()) 
				+ fillUp(Integer.toHexString(blue).toUpperCase());
	}

	private String fillUp(String hexString) {
		if (hexString.length() < 2) {
			return "0"+hexString;
		}
		
		return hexString;
	}
}
