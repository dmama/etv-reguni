package ch.vd.unireg.indexer.tiers;

import java.util.EnumSet;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.indexer.GlobalIndexInterface;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class TiersIndexerHibernateInterceptorTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersIndexerHibernateInterceptorTest.class);

	private static final String DB_UNIT_DATA_FILE = "TiersIndexerHibernateInterceptorTest.xml";

	private GlobalTiersSearcher searcher;
	private GlobalTiersIndexer indexer;
	private TiersDAO tiersDAO;

	private final RegDate dateNaissance1 = RegDate.get(2003, 2, 22);
	private final RegDate dateNaissance2 = RegDate.get(2002, 11, 29);

	public TiersIndexerHibernateInterceptorTest() {
		setWantIndexationTiers(true);
	}

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
		indexer = getBean(GlobalTiersIndexer.class, "globalTiersIndexer");

		// Overwrite l'index
		final GlobalIndexInterface globalIndex = getBean(GlobalIndexInterface.class, "globalTiersIndex");
		globalIndex.overwriteIndex();

		serviceCivil.setUp(new DefaultMockServiceCivil());

	}

	private ForFiscalPrincipalPP createForPrincipal(int ofs, RegDate date) {
		final ForFiscalPrincipalPP ffp = new ForFiscalPrincipalPP();
		ffp.setDateDebut(date);
		ffp.setNumeroOfsAutoriteFiscale(ofs); // Lausanne
		ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ffp.setModeImposition(ModeImposition.ORDINAIRE);
		ffp.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp.setMotifOuverture(MotifFor.ARRIVEE_HC);
		return ffp;
	}

	private PersonnePhysique createAndSaveNonHabitant() throws Exception {

		final PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom("Bla");
		nh.setPrenomUsuel("Blo");
		nh.setDateNaissance(dateNaissance1);

		final ForFiscalPrincipal ffp = createForPrincipal(5586, RegDate.get(2008, 3, 3));
		nh.addForFiscal(ffp);

		return (PersonnePhysique) tiersDAO.save(nh);
	}

	private PersonnePhysique createAndSaveHabitant(long noInd) throws Exception {
		final PersonnePhysique h = new PersonnePhysique(true);
		h.setNumeroIndividu(noInd);
		return (PersonnePhysique)tiersDAO.save(h);
	}

	@Test(timeout = 120000)
	@Transactional(rollbackFor = Throwable.class)
	public void testIndexationOnCreate() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique nh = createAndSaveNonHabitant();
				assertNotNull(nh);

				// Ne doit pas trouver le tiers
				{
					TiersCriteria criteria = new TiersCriteria();
					criteria.setNomRaison("bla");
					List<TiersIndexedData> list = searcher.search(criteria);
					assertEquals(0, list.size());
				}

				return nh.getNumero();
			}
		});

		globalTiersIndexer.sync();

		// On doit trouver le tiers
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("bla");
			List<TiersIndexedData> list = searcher.search(criteria);
			assertEquals(1, list.size());
		}

		// Il ne doit pas être dirty
		{
			Tiers tiers = tiersDAO.get(id);
			assertNotNull(tiers);
			assertFalse(tiers.isDirty());
		}
	}

	@Test(timeout = 120000)
	@Transactional(rollbackFor = Throwable.class)
	public void testIndexationOnUpdate() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique nh = createAndSaveNonHabitant();
				return nh.getId();
			}
		});

		globalTiersIndexer.sync();

		// On peut trouver le tiers sur son ancienne date
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setDateNaissanceInscriptionRC(dateNaissance1);
			List<?> list = searcher.search(criteria);
			assertEquals(1, list.size());
		}

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique nh = (PersonnePhysique)tiersDAO.get(id);
				nh.setDateNaissance(dateNaissance2);

				// On peut trouver le tiers sur son ancienne date
				{
					TiersCriteria criteria = new TiersCriteria();
					criteria.setDateNaissanceInscriptionRC(dateNaissance1);
					List<TiersIndexedData> list = searcher.search(criteria);
					assertEquals(1, list.size());
					assertEquals(id, list.get(0).getNumero());
				}

				// Mais pas sur sa nouvelle
				{
					TiersCriteria criteria = new TiersCriteria();
					criteria.setDateNaissanceInscriptionRC(dateNaissance2);
					List<?> list = searcher.search(criteria);
					assertEquals(0, list.size());
				}
				return null;
			}
		});

		globalTiersIndexer.sync();

		// La date est changée en base et dans l'indexer
		{
			PersonnePhysique nh = (PersonnePhysique)tiersDAO.get(id);
			assertEquals(dateNaissance2, nh.getDateNaissance());
		}

		// On peut trouver le tiers sur sa nouvelle date
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setDateNaissanceInscriptionRC(dateNaissance2);
			List<TiersIndexedData> list = searcher.search(criteria);
			int n = list.size();
			assertEquals(1, n);
			assertEquals(id, list.get(0).getNumero());
		}

		// Il ne doit pas être dirty
		{
			Tiers tiers = tiersDAO.get(id);
			assertNotNull(tiers);
			assertFalse(tiers.isDirty());
		}
	}

	/**
	 * On créée 2 tiers:
	 *   - 1 Habitant dont le NO_INDIVIDU n'existe pas => ca pète l'indexation
	 *   - 1 NonHabitant qui va s'indexer correctement
	 *  => Résultat, on doit pouvoir chercher le NH mais pas l'Habitant
	 */
	@Test(timeout = 120000)
	@Transactional(rollbackFor = Throwable.class)
	public void testPartialIndexation() throws Exception {

		final class Numeros {
			Long nhid;
			Long hid;
		}
		final Numeros numeros = new Numeros();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nh = createAndSaveNonHabitant(); // NH OK
				numeros.nhid = nh.getId();
				PersonnePhysique h = createAndSaveHabitant(1235643453L); // NO_IND inexistant
				numeros.hid = h.getId();

				// Ce commit() devrait faire peter une exception pour 1 des 2 tiers indexé
				LOGGER.warn("L'exception ci-dessous générée par l'indexation est normale!");
				return null;
			}
		});
		LOGGER.warn("L'exception ci-dessus générée par l'indexation est normale!");

		globalTiersIndexer.sync();

		// On doit trouver le NH
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(numeros.nhid);
			List<TiersIndexedData> list = searcher.search(criteria);
			int n = list.size();
			assertEquals(1, n);
			assertEquals(numeros.nhid, list.get(0).getNumero());
		}

		// On doit PAS trouver l'Habitant
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(numeros.hid);
			List<TiersIndexedData> list = searcher.search(criteria);
			int n = list.size();
			assertEquals(0, n);
		}
		// Mais on doit le trouver dans la base et il doit être "dirty"
		{
			PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(numeros.hid);
			assertTrue(hab.isDirty());
		}
		// Le NH doit etre clean
		{
			PersonnePhysique nhab = (PersonnePhysique)tiersDAO.get(numeros.nhid);
			assertFalse(nhab.isDirty());
		}
	}

	@Test(timeout = 120000)
	@Transactional(rollbackFor = Throwable.class)
	public void testIndexationOnModifyFor() throws Exception {

		LOGGER.info("==== testIndexationOnModifyFor START ====");

		LOGGER.info("==== testIndexationOnModifyFor MODIF 1 ====");
		final long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique nh = createAndSaveNonHabitant();
				assertNotNull(nh);
				long id = nh.getNumero();

				// Ne doit pas trouver le tiers
				{
					TiersCriteria criteria = new TiersCriteria();
					criteria.setNomRaison("bla");
					List<TiersIndexedData> list = searcher.search(criteria);
					assertEquals(0, list.size());
				}
				return id;
			}
		});

		globalTiersIndexer.sync();
		LOGGER.info("==== testIndexationOnModifyFor après SYNC 1 ====");

		// On doit trouver le tiers
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("bla");
			List<TiersIndexedData> list = searcher.search(criteria);
			assertEquals(1, list.size());
		}
		// Son for est a Lausanne
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNoOfsFor("5586");
			List<TiersIndexedData> list = searcher.search(criteria);
			assertEquals(1, list.size());
		}
		// Il ne doit pas être dirty
		{
			Tiers tiers = tiersDAO.get(id);
			assertNotNull(tiers);
			assertFalse(tiers.isDirty());
		}

		LOGGER.info("==== testIndexationOnModifyFor MODIF 2 ====");
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// On modifie son for
				PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(id);
				ForFiscalPrincipal ffp = hab.getForFiscalPrincipalAt(null);
				assertNotNull(ffp);
				ffp.setNumeroOfsAutoriteFiscale(5477);
				return null;
			}
		});

		globalTiersIndexer.sync();
		LOGGER.info("==== testIndexationOnModifyFor après SYNC 2 ====");

		// On le trouve plus a Lausanne
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNoOfsFor("5586");
			List<TiersIndexedData> list = searcher.search(criteria);
			assertEquals(0, list.size());
		}
		// Son for est maintenant a Cossonay
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNoOfsFor("5477");
			List<TiersIndexedData> list = searcher.search(criteria);
			assertEquals(1, list.size());
		}
		// Il ne doit pas être dirty
		{
			Tiers tiers = tiersDAO.get(id);
			assertNotNull(tiers);
			assertFalse(tiers.isDirty());
		}

		LOGGER.info("==== testIndexationOnModifyFor MODIF 3 ====");
		// On ajoute un for
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nhab = (PersonnePhysique)tiersDAO.get(id);
				ForFiscalPrincipal ffp = nhab.getForFiscalPrincipalAt(null);
				assertNotNull(ffp);
				RegDate date = RegDate.get(2008, 6, 6);
				ffp.setDateFin(date);
				ffp.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);

				// Villars
				ffp = createForPrincipal(5652, RegDate.get(2008, 6, 7));
				ffp.setDateDebut(date.addDays(1));
				ffp.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
				nhab.addForFiscal(ffp);
				return null;
			}
		});

		globalTiersIndexer.sync();
		LOGGER.info("==== testIndexationOnModifyFor après SYNC 3 ====");

		// Son for est maintenant a Villars
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNoOfsFor("5652");
			List<TiersIndexedData> list = searcher.search(criteria);
			assertEquals(1, list.size());
		}
		// Et on le trouve plus a Cossonay en Principal actif
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setForPrincipalActif(true);
			criteria.setNoOfsFor("5477");
			List<TiersIndexedData> list = searcher.search(criteria);
			assertEquals(0, list.size());
		}
		// Mais on le trouve a Cossonay en Inactif
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNoOfsFor("5477");
			List<TiersIndexedData> list = searcher.search(criteria);
			assertEquals(1, list.size());
		}
		// Il ne doit pas être dirty
		{
			Tiers tiers = tiersDAO.get(id);
			assertNotNull(tiers);
			assertFalse(tiers.isDirty());
		}

		LOGGER.info("==== testIndexationOnModifyFor END ====");
	}

	@Test(timeout = 120000)
	@Transactional(rollbackFor = Throwable.class)
	public void testIndexationOnModificationEtatEntreprise() throws Exception {

		final long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				final RegDate dateDebut = date(2009, 1, 1);
				addRaisonSociale(entreprise, dateDebut, null, "Bla");
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.FONDATION);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);     // tous les 31.12 depuis 2009
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				addEtatEntreprise(entreprise, dateDebut, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);
				addEtatEntreprise(entreprise, dateDebut, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);

				// Ne doit pas trouver le tiers (l'indexation ne se fait qu'au commit de la transaction)
				{
					final TiersCriteria criteria = new TiersCriteria();
					criteria.setNomRaison("bla");
					final List<TiersIndexedData> list = searcher.search(criteria);
					assertEquals(0, list.size());
				}

				return entreprise.getNumero();
			}
		});

		globalTiersIndexer.sync();

		// On doit trouver le tiers
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("bla");
			final List<TiersIndexedData> list = searcher.search(criteria);
			assertEquals(1, list.size());

			final TiersIndexedData data = list.get(0);
			assertNotNull(data);
			assertEquals(TypeEtatEntreprise.INSCRITE_RC, data.getEtatEntreprise());
			assertEquals(EnumSet.of(TypeEtatEntreprise.INSCRITE_RC, TypeEtatEntreprise.FONDEE), data.getTousEtatsEntreprise());
		}
		// Il ne doit pas être dirty
		{
			final Tiers tiers = tiersDAO.get(id);
			assertNotNull(tiers);
			assertFalse(tiers.isDirty());
		}

		// Annulation de l'état INSCRITE_RC
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				final EtatEntreprise etatActuel = entreprise.getEtatActuel();
				assertNotNull(etatActuel);
				assertEquals(TypeEtatEntreprise.INSCRITE_RC, etatActuel.getType());
				assertFalse(etatActuel.isAnnule());

				etatActuel.setAnnule(true);
			}
		});

		globalTiersIndexer.sync();

		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("bla");
			final List<TiersIndexedData> list = searcher.search(criteria);
			assertEquals(1, list.size());

			final TiersIndexedData data = list.get(0);
			assertNotNull(data);
			assertEquals(TypeEtatEntreprise.FONDEE, data.getEtatEntreprise());
			assertEquals(EnumSet.of(TypeEtatEntreprise.FONDEE), data.getTousEtatsEntreprise());
		}
		// Il ne doit pas être dirty
		{
			Tiers tiers = tiersDAO.get(id);
			assertNotNull(tiers);
			assertFalse(tiers.isDirty());
		}
	}

	/**
	 * Vérifie que le flag dirty est bien mis lorsqu'un tiers est mis-à-jour dans la base alors que l'indexation on-the-fly est désactivée.
	 */
	@Test(timeout = 120000)
	@Transactional(rollbackFor = Throwable.class)
	public void testDisabledOnTheFlyIndexation() throws Exception {

		// On crée un tiers
		final long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique nh = createAndSaveNonHabitant();
				assertNotNull(nh);
				return nh.getNumero();
			}
		});

		globalTiersIndexer.sync();

		// On doit trouver le tiers et il ne doit pas être dirty
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				TiersCriteria criteria = new TiersCriteria();
				criteria.setNomRaison("Bla");
				final List<TiersIndexedData> list = searcher.search(criteria);
				assertEquals(1, list.size());
				assertEquals("Blo Bla", list.get(0).getNom1());

				final PersonnePhysique hab = (PersonnePhysique) tiersDAO.get(id);
				assertFalse(hab.isDirty());
				return null;
			}
		});

		// On désactive l'indexation on the fly
		indexer.onTheFlyIndexationSwitch().setEnabled(false);
		try {

			// On change le prénom du tiers
			doInNewTransaction(new TxCallback<Object>() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					PersonnePhysique nhab = (PersonnePhysique) tiersDAO.get(id);
					nhab.setPrenomUsuel("Marcel");
					return null;
				}
			});

			globalTiersIndexer.sync();

			// On doit toujours trouver le tiers, mais il ne doit pas avoir été réindexé et il doit être dirty
			{
				TiersCriteria criteria = new TiersCriteria();
				criteria.setNomRaison("Bla");
				final List<TiersIndexedData> list = searcher.search(criteria);
				assertEquals(1, list.size());
				assertEquals("Blo Bla", list.get(0).getNom1());

				final PersonnePhysique hab = (PersonnePhysique) tiersDAO.get(id);
				assertTrue(hab.isDirty());
			}
		}
		finally {
			indexer.onTheFlyIndexationSwitch().setEnabled(true);
		}
	}

	/**
	 * Vérifie que le flag dirty est bien resetté lorsqu'un tiers dirty est finalement indexé sans erreur.
	 */
	@Test(timeout = 120000)
	@Transactional(rollbackFor = Throwable.class)
	public void testResetIndexDirtyFlag() throws Exception {

		// Charge un tiers dirty
		final long id = 6791;
		setWantIndexationTiers(false);
		loadDatabase(DB_UNIT_DATA_FILE);

		// Le tiers doit être dirty et on ne doit pas trouver dans l'indexeur
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique nh = (PersonnePhysique) tiersDAO.get(id);
				assertTrue(nh.isDirty());

				TiersCriteria criteria = new TiersCriteria();
				criteria.setNomRaison("Bla");
				final List<TiersIndexedData> list = searcher.search(criteria);
				assertEquals(0, list.size());

				return null;
			}
		});

		setWantIndexationTiers(true);
		
		// On change le prénom du tiers
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nhab = (PersonnePhysique) tiersDAO.get(id);
				nhab.setPrenomUsuel("Marcel");
				return null;
			}
		});

		globalTiersIndexer.sync();
		
		// On doit trouver le tiers, il doit avoir été indexé et il ne doit plus être dirty
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Bla");
			final List<TiersIndexedData> list = searcher.search(criteria);
			assertEquals(1, list.size());
			assertEquals("Marcel Bla", list.get(0).getNom1());

			final PersonnePhysique hab = (PersonnePhysique) tiersDAO.get(id);
			assertFalse(hab.isDirty());
		}
	}

	/**
	 * [UNIREG-1988] Vérifie qu'un tiers nouvellement créé n'est pas indexé si la transaction est rollée-back
	 */
	@Test       //(timeout = 120000)
	@Transactional(rollbackFor = Throwable.class)
	public void testIndexOnRollback() throws Exception {

		// l'indexeur doit être vide
		globalTiersIndexer.overwriteIndex();
		assertEmpty(searcher.getAllIds());

		class Ids {
			long pp;
		}
		final Ids ids = new Ids();
		
		// crée un tiers qui ne valide pas
		try {
			doInNewTransactionAndSession(new TxCallback<Object>() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {

					final PersonnePhysique pp = addNonHabitant("Arnold", "Fellow", date(1960, 1, 1), Sexe.MASCULIN);
					ids.pp = pp.getNumero();

					hibernateTemplate.flush(); // pour être sûr que le tiers est bien inséré en base (mais attention, la transaction est toujours ouverte)

					// on ajoute deux fors principaux actifs en même temps, de manière à provoquer une erreur de validation à la sauvegarde finale
					addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Aubonne);
					addForPrincipal(pp, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Aubonne);
					return null;
				}
			});
			fail("Le tiers qui ne valide pas aurait dû lever une exception");
		}
		catch (Exception e) {
			assertContains("PersonnePhysique #" + ids.pp + " - 1 erreur(s) - 0 avertissement(s):\n" +
					" [E] Le for principal qui commence le 01.01.1990 chevauche le for précédent\n", e.getMessage());
		}

		// le tiers ne doit pas exister dans la base, ni dans l'indexeur
		assertEmpty(getAllPersonnesPhysiques());
		assertEmpty(searcher.getAllIds());
	}

	private List<PersonnePhysique> getAllPersonnesPhysiques() {
		return allTiersOfType(PersonnePhysique.class);
	}
}
