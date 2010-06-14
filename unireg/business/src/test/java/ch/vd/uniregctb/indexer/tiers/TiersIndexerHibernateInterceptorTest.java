package ch.vd.uniregctb.indexer.tiers;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.*;
import ch.vd.uniregctb.type.*;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import java.util.List;

import static junit.framework.Assert.*;

@SuppressWarnings({"JavaDoc"})
public class TiersIndexerHibernateInterceptorTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(TiersIndexerHibernateInterceptorTest.class);

	private static final String DB_UNIT_DATA_FILE = "TiersIndexerHibernateInterceptorTest.xml";

	private GlobalTiersSearcher searcher;
	private GlobalTiersIndexer indexer;
	private TiersDAO tiersDAO;

	private final RegDate dateNaissance1 = RegDate.get(2003, 2, 22);
	private final RegDate dateNaissance2 = RegDate.get(2002, 11, 29);

	public TiersIndexerHibernateInterceptorTest() {
		setWantIndexation(true);
	}

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
		indexer = getBean(GlobalTiersIndexer.class, "globalTiersIndexer");

		// Overwrite l'index
		GlobalIndexInterface globalIndex = getBean(GlobalIndexInterface.class, "globalIndex");
		globalIndex.overwriteIndex();

		serviceCivil.setUp(new DefaultMockServiceCivil());

	}

	private ForFiscalPrincipal createForPrincipal(int ofs, RegDate date) {

		ForFiscalPrincipal ffp = new ForFiscalPrincipal();
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

		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom("Bla");
		nh.setPrenom("Blo");
		nh.setDateNaissance(dateNaissance1);

		ForFiscalPrincipal ffp = createForPrincipal(5586, RegDate.get(2008, 3, 3));
		nh.addForFiscal(ffp);

		return (PersonnePhysique)tiersDAO.save(nh);
	}
	private PersonnePhysique createAndSaveHabitant(long noInd) throws Exception {

		PersonnePhysique h = new PersonnePhysique(true);
		h.setNumeroIndividu(noInd);

		return (PersonnePhysique)tiersDAO.save(h);
	}

	@Test
	public void testIndexationOnCreate() throws Exception {

		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
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

	@Test
	public void testIndexationOnUpdate() throws Exception {

		final Long id = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nh = createAndSaveNonHabitant();
				return nh.getId();
			}
		});

		globalTiersIndexer.sync();

		// On peut trouver le tiers sur son ancienne date
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setDateNaissance(dateNaissance1);
			List<?> list = searcher.search(criteria);
			assertEquals(1, list.size());
		}

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique nh = (PersonnePhysique)tiersDAO.get(id);
				nh.setDateNaissance(dateNaissance2);

				// On peut trouver le tiers sur son ancienne date
				{
					TiersCriteria criteria = new TiersCriteria();
					criteria.setDateNaissance(dateNaissance1);
					List<TiersIndexedData> list = searcher.search(criteria);
					assertEquals(1, list.size());
					assertEquals(id, list.get(0).getNumero());
				}

				// Mais pas sur sa nouvelle
				{
					TiersCriteria criteria = new TiersCriteria();
					criteria.setDateNaissance(dateNaissance2);
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
			criteria.setDateNaissance(dateNaissance2);
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
	@Test
	public void testPartialIndexation() throws Exception {

		final class Numeros {
			Long nhid;
			Long hid;
		}
		final Numeros numeros = new Numeros();
		doInNewTransaction(new TxCallback() {
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

	@Test
	public void testIndexationOnModifyFor() throws Exception {

		final long id = (Long)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
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

		doInNewTransaction(new TxCallback() {
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

		// On ajoute un for
		doInNewTransaction(new TxCallback() {
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
	}

	/**
	 * Vérifie que le flag dirty est bien mis lorsqu'un tiers est mis-à-jour dans la base alors que l'indexation on-the-fly est désactivée.
	 */
	@Test
	public void testDisabledOnTheFlyIndexation() throws Exception {

		// On crée un tiers
		final long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nh = createAndSaveNonHabitant();
				assertNotNull(nh);
				return nh.getNumero();
			}
		});

		globalTiersIndexer.sync();

		// On doit trouver le tiers et il ne doit pas être dirty
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				TiersCriteria criteria = new TiersCriteria();
				criteria.setNomRaison("Bla");
				final List<TiersIndexedData> list = searcher.search(criteria);
				assertEquals(1, list.size());
				assertEquals("Bla Blo", list.get(0).getNom1());

				final PersonnePhysique hab = (PersonnePhysique) tiersDAO.get(id);
				assertFalse(hab.isDirty());
				return null;
			}
		});

		try {
			// On désactive l'indexation on the fly
			indexer.setOnTheFlyIndexation(false);

			// On change le prénom du tiers
			doInNewTransaction(new TxCallback() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					PersonnePhysique nhab = (PersonnePhysique) tiersDAO.get(id);
					nhab.setPrenom("Marcel");
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
				assertEquals("Bla Blo", list.get(0).getNom1());

				final PersonnePhysique hab = (PersonnePhysique) tiersDAO.get(id);
				assertTrue(hab.isDirty());
			}
		}
		finally {
			indexer.setOnTheFlyIndexation(true);
		}
	}

	/**
	 * Vérifie que le flag dirty est bien resetté lorsqu'un tiers dirty est finalement indexé sans erreur.
	 */
	@Test
	public void testResetIndexDirtyFlag() throws Exception {

		// Charge un tiers dirty
		final long id = 6791;
		setWantIndexation(false);
		loadDatabase(DB_UNIT_DATA_FILE);

		// Le tiers doit être dirty et on ne doit pas trouver dans l'indexeur
		doInNewTransaction(new TxCallback() {
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

		setWantIndexation(true);
		
		// On change le prénom du tiers
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nhab = (PersonnePhysique) tiersDAO.get(id);
				nhab.setPrenom("Marcel");
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
			assertEquals("Bla Marcel", list.get(0).getNom1());

			final PersonnePhysique hab = (PersonnePhysique) tiersDAO.get(id);
			assertFalse(hab.isDirty());
		}
	}

	/**
	 * [UNIREG-1988] Vérifie qu'un tiers nouvellement créé n'est pas indexé si la transaction est rollée-back
	 */
	@Test
	public void testIndexOnRollback() throws Exception {

		// l'indexeur doit être vide
		assertEmpty(searcher.getAllIds());

		class Ids {
			long pp;
		}
		final Ids ids = new Ids();
		
		// crée un tiers qui ne valide pas
		try {
			doInNewTransactionAndSession(new TxCallback() {
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
			assertEquals("ch.vd.registre.base.validation.ValidationException: PersonnePhysique #" + ids.pp + " - 1 erreur(s) - 0 warning(s):\n" +
					" [E] Le for principal qui commence le 01.01.1990 chevauche le for précédent\n", e.getMessage());
		}

		// le tiers ne doit pas exister dans la base, ni dans l'indexeur
		assertEmpty(tiersDAO.getAllIds());
		assertEmpty(searcher.getAllIds());
	}
}
