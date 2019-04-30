package ch.vd.unireg.metier;

import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.BusinessTestingConstants;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TarifImpotSource;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests des méthodes du service métier avec le vrai service de tâches (et pas un mock).
 */
@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES
})
public class MetierServiceWithTachesTest extends BusinessTest {

	private MetierService metierService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		metierService = getBean(MetierService.class, "metierService");

		setWantSynchroTache(true);
	}

	/**
	 * [SIFISC-28465] Ce test vérifie que l'annulation d'une reprise de vie commune (= annulation du ménage) ne provoque pas d'optimistic exception lorsque :
	 * <ul>
	 * <li>le ménage possède un for fiscal secondaire</li>
	 * <li>un des deux membres avait déménagé avant le remariage (motif ouverture = déménagement VD)</li>
	 * </ul>
	 * ... ce qui provoque l'émission d'un tâche de contrôle de dossier.
	 */
	@Test
	public void testAnnulerMariageCoupleAvecForPrincipalEtForSecondaire() throws Exception {

		final int noIndMadame = 254000;
		final int noIndMonsieur = 254001;
		final RegDate datePremierMariage = RegDate.get(2008, 1, 1);
		final RegDate dateAchatImmeuble = RegDate.get(2010, 1, 1);
		final RegDate dateDivorce = RegDate.get(2013, 1, 1);
		final RegDate dateDemenagement = RegDate.get(2014, 1, 1);
		final RegDate dateSecondMariage = RegDate.get(2017, 1, 1);
		final MotifFor motifMariage = MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION;
		final MotifFor motifDivorce = MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT;

		doInNewTransaction(status -> {
			for (int i = 2007; i < RegDate.get().year(); i++) {
				addPeriodeFiscale(i);
			}
			return null;
		});

		class Ids {
			Long monsieur;
			Long madame;
			Long menage;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu urs = addIndividu(noIndMonsieur, RegDate.get(1977, 11, 6), "Bontempi", "Urs", Sexe.MASCULIN);
				addNationalite(urs, MockPays.Suisse, RegDate.get(1970, 1, 1), null);
				addAdresse(urs, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, null, null);

				final MockIndividu lucette = addIndividu(noIndMadame, RegDate.get(1950, 2, 3), "Bontempi", "Lucette", Sexe.FEMININ);
				addNationalite(lucette, MockPays.Suisse, RegDate.get(1970, 1, 1), null);
				addAdresse(lucette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeLaGare, null, null, datePremierMariage.getOneDayBefore());
				addAdresse(lucette, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, datePremierMariage, dateDivorce.getOneDayBefore());
				addAdresse(lucette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeLaGare, null, dateDivorce, dateSecondMariage.getOneDayBefore());
				addAdresse(lucette, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateSecondMariage, null);

				marieIndividus(urs, lucette, dateSecondMariage);
			}
		});

		// on crée un ménage-commun dont les membres se sont séparés puis remariés
		doInNewTransaction(status -> {
			// monsieur reste à Bussigny
			final PersonnePhysique monsieur = addHabitant(noIndMonsieur);
			addForPrincipal(monsieur, RegDate.get(2007, 1, 1), MotifFor.ARRIVEE_HC, datePremierMariage.getOneDayBefore(), motifMariage, MockCommune.Bussigny);
			addForPrincipal(monsieur, dateDivorce, motifDivorce, dateSecondMariage.getOneDayBefore(), motifMariage, MockCommune.Bussigny);

			// madame bouge entre Lausanne et Bussigny
			final PersonnePhysique madame = addHabitant(noIndMadame);
			addForPrincipal(madame, RegDate.get(2007, 1, 1), MotifFor.ARRIVEE_HC, datePremierMariage.getOneDayBefore(), motifMariage, MockCommune.Lausanne);
			addForPrincipal(madame, dateDivorce, motifDivorce, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny);
			addForPrincipal(madame, dateDemenagement, MotifFor.DEMENAGEMENT_VD, dateSecondMariage.getOneDayBefore(), motifMariage, MockCommune.Lausanne);
			addForSecondaire(madame, dateDivorce, motifDivorce, dateSecondMariage.getOneDayBefore(), motifMariage, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);

			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, datePremierMariage, dateDivorce.getOneDayBefore());
			final MenageCommun menage = ensemble.getMenage();
			addAppartenanceMenage(menage, monsieur, dateSecondMariage, null, false);
			addAppartenanceMenage(menage, madame, dateSecondMariage, null, false);
			addForPrincipal(menage, datePremierMariage, motifMariage, dateDivorce.getOneDayBefore(), motifDivorce, MockCommune.Bussigny);
			addForSecondaire(menage, dateAchatImmeuble, MotifFor.ACHAT_IMMOBILIER, dateDivorce.getOneDayBefore(), motifDivorce, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);
			addForPrincipal(menage, dateSecondMariage, motifMariage, MockCommune.Bussigny);
			addForSecondaire(menage, dateSecondMariage, motifMariage, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);

			final SituationFamilleMenageCommun situation = addSituation(menage, dateSecondMariage, null, 0, TarifImpotSource.NORMAL, EtatCivil.MARIE);
			situation.setContribuablePrincipalId(monsieur.getId());

			ids.monsieur = monsieur.getId();
			ids.madame = madame.getId();
			ids.menage = menage.getId();
			return null;
		});

		doInNewTransaction(status -> {

			final PersonnePhysique monsieur = (PersonnePhysique) tiersDAO.get(ids.monsieur);
			assertNotNull(monsieur);
			final PersonnePhysique madame = (PersonnePhysique) tiersDAO.get(ids.madame);
			assertNotNull(madame);

			// on annule le second mariage
			metierService.annuleMariage(monsieur, madame, dateSecondMariage, null);

			// les fors fiscaux de monsieur et madame doivent être réouverts
			final ForFiscalPrincipalPP ffpMonsieur = monsieur.getDernierForFiscalPrincipal();
			assertNotNull(ffpMonsieur);
			assertEquals(dateDivorce, ffpMonsieur.getDateDebut());
			assertNull(ffpMonsieur.getDateFin());

			final List<ForFiscal> forsMadame = madame.getForsFiscauxValidAt(null);
			assertNotNull(forsMadame);
			assertEquals(2, forsMadame.size());
			forsMadame.sort(Comparator.comparing(ForFiscal::getId));

			final ForFiscalPrincipalPP ffpMadame = (ForFiscalPrincipalPP) forsMadame.get(0);
			assertNotNull(ffpMadame);
			assertEquals(dateDemenagement, ffpMadame.getDateDebut());
			assertNull(ffpMadame.getDateFin());

			final ForFiscalSecondaire ffsMadame = (ForFiscalSecondaire) forsMadame.get(1);
			assertNotNull(ffsMadame);
			assertEquals(dateDivorce, ffsMadame.getDateDebut());
			assertNull(ffsMadame.getDateFin());

			// les rapports d'appartenance ménage doivent être réouverts
			final RapportEntreTiers appartenanceMonsieur = monsieur.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
			assertNotNull(appartenanceMonsieur);
			assertEquals(datePremierMariage, appartenanceMonsieur.getDateDebut());
			assertEquals(dateDivorce.getOneDayBefore(), appartenanceMonsieur.getDateFin());

			final RapportEntreTiers appartenanceMadame = madame.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
			assertNotNull(appartenanceMadame);
			assertEquals(datePremierMariage, appartenanceMadame.getDateDebut());
			assertEquals(dateDivorce.getOneDayBefore(), appartenanceMadame.getDateFin());

			return null;
		});
	}
}
