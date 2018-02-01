package ch.vd.unireg.common;

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
		final MovingWindow<Long> mw = new MovingWindow<>(Collections.singletonList(42L));
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
			Assert.assertEquals(Collections.singletonList(42L), snapshot.getAllPrevious());
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
			Assert.assertEquals(Collections.singletonList(12L), snapshot.getAllNext());
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
			Assert.assertEquals(Collections.singletonList(42L), snapshot.getAllPrevious());
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
			Assert.assertEquals(Collections.singletonList(12L), snapshot.getAllNext());
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

	@Test
	public void testRemoveNotSupportedByUnderlying() throws Exception {
		final List<Integer> list = Collections.unmodifiableList(Arrays.asList(42, 16, 65, 12, 43));
		final MovingWindow<Integer> mw = new MovingWindow<>(list);
		Assert.assertTrue(mw.hasNext());

		try {
			mw.remove();
			Assert.fail("On ne devrait pas supporter ça !!");
		}
		catch (UnsupportedOperationException e) {
			// c'est bien ce qui est attendu
		}

		final MovingWindow.Snapshot<Integer> snapshot = mw.next();
		Assert.assertNotNull(snapshot);

		try {
			mw.remove();
			Assert.fail("On ne devrait pas supporter ça !!");
		}
		catch (UnsupportedOperationException e) {
			// c'est bien ce qui est attendu
		}

		// maintenant, il faut quand même vérifier que
		// 1. le snapshot que j'ai reçu est toujours correct
		// 2. le snapshot suivant l'est tout autant
		Assert.assertEquals((Integer) 42, snapshot.getCurrent());
		Assert.assertEquals(Arrays.asList(16, 65, 12, 43), snapshot.getAllNext());
		Assert.assertEquals(Collections.<Integer>emptyList(), snapshot.getAllPrevious());
		Assert.assertEquals((Integer) 16, snapshot.getNext());
		Assert.assertEquals((Integer) 65, snapshot.getNextAfterNext());
		Assert.assertNull(snapshot.getPrevious());
		Assert.assertNull(snapshot.getPreviousBeforePrevious());

		Assert.assertTrue(mw.hasNext());
		final MovingWindow.Snapshot<Integer> newSnapshot = mw.next();
		Assert.assertNotNull(newSnapshot);
		Assert.assertEquals((Integer) 16, newSnapshot.getCurrent());
		Assert.assertEquals(Arrays.asList(65, 12, 43), newSnapshot.getAllNext());
		Assert.assertEquals(Collections.singletonList(42), newSnapshot.getAllPrevious());
		Assert.assertEquals((Integer) 65, newSnapshot.getNext());
		Assert.assertEquals((Integer) 12, newSnapshot.getNextAfterNext());
		Assert.assertEquals((Integer) 42, newSnapshot.getPrevious());
		Assert.assertNull(newSnapshot.getPreviousBeforePrevious());
	}

	@Test
	public void testRemove() throws Exception {
		final List<Integer> list = new ArrayList<>(Arrays.asList(42, 16, 65, 12, 43));
		final MovingWindow<Integer> mw = new MovingWindow<>(list);
		Assert.assertTrue(mw.hasNext());

		try {
			mw.remove();
			Assert.fail("Mais... next() n'a même pas encore été appelé !");
		}
		catch (IllegalStateException e) {
			// ce qui est attendu, parce que next() n'a pas encore été appelé...
		}

		final MovingWindow.Snapshot<Integer> snapshot = mw.next();
		Assert.assertNotNull(snapshot);
		Assert.assertEquals((Integer) 42, snapshot.getCurrent());
		Assert.assertEquals(Arrays.asList(16, 65, 12, 43), snapshot.getAllNext());
		Assert.assertEquals(Collections.<Integer>emptyList(), snapshot.getAllPrevious());
		Assert.assertEquals((Integer) 16, snapshot.getNext());
		Assert.assertEquals((Integer) 65, snapshot.getNextAfterNext());
		Assert.assertNull(snapshot.getPrevious());
		Assert.assertNull(snapshot.getPreviousBeforePrevious());

		// retrait de la première valeur de la liste...
		mw.remove();

		// le snapshot ne doit pas avoir été modifié
		Assert.assertEquals((Integer) 42, snapshot.getCurrent());
		Assert.assertEquals(Arrays.asList(16, 65, 12, 43), snapshot.getAllNext());
		Assert.assertEquals(Collections.<Integer>emptyList(), snapshot.getAllPrevious());
		Assert.assertEquals((Integer) 16, snapshot.getNext());
		Assert.assertEquals((Integer) 65, snapshot.getNextAfterNext());
		Assert.assertNull(snapshot.getPrevious());
		Assert.assertNull(snapshot.getPreviousBeforePrevious());

		// et la liste a bien été modifiée conformément aux souhaits émis
		Assert.assertEquals(Arrays.asList(16, 65, 12, 43), list);

		// mais si je prends le snapshot suivant, lui ne doit plus avoir de référence vers l'élément détruit
		{
			Assert.assertTrue(mw.hasNext());
			final MovingWindow.Snapshot<Integer> snap = mw.next();
			Assert.assertNotNull(snap);
			Assert.assertNotNull(snap);
			Assert.assertEquals((Integer) 16, snap.getCurrent());
			Assert.assertEquals(Arrays.asList(65, 12, 43), snap.getAllNext());
			Assert.assertEquals(Collections.<Integer>emptyList(), snap.getAllPrevious());
			Assert.assertEquals((Integer) 65, snap.getNext());
			Assert.assertEquals((Integer) 12, snap.getNextAfterNext());
			Assert.assertNull(snap.getPrevious());
			Assert.assertNull(snap.getPreviousBeforePrevious());
		}

		// avançons un peu, pour vérifier qu'on peut enlever aussi un élément au milieu de la liste
		{
			Assert.assertTrue(mw.hasNext());
			final MovingWindow.Snapshot<Integer> snap = mw.next();
			Assert.assertNotNull(snap);
			Assert.assertEquals((Integer) 65, snap.getCurrent());
			Assert.assertEquals(Arrays.asList(12, 43), snap.getAllNext());
			Assert.assertEquals(Collections.singletonList(16), snap.getAllPrevious());
			Assert.assertEquals((Integer) 12, snap.getNext());
			Assert.assertEquals((Integer) 43, snap.getNextAfterNext());
			Assert.assertEquals((Integer) 16, snap.getPrevious());
			Assert.assertNull(snap.getPreviousBeforePrevious());

			// on l'enlève !
			mw.remove();

			// pas de modification dans le snapshot déjà pris
			Assert.assertEquals((Integer) 65, snap.getCurrent());
			Assert.assertEquals(Arrays.asList(12, 43), snap.getAllNext());
			Assert.assertEquals(Collections.singletonList(16), snap.getAllPrevious());
			Assert.assertEquals((Integer) 12, snap.getNext());
			Assert.assertEquals((Integer) 43, snap.getNextAfterNext());
			Assert.assertEquals((Integer) 16, snap.getPrevious());
			Assert.assertNull(snap.getPreviousBeforePrevious());
		}

		// et la liste a bien été modifiée conformément aux souhaits émis
		Assert.assertEquals(Arrays.asList(16, 12, 43), list);

		// si je prends le snapshot suivant, lui ne doit plus avoir de référence vers l'élément détruit
		{
			Assert.assertTrue(mw.hasNext());
			final MovingWindow.Snapshot<Integer> snap = mw.next();
			Assert.assertNotNull(snap);
			Assert.assertNotNull(snap);
			Assert.assertEquals((Integer) 12, snap.getCurrent());
			Assert.assertEquals(Collections.singletonList(43), snap.getAllNext());
			Assert.assertEquals(Collections.singletonList(16), snap.getAllPrevious());
			Assert.assertEquals((Integer) 43, snap.getNext());
			Assert.assertNull(snap.getNextAfterNext());
			Assert.assertEquals((Integer) 16, snap.getPrevious());
			Assert.assertNull(snap.getPreviousBeforePrevious());
		}

		// avançons un peu, pour vérifier qu'on peut enlever aussi un élément à la fin de la liste
		{
			Assert.assertTrue(mw.hasNext());
			final MovingWindow.Snapshot<Integer> snap = mw.next();
			Assert.assertNotNull(snap);
			Assert.assertEquals((Integer) 43, snap.getCurrent());
			Assert.assertEquals(Collections.<Integer>emptyList(), snap.getAllNext());
			Assert.assertEquals(Arrays.asList(12, 16), snap.getAllPrevious());
			Assert.assertNull(snap.getNext());
			Assert.assertNull(snap.getNextAfterNext());
			Assert.assertEquals((Integer) 12, snap.getPrevious());
			Assert.assertEquals((Integer) 16, snap.getPreviousBeforePrevious());

			// on l'enlève !
			mw.remove();

			// pas de modification dans le snapshot déjà pris
			Assert.assertEquals((Integer) 43, snap.getCurrent());
			Assert.assertEquals(Collections.<Integer>emptyList(), snap.getAllNext());
			Assert.assertEquals(Arrays.asList(12, 16), snap.getAllPrevious());
			Assert.assertNull(snap.getNext());
			Assert.assertNull(snap.getNextAfterNext());
			Assert.assertEquals((Integer) 12, snap.getPrevious());
			Assert.assertEquals((Integer) 16, snap.getPreviousBeforePrevious());
		}


		// et c'est fini, il n'y a plus rien dans l'itérateur
		Assert.assertFalse(mw.hasNext());

		// et la liste a bien été modifiée conformément aux souhaits émis
		Assert.assertEquals(Arrays.asList(16, 12), list);
	}
}
