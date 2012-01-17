package ch.vd.uniregctb.evenement.civil.interne.changement.nom;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class ChangementNomTest extends AbstractEvenementCivilInterneTest {

	private static final long NUMERO_CONTRIBUABLE = 6791L;
	private static final long NUMERO_CONTRIBUABLE_DIRTY = 6792L;

	private static final Logger LOGGER = Logger.getLogger(ChangementNomTest.class);

	private static final Long NO_INDIVIDU = 34567L;
	private static final Long NO_INDIVIDU_DIRTY = 6789L;

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "ChangementNomTest.xml";

	/**
	 * L'index global.
	 */
	private GlobalTiersSearcher searcher;
	private TiersDAO tiersDAO;
	private DefaultMockServiceCivil mockServiceCivil;

	public ChangementNomTest() {
		setWantIndexation(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		mockServiceCivil = new DefaultMockServiceCivil();
	}

	@Test
	public void testHandle() throws Exception {

		serviceCivil.setUp(mockServiceCivil);
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
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Tiers t = tiersDAO.get(NUMERO_CONTRIBUABLE);
				assertFalse(t.isDirty());
				return null;
			}
		});

		// changement du nom dans le registre civil
		final MockIndividu individu = mockServiceCivil.getIndividu(NO_INDIVIDU);
		individu.setNom("Dupuid");

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// déclenchement de l'événement
				final Long principalPPId = tiersDAO.getNumeroPPByNumeroIndividu(individu.getNoTechnique(), true);
				final ChangementNom chgtNom = new ChangementNom(individu, principalPPId, null, null, RegDate.get(), 4848, context);

				List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
				List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

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
		doInNewTransaction(new TxCallback<Object>() {
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

		serviceCivil.setUp(mockServiceCivil);
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
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Connection con = dataSource.getConnection();
				try {
					final Statement st = con.createStatement();
					try {
						st.execute("update TIERS set INDEX_DIRTY=" + dialect.toBooleanValueString(true) + " where NUMERO = 6792");
					}
					finally {
						st.close();
					}
				}
				finally {
					con.close();
				}
				return null;
			}
		});

		// le tiers doit être dirty (précondition)
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Tiers t = tiersDAO.get(NUMERO_CONTRIBUABLE_DIRTY);
				assertTrue(t.isDirty());
				return null;
			}
		});

		// changement du nom dans le registre civil
		final MockIndividu individu = mockServiceCivil.getIndividu(NO_INDIVIDU_DIRTY);
		individu.setNom("Woux"); // Julie Woux

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// déclenchement de l'événement
				final Long principalPPId = tiersDAO.getNumeroPPByNumeroIndividu(individu.getNoTechnique(), true);
				final ChangementNom chgtNom = new ChangementNom(individu, principalPPId, null, null, RegDate.get(), 4848, context);

				List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
				List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

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
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Tiers t = tiersDAO.get(NUMERO_CONTRIBUABLE_DIRTY);
				assertFalse(t.isDirty());
				return null;
			}
		});
	}
}
