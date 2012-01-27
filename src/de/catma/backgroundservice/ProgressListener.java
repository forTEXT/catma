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
 * A listener for progress.
 *
 * @author Marco Petris
 *
 */
public interface ProgressListener {
	/**
	 * Called when the execution has finished.
	 */
	public void finish();
	/**
	 * Initialization of the execution.
	 * @param jobSize the size of the job, e. g. 100%
	 * @param jobName the name of the job
	 * @param args optional arguments 
	 * (can be used to support internationalization of the jobName)
	 */
	public void start( int jobSize, String jobName, Object... args );
	/**
	 * Indicates the current progress.
	 * @param alreadyDoneSize the portion of the work which has already be done, 
	 * must be a fragment of the jobSize given at {@link #start}.
	 */
	public void update( int alreadyDoneSize );
	/**
	 * Indicats an indeterminate jobsize.
	 * @param indeterminate flag: true->start indeterminate job, false->stop
	 * @param jobName the name of the job
	 * @param args optional arguments
	 * (can be used to support internationalization of the jobName)
	 */
	public void setIndeterminate( 
			boolean indeterminate, String jobName, Object... args );
}