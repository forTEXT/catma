package de.catma.repository.db.jooq;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Record;
import org.jooq.Result;

import com.google.common.base.Function;

public class ResultUtil {

	public static <T> Map<T, List<Record>> asGroups(
			Function<Record, T> keyMapper, Result<Record> records) {
		LinkedHashMap<T, List<Record>> groups = new LinkedHashMap<T, List<Record>>();
		
		for (Record r : records) {
			List<Record> group = null;
			T key = keyMapper.apply(r);
			if (!groups.containsKey(key)) {
				group = new ArrayList<Record>();
				groups.put(key, group);
			}
			else {
				group = groups.get(key);
			}
			
			group.add(r);
		}
		
		return groups;
	}
}
