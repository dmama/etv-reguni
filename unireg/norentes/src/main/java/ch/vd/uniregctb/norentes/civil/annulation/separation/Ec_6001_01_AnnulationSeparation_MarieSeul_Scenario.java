package ch.vd.uniregctb.norentes.civil.annulation.separation;

import annotation.Check;
import annotation.Etape;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement annulation de séparation avec un habitant marié seul.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_6001_01_AnnulationSeparation_MarieSeul_Scenario extends AbstractAnnulationSeparationScenario {

	public static final String NAME = "6001_01_AnnulationSeparation";

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de séparation avec un habitant marié seul.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndPierre = 12345; // Pierre

	private long noHabPierre;
	private long noMenage;

	private final RegDate dateDebutSuisse = RegDate.get(1980, 3, 1);
	private final RegDate dateMariage = RegDate.get(1986, 4, 8);
	private final RegDate dateSeparation = RegDate.get(2006, 9, 12);
	private final MockCommune commune = MockCommune.Vevey;

	@Etape(id=1, descr="Chargement de l'habitant marié seul")
	public void step1() {
		// Pierre
		final PersonnePhysique pierre = addHabitant(noIndPierre);
		noHabPierre = pierre.getNumero();

		ForFiscalPrincipal ffp = addForFiscalPrincipal(pierre, commune, dateDebutSuisse, dateMariage.getOneDayBefore(), MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp.setModeImposition(ModeImposition.SOURCE);

		ffp = addForFiscalPrincipal(pierre, commune, dateSeparation, null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null);
		ffp.setModeImposition(ModeImposition.SOURCE);

		pierre.setBlocageRemboursementAutomatique(false);

		// Ménage
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, pierre, dateMariage, dateSeparation.getOneDayBefore());

		ffp = addForFiscalPrincipal(menage, commune, dateMariage, dateSeparation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
		ffp.setModeImposition(ModeImposition.SOURCE);
	}

	private void assertBlocageRemboursementAuto(boolean flagPierre, boolean flagMenage) {
		assertBlocageRemboursementAutomatique(flagPierre, tiersDAO.get(noHabPierre));
		assertBlocageRemboursementAutomatique(flagMenage, tiersDAO.get(noMenage));
	}

	@Check(id=1, descr="Vérifie que l'habitant Pierre n'a aucun For fiscal principal ouvert et que le ménage a un For fiscal principal ouvert")
	public void check1() {
		{
			PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			ForFiscalPrincipal ffp = pierre.getForFiscalPrincipalAt(null);
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " inexistant");
			assertEquals(dateSeparation, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.SOURCE, ffp.getModeImposition(), "Le mode d'imposition n'est pas SOURCE");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomMajuscule());
		}
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = mc.getForFiscalPrincipalAt(null);
			assertNull(ffp, "Le ménage devrait avoir son for fermé");
			ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Dernier For principal du ménage " + mc.getNumero() + " inexistant");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNotNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.SOURCE, ffp.getModeImposition(), "Le mode d'imposition n'est pas SOURCE");
		}

		assertBlocageRemboursementAuto(false, true);
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation de Séparation")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_SEPARATION, noIndPierre, dateSeparation, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le dernier For principal du ménage a été rouvert et celui de Pierre annulé")
	public void check2() {
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le for du ménage Pierre doit être fermé");
			assertEquals(dateMariage, ffp.getDateDebut(), "Le dernier for trouvé n'est pas le bon");
			assertNull(ffp.getDateFin(), "Le dernier for trouvé n'est pas le bon");
			for (ForFiscal forFiscal : mc.getForsFiscaux()) {
				// recherche des fors fermés avec date de fin égal à celle de la séparation
				// ces fors doivent être annulés
				if (forFiscal.getDateFin() != null && dateSeparation.equals(forFiscal.getDateFin())) {
					assertEquals(true, forFiscal.isAnnule(), "Les fors fiscaux créés lors de la séparation doivent être annulés");
				}
			}
		}
		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabPierre), dateSeparation);

		assertBlocageRemboursementAuto(true, false);
	}

}
