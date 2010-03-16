package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

public class ArraysHelper {

	public static List<String> asStringList(String[] fields) {
		Assert.notNull(fields);
		
		List<String> list = new ArrayList<String>();
		for (String field : fields) {
			list.add(field);
		}
		return list;
	}

	public static String[] toStringArray(List<String> list) {
		Assert.notNull(list);

		String[] tab = new String[list.size()];
		for (int i = 0; i < tab.length; i++) {
			String str = list.get(i);
			tab[i] = str;
		}
		return tab;
	}

	public static String[] concatStringArrays(String[] fields1, String[] fields2) {
		Assert.notNull(fields1);
		Assert.notNull(fields2);
		
		List<String> list = asStringList(fields1);
		List<String> list2 = asStringList(fields2);
		list.addAll(list2);
		return toStringArray(list);
	}

	public static List<String> concatStringLists(List<String> list1, List<String> list2) {
		Assert.notNull(list1);
		Assert.notNull(list2);
		
		ArrayList<String> list = new ArrayList<String>();
		list.addAll(list1);
		list.addAll(list2);
		return list;
	}

}
