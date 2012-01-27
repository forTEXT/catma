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

import java.util.concurrent.Callable;

/**
 * A {@link Callable} which allows to set a listener for execution progress.
 *
 * @author Marco Petris
 *
 * @param <V>
 */
public interface ProgressCallable<V> extends Callable<V> {
	/**
	 * @param progressListener this listener can be notified by the implementer 
	 * of the {@link ProgressCallable}.
	 */
	public void setProgressListener( ProgressListener progressListener );
}