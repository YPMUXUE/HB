package common.util;


import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class MyCollectionUtils {
	public static <R, T> Map<R, T> collectionToMap(Collection<T> collection, Function<T, R> consumer) {
		if (CollectionUtils.isEmpty(collection) || consumer == null) {
			return new HashMap<>(0);
		}
		Map<R, T> map = new HashMap<>(collection.size());
		for (T t : collection) {
			R r = consumer.apply(t);
			map.put(r, t);
		}
		return map;
	}

	public static <T> List<T> filter(Collection<T> list, Predicate<T> predicate) {
		List<T> result = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(list) && predicate != null) {
			for (T t : list) {
				if (predicate.test(t)) {
					result.add(t);
				}
			}
		}
		return result;
	}
}
