package ch.vd.uniregctb.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class BatchIteratorTest extends WithoutSpringTest {

	@Test
	public void testEmptyIterator() {
		Iterator<Object> empty = Collections.emptyList().iterator();
		BatchIterator<Object> iter = new BatchIterator<Object>(empty, 100);
		assertFalse(iter.hasNext());
	}

	@Test
	public void testEmptyList() {
		List<Object> empty = Collections.emptyList();
		BatchIterator<Object> iter = new BatchIterator<Object>(empty, 100);
		assertFalse(iter.hasNext());
	}

	@Test
	public void testZeroBatchSize() {
		try {
			new BatchIterator<Object>(Collections.<Object> emptyList(), 0);
			fail();
		}
		catch (IllegalArgumentException e) {
			// ok
		}
	}

	@Test
	public void testOneBatch() {
		List<Integer> list = new ArrayList<Integer>();
		list.add(1);
		list.add(2);
		list.add(3);
		list.add(4);
		list.add(5);

		BatchIterator<Integer> iter = new BatchIterator<Integer>(list, 100);
		assertTrue(iter.hasNext());
		assertEquals(0, iter.getPercent());

		final Iterator<Integer> batch = iter.next();
		assertNotNull(batch);
		assertEquals(100, iter.getPercent());
		assertTrue(batch.hasNext());
		assertEquals(Integer.valueOf(1), batch.next());
		assertTrue(batch.hasNext());
		assertEquals(Integer.valueOf(2), batch.next());
		assertTrue(batch.hasNext());
		assertEquals(Integer.valueOf(3), batch.next());
		assertTrue(batch.hasNext());
		assertEquals(Integer.valueOf(4), batch.next());
		assertTrue(batch.hasNext());
		assertEquals(Integer.valueOf(5), batch.next());
		assertFalse(batch.hasNext());
	}

	@Test
	public void testThreeBatches() {
		List<Integer> list = new ArrayList<Integer>();
		list.add(1);
		list.add(2);
		list.add(3);
		list.add(4);
		list.add(5);

		BatchIterator<Integer> iter = new BatchIterator<Integer>(list, 2);

		assertTrue(iter.hasNext());
		assertEquals(0, iter.getPercent());
		final Iterator<Integer> batch1 = iter.next();
		assertNotNull(batch1);
		assertEquals(33, iter.getPercent());
		assertTrue(batch1.hasNext());
		assertEquals(Integer.valueOf(1), batch1.next());
		assertTrue(batch1.hasNext());
		assertEquals(Integer.valueOf(2), batch1.next());
		assertFalse(batch1.hasNext());

		assertTrue(iter.hasNext());
		final Iterator<Integer> batch2 = iter.next();
		assertNotNull(batch2);
		assertEquals(66, iter.getPercent());
		assertTrue(batch2.hasNext());
		assertEquals(Integer.valueOf(3), batch2.next());
		assertTrue(batch2.hasNext());
		assertEquals(Integer.valueOf(4), batch2.next());
		assertFalse(batch2.hasNext());

		assertTrue(iter.hasNext());
		final Iterator<Integer> batch3 = iter.next();
		assertNotNull(batch3);
		assertEquals(100, iter.getPercent());
		assertTrue(batch3.hasNext());
		assertEquals(Integer.valueOf(5), batch3.next());
		assertFalse(batch3.hasNext());

		assertFalse(iter.hasNext());
	}

	@Test
	public void testCalculateSize() {
		assertEquals(0, BatchIterator.calculateSize(0, 100));
		assertEquals(1, BatchIterator.calculateSize(1, 100));
		assertEquals(1, BatchIterator.calculateSize(10, 100));
		assertEquals(1, BatchIterator.calculateSize(100, 100));
		assertEquals(2, BatchIterator.calculateSize(101, 100));
		assertEquals(2, BatchIterator.calculateSize(199, 100));
		assertEquals(2, BatchIterator.calculateSize(200, 100));
		assertEquals(3, BatchIterator.calculateSize(201, 100));
	}
}
