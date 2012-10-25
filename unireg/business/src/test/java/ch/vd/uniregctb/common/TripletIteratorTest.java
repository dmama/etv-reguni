package ch.vd.uniregctb.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class TripletIteratorTest extends WithoutSpringTest {

	@Test
	public void testEmptyIterator() {
		Iterator<Object> empty = Collections.emptyList().iterator();
		TripletIterator<Object> iter = new TripletIterator<Object>(empty);
		assertFalse(iter.hasNext());
	}

	@Test
	public void testOneElementIterator() {
		List<Integer> list = new ArrayList<Integer>();
		list.add(Integer.valueOf(0));

		TripletIterator<Integer> iter = new TripletIterator<Integer>(list.iterator());
		assertTrue(iter.hasNext());
		assertTriplet(null, Integer.valueOf(0), null, iter.next());
		assertFalse(iter.hasNext());
	}

	@Test
	public void testTwoElementsIterator() {
		List<Integer> list = new ArrayList<Integer>();
		list.add(Integer.valueOf(0));
		list.add(Integer.valueOf(1));

		TripletIterator<Integer> iter = new TripletIterator<Integer>(list.iterator());
		assertTrue(iter.hasNext());
		assertTriplet(null, Integer.valueOf(0), Integer.valueOf(1), iter.next());
		assertTrue(iter.hasNext());
		assertTriplet(Integer.valueOf(0), Integer.valueOf(1), null, iter.next());
		assertFalse(iter.hasNext());
	}

	@Test
	public void testThreeElementsIterator() {
		List<Integer> list = new ArrayList<Integer>();
		list.add(Integer.valueOf(0));
		list.add(Integer.valueOf(1));
		list.add(Integer.valueOf(2));

		TripletIterator<Integer> iter = new TripletIterator<Integer>(list.iterator());
		assertTrue(iter.hasNext());
		assertTriplet(null, Integer.valueOf(0), Integer.valueOf(1), iter.next());
		assertTrue(iter.hasNext());
		assertTriplet(Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2), iter.next());
		assertTrue(iter.hasNext());
		assertTriplet(Integer.valueOf(1), Integer.valueOf(2), null, iter.next());
		assertFalse(iter.hasNext());
	}

	@Test
	public void testFourElementsIterator() {
		List<Integer> list = new ArrayList<Integer>();
		list.add(Integer.valueOf(0));
		list.add(Integer.valueOf(1));
		list.add(Integer.valueOf(2));
		list.add(Integer.valueOf(3));

		TripletIterator<Integer> iter = new TripletIterator<Integer>(list.iterator());
		assertTrue(iter.hasNext());
		assertTriplet(null, Integer.valueOf(0), Integer.valueOf(1), iter.next());
		assertTrue(iter.hasNext());
		assertTriplet(Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2), iter.next());
		assertTrue(iter.hasNext());
		assertTriplet(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3), iter.next());
		assertTrue(iter.hasNext());
		assertTriplet(Integer.valueOf(2), Integer.valueOf(3), null, iter.next());
		assertFalse(iter.hasNext());
	}

	private static void assertTriplet(Object previous, Object current, Object next, Triplet<?> triplet) {
		assertNotNull(triplet);
		assertEquals(previous, triplet.previous);
		assertEquals(current, triplet.current);
		assertEquals(next, triplet.next);
	}
}
