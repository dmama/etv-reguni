package ch.vd.unireg.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

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
	private static <T> List<T> L(T... t) {
		final ArrayList<T> list = new ArrayList<T>(t.length);
		list.addAll(Arrays.asList(t));
		return list;
	}

	@Test
	public void testMerged() throws Exception {
		final List<String> left = L("a", "b", "z");
		final List<String> right = L("f", "r", "t");
		final Iterable<String> merged = CollectionsUtils.merged(left, right);

		// une première fois...
		{
			final List<String> collected = StreamSupport.stream(merged.spliterator(), false).collect(Collectors.toList());
			assertEquals(L("a", "b", "z", "f", "r", "t"), collected);
		}

		// ... et une deuxième fois, pour vérifier que l'itérable est manipulable plusieurs fois
		{
			final List<String> collected = StreamSupport.stream(merged.spliterator(), false).collect(Collectors.toList());
			assertEquals(L("a", "b", "z", "f", "r", "t"), collected);
		}
	}

	@Test
	public void testRevertedOrder() throws Exception {
		final List<String> list = L("a", "b", "z");
		final Iterable<String> reversed = CollectionsUtils.revertedOrder(list);

		// une première fois...
		{
			final List<String> collected = StreamSupport.stream(reversed.spliterator(), false).collect(Collectors.toList());
			assertEquals(L("z", "b", "a"), collected);
		}

		// ... et une deuxième fois, pour vérifier que l'itérable est manipulable plusieurs fois
		{
			final List<String> collected = StreamSupport.stream(reversed.spliterator(), false).collect(Collectors.toList());
			assertEquals(L("z", "b", "a"), collected);
		}
	}

	/**
	 * Vérifie que le mapping fonctionne bien dans le cas limite où l'exécuteur service est limité à un thread.
	 */
	@Test
	public void testParallelMapOneThread() {

		final List<Integer> ids = IntStream.range(0, 10).boxed().collect(Collectors.toList());
		final ExecutorService executor = Executors.newSingleThreadExecutor();

		final List<Integer> list = CollectionsUtils.parallelMap(ids, id -> id * 10, executor);
		assertEquals(10, list.size());
		assertEquals(Integer.valueOf(0), list.get(0));
		assertEquals(Integer.valueOf(10), list.get(1));
		assertEquals(Integer.valueOf(20), list.get(2));
		assertEquals(Integer.valueOf(30), list.get(3));
		assertEquals(Integer.valueOf(40), list.get(4));
		assertEquals(Integer.valueOf(50), list.get(5));
		assertEquals(Integer.valueOf(60), list.get(6));
		assertEquals(Integer.valueOf(70), list.get(7));
		assertEquals(Integer.valueOf(80), list.get(8));
		assertEquals(Integer.valueOf(90), list.get(9));

		executor.shutdown();
	}

	/**
	 * Vérifie que le mapping fonctionne bien dans le cas où l'exécuteur service permet 10 threads.
	 */
	@Test
	public void testParallelMapTenThreads() {

		final int count = 10000;
		final List<Integer> ids = IntStream.range(0, count).boxed().collect(Collectors.toList());
		final ExecutorService executor = Executors.newFixedThreadPool(10);

		final List<Integer> list = CollectionsUtils.parallelMap(ids, id -> id * 10, executor);
		list.sort(Comparator.naturalOrder());
		assertEquals(count, list.size());

		for(int i = 0; i < count; ++i) {
			assertEquals(Integer.valueOf(i * 10), list.get(i));
		}

		executor.shutdown();
	}

	/**
	 * Vérifie que les fonctions de mapping sont bien exécutées en parallèle.
	 */
	@Test(timeout = 1500)
	public void testParallelMapTenThreadsWithSleep() {

		final int count = 10;
		final List<Integer> ids = IntStream.range(0, count).boxed().collect(Collectors.toList());
		final ExecutorService executor = Executors.newFixedThreadPool(10);

		// 10 ids mappés en 1s sur 10 threads en parallèle -> temps total devrait être en dessous de 1,5 secondes
		final List<Integer> list = CollectionsUtils.parallelMap(ids, id -> {
			sleep(1000);
			return id * 10;
		}, executor);
		list.sort(Comparator.naturalOrder());
		assertEquals(count, list.size());

		for(int i = 0; i < count; ++i) {
			assertEquals(Integer.valueOf(i * 10), list.get(i));
		}

		executor.shutdown();
	}

	private static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}