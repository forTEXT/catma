package de.catma.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Collections3 {
	
	public static interface Function3<T, S> {
		public S apply(T t);
	}
	
	/**
	 * @return all elements that are present in the first collection but not present in the second.
	 */
	public static <T> Collection<T> getSetDifference(Collection<T> col1, Collection<T> col2) {
		List<T> sDiff = new ArrayList<T>();
		
		for (T t : col1) {
			if (!col2.contains(t)) {
				sDiff.add(t);
			}
		}
		
		return sDiff;
	}
	
	public static <T> Collection<T> getUnion(Collection<T> col1, Collection<T> col2) {
		List<T> union = new ArrayList<T>();
		union.addAll(col1);
		union.addAll(col2);
		return union;
	}
	
	public static <T> Collection<T> getUnion(T[] col1, T... col2) {
		if (col2 == null) {
			return Arrays.asList(col1);
		}
		return getUnion(Arrays.asList(col1), Arrays.asList(col2));
	}

	public static <T, S> Collection<S> transform(
			Collection<T> children,
			Function3<T, S> function) {
		
		ArrayList<S> result = new ArrayList<S>();
		
		for (T t : children) {
			result.add(function.apply(t));
		}
		
		return result;
	}
}
