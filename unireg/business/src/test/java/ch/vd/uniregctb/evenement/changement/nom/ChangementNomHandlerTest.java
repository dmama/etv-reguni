package ch.vd.uniregctb.evenement.changement.nom;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockHistoriqueIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChangementNomHandlerTest extends AbstractEvenementHandlerTest {

	private static final long NUMERO_CONTRIBUABLE = 6791L;
	private static final long NUMERO_CONTRIBUABLE_DIRTY = 6792L;

	private static final Logger LOGGER = Logger.getLogger(ChangementNomHandlerTest.class);

	private static final Long NO_INDIVIDU = 34567L;
	private static final Long NO_INDIVIDU_DIRTY = 6789L;

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "ChangementNomHandlerTest.xml";

	/**
	 * L'index global.
	 */
	private GlobalTiersSearcher searcher;
	private TiersDAO tiersDAO;

	public ChangementNomHandlerTest() {
		setWantIndexation(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
	}

	@Test
	public void testHandle() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de changement de nom.");

		// Rech du tiers avant modif
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(NUMERO_CONTRIBUABLE);
		List<TiersIndexedData> list = searcher.search(criteria);
		Assert.isTrue(list.size() == 1, "Le tiers n'a pas été indexé");
		TiersIndexedData tiers = list.get(0);
		Assert.isTrue(tiers.getNumero().equals(NUMERO_CONTRIBUABLE), "Le numéro du tiers est incorrect");

		// le tiers ne doit pas être dirty (précondition)
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Tiers t = tiersDAO.get(NUMERO_CONTRIBUABLE);
				assertFalse(t.isDirty());
				return null;
			}
		});

		// changement du nom dans le registre civil
		final Individu individu = serviceCivil.getIndividu(NO_INDIVIDU, 2008);
		MockHistoriqueIndividu historiqueIndividu = (MockHistoriqueIndividu) individu.getDernierHistoriqueIndividu();
		historiqueIndividu.setNom("Dupuid");

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// déclenchement de l'événement
				final MockChangementNom chgtNom = new MockChangementNom();
				chgtNom.setIndividu(individu);
				chgtNom.setType(TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM);
				chgtNom.setNumeroEvenement(1452L);
				chgtNom.setDate(RegDate.get());
				chgtNom.setNumeroOfsCommuneAnnonce(4848);
				chgtNom.init(tiersDAO);
				chgtNom.setHandler(evenementCivilHandler);

				List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
				List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

				chgtNom.checkCompleteness(erreurs, warnings); // ne fait rien
				chgtNom.validate(erreurs, warnings);// ne fait rien
				chgtNom.handle(warnings);

				Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement du changement de nom");
				return null;
			}
		});

		indexer.sync();

		{
			// on cherche de nouveau
			List<TiersIndexedData> l = searcher.search(criteria);
			LOGGER.debug("numero : " + l.get(0).getNumero());
			LOGGER.debug("nom : " + l.get(0).getNom1());
			Assert.isTrue(l.size() == 1, "L'indexation n'a pas fonctionné");

			// on verifie que le changement a bien été effectué
			Assert.isTrue(l.get(0).getNom1().endsWith("Dupuid"), "le nouveau nom n'a pas été indexé");
		}

		// le tiers ne doit pas être dirty (postcondition)
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Tiers t = tiersDAO.get(NUMERO_CONTRIBUABLE);
				assertFalse(t.isDirty());
				return null;
			}
		});
	}

	/**
	 * [UNIREG-757] Cas du contribuable qui n'a pas pu être réindexé avant l'arrivée de l'événement (= index dirty) pour une raison ou une
	 * autre
	 */
	@Test
	public void testHandleTiersDirty() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE); // l'indexeur va passer en 'non-dirty' le tiers n°6792, pas moyen de désactiver ça...

		LOGGER.debug("Test de traitement d'un événement de changement de nom.");

		// Rech du tiers avant modif
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(NUMERO_CONTRIBUABLE_DIRTY);
		List<TiersIndexedData> list = searcher.search(criteria);
		Assert.isTrue(list.size() == 1, "Le tiers n'a pas été indexé");
		TiersIndexedData tiers = list.get(0);
		Assert.isTrue(tiers.getNumero().equals(NUMERO_CONTRIBUABLE_DIRTY), "Le numéro du tiers est incorrect");

		// on est obligé de mettre-à-jour la base dans le dos d'hibernate et de le faire après avoir indexé la base (voir commentaire sur
		// appel de loadDatabase()).
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Connection con = dataSource.getConnection();
				try {
					Statement st = con.createStatement();
					st.execute("update TIERS set INDEX_DIRTY=" + dialect.toBooleanValueString(true) + " where NUMERO = 6792");
				}
				finally {
					con.close();
				}
				return null;
			}
		});

		// le tiers doit être dirty (précondition)
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Tiers t = tiersDAO.get(NUMERO_CONTRIBUABLE_DIRTY);
				assertTrue(t.isDirty());
				return null;
			}
		});

		// changement du nom dans le registre civil
		final Individu individu = serviceCivil.getIndividu(NO_INDIVIDU_DIRTY, 2008);
		MockHistoriqueIndividu historiqueIndividu = (MockHistoriqueIndividu) individu.getDernierHistoriqueIndividu();
		historiqueIndividu.setNom("Woux"); // Julie Woux

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// déclenchement de l'événement
				final MockChangementNom chgtNom = new MockChangementNom();
				chgtNom.setIndividu(individu);
				chgtNom.setType(TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM);
				chgtNom.setNumeroEvenement(1453L);
				chgtNom.setDate(RegDate.get());
				chgtNom.setNumeroOfsCommuneAnnonce(4848);
				chgtNom.init(tiersDAO);
				chgtNom.setHandler(evenementCivilHandler);

				List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
				List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

				chgtNom.checkCompleteness(erreurs, warnings); // ne fait rien
				chgtNom.validate(erreurs, warnings);// ne fait rien
				chgtNom.handle(warnings);

				Assert.isTrue(erreurs.isEmpty(), "Une erreur est survenue lors du traitement du changement de nom");
				return null;
			}
		});

		indexer.sync();

		{
			// on cherche de nouveau
			List<TiersIndexedData> l = searcher.search(criteria);
			LOGGER.debug("numero : " + l.get(0).getNumero());
			LOGGER.debug("nom : " + l.get(0).getNom1());
			Assert.isTrue(l.size() == 1, "L'indexation n'a pas fonctionné");

			// on verifie que le changement a bien été effectué
			assertEquals("le nouveau nom n'a pas été indexé", "Julie Woux", l.get(0).getNom1());
		}

		// le tiers ne doit plus être dirty (postcondition)
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Tiers t = tiersDAO.get(NUMERO_CONTRIBUABLE_DIRTY);
				assertFalse(t.isDirty());
				return null;
			}
		});
	}
}
