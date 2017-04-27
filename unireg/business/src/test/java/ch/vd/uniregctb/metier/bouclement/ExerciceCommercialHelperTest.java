package ch.vd.uniregctb.metier.bouclement;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;

public class ExerciceCommercialHelperTest extends BusinessTest {

	private ExerciceCommercialHelper helper;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		helper = new ExerciceCommercialHelper(tiersService);
	}

	@Test
	public void testExercicesExposablesSansFors() throws Exception {

		final RegDate dateDebut = date(2009, 4, 1);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// rien
			}
		});

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Toto & cie");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// test lui-même
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				final List<ExerciceCommercial> exercicesBruts = tiersService.getExercicesCommerciaux(entreprise);
				Assert.assertFalse(exercicesBruts.isEmpty());
				final List<ExerciceCommercial> exercicesExposables = helper.getExercicesCommerciauxExposables(entreprise);
				Assert.assertTrue(exercicesExposables.isEmpty());
			}
		});
	}

	@Test
	public void testExercicesExposablesSansForsIBC() throws Exception {

		final RegDate dateDebut = date(2009, 4, 1);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// rien
			}
		});

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Toto & cie");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SNC);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne, GenreImpot.REVENU_FORTUNE);
			return entreprise.getNumero();
		});

		// test lui-même
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				final List<ExerciceCommercial> exercicesBruts = tiersService.getExercicesCommerciaux(entreprise);
				Assert.assertFalse(exercicesBruts.isEmpty());
				final List<ExerciceCommercial> exercicesExposables = helper.getExercicesCommerciauxExposables(entreprise);
				Assert.assertTrue(exercicesExposables.isEmpty());
			}
		});
	}

	@Test
	public void testExercicesExposablesAvecForIBC() throws Exception {

		final RegDate dateDebut = date(2009, 4, 1);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// rien
			}
		});

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Toto & cie");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne, GenreImpot.BENEFICE_CAPITAL);
			return entreprise.getNumero();
		});

		// test lui-même
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				final List<ExerciceCommercial> exercicesBruts = tiersService.getExercicesCommerciaux(entreprise);
				Assert.assertFalse(exercicesBruts.isEmpty());
				final List<ExerciceCommercial> exercicesExposables = helper.getExercicesCommerciauxExposables(entreprise);
				Assert.assertEquals(exercicesBruts, exercicesExposables);
			}
		});
	}
}