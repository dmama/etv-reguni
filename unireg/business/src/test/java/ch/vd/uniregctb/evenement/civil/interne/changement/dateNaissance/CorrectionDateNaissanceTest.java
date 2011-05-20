package ch.vd.uniregctb.evenement.civil.interne.changement.dateNaissance;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class CorrectionDateNaissanceTest extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = Logger.getLogger(CorrectionDateNaissanceTest.class);

	/** Le numéro d'individu */
	private static final Long NO_INDIVIDU = 54321L;

	/** Le numero de contribuable */
	private static final long NUMERO_CONTRIBUABLE = 10000;

	protected static final RegDate DATE_NAISSANCE_CORRIGEE = RegDate.get(1961, 3, 8); //19610312

	/** Le numéro d'individu pour test avec erreur */
	private static final long NO_INDIVIDU_ERREUR = 34567;

	/** Le numero de contribuable */
	private static final long NUMERO_CONTRIBUABLE_ERREUR = 6791L;

	protected static final RegDate DATE_NAISSANCE_CORRIGEE_ERREUR = RegDate.get(1964, 10, 8); //19640408

	/** Le fichier de données de test. */
	private static final String DB_UNIT_DATA_FILE = "CorrectionDateNaissanceTest.xml";

	/** L'index global. */
	private GlobalTiersSearcher searcher;

	public CorrectionDateNaissanceTest() {
		setWantIndexation(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");

	}

	@Test
	public void testHandle() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de changement de date de naissance.");

		indexer.sync();

		// Rech du tiers avant modif
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(NUMERO_CONTRIBUABLE);
		List<TiersIndexedData> list = searcher.search(criteria);
		Assert.isTrue(list.size() == 1, "Le tiers n'a pas été indexé");
		TiersIndexedData tiers = list.get(0);
		Assert.isTrue(tiers.getNumero().equals(NUMERO_CONTRIBUABLE), "Le numéro du tiers est incorrect");

		// changement de la date de naissance dans le registre civil
		doModificationIndividu(NO_INDIVIDU, new IndividuModification() {
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateNaissance(DATE_NAISSANCE_CORRIGEE);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// déclenchement de l'événement
				final Individu individu = serviceCivil.getIndividu(NO_INDIVIDU, 2008);
				final CorrectionDateNaissance correctionDateNaissane = createValidCorrectionDateNaissane(individu, DATE_NAISSANCE_CORRIGEE);

				final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
				final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

				correctionDateNaissane.checkCompleteness(erreurs, warnings);
				assertEmpty("Une erreur est survenue lors du checkCompleteness de correction de date de naissance.", erreurs);
				correctionDateNaissane.validate(erreurs, warnings);
				assertEmpty("Une erreur est survenue lors du validate de correction de date de naissance.", erreurs);
				correctionDateNaissane.handle(warnings);

				return null;
			}
		});

		indexer.sync();

		{
			// on cherche de nouveau
			List<TiersIndexedData> l = searcher.search(criteria);
			LOGGER.debug("numero : " + l.get(0).getNumero());
			LOGGER.debug ("nom : " + l.get(0).getNom1());
			Assert.isTrue(l.size() == 1, "L'indexation n'a pas fonctionné");

			// on verifie que le changement a bien été effectué
			String dateNaissance = String.format("%4d%02d%02d", DATE_NAISSANCE_CORRIGEE.year(), DATE_NAISSANCE_CORRIGEE.month(), DATE_NAISSANCE_CORRIGEE.day());
			Assert.isTrue(l.get(0).getDateNaissance().equals(dateNaissance), "la nouvelle date de naissance n'a pas été indexé");
		}

	}

	@Test
	public void testHandleWithErrors() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de changement de date de naissance (avec erreurs).");

		indexer.sync();

		// Rech du tiers avant modif
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(NUMERO_CONTRIBUABLE_ERREUR);
		List<TiersIndexedData> list = searcher.search(criteria);
		Assert.isTrue(list.size() == 1, "Le tiers n'a pas été indexé");
		TiersIndexedData tiers = list.get(0);
		Assert.isTrue(tiers.getNumero().equals(NUMERO_CONTRIBUABLE_ERREUR), "Le numéro du tiers est incorrect");

		// changement de la date de naissance dans le registre civil
		doModificationIndividu(NO_INDIVIDU_ERREUR, new IndividuModification() {
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateNaissance(DATE_NAISSANCE_CORRIGEE_ERREUR);
			}
		});

		try {
			doInNewTransaction(new TxCallback<Object>() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					// déclenchement de l'événement
					final Individu individu = serviceCivil.getIndividu(NO_INDIVIDU_ERREUR, 2008);
					CorrectionDateNaissance correctionDateNaissane = createValidCorrectionDateNaissane(individu, DATE_NAISSANCE_CORRIGEE_ERREUR);

					final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
					final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

					correctionDateNaissane.checkCompleteness(erreurs, warnings);
					assertEmpty("Une erreur est survenue lors du checkCompleteness de correction de date de naissance.", erreurs);
					correctionDateNaissane.validate(erreurs, warnings);
					assertEmpty("Une erreur est survenue lors du validate de correction de date de naissance.", erreurs);
					correctionDateNaissane.handle(warnings);

					return null;
				}
			});
			fail("On aurait dû recevoir une erreur de validation à cause premier for fiscal principal qui se retrouve avec une date de début après la date de fin.");
		}
		catch (ValidationException echec) {
			// ok
		}
	}

	private CorrectionDateNaissance createValidCorrectionDateNaissane(Individu individu, RegDate dateNaissanceCorrigee) {

		final Long principalPPId = tiersDAO.getNumeroPPByNumeroIndividu(individu.getNoTechnique(), true);
		CorrectionDateNaissance correctionDateNaissane = new CorrectionDateNaissance(individu, principalPPId, null, null, dateNaissanceCorrigee, 5652, context);

		return correctionDateNaissane;
	}

	/**
	 * [UNIREG-1321] Teste que la correction d'une date de naissance sur un contribuable marqué comme dirty dans la base (= devant être
	 * réindexé) ne provoque pas d'optimistic locking exception (ce qui était le cas à cause d'un flush intermédiaire de la session
	 * Hibernate entre le moment où la date de naissance est mise-à-jour et le moment où le flag index dirty est resetté).
	 */
	@Test
	public void testHandleSurContribuableDirty() throws Exception {

		final long noIndJean = 1234L;
		final RegDate dateNaissance = date(1973, 4, 27);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndJean, dateNaissance, "Jean", "De Florette", true);
			}
		});

		final class Ids {
			long jean;
		}
		final Ids ids = new Ids();

		// Crée un individu dans la base
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique jean = addHabitant(noIndJean);
				ids.jean = jean.getNumero();
				return null;
			}
		});

		// Flag cet individu comme dirty (on doit bypasser Hibernate, autrement l'intercepteur va réindexer le tiers automatiquement et
		// resetter le flag)
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Connection con = dataSource.getConnection();
				try {

					final Statement stat = con.createStatement();
					try {
						stat.execute("update TIERS set INDEX_DIRTY = " + dialect.toBooleanValueString(true) + " where NUMERO = " + ids.jean);
					}
					finally {
						stat.close();
					}
				}
				finally {
					con.close();
				}
				return null;
			}
		});

		// Déclenchement de l'événement
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final Individu individu = serviceCivil.getIndividu(noIndJean, 2008);
				CorrectionDateNaissance correctionDateNaissane = createValidCorrectionDateNaissane(individu, dateNaissance);

				final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
				final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

				correctionDateNaissane.checkCompleteness(erreurs, warnings);
				assertEmpty(erreurs);
				correctionDateNaissane.validate(erreurs, warnings);
				assertEmpty(erreurs);
				correctionDateNaissane.handle(warnings);
				return null;
			}
		});

		indexer.sync();

		// Vérifie que la date de naissance à bien été corrigée (= cachée sur la personne physique) et que la personne physique n'est plus
		// flaggée comme dirty.
		final PersonnePhysique jean = (PersonnePhysique) tiersDAO.get(ids.jean);
		assertNotNull(jean);
		assertFalse(jean.isDirty());
		assertEquals(dateNaissance, jean.getDateNaissance());
	}

	/**
	 * Teste que la correction d'une date de naissance sur un contribuable qui provoque une changement de l'année de majorité provoque bien
	 * une erreur si le contribuable possède des déclarations.
	 */
	@Test
	public void testHandleErreurAnneeDeMajoriteDifferente() throws Exception {

		final long noIndHuguette = 1234L;
		final RegDate ancienneDateNaissance = date(1991, 4, 27);
		final RegDate ancienneDateMajorite = ancienneDateNaissance.addYears(FiscalDateHelper.AGE_MAJORITE);
		final RegDate dateNaissance = date(1990, 4, 27);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndHuguette, dateNaissance, "Huguette", "Marcot", true);
			}
		});

		// Crée un individu dans la base, avec un for principal déjà ouvert à son ancienne date de majorité
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PersonnePhysique huguette = addHabitant(noIndHuguette);
				addForPrincipal(huguette, ancienneDateMajorite, MotifFor.MAJORITE, MockCommune.Lausanne);

				final PeriodeFiscale periode = addPeriodeFiscale(2008);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				addDeclarationImpot(huguette, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);

				return null;
			}
		});

		try {
			// Déclenchement de l'événement
			doInNewTransaction(new TxCallback<Object>() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {

					final Individu individu = serviceCivil.getIndividu(noIndHuguette, 2008);
					CorrectionDateNaissance correctionDateNaissane = createValidCorrectionDateNaissane(individu, dateNaissance);

					final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
					final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

					correctionDateNaissane.checkCompleteness(erreurs, warnings);
					assertEmpty(erreurs);
					correctionDateNaissane.validate(erreurs, warnings);
					assertEmpty(erreurs);
					correctionDateNaissane.handle(warnings);
					return null;
				}
			});
			fail("Le changement d'année de la date de majorité aurait dû lever une exception.");
		}
		catch (EvenementCivilException e) {
			assertEquals(
					"L'ancienne (27.04.2009) et la nouvelle date de majorité (27.04.2008) ne tombent pas sur la même année. Veuillez vérifier les DIs.",
					e.getMessage());
		}
	}
}
