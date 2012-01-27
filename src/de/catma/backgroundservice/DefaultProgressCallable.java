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
 * Default implementation of a {@link ProgressCallable}. It adds a field 
 * to maintain the instance of the {@link ProgressListener}.
 *
 * @author Marco Petris
 *
 * @param <V>
 */
public abstract class DefaultProgressCallable<V> 
	implements ProgressCallable<V> {
	private ProgressListener progressListener;
	
	/* (non-Javadoc)
	 * @see org.catma.backgroundservice.ProgressCallable#setProgressListener(org.catma.backgroundservice.ProgressListener)
	 */
	public void setProgressListener( ProgressListener progressListener ) {
		this.progressListener = progressListener;
	}
	
	/**
	 * @return the progress listener
	 */
	public ProgressListener getProgressListener() {
		return progressListener;
	}
	
}