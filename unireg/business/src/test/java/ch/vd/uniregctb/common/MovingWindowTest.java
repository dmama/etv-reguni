package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

public class MovingWindowTest extends WithoutSpringTest {

	@Test
	public void testSourceVide() throws Exception {
		final MovingWindow<?> mw = new MovingWindow<>(Collections.emptyList());
		Assert.assertFalse(mw.hasNext());

		try {
			mw.next();
			Assert.fail("Aurait dû être refusé!");
		}
		catch (NoSuchElementException e) {
			// tout va bien...
		}
	}

	@Test
	public void testSourceUnElement() throws Exception {
		final MovingWindow<Long> mw = new MovingWindow<>(Arrays.asList(42L));
		Assert.assertTrue(mw.hasNext());

		final MovingWindow.Snapshot<Long> snapshot = mw.next();
		Assert.assertNotNull(snapshot);
		Assert.assertEquals((Long) 42L, snapshot.getCurrent());
		Assert.assertEquals(Collections.<Long>emptyList(), snapshot.getAllNext());
		Assert.assertEquals(Collections.<Long>emptyList(), snapshot.getAllPrevious());
		Assert.assertNull(snapshot.getNext());
		Assert.assertNull(snapshot.getNextAfterNext());
		Assert.assertNull(snapshot.getPrevious());
		Assert.assertNull(snapshot.getPreviousBeforePrevious());

		Assert.assertFalse(mw.hasNext());
		try {
			mw.next();
			Assert.fail("Aurait dû être refusé!");
		}
		catch (NoSuchElementException e) {
			// tout va bien...
		}
	}

	@Test
	public void testSourcePlusieursElements() throws Exception {
		final MovingWindow<Long> mw = new MovingWindow<>(Arrays.asList(42L, 26L, 123L, 87L, 12L));
		Assert.assertTrue(mw.hasNext());

		{
			Assert.assertTrue(mw.hasNext());
			final MovingWindow.Snapshot<Long> snapshot = mw.next();
			Assert.assertNotNull(snapshot);
			Assert.assertEquals((Long) 42L, snapshot.getCurrent());
			Assert.assertEquals(Arrays.asList(26L, 123L, 87L, 12L), snapshot.getAllNext());
			Assert.assertEquals(Collections.<Long>emptyList(), snapshot.getAllPrevious());
			Assert.assertEquals((Long) 26L, snapshot.getNext());
			Assert.assertEquals((Long) 123L, snapshot.getNextAfterNext());
			Assert.assertNull(snapshot.getPrevious());
			Assert.assertNull(snapshot.getPreviousBeforePrevious());
		}
		{
			Assert.assertTrue(mw.hasNext());
			final MovingWindow.Snapshot<Long> snapshot = mw.next();
			Assert.assertNotNull(snapshot);
			Assert.assertEquals((Long) 26L, snapshot.getCurrent());
			Assert.assertEquals(Arrays.asList(123L, 87L, 12L), snapshot.getAllNext());
			Assert.assertEquals(Arrays.asList(42L), snapshot.getAllPrevious());
			Assert.assertEquals((Long) 123L, snapshot.getNext());
			Assert.assertEquals((Long) 87L, snapshot.getNextAfterNext());
			Assert.assertEquals((Long) 42L, snapshot.getPrevious());
			Assert.assertNull(snapshot.getPreviousBeforePrevious());
		}
		{
			Assert.assertTrue(mw.hasNext());
			final MovingWindow.Snapshot<Long> snapshot = mw.next();
			Assert.assertNotNull(snapshot);
			Assert.assertEquals((Long) 123L, snapshot.getCurrent());
			Assert.assertEquals(Arrays.asList(87L, 12L), snapshot.getAllNext());
			Assert.assertEquals(Arrays.asList(26L, 42L), snapshot.getAllPrevious());
			Assert.assertEquals((Long) 87L, snapshot.getNext());
			Assert.assertEquals((Long) 12L, snapshot.getNextAfterNext());
			Assert.assertEquals((Long) 26L, snapshot.getPrevious());
			Assert.assertEquals((Long) 42L, snapshot.getPreviousBeforePrevious());
		}
		{
			Assert.assertTrue(mw.hasNext());
			final MovingWindow.Snapshot<Long> snapshot = mw.next();
			Assert.assertNotNull(snapshot);
			Assert.assertEquals((Long) 87L, snapshot.getCurrent());
			Assert.assertEquals(Arrays.asList(12L), snapshot.getAllNext());
			Assert.assertEquals(Arrays.asList(123L, 26L, 42L), snapshot.getAllPrevious());
			Assert.assertEquals((Long) 12L, snapshot.getNext());
			Assert.assertNull(snapshot.getNextAfterNext());
			Assert.assertEquals((Long) 123L, snapshot.getPrevious());
			Assert.assertEquals((Long) 26L, snapshot.getPreviousBeforePrevious());
		}
		{
			Assert.assertTrue(mw.hasNext());
			final MovingWindow.Snapshot<Long> snapshot = mw.next();
			Assert.assertNotNull(snapshot);
			Assert.assertEquals((Long) 12L, snapshot.getCurrent());
			Assert.assertEquals(Collections.<Long>emptyList(), snapshot.getAllNext());
			Assert.assertEquals(Arrays.asList(87L, 123L, 26L, 42L), snapshot.getAllPrevious());
			Assert.assertNull(snapshot.getNext());
			Assert.assertNull(snapshot.getNextAfterNext());
			Assert.assertEquals((Long) 87L, snapshot.getPrevious());
			Assert.assertEquals((Long) 123L, snapshot.getPreviousBeforePrevious());
		}

		Assert.assertFalse(mw.hasNext());
		try {
			mw.next();
			Assert.fail("Aurait dû être refusé!");
		}
		catch (NoSuchElementException e) {
			// tout va bien...
		}
	}

