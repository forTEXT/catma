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
package de.catma.backgroundservice;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


public class LogProgressListener implements ProgressListener {
	
	private Logger logger = Logger.getLogger(LogProgressListener.class.getName());

	public void setProgress(String value, Object... args) {
		logger.info(
			value + ((args != null)? (" " +Arrays.toString(args)):""));
	}

	public void setException(Throwable t) {
		logger.log(Level.SEVERE, "error during job execution", t);
	}

}
