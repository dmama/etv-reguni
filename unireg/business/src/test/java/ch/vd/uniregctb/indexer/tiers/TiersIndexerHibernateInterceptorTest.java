package ch.vd.uniregctb.indexer.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class TiersIndexerHibernateInterceptorTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(TiersIndexerHibernateInterceptorTest.class);

	private static final String DB_UNIT_DATA_FILE = "TiersIndexerHibernateInterceptorTest.xml";

	private GlobalTiersSearcher searcher;
	private GlobalTiersIndexer indexer;
	private TiersDAO tiersDAO;
	private boolean savedIndexerValue;

	private final RegDate dateNaissance1 = RegDate.get(2003, 2, 22);
	private final RegDate dateNaissance2 = RegDate.get(2002, 11, 29);

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

		savedIndexerValue = indexer.isThrowOnTheFlyException();
	}

	@Override
	public void onTearDown() throws Exception {

		indexer.setThrowOnTheFlyException(savedIndexerValue);

		super.onTearDown();
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
				Long id = nh.getId();
				return id;
			}
		});

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
	public void testPartialIndexationDontSendOnTheFlyException() throws Exception {

		indexer.setThrowOnTheFlyException(false);

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

	/**
	 * On créée 2 tiers:
	 *   - 1 Habitant dont le NO_INDIVIDU n'existe pas => ca pète l'indexation
	 *   - 1 NonHabitant qui va ne va pas etre indexé correctement non plus
	 *  => Résultat, on doit pouvoir chercher ni le NH ni l'Habitant
	 */
	@Test
	public void testPartialIndexationSendOnTheFlyException() throws Exception {

		indexer.setThrowOnTheFlyException(true);

		// Ce commit() devrait faire peter une exception pour 1 des 2 tiers indexé
		try {
			doInNewTransaction(new TxCallback() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					createAndSaveNonHabitant(); // NH OK
					createAndSaveHabitant(1235643453L); // NO_IND inexistant
					return null;
				}
			});
			fail();
		}
		catch (Exception e) {
			// C'est tout bon
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
				long id = nh.getNumero();
				return id;
			}
		});

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

		// On change le prénom du tiers
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nhab = (PersonnePhysique) tiersDAO.get(id);
				nhab.setPrenom("Marcel");
				return null;
			}
		});

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
}
