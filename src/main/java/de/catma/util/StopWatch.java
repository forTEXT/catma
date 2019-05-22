/*
 *    CATMA Computer Aided Text Markup and Analysis
 * 
 *    Copyright (C) 2009  University Of Hamburg
 * 
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 * 
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.catma.util;

/**
 * A simple stopwatch to measure execution time.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class StopWatch {
    private long ms;
    private String name = "";
    
    public StopWatch(String name) {
    	this();
		this.name = name;
	}

	/**
     * Constructor. Starts the watch.
     */
    public StopWatch() {
        ms = System.currentTimeMillis();
    }

    /**
     * @return a string representation of the milliseconds passed since the watch started
     */
    @Override
    public String toString() {
        return name + " " + String.valueOf((System.currentTimeMillis()-ms));
    }
}
