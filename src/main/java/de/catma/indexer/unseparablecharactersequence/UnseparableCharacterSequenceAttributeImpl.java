/*
 * CATMA Computer Aided Text Markup and Analysis
 *
 *    Copyright (C) 2008-2010  University Of Hamburg
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

package de.catma.indexer.unseparablecharactersequence;

import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/**
 *  An attribute that states if a sequence is separable or not.
 *
 * @author Marco Petris
 *
 */
public class UnseparableCharacterSequenceAttributeImpl
        extends AttributeImpl implements UnseparableCharacterSequenceAttribute  {

    private boolean isUnseparable;

    /**
     * Constructor.
     */
    public UnseparableCharacterSequenceAttributeImpl() {
        isUnseparable = false;
    }

    public boolean isUnseparable() {
        return isUnseparable;
    }

    public void setUnseparable(boolean unseparable) {
        isUnseparable = unseparable;
    }

    @Override
    public void clear() {
        isUnseparable = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UnseparableCharacterSequenceAttributeImpl that = (UnseparableCharacterSequenceAttributeImpl) o;

        return isUnseparable == that.isUnseparable;
    }

    @Override
    public int hashCode() {
        return (isUnseparable ? 1 : 0);
    }

    @Override
    public void copyTo(AttributeImpl target) {
        if (target instanceof UnseparableCharacterSequenceAttribute) {
            ((UnseparableCharacterSequenceAttribute)target).setUnseparable(isUnseparable);
        }
        else {
            throw new IllegalArgumentException(
                    "must implement inteface " +
                            UnseparableCharacterSequenceAttribute.class.getName()); 
        }
    }
    
    @Override
    public void reflectWith(AttributeReflector reflector) {
    	reflector.reflect(UnseparableCharacterSequenceAttribute.class, "isUnseparable", isUnseparable);
    	
    }
}
