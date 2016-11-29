package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CollectionsUtilsTest {

	@Test
	public void testRemoveCommonElementsCasSimple() throws Exception {
		final List<String> left = L("a", "b", "c");
		final List<String> right = L("a", "b", "d");
		CollectionsUtils.removeCommonElements(left, right, null);
		assertEquals(L("c"), left);
		assertEquals(L("d"), right);
	}

	@Test
	public void testRemoveCommonElementsEqualityFunctor() throws Exception {
		final List<String> left = L("axx", "byy", "czz");
		final List<String> right = L("auu", "bvv", "dww");
		// un function d'égalité qui ne considère que le premier caractère
		CollectionsUtils.removeCommonElements(left, right, (l, r) -> l.charAt(0) == r.charAt(0));
		assertEquals(L("czz"), left);
		assertEquals(L("dww"), right);
	}

	@Test
	public void testRemoveCommonElementsDansLeDesordre() throws Exception {
		final List<String> left = L("a", "b", "c");
		final List<String> right = L("d", "b", "a");
		CollectionsUtils.removeCommonElements(left, right, null);
		assertEquals(L("c"), left);
		assertEquals(L("d"), right);
	}

	@Test
	public void testRemoveCommonElementsCollectionsVides() throws Exception {
		final List<String> left = L();
		final List<String> right = L("d", "b", "a");
		CollectionsUtils.removeCommonElements(left, right, null);
		assertEquals(L(), left);
		assertEquals(L("d", "b", "a"), right);
	}

	@Test
	public void testRemoveCommonElementsUneSeuleCollection() throws Exception {
		final List<String> left = L("a", "b", "c");
		CollectionsUtils.removeCommonElements(left, left, (l, r) -> {
			fail("Ne devrait pas comparer les éléments quand la même liste est passée deux fois en paramètre");
			return false;
		});
		assertEquals(L(), left);
	}

	@Test
	public void testRemoveCommonElementsElementsDupliques() throws Exception {
		final List<String> left = L("a", "a", "b", "c", "a");
		final List<String> right = L("a", "d", "e");
		CollectionsUtils.removeCommonElements(left, right, null);
		assertEquals(L("a", "b", "c", "a"), left);
		assertEquals(L("d", "e"), right);
	}

	@SafeVarargs
	@NotNull
	private static <T> ArrayList<T> L(T... t) {
		final ArrayList<T> list = new ArrayList<T>(t.length);
		Arrays.stream(t).forEach(list::add);
		return list;
	}
}