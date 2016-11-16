package ch.vd.uniregctb.registrefoncier.dao;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeRapprochementRF;

public class RapprochementRFDAOTest extends CoreDAOTest {

	private RapprochementRFDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.dao = getBean(RapprochementRFDAO.class, "rapprochementRFDAO");
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindByContribuable() throws Exception {

		final PersonnePhysique pp = addNonHabitant("Federico", "Felino", date(1967, 3, 12), Sexe.MASCULIN);
		final PersonnePhysiqueRF pprf1 = addPersonnePhysiqueRF("Feredico Francesco", "Felino", date(1967, 3, 12), "85347843278", 3243L, null);
		final PersonnePhysiqueRF pprf2 = addPersonnePhysiqueRF("Feredica Francesca", "Felino", date(1967, 1, 2), "85344537836", 4321L, null);
		final PersonnePhysiqueRF pprf3 = addPersonnePhysiqueRF("Feredica Francesca", "Giannino", date(1975, 7, 31), "84638452", 7546L, null);

		addRapprochementRF(date(2015, 3, 1), null, TypeRapprochementRF.MANUEL, pp, pprf1, false);
		addRapprochementRF(date(2015, 2, 25), null, TypeRapprochementRF.AUTO, pp, pprf2, true);

		final List<RapprochementRF> pourContribuable = dao.findByContribuable(pp.getNumero());
		Assert.assertNotNull(pourContribuable);
		Assert.assertEquals(2, pourContribuable.size());
		final List<RapprochementRF> triesPourContribuable = pourContribuable.stream()
				.sorted(Comparator.comparing(RapprochementRF::getId))
				.collect(Collectors.toList());
		{
			final RapprochementRF rrf = triesPourContribuable.get(0);
			Assert.assertNotNull(rrf);
			Assert.assertFalse(rrf.isAnnule());
			Assert.assertEquals(date(2015, 3, 1), rrf.getDateDebut());
			Assert.assertNull(rrf.getDateFin());
			Assert.assertEquals(TypeRapprochementRF.MANUEL, rrf.getTypeRapprochement());
			Assert.assertSame(pprf1, rrf.getTiersRF());
		}
		{
			final RapprochementRF rrf = triesPourContribuable.get(1);
			Assert.assertNotNull(rrf);
			Assert.assertTrue(rrf.isAnnule());
			Assert.assertEquals(date(2015, 2, 25), rrf.getDateDebut());
			Assert.assertNull(rrf.getDateFin());
			Assert.assertEquals(TypeRapprochementRF.AUTO, rrf.getTypeRapprochement());
			Assert.assertSame(pprf2, rrf.getTiersRF());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void findTiersRFSansRapprochementDuTout() throws Exception {

		final PersonnePhysique pp = addNonHabitant("Federico", "Felino", date(1967, 3, 12), Sexe.MASCULIN);
		final PersonnePhysiqueRF pprf1 = addPersonnePhysiqueRF("Feredico Francesco", "Felino", date(1967, 3, 12), "85347843278", 3243L, null);
		final PersonnePhysiqueRF pprf2 = addPersonnePhysiqueRF("Feredica Francesca", "Felino", date(1967, 1, 2), "85344537836", 4321L, null);
		final PersonnePhysiqueRF pprf3 = addPersonnePhysiqueRF("Feredica Francesca", "Giannino", date(1975, 7, 31), "84638452", 7546L, null);

		addRapprochementRF(date(2015, 3, 1), null, TypeRapprochementRF.MANUEL, pp, pprf1, false);
		addRapprochementRF(date(2015, 2, 25), null, TypeRapprochementRF.AUTO, pp, pprf2, true);

		final List<TiersRF> sansRapprochement = dao.findTiersRFSansRapprochement(null);
		Assert.assertNotNull(sansRapprochement);
		Assert.assertEquals(2, sansRapprochement.size());
		final List<TiersRF> triesSansRapprochement = sansRapprochement.stream()
				.sorted(Comparator.comparing(TiersRF::getNoRF))
				.collect(Collectors.toList());
		{
			final TiersRF rf = triesSansRapprochement.get(0);
			Assert.assertNotNull(rf);
			Assert.assertFalse(rf.isAnnule());
			Assert.assertSame(pprf2, rf);
		}
		{
			final TiersRF rf = triesSansRapprochement.get(1);
			Assert.assertNotNull(rf);
			Assert.assertFalse(rf.isAnnule());
			Assert.assertSame(pprf3, rf);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void findTiersRFSansRapprochementADate() throws Exception {

		final PersonnePhysique pp = addNonHabitant("Federico", "Felino", date(1967, 3, 12), Sexe.MASCULIN);
		final PersonnePhysiqueRF pprf1 = addPersonnePhysiqueRF("Feredico Francesco", "Felino", date(1967, 3, 12), "85347843278", 3243L, null);
		final PersonnePhysiqueRF pprf2 = addPersonnePhysiqueRF("Feredica Francesca", "Felino", date(1967, 1, 2), "85344537836", 4321L, null);
		final PersonnePhysiqueRF pprf3 = addPersonnePhysiqueRF("Feredica Francesca", "Giannino", date(1975, 7, 31), "84638452", 7546L, null);

		addRapprochementRF(null, date(2015, 2, 28), TypeRapprochementRF.AUTO, pp, pprf1, false);
		addRapprochementRF(date(2015, 3, 1), null, TypeRapprochementRF.MANUEL, pp, pprf2, false);

		// au 1.1.2010
		{
			final List<TiersRF> sansRapprochement = dao.findTiersRFSansRapprochement(date(2010, 1, 1));
			Assert.assertNotNull(sansRapprochement);
			Assert.assertEquals(2, sansRapprochement.size());
			final List<TiersRF> triesSansRapprochement = sansRapprochement.stream()
					.sorted(Comparator.comparing(TiersRF::getNoRF))
					.collect(Collectors.toList());
			{
				final TiersRF rf = triesSansRapprochement.get(0);
				Assert.assertNotNull(rf);
				Assert.assertFalse(rf.isAnnule());
				Assert.assertSame(pprf2, rf);
			}
			{
				final TiersRF rf = triesSansRapprochement.get(1);
				Assert.assertNotNull(rf);
				Assert.assertFalse(rf.isAnnule());
				Assert.assertSame(pprf3, rf);
			}
		}

		// au 1.1.2016
		{
			final List<TiersRF> sansRapprochement = dao.findTiersRFSansRapprochement(date(2016, 1, 1));
			Assert.assertNotNull(sansRapprochement);
			Assert.assertEquals(2, sansRapprochement.size());
			final List<TiersRF> triesSansRapprochement = sansRapprochement.stream()
					.sorted(Comparator.comparing(TiersRF::getNoRF))
					.collect(Collectors.toList());
			{
				final TiersRF rf = triesSansRapprochement.get(0);
				Assert.assertNotNull(rf);
				Assert.assertFalse(rf.isAnnule());
				Assert.assertSame(pprf1, rf);
			}
			{
				final TiersRF rf = triesSansRapprochement.get(1);
				Assert.assertNotNull(rf);
				Assert.assertFalse(rf.isAnnule());
				Assert.assertSame(pprf3, rf);
			}
		}
	}
}
