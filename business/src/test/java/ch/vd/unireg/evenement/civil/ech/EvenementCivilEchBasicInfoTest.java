package ch.vd.unireg.evenement.civil.ech;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class EvenementCivilEchBasicInfoTest extends WithoutSpringTest {

	private static EvenementCivilEchBasicInfo buildBasicInfo(long id, long noIndividu, EtatEvenementCivil etat, TypeEvenementCivilEch type, ActionEvenementCivilEch action,
	                                                         @Nullable Long idReference, RegDate date, EvenementCivilEchBasicInfo... referrers) {
		final EvenementCivilEchBasicInfo info = new EvenementCivilEchBasicInfo(id, noIndividu, etat, type, action, idReference, date, "UT");
		if (referrers != null && referrers.length > 0) {
			info.setReferrers(Arrays.asList(referrers));
		}
		return info;
	}

	@Test
	public void testReferrerSortingEmpty() throws Exception {
		final EvenementCivilEchBasicInfo info = buildBasicInfo(1L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, 3L, RegDate.get());
		final List<EvenementCivilEchBasicInfo> referrers = info.getSortedReferrers();
		Assert.assertNotNull(referrers);
		Assert.assertEquals(0, referrers.size());
	}

	@Test
	public void testReferrerSortingOneElement() throws Exception {
		final EvenementCivilEchBasicInfo referrer = buildBasicInfo(5L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, 1L, RegDate.get());
		final EvenementCivilEchBasicInfo info = buildBasicInfo(1L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, 3L, RegDate.get(), referrer);
		final List<EvenementCivilEchBasicInfo> referrers = info.getSortedReferrers();
		Assert.assertNotNull(referrers);
		Assert.assertEquals(1, referrers.size());

		final EvenementCivilEchBasicInfo foundRef = referrers.get(0);
		Assert.assertEquals(referrer, foundRef);
	}

	@Test
	public void testCacheForReferrerSorting() throws Exception {
		final EvenementCivilEchBasicInfo referrer1 = buildBasicInfo(5L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, 1L, RegDate.get());
		final EvenementCivilEchBasicInfo referrer2 = buildBasicInfo(6L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.ANNULATION, 1L, RegDate.get());
		final EvenementCivilEchBasicInfo info = buildBasicInfo(1L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, 3L, RegDate.get(), referrer1, referrer2);
		final List<EvenementCivilEchBasicInfo> referrers = info.getSortedReferrers();
		Assert.assertNotNull(referrers);
		Assert.assertEquals(2, referrers.size());

		// deuxième appel, on doit trouver exactement la même instance de collection
		Assert.assertSame(referrers, info.getSortedReferrers());

		// nouvel ajout de referrer
		{
			final EvenementCivilEchBasicInfo referrer3 = buildBasicInfo(7L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, 1L, RegDate.get());
			info.addReferrer(referrer3);
			final List<EvenementCivilEchBasicInfo> newReferrers = info.getSortedReferrers();
			Assert.assertNotNull(newReferrers);
			Assert.assertEquals(3, newReferrers.size());
			Assert.assertEquals(2, referrers.size());
		}

		// assignation globale
		{
			info.setReferrers(referrers);
			final List<EvenementCivilEchBasicInfo> newReferrers = info.getSortedReferrers();
			Assert.assertNotNull(newReferrers);
			Assert.assertEquals(2, newReferrers.size());
			Assert.assertNotSame(newReferrers, referrers);
		}
	}

	@Test
	public void testReferrerSortingOrderLinear() throws Exception {

		//
		// 5 -> 1
		// 8 -> 7 -> 6 -> 1
		//
		// ordre attendu : (1, principal), 5, 6, 7, 8

		final EvenementCivilEchBasicInfo referrer1 = buildBasicInfo(5L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, 1L, RegDate.get());
		final EvenementCivilEchBasicInfo referrer2 = buildBasicInfo(6L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, 1L, RegDate.get());
		final EvenementCivilEchBasicInfo referrer3 = buildBasicInfo(7L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.ANNULATION, 6L, RegDate.get());
		final EvenementCivilEchBasicInfo referrer4 = buildBasicInfo(8L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.ANNULATION, 7L, RegDate.get());
		final EvenementCivilEchBasicInfo info = buildBasicInfo(1L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, 3L, RegDate.get(),
		                                                       referrer4, referrer3, referrer2, referrer1);

		final List<EvenementCivilEchBasicInfo> sorted = info.getSortedReferrers();
		Assert.assertNotNull(sorted);
		Assert.assertEquals(4, sorted.size());
		Assert.assertSame(referrer1, sorted.get(0));
		Assert.assertSame(referrer2, sorted.get(1));
		Assert.assertSame(referrer3, sorted.get(2));
		Assert.assertSame(referrer4, sorted.get(3));
	}

	@Test
	public void testReferrerSortingOrderTree() throws Exception {

		//
		// 5 -> 1
		// 7 -> 6 -> 1
		// 8 -> 1
		//
		// ordre attendu : (1, principal), 5, 6, 8, 7

		final EvenementCivilEchBasicInfo referrer1 = buildBasicInfo(5L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, 1L, RegDate.get());
		final EvenementCivilEchBasicInfo referrer2 = buildBasicInfo(6L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, 1L, RegDate.get());
		final EvenementCivilEchBasicInfo referrer3 = buildBasicInfo(7L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.ANNULATION, 6L, RegDate.get());
		final EvenementCivilEchBasicInfo referrer4 = buildBasicInfo(8L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.ANNULATION, 1L, RegDate.get());
		final EvenementCivilEchBasicInfo info = buildBasicInfo(1L, 2L, EtatEvenementCivil.A_TRAITER, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, 3L, RegDate.get(),
		                                                       referrer4, referrer3, referrer2, referrer1);

		final List<EvenementCivilEchBasicInfo> sorted = info.getSortedReferrers();
		Assert.assertNotNull(sorted);
		Assert.assertEquals(4, sorted.size());
		Assert.assertSame(referrer1, sorted.get(0));
		Assert.assertSame(referrer2, sorted.get(1));
		Assert.assertSame(referrer4, sorted.get(2));
		Assert.assertSame(referrer3, sorted.get(3));
	}
}