	/**
	 * Ici on récolte tous les résulats de l'itération avant de les regarder, pour vérifier que les éléments fournis ne sont pas modifiés
	 * après coup (= immutables) par l'itération elle-même
	 */
	@Test
	public void testImmutabiliteSnapshot() throws Exception {
		final List<Long> source = Arrays.asList(42L, 26L, 123L, 87L, 12L);
		final MovingWindow<Long> mw = new MovingWindow<>(source);
		final List<MovingWindow.Snapshot<Long>> recup = new ArrayList<>(source.size());
		while (mw.hasNext()) {
			recup.add(mw.next());
		}
		try {
			mw.next();
			Assert.fail("Aurait dû être refusé!");
		}
		catch (NoSuchElementException e) {
			// tout va bien...
		}

		Assert.assertEquals(source.size(), recup.size());
		{
			final MovingWindow.Snapshot<Long> snapshot = recup.get(0);
			Assert.assertNotNull(snapshot);
			Assert.assertEquals((Long) 42L, snapshot.getCurrent());
			Assert.assertEquals(Arrays.asList(26L, 123L, 87L, 12L), snapshot.getAllNext());
			Assert.assertEquals(Collections.<Long>emptyList(), snapshot.getAllPrevious());
			Assert.assertEquals((Long) 26L, snapshot.getNext());
			Assert.assertEquals((Long) 123L, snapshot.getNextAfterNext());
			Assert.assertNull(snapshot.getPrevious());
			Assert.assertNull(snapshot.getPreviousBeforePrevious());
		}
		{
			final MovingWindow.Snapshot<Long> snapshot = recup.get(1);
			Assert.assertNotNull(snapshot);
			Assert.assertEquals((Long) 26L, snapshot.getCurrent());
			Assert.assertEquals(Arrays.asList(123L, 87L, 12L), snapshot.getAllNext());
			Assert.assertEquals(Arrays.asList(42L), snapshot.getAllPrevious());
			Assert.assertEquals((Long) 123L, snapshot.getNext());
			Assert.assertEquals((Long) 87L, snapshot.getNextAfterNext());
			Assert.assertEquals((Long) 42L, snapshot.getPrevious());
			Assert.assertNull(snapshot.getPreviousBeforePrevious());
		}
		{
			final MovingWindow.Snapshot<Long> snapshot = recup.get(2);
			Assert.assertNotNull(snapshot);
			Assert.assertEquals((Long) 123L, snapshot.getCurrent());
			Assert.assertEquals(Arrays.asList(87L, 12L), snapshot.getAllNext());
			Assert.assertEquals(Arrays.asList(26L, 42L), snapshot.getAllPrevious());
			Assert.assertEquals((Long) 87L, snapshot.getNext());
			Assert.assertEquals((Long) 12L, snapshot.getNextAfterNext());
			Assert.assertEquals((Long) 26L, snapshot.getPrevious());
			Assert.assertEquals((Long) 42L, snapshot.getPreviousBeforePrevious());
		}
		{
			final MovingWindow.Snapshot<Long> snapshot = recup.get(3);
			Assert.assertNotNull(snapshot);
			Assert.assertEquals((Long) 87L, snapshot.getCurrent());
			Assert.assertEquals(Arrays.asList(12L), snapshot.getAllNext());
			Assert.assertEquals(Arrays.asList(123L, 26L, 42L), snapshot.getAllPrevious());
			Assert.assertEquals((Long) 12L, snapshot.getNext());
			Assert.assertNull(snapshot.getNextAfterNext());
			Assert.assertEquals((Long) 123L, snapshot.getPrevious());
			Assert.assertEquals((Long) 26L, snapshot.getPreviousBeforePrevious());
		}
		{
			final MovingWindow.Snapshot<Long> snapshot = recup.get(4);
			Assert.assertNotNull(snapshot);
			Assert.assertEquals((Long) 12L, snapshot.getCurrent());
			Assert.assertEquals(Collections.<Long>emptyList(), snapshot.getAllNext());
			Assert.assertEquals(Arrays.asList(87L, 123L, 26L, 42L), snapshot.getAllPrevious());
			Assert.assertNull(snapshot.getNext());
			Assert.assertNull(snapshot.getNextAfterNext());
			Assert.assertEquals((Long) 87L, snapshot.getPrevious());
			Assert.assertEquals((Long) 123L, snapshot.getPreviousBeforePrevious());
		}
	}
}
