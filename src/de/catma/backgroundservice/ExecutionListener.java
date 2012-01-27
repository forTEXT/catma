/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009  University Of Hamburg
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

/**
 * A listener which will be notified by the {@link BackgroundService} when the  
 * execution of a {@link ProgressCallable} has been {@link #done}. 
 *
 * @author Marco Petris
 *
 * @param <X> the type of the result of the execution
 */
public interface ExecutionListener<X> {
	/**
	 * Will be called when the execution has been done.
	 * @param result may be null if there was no result.
	 */
	public void done( X result );
}