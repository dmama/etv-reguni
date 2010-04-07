package ch.vd.uniregctb.situationfamille;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import ch.vd.uniregctb.tiers.*;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.situationfamille.ReinitialiserBaremeDoubleGainResults.Erreur;
import ch.vd.uniregctb.situationfamille.ReinitialiserBaremeDoubleGainResults.ErreurType;
import ch.vd.uniregctb.situationfamille.ReinitialiserBaremeDoubleGainResults.Ignore;
import ch.vd.uniregctb.situationfamille.ReinitialiserBaremeDoubleGainResults.IgnoreType;
import ch.vd.uniregctb.situationfamille.ReinitialiserBaremeDoubleGainResults.Situation;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TarifImpotSource;

/**
 * Testos della procezzario e pizza.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ReinitialiserBaremeDoubleGainProcessorTest extends BusinessTest {

	private ReinitialiserBaremeDoubleGainProcessor processor;
	private SituationFamilleDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		SituationFamilleService service = getBean(SituationFamilleService.class, "situationFamilleService");
		dao = getBean(SituationFamilleDAO.class, "situationFamilleDAO");

		processor = new ReinitialiserBaremeDoubleGainProcessor(service, hibernateTemplate, transactionManager);
	}

	@Test
	public void testTraiterSituationIdNull() {
		try {
			processor.traiterSituation(null, date(2000, 1, 1));
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("L'id doit être spécifié.", e.getMessage());
		}
	}

	@Test
	public void testTraiterSituationInexistante() {
		try {
			processor.traiterSituation(12345L, date(2000, 1, 1));
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La situation de famille n'existe pas.", e.getMessage());
		}
	}

	@Test
	public void testTraiterSituationBaremeNormal() throws Exception {

		final RegDate dateTraitement = date(2007, 1, 1);

		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique julien = addNonHabitant("Julien", "Renard", date(1970, 12, 27), Sexe.MASCULIN);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(julien, null, date(2001, 3, 2), null);
				final MenageCommun menage = ensemble.getMenage();
				final SituationFamilleMenageCommun situation = addSituation(menage, date(2005, 1, 1), null, 0, TarifImpotSource.NORMAL);
				return situation.getId();
			}
		});

		final ReinitialiserBaremeDoubleGainResults rapport = new ReinitialiserBaremeDoubleGainResults(dateTraitement);

		processor.setRapport(rapport);
		processor.traiterSituation(id, dateTraitement);

		// la situation ne doit pas avoir été traitée
		assertEquals(1, rapport.nbSituationsTotal);
		assertEmpty(rapport.situationsTraitees);
		assertEmpty(rapport.situationsEnErrors);
		assertEquals(1, rapport.situationsIgnorees.size());

		final Ignore ignore = rapport.situationsIgnorees.get(0);
		assertNotNull(ignore);
		assertEquals(IgnoreType.BAREME_NON_DOUBLE_GAIN, ignore.raison);
		assertEquals("Le barême n'est pas double-gain. Attendu = DOUBLE_GAIN, constaté = NORMAL. Erreur dans la requête SQL ?", ignore
				.getDescriptionRaison());

		assertEquals(1, dao.getCount(SituationFamilleMenageCommun.class));
	}

	@Test
	public void testTraiterSituationDoubleGain() throws Exception {

		final RegDate dateTraitement = date(2007, 1, 1);

		class Ids {
			long menage;
			long situation;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique julien = addNonHabitant("Julien", "Renard", date(1970, 12, 27), Sexe.MASCULIN);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(julien, null, date(2001, 3, 2), null);
				final MenageCommun menage = ensemble.getMenage();
				ids.menage = menage.getNumero();

				final SituationFamilleMenageCommun situation = addSituation(menage, date(2005, 1, 1), null, 0, TarifImpotSource.DOUBLE_GAIN);
				ids.situation = situation.getId();

				return null;
			}
		});

		final ReinitialiserBaremeDoubleGainResults rapport = new ReinitialiserBaremeDoubleGainResults(dateTraitement);

		processor.setRapport(rapport);
		processor.traiterSituation(ids.situation, dateTraitement);

		// la situation doit avoir été traitée
		assertEquals(1, rapport.nbSituationsTotal);
		assertEmpty(rapport.situationsIgnorees);
		assertEmpty(rapport.situationsEnErrors);
		assertEquals(1, rapport.situationsTraitees.size());

		final Situation traitee = rapport.situationsTraitees.get(0);
		assertNotNull(traitee);
		assertEquals(ids.menage, traitee.ctbId);
		assertEquals(ids.situation, traitee.ancienneId);
		assertNotNull(traitee.nouvelleId);

		assertEquals(2, dao.getCount(SituationFamilleMenageCommun.class));

		final List<SituationFamille> list = dao.getAll();
		assertNotNull(list);
		assertEquals(2, list.size());
		Collections.sort(list, new DateRangeComparator<SituationFamille>());

		final SituationFamilleMenageCommun situation0 = (SituationFamilleMenageCommun) list.get(0);
		assertEquals(traitee.ancienneId, situation0.getId().longValue());
		assertSituation(date(2005, 1, 1), date(2006, 12, 31), 0, TarifImpotSource.DOUBLE_GAIN, situation0);

		final SituationFamilleMenageCommun situation1 = (SituationFamilleMenageCommun) list.get(1);
		assertEquals(traitee.nouvelleId, situation1.getId().longValue());
		assertSituation(date(2007, 1, 1), null, 0, TarifImpotSource.NORMAL, situation1);
	}

	@Test
	public void testRetrieveSituationsDoubleGainBaseVide() {
		assertEmpty(processor.retrieveSituationsDoubleGain(date(2007, 1, 1)));
	}

	@Test
	public void testRetrieveSituationsDoubleGainDiversesSituations() throws Exception {

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique julien = addNonHabitant("Julien", "Renard", date(1970, 12, 27), Sexe.MASCULIN);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(julien, null, date(2001, 3, 2), null);
				final MenageCommun menage = ensemble.getMenage();
				addSituation(menage, date(2001, 3, 2), date(2003, 10, 1), 0, TarifImpotSource.NORMAL);
				addSituation(menage, date(2003, 10, 2), date(2005, 12, 31), 0, TarifImpotSource.DOUBLE_GAIN);
				addSituation(menage, date(2007, 1, 1), null, 0, TarifImpotSource.DOUBLE_GAIN);
				return null;
			}
		});

		assertEmpty(processor.retrieveSituationsDoubleGain(date(2000, 1, 1)));
		assertEmpty(processor.retrieveSituationsDoubleGain(date(2001, 1, 1)));
		assertEmpty(processor.retrieveSituationsDoubleGain(date(2002, 1, 1)));
		assertEmpty(processor.retrieveSituationsDoubleGain(date(2003, 1, 1)));
		assertEquals(1, processor.retrieveSituationsDoubleGain(date(2004, 1, 1)).size());
		assertEquals(1, processor.retrieveSituationsDoubleGain(date(2005, 1, 1)).size());
		assertEmpty(processor.retrieveSituationsDoubleGain(date(2006, 1, 1)));
		assertEquals(1, processor.retrieveSituationsDoubleGain(date(2007, 1, 1)).size());
		assertEquals(1, processor.retrieveSituationsDoubleGain(date(2008, 1, 1)).size());
	}

	@Test
	public void testRunBaseVide() {

		final ReinitialiserBaremeDoubleGainResults rapport = processor.run(date(2007,1,1), null);
		assertEquals(0, rapport.nbSituationsTotal);
		assertEmpty(rapport.situationsTraitees);
		assertEmpty(rapport.situationsIgnorees);
		assertEmpty(rapport.situationsEnErrors);
	}

	@Test
	public void testRunSituationDoubleGain() throws Exception {

		class Ids {
			long menage;
			long situation;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique julien = addNonHabitant("Julien", "Renard", date(1970, 12, 27), Sexe.MASCULIN);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(julien, null, date(2001, 3, 2), null);
				final MenageCommun menage = ensemble.getMenage();
				ids.menage = menage.getNumero();

				final SituationFamilleMenageCommun situation = addSituation(menage, date(2005, 1, 1), null, 0, TarifImpotSource.DOUBLE_GAIN);
				ids.situation = situation.getId();

				return null;
			}
		});

		final ReinitialiserBaremeDoubleGainResults rapport = processor.run(date(2007, 1, 1), null);

		// la situation doit avoir été traitée
		assertEquals(1, rapport.nbSituationsTotal);
		assertEmpty(rapport.situationsIgnorees);
		assertEmpty(rapport.situationsEnErrors);
		assertEquals(1, rapport.situationsTraitees.size());

		final Situation traitee = rapport.situationsTraitees.get(0);
		assertNotNull(traitee);
		assertEquals(ids.menage, traitee.ctbId);
		assertEquals(ids.situation, traitee.ancienneId);
		assertNotNull(traitee.nouvelleId);

		assertEquals(2, dao.getCount(SituationFamilleMenageCommun.class));

		final List<SituationFamille> list = dao.getAll();
		assertNotNull(list);
		assertEquals(2, list.size());
		Collections.sort(list, new DateRangeComparator<SituationFamille>());

		final SituationFamilleMenageCommun situation0 = (SituationFamilleMenageCommun) list.get(0);
		assertEquals(traitee.ancienneId, situation0.getId().longValue());
		assertSituation(date(2005, 1, 1), date(2006, 12, 31), 0, TarifImpotSource.DOUBLE_GAIN, situation0);

		final SituationFamilleMenageCommun situation1 = (SituationFamilleMenageCommun) list.get(1);
		assertEquals(traitee.nouvelleId, situation1.getId().longValue());
		assertSituation(date(2007, 1, 1), null, 0, TarifImpotSource.NORMAL, situation1);
	}

	/**
	 * Cas spécial du traitement à rebours : en premier 2010, puis 2009 (pour faire compliqué, ça ne devrait pas arriver en production).
	 */
	@NotTransactional
	@Test
	public void testRunCasSpecial() throws Exception {


		class Ids {
			long menage;
			long situation;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique julien = addNonHabitant("Julien", "Renard", date(1970, 12, 27), Sexe.MASCULIN);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(julien, null, date(2001, 3, 2), null);
				final MenageCommun menage = ensemble.getMenage();
				ids.menage = menage.getNumero();

				final SituationFamilleMenageCommun situation = addSituation(menage, date(2005, 1, 1), null, 0, TarifImpotSource.DOUBLE_GAIN);
				ids.situation = situation.getId();

				return null;
			}
		});

		// d'abord 2010
		{
			final RegDate dateTraitement = date(2010, 1, 1);
			final ReinitialiserBaremeDoubleGainResults rapport = processor.run(dateTraitement, null);

			doInNewTransaction(new TxCallback() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {

					// une situation doit avoir été traitée
					assertEquals(1, rapport.nbSituationsTotal);
					assertEmpty(rapport.situationsIgnorees);
					assertEmpty(rapport.situationsEnErrors);
					assertEquals(1, rapport.situationsTraitees.size());

					final Situation traitee = rapport.situationsTraitees.get(0);
					assertNotNull(traitee);
					assertEquals(ids.menage, traitee.ctbId);
					assertEquals(ids.situation, traitee.ancienneId);
					assertNotNull(traitee.nouvelleId);

					assertEquals(2, dao.getCount(SituationFamilleMenageCommun.class));

					final List<SituationFamille> list = dao.getAll();
					assertNotNull(list);
					assertEquals(2, list.size());
					Collections.sort(list, new DateRangeComparator<SituationFamille>());

					final SituationFamilleMenageCommun situation0 = (SituationFamilleMenageCommun) list.get(0);
					assertSituation(date(2005, 1, 1), date(2009, 12, 31), 0, TarifImpotSource.DOUBLE_GAIN, situation0);

					final SituationFamilleMenageCommun situation1 = (SituationFamilleMenageCommun) list.get(1);
					assertSituation(date(2010, 1, 1), null, 0, TarifImpotSource.NORMAL, situation1);

					return null;
				}
			});

		}

		// ensuite 2009 : erreur car la situation de famille pour la période 2009.01.01-2009.12.31 entre en collision avec celles existantes
		{
			final RegDate dateTraitement = date(2009, 1, 1);
			final ReinitialiserBaremeDoubleGainResults rapport = processor.run(dateTraitement, null);

			doInNewTransaction(new TxCallback() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {

					// une situation doit avoir été traitée
					assertEquals(1, rapport.nbSituationsTotal);
					assertEmpty(rapport.situationsIgnorees);
					assertEmpty(rapport.situationsTraitees);
					assertEquals(1, rapport.situationsEnErrors.size());

					final Erreur erreur = rapport.situationsEnErrors.get(0);
					assertNotNull(erreur);
					assertEquals(0, erreur.noCtb);
					assertEquals(ids.situation, erreur.situationId);
					assertEquals(ErreurType.EXCEPTION, erreur.raison);

					// pas de changement dans la base
					assertEquals(2, dao.getCount(SituationFamilleMenageCommun.class));

					final List<SituationFamille> list = dao.getAll();
					assertNotNull(list);
					assertEquals(2, list.size());
					Collections.sort(list, new DateRangeComparator<SituationFamille>());

					final SituationFamilleMenageCommun situation0 = (SituationFamilleMenageCommun) list.get(0);
					assertSituation(date(2005, 1, 1), date(2009, 12, 31), 0, TarifImpotSource.DOUBLE_GAIN, situation0);

					final SituationFamilleMenageCommun situation1 = (SituationFamilleMenageCommun) list.get(1);
					assertSituation(date(2010, 1, 1), null, 0, TarifImpotSource.NORMAL, situation1);

					return null;
				}
			});
		}
	}
}
