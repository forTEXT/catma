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

package de.catma.queryengine;


/**
 * The usual math compare operators.
 *
 * @author Marco Petris
 *
 */
public enum CompareOperator {

    /**
     * =
     */
    EQUAL("=", new Condition() {
        public <T extends Comparable<T>> boolean isTrue(T o1, T o2) {
            return o1.equals(o2);
        }
    }),
    /**
     * >
     */
    GREATERTHAN(">", new Condition() {
        public <T extends Comparable<T>> boolean isTrue(T o1, T o2) {
            return (o1.compareTo(o2) > 0);
        }
    }),
    /**
     * <
     */
    LESSTHAN("<", new Condition() {
        public <T extends Comparable<T>> boolean isTrue(T o1, T o2) {
            return (o1.compareTo(o2) < 0);
        }
    }),
    /**
     * >=
     */
    GREATEROREQUALTHAN(">=", new Condition() {
        public <T extends Comparable<T>> boolean isTrue(T o1, T o2) {
            return (o1.compareTo(o2) >= 0);
        }
    }),
    /**
     * <=
     */
    LESSOREQUALTHAN("<=", new Condition() {
        public <T extends Comparable<T>> boolean isTrue(T o1, T o2) {
            return (o1.compareTo(o2) <= 0);
        }
    }),
    ;

    /**
     * A condition with two operands that can be checked.
     */
    public static interface Condition {
        /**
         *
         * @param o1 first operand
         * @param o2 second operand
         * @param <T> type of the operands
         * @return <code>true</code> if the condition for the given operands holds true
         */
        public <T extends Comparable<T>> boolean isTrue(T o1, T o2);
    }
    
    private String value;
    private Condition condition;

    /**
     * Constructor
     * @param value the string representation of the condition's operator
     * @param condition the condition for the given string representation
     */
    CompareOperator(String value, Condition condition) {
        this.value = value;
        this.condition = condition;
    }

    /**
     * Retrieves the operator for the given string representation
     * @param value the string representation of the operator
     * @return the corresponding operator
     * @throws IllegalArgumentException if there is no operator for the given string representation
     */
    public static CompareOperator getOperatorFor(String value) {
        for(CompareOperator op : CompareOperator.values()) {
            if (op.value.equals(value)) {
                return op;
            }
        }
        throw new IllegalArgumentException(
                "Value " + value + " is not a known operator");
    }

    /**
     * @return the condition for this operator
     */
    public Condition getCondition() {
        return condition;
    }
    
    @Override
    public String toString() {
    	return value;
    }

}
