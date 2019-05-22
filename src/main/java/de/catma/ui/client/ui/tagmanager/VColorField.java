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
package de.catma.ui.client.ui.tagmanager;

import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.Composite;
import com.vaadin.client.ui.Field;

public class VColorField extends Composite implements Field {
	public interface VColorFieldListener {
		public void colorChanged(String hexColor);
	}
	
//	private ColorPicker colorPicker;
	private VColorFieldListener colorFieldListener;
	
	public VColorField() {
//		colorPicker = new ColorPicker() {
//			@Override
//			public void onChange(Widget sender) {
//				super.onChange(sender);
//				colorFieldListener.colorChanged(getHexColor());
//			}
//		};
//		int [] randomColor = getRandomColor();
//		try {
//			colorPicker.setRGB( randomColor[0], randomColor[1], randomColor[2]);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		initWidget(colorPicker);
	}
	
	public void setHexColor(String hexColor) {
//		try {
//			colorPicker.setHex(hexColor);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}			
	}
	
	public void setColorFieldListener(VColorFieldListener colorFieldListener) {
		this.colorFieldListener = colorFieldListener;
	}
	
	private int[] getRandomColor() {
		int i = Random.nextInt(3);
		switch(i) {
			case 0 : {
				return new int[] { 255, 0, 0};
			}
			case 1 : {
				return new int[] { 0, 255, 0};
			}
			case 2 : {
				return new int[] { 0, 0, 255};
			}
			default : {
				return new int[] { 0, 0, 255};
			}
		}
	}
}
