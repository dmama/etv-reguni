package ch.vd.unireg.indexer.jobs;

import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.indexer.GlobalIndexInterface;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TypeTiers;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DatabaseIndexationProcessorTest extends BusinessTest {

	private DatabaseIndexationProcessor indexationProcessor;
	private GlobalTiersIndexer indexer;
	private GlobalTiersSearcher searcher;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final StatsService statsService = getBean(StatsService.class, "statsService");
		final GlobalIndexInterface globalIndex = getBean(GlobalIndexInterface.class, "globalTiersIndex");

		this.indexer = globalTiersIndexer;
		this.searcher = globalTiersSearcher;
		this.indexationProcessor = new DatabaseIndexationProcessor(tiersDAO, null, statsService, searcher, indexer, globalIndex, sessionFactory, transactionManager, audit);

		this.indexer.overwriteIndex();
		setWantIndexationTiers(false); // -> va mettre le flag dirty sur tous les tiers modifiés
	}

	/**
	 * [UNIREG-1979] Vérifie que l'indexation des dirty va bien réindexer un tiers dirty malgré une date de réindexation schedulée dans le futur. Après l'exécution, le tiers doit avoir été réindexé, il
	 * ne doit plus être dirty <b>et</b> la date de schedule doit être inchangée.
	 */
	@Test
	public void testDeleteTiersIndexedBeforeAllTypes() throws Exception {

		setWantIndexationTiers(true);

		// on indexe deux personnes
		doInNewTransaction(status -> {
			addNonHabitant("Jean", "Test1", date(1960, 3, 3), Sexe.MASCULIN);
			addNonHabitant("Jean", "Test2", date(1960, 3, 3), Sexe.MASCULIN);
			return null;
		});

		indexer.sync();

		// on indexe une personne supplémentaire
		final Date dateAvantPP3 = new Date();
		doInNewTransaction(status -> {
			addNonHabitant("Jean", "Test3", date(1960, 3, 3), Sexe.MASCULIN);
			return null;
		});

		indexer.sync();

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Jean");

		// on vérifie que les trois personnes sont dans l'indexe
		{
			assertEquals(3, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(3, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Test1", results.get(0).getNom1());
			assertEquals("Jean Test2", results.get(1).getNom1());
			assertEquals("Jean Test3", results.get(2).getNom1());
		}

		// on efface les tiers indexés avant le tiers PP3
		indexationProcessor.deleteTiersIndexedBefore(dateAvantPP3, null, EnumSet.allOf(TypeTiers.class));

		// on vérifie que seule la personne PP3 reste
		{
			assertEquals(1, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(1, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Test3", results.get(0).getNom1());
		}
	}

	/**
	 * [UNIREG-1979] Vérifie que l'indexation des dirty va bien réindexer un tiers dirty malgré une date de réindexation schedulée dans le futur. Après l'exécution, le tiers doit avoir été réindexé, il
	 * ne doit plus être dirty <b>et</b> la date de schedule doit être inchangée.
	 */
	@Test
	public void testDeleteTiersIndexedBeforeOneType() throws Exception {

		setWantIndexationTiers(true);

		// on indexe deux personnes et une entreprise
		doInNewTransaction(status -> {
			addNonHabitant("Jean", "Test1", date(1960, 3, 3), Sexe.MASCULIN);
			addNonHabitant("Jean", "Test2", date(1960, 3, 3), Sexe.MASCULIN);
			addEntrepriseInconnueAuCivil("Jean Entreprise1", date(1970, 1, 1));
			return null;
		});

		indexer.sync();

		// on indexe une personne et une entreprise supplémentaire
		final Date dateAvantAjout = new Date();
		doInNewTransaction(status -> {
			addNonHabitant("Jean", "Test3", date(1960, 3, 3), Sexe.MASCULIN);
			addEntrepriseInconnueAuCivil("Jean Entreprise2", date(1970, 1, 1));
			return null;
		});

		indexer.sync();

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Jean");

		// on vérifie que les cinq tiers sont dans l'indexe
		{
			assertEquals(5, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(5, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Entreprise1", results.get(0).getNom1());
			assertEquals("Jean Entreprise2", results.get(1).getNom1());
			assertEquals("Jean Test1", results.get(2).getNom1());
			assertEquals("Jean Test2", results.get(3).getNom1());
			assertEquals("Jean Test3", results.get(4).getNom1());
		}

		// on efface les personnes physiques indexées avant l'ajout
		indexationProcessor.deleteTiersIndexedBefore(dateAvantAjout, null, EnumSet.of(TypeTiers.PERSONNE_PHYSIQUE));

		// on vérifie que seules les personne Test1 et Test2 ont été effacées (et que l'entreprise Entreprise1 n'a pas été touchée)
		{
			assertEquals(3, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(3, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Entreprise1", results.get(0).getNom1());
			assertEquals("Jean Entreprise2", results.get(1).getNom1());
			assertEquals("Jean Test3", results.get(2).getNom1());
		}
	}

	/**
	 * [SIFISC-27025] Ce test vérifie que le mode FULL_INCREMENTAL fonctionne bien dans le cas où ne veut réindexer que les entreprises.
	 */
	@Test
	public void testRunFullIncrementalWithType() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				// vide
			}
		});

		truncateDatabase();
		setWantIndexationTiers(true);

		class Ids {
			Long pp1;
			Long pm1;
		}
		final Ids ids = new Ids();

		// on indexe deux entreprises et trois personnes
		doInNewTransaction(status -> {
			final Entreprise pm1 = addEntrepriseInconnueAuCivil("Jean Entreprise1", date(1970, 1, 1));
			addEntrepriseInconnueAuCivil("Jean Entreprise2", date(1970, 1, 1));
			final PersonnePhysique pp1 = addNonHabitant("Jean", "Test1", date(1960, 3, 3), Sexe.MASCULIN);
			addNonHabitant("Jean", "Test2", date(1960, 3, 3), Sexe.MASCULIN);
			addNonHabitant("Jean", "Test3", date(1960, 3, 3), Sexe.MASCULIN);
			ids.pm1 = pm1.getId();
			ids.pp1 = pp1.getId();
			return null;
		});
		indexer.sync();

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Jean");

		// on vérifie que les cinq tiers sont dans l'indexe
		{
			assertEquals(5, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(5, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Entreprise1", results.get(0).getNom1());
			assertEquals("Jean Entreprise2", results.get(1).getNom1());
			assertEquals("Jean Test1", results.get(2).getNom1());
			assertEquals("Jean Test2", results.get(3).getNom1());
			assertEquals("Jean Test3", results.get(4).getNom1());
		}

		setWantIndexationTiers(false);

		// on supprime une entreprise, une personne et on ajoute une autre entreprise et une autre personne
		doInNewTransaction(status -> {
			tiersDAO.remove(ids.pm1);
			tiersDAO.remove(ids.pp1);
			addEntrepriseInconnueAuCivil("Jean Entreprise3", date(1970, 1, 1));
			addNonHabitant("Jean", "Test4", date(1960, 3, 3), Sexe.MASCULIN);
			return null;
		});

		// l'indexe doit être inchangé
		{
			assertEquals(5, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(5, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Entreprise1", results.get(0).getNom1());
			assertEquals("Jean Entreprise2", results.get(1).getNom1());
			assertEquals("Jean Test1", results.get(2).getNom1());
			assertEquals("Jean Test2", results.get(3).getNom1());
			assertEquals("Jean Test3", results.get(4).getNom1());
		}

		// on réindexe les entreprises uniquement en mode FULL_INCREMENTAL
		final DatabaseIndexationResults procResults = indexationProcessor.run(GlobalTiersIndexer.Mode.FULL_INCREMENTAL, EnumSet.of(TypeTiers.ENTREPRISE), 4, null);
		assertEquals(2, procResults.getIndexes().size()); // les deux entreprises

		// on vérifie que :
		//  - Entreprise3 est maintenant indexée et que Entreprise1 ne l'est plus
		//  - Test1 est toujours indexés et que Test4 ne l'est toujours pas
		{
			assertEquals(5, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(5, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Entreprise2", results.get(0).getNom1());
			assertEquals("Jean Entreprise3", results.get(1).getNom1());
			assertEquals("Jean Test1", results.get(2).getNom1());
			assertEquals("Jean Test2", results.get(3).getNom1());
			assertEquals("Jean Test3", results.get(4).getNom1());
		}
	}

	/**
	 * Test que l'office d'impôt est bien mise-à-jour lors de l'indexation asynchrone
	 */
	@Test
	public void testUpdateOID() throws Exception {

		class Ids {
			public long dupres;
			public long duclou;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Contribuable sans for
				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Dupres");
				nh = (PersonnePhysique) tiersDAO.save(nh);
				ids.dupres = nh.getNumero();

				// Contribuable avec for
				nh = new PersonnePhysique(false);
				nh.setNom("Duclou");
				{
					ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
					f.setDateDebut(date(2000, 1, 1));
					f.setDateFin(null);
					f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
					f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
					f.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
					f.setMotifRattachement(MotifRattachement.DOMICILE);
					f.setModeImposition(ModeImposition.ORDINAIRE);
					f.setMotifOuverture(MotifFor.ARRIVEE_HC);
					nh.addForFiscal(f);
				}
				nh = (PersonnePhysique) tiersDAO.save(nh);
				ids.duclou = nh.getNumero();

				return null;
			}
		});

		indexationProcessor.run(GlobalTiersIndexer.Mode.FULL, EnumSet.allOf(TypeTiers.class), 1, null);
		indexer.sync();

		doInNewTransaction(status -> {
			// Contribuable sans for
			Tiers nh = tiersDAO.get(ids.dupres);
			assertNotNull(nh);
			assertNull(nh.getOfficeImpotId());

			// Contribuable avec for
			nh = tiersDAO.get(ids.duclou);
			assertNotNull(nh);
			assertEquals((Integer)MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), nh.getOfficeImpotId());
			return null;
		});
	}
}