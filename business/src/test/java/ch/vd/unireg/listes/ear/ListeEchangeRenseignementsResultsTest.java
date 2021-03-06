package ch.vd.unireg.listes.ear;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;

public class ListeEchangeRenseignementsResultsTest extends BusinessTest

{

	private final RegDate dateTraitement = RegDate.get();
	private final int anneeEar = 2017;

	private AdresseService adresseService;
	private TiersService tiersService;
	private AssujettissementService assujettissementService;

	private ListeEchangeRenseignementsResults results;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		adresseService = getBean(AdresseService.class, "adresseService");
		tiersService = getBean(TiersService.class, "tiersService");
		assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		results = new ListeEchangeRenseignementsResults(dateTraitement, 1, anneeEar, true, true, tiersService, assujettissementService, adresseService);

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void determineCauseIgnorance() throws Exception {

		//Ctb non assujetti

		{
			final PersonnePhysique armand = addNonHabitant("Armand", "tutu", date(1987, 12, 12), Sexe.MASCULIN);
			final ForFiscalPrincipalPP ffsRenens = addForPrincipal(armand, date(2005, 12, 12), MotifFor.MAJORITE, date(2016, 3, 1), MotifFor.DEPART_HC, MockCommune.Renens);
			Assert.assertEquals(ListeEchangeRenseignementsResults.CauseIgnorance.NON_ASSUJETTI, results.determineCauseIgnorance(armand));

		}
		//Ctb assujetti avec absence de for vaudois à la fin de l'assujettissement

		{
			final PersonnePhysique armand = addNonHabitant("Armand", "tutu", date(1987, 12, 12), Sexe.MASCULIN);
			addForPrincipal(armand, date(2005, 12, 12), MotifFor.ARRIVEE_HS, MockCommune.Bern);
			addForSecondaire(armand, date(2015, 5, 5), MotifFor.ACHAT_IMMOBILIER, date(2017, 5, 30), MotifFor.VENTE_IMMOBILIER, MockCommune.Aigle, MotifRattachement.IMMEUBLE_PRIVE);
			Assert.assertEquals(ListeEchangeRenseignementsResults.CauseIgnorance.NON_ASSUJETTI_VAUDOIS, results.determineCauseIgnorance(armand));

		}
		//Notre chère armand a toujours sa maison sur vaud
			// Hors-canton avec une maison sur vaud

		{
			final PersonnePhysique armand = addNonHabitant("Armand", "tutu", date(1987, 12, 12), Sexe.MASCULIN);
			addForPrincipal(armand, date(2005, 12, 12), MotifFor.ARRIVEE_HS, MockCommune.Bern);
			addForSecondaire(armand, date(2015, 5, 5), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aigle, MotifRattachement.IMMEUBLE_PRIVE);
			Assert.assertEquals(ListeEchangeRenseignementsResults.CauseIgnorance.NON_ASSUJETTI_VAUDOIS,results.determineCauseIgnorance(armand));

		}

		//Sourcier sans rapport de travail sur la PF
		{
			final PersonnePhysique armand = addNonHabitant("Armand", "tutu", date(1987, 12, 12), Sexe.MASCULIN);
			addForPrincipal(armand, date(2005, 12, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);

			Assert.assertEquals(ListeEchangeRenseignementsResults.CauseIgnorance.ABSENCE_RAPPORT_TRAVAIL_SOURCIER, results.determineCauseIgnorance(armand));

		}

		//Sourcier sans adresse sur vaud  sur la PF
		{
			final PersonnePhysique armand = addNonHabitant("Armand", "tutu", date(1987, 12, 12), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur();
			addForPrincipal(armand, date(2005, 12, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
			addRapportPrestationImposable(dpi, armand, date(2015, 1, 1), null, false);
			//Il est pris en comp
			Assert.assertEquals(ListeEchangeRenseignementsResults.CauseIgnorance.ABSENCE_DOMICILE_VAUDOIS, results.determineCauseIgnorance(armand));

		}


		//Sourcier ignoré car avec juste une adresse courrier
		{
			final PersonnePhysique armand = addNonHabitant("Armand", "tutu", date(1987, 12, 12), Sexe.MASCULIN);
			addAdresseSuisse(armand, TypeAdresseTiers.COURRIER, date(1987, 12, 12), null, MockRue.Lausanne.AvenueDeBeaulieu);
			final DebiteurPrestationImposable dpi = addDebiteur();
			addForPrincipal(armand, date(2005, 12, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
			addRapportPrestationImposable(dpi, armand, date(2015, 1, 1), null, false);
			//Il est pris en comp
			Assert.assertEquals(ListeEchangeRenseignementsResults.CauseIgnorance.ABSENCE_DOMICILE_VAUDOIS, results.determineCauseIgnorance(armand));

		}

		//Sourcier non ignoré car répondant à tout les critères
		{
			final long noIndividu = 1254732L;
			final RegDate dateNaissance = date(1987, 12, 12);
			final PersonnePhysique armand = addHabitant(noIndividu);


			// mise en place civile
			serviceCivil.setUp(new MockIndividuConnector() {
				@Override
				protected void init() {
					final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Armand", "Tutu", Sexe.MASCULIN);
					addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateNaissance, null);
				}
			});

			final DebiteurPrestationImposable dpi = addDebiteur();
			addForPrincipal(armand, date(2005, 12, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
			addRapportPrestationImposable(dpi, armand, date(2015, 1, 1), null, false);
			//Il est pris en compte
			Assert.assertNull(results.determineCauseIgnorance(armand));

		}
			// Ménage commun HC avec un immeuble
			{
				final PersonnePhysique armand = addNonHabitant("Armand", "tutu", date(1987, 12, 12), Sexe.MASCULIN);
				final PersonnePhysique armande = addNonHabitant("Armande", "tutu", date(1987, 12, 12), Sexe.FEMININ);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(armand, armande, date(2005, 1, 1), null);
				final MenageCommun menage = ensemble.getMenage();

				addAdresseSuisse(menage, TypeAdresseTiers.COURRIER, date(1987, 12, 12), null, MockRue.Lausanne.AvenueDeBeaulieu);
				addForPrincipal(menage, date(2005, 1, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Geneve, ModeImposition.ORDINAIRE);
				addForSecondaire(menage, date(2005, 1, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);

	Assert.assertEquals(ListeEchangeRenseignementsResults.CauseIgnorance.NON_ASSUJETTI_VAUDOIS, results.determineCauseIgnorance(menage));
			}

		// Sourcière assujetti hors suisse SIFISC-29785
		{

			final long noIndividu = 1254732L;
			final RegDate dateNaissance = date(1987, 12, 12);
			final PersonnePhysique armand = addHabitant(noIndividu);
			final RegDate dateDebut = date(2014, 12, 16);
			final RegDate dateFin = date(2017, 8, 5);

			// mise en place civile
			serviceCivil.setUp(new MockIndividuConnector() {
				@Override
				protected void init() {
					final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Armande", "Tutu", Sexe.FEMININ);
					addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateDebut, dateFin);
				}
			});

			final PersonnePhysique armande = addHabitant(noIndividu);

			addForPrincipal(armande, dateDebut, MotifFor.ARRIVEE_HS,
					dateFin,MotifFor.DEPART_HS,MockCommune.Lausanne, ModeImposition.SOURCE);
			addForPrincipal(armande, date(2017, 8, 6), MotifFor.DEPART_HS,
					date(2018, 6, 19),MotifFor.ARRIVEE_HS, MockPays.Maroc, ModeImposition.SOURCE);
			addForPrincipal(armande, date(2018, 6, 20), MotifFor.ARRIVEE_HS,
					MockCommune.Lausanne, ModeImposition.SOURCE);
			final DebiteurPrestationImposable dpi = addDebiteur();
			addRapportPrestationImposable(dpi, armande, date(2017, 2, 1), date(2017, 10, 31), false);
			addRapportPrestationImposable(dpi, armande, date(2018, 7, 9), date(2018, 8, 31), false);
			Assert.assertNull(results.determineCauseIgnorance(armande));
		}

		//Sourcier Mixte 1  et sans rapport de travail doit être pris en compte sur la PF
		{
			final PersonnePhysique armand = addNonHabitant("Armand", "tutu", date(1987, 12, 12), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur();
			addForPrincipal(armand, date(2005, 12, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
			//Il est pris en compte
			Assert.assertNull(results.determineCauseIgnorance(armand));

		}

		//Sourcier Mixte 2  et sans rapport de travail doit être pris en compte sur la PF
		{
			final PersonnePhysique armand = addNonHabitant("Armand", "tutu", date(1987, 12, 12), Sexe.MASCULIN);
			final DebiteurPrestationImposable dpi = addDebiteur();
			addForPrincipal(armand, date(2005, 12, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
			//Il est pris en compte
			Assert.assertNull(results.determineCauseIgnorance(armand));

		}

	}
}