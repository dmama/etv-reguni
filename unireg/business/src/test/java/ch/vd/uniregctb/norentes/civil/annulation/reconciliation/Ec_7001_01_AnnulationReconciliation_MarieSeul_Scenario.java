package ch.vd.uniregctb.norentes.civil.annulation.reconciliation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement annulation de réconciliation avec un habitant marié seul.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_7001_01_AnnulationReconciliation_MarieSeul_Scenario extends AbstractAnnulationReconciliationScenario  {

	public static final String NAME = "7001_01_AnnulationReconciliation";

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de réconciliation avec un habitant marié seul.";
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
	private final RegDate dateSeparation = RegDate.get(2005, 9, 12);
	private final RegDate dateReconciliation = RegDate.get(2005, 11, 3);
	private final MockCommune commune = MockCommune.Lausanne;

	@Etape(id=1, descr="Chargement de l'habitant marié seul")
	public void step1() {
		// Pierre
		PersonnePhysique pierre = addHabitant(noIndPierre);
		noHabPierre = pierre.getNumero();

		ForFiscalPrincipal ffp = addForFiscalPrincipal(pierre, commune, dateDebutSuisse, dateMariage.getOneDayBefore(), MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp.setModeImposition(ModeImposition.SOURCE);

		ffp = addForFiscalPrincipal(pierre, commune, dateSeparation, dateReconciliation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp.setModeImposition(ModeImposition.SOURCE);

		// Ménage
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, pierre, dateMariage, dateSeparation.getOneDayBefore());
		tiersService.addTiersToCouple(menage, pierre, dateReconciliation, null);

		ffp = addForFiscalPrincipal(menage, commune, dateMariage, dateSeparation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
		ffp.setModeImposition(ModeImposition.SOURCE);

		ffp = addForFiscalPrincipal(menage, commune, dateReconciliation, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
		ffp.setModeImposition(ModeImposition.SOURCE);
	}

	@Check(id=1, descr="Vérifie que l'habitant Pierre n'a aucun For fiscal principal ouvert et que le ménage a un For fiscal principal ouvert")
	public void check1() {
		{
			PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			ForFiscalPrincipal ffp = pierre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " inexistant");
			assertEquals(dateSeparation, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(dateReconciliation.getOneDayBefore(), ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.SOURCE, ffp.getModeImposition(), "Le mode d'imposition n'est pas SOURCE");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomMajuscule());
		}
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " inexistant");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.SOURCE, ffp.getModeImposition(), "Le mode d'imposition n'est pas SOURCE");
		}
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation de Réconciliation")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_RECONCILIATION, noIndPierre, dateReconciliation, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le dernier For principal du ménage a été annulé et celui de Pierre rouvert")
	public void check2() {
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le for du ménage Pierre doit être fermé");
			assertEquals(dateMariage, ffp.getDateDebut(), "Le dernier for trouvé n'est pas le bon");
			assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin(), "Le dernier for trouvé n'est pas le bon");
			for (ForFiscal forFiscal : mc.getForsFiscaux()) {
				if (forFiscal.getDateFin() == null && dateReconciliation.equals(forFiscal.getDateDebut())) {
					assertEquals(true, forFiscal.isAnnule(), "Les fors fiscaux créés lors de la réconciliation doivent être annulés");
				}
			}
		}
		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabPierre), dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateReconciliation);
	}
}
