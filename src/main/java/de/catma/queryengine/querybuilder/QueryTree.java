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

package de.catma.queryengine.querybuilder;

import de.catma.queryengine.MatchMode;

/**
 * A tree of queries. The queries are connected via inclusion, exclusion or refinement.
 *
 * @author Marco Petris
 *
 * @see org.catma.queryengine.ExclusionQuery
 * @see org.catma.queryengine.UnionQuery
 * @see org.catma.queryengine.Refinement
 *
 */
public class QueryTree {

    /**
     * A node of the tree.
     */
    private static class Node {
        private Node left;
        private Node right;
        private String value;
		private String postfix = "";

        /**
         * Constructor for an empty node.
         */
        private Node() {
        }

        /**
         * Constructor.
         * @param value the value of this node, i. e. a query or one of the connector strings that are allowed to
         * chain queries together.
         * @param postfix e.g a {@link MatchMode} or an empty string (<code>null</code> is not allowed)
         *
         * @see org.catma.queryengine.ExclusionQuery
         * @see org.catma.queryengine.UnionQuery
         * @see org.catma.queryengine.Refinement
         */
        private Node(String value, String postfix) {
            this.value = value;
            this.postfix = postfix;
        }

        /**
         * @return a representation of this node with correct parenthesis
         */
        @Override
        public String toString() {
            if ((left != null)&&(right != null)) {
                return "(" + left + ") " + value + " (" +right + ") " + postfix;
            }
            else if (left != null) {
                return "(" + left + ") " + value + " (NA) " + postfix;
            }
            else if (value != null){
                return value + postfix;
            }
            else {
                return "";
            }
        }
    }

    private Node curNode;

    /**
     * Constructor.
     */
    public QueryTree() {
        curNode = new Node();
    }

    /**
     * Adds the given element to this tree.
     *
     * @param element the element to add, this can be a query string or one of the connector strings
     */
    public void add(String element) {
    	add(element, "");
    }
    /**
     * Adds the given element to this tree.
     *
     * @param element the element to add, this can be a query string or one of the connector strings
     * @param postfix e.g a {@link MatchMode} or an empty string (<code>null</code> is not allowed)
     */
    public void add(String element, String postfix) {

        // elements are added in that order 1. value 2. left node 3. right node
        // that ensures a correct construction of the tree

        if (curNode.value == null) {
            curNode.value = element;
            curNode.postfix = postfix;
        }
        else {
            if ((curNode.left == null) || (curNode.right != null)) {
                Node node = new Node(element, postfix);
                node.left = curNode;
                curNode = node;
            }
            else if (curNode.right == null) {
                curNode.right = new Node(element, postfix);
            }
        }
    }

    /**
     * Removes the last element that has been added.
     */
    public void removeLast() {
        // the elements are removed in the reversed order they are added
        // see add-method

        if (curNode.right != null) {
            curNode.right = null;
        }
        else if (curNode.left != null) {
            curNode = curNode.left;
        }
        else {
            curNode.value = null;
        }
    }

    
    public String getLast() {
        return curNode.value;
    }
    
    @Override
    public String toString() {
        return curNode.toString();
    }

    /**
     * This method gives a preview string representation of how things would look like if the given
     * element would be added to the tree.
     * @param nextElement the element we want to "add" to generate the preview
     * @return the preview representation of this tree
     */
    public String toString(String nextElement) {
        add(nextElement);
        String buf = toString();
        removeLast();
        
        return buf;
    }

    /**
     * @return <code>true</code> if this node has no children, else <code>false</code>
     */
    public boolean isLeaf() {
        return (curNode.left == null)&&(curNode.right == null);
    }
}
