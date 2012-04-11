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
 * Scénario d'un événement annulation de réconciliation avec un couple réconcilié.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_7001_02_AnnulationReconciliation_CoupleReconcilie_Scenario extends AbstractAnnulationReconciliationScenario {

	public static final String NAME = "7001_02_AnnulationReconciliation";

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de réconciliation avec un couple réconcilié.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final MockCommune commune = MockCommune.Lausanne;
	private final RegDate dateDebutMomo = RegDate.get(1977, 1, 4);
	private final RegDate dateDebutBea = RegDate.get(1981, 8, 20);
	private final RegDate dateMariage = RegDate.get(1986, 4, 8);
	private final RegDate dateSeparation = RegDate.get(2004, 3, 2);
	private final RegDate dateReconciliation = RegDate.get(2005, 7, 14);

	private static final long noIndMomo = 54321; // Maurice
	private static final long noIndBea = 23456; // Béatrice

	private long noHabMomo;
	private long noHabBea;
	private long noMenage;

	@Etape(id=1, descr="Chargement du couple")
	public void step1() {
		// Maurice
		PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();
		ForFiscalPrincipal ffp = addForFiscalPrincipal(momo, commune, dateDebutMomo, dateMariage.getOneDayBefore(), MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp.setModeImposition(ModeImposition.ORDINAIRE);

		ffp = addForFiscalPrincipal(momo, commune, dateSeparation, dateReconciliation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp.setModeImposition(ModeImposition.ORDINAIRE);

		// Béatrice
		PersonnePhysique bea = addHabitant(noIndBea);
		noHabBea = bea.getNumero();
		ffp = addForFiscalPrincipal(bea, commune, dateDebutBea, dateMariage.getOneDayBefore(), MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp.setModeImposition(ModeImposition.ORDINAIRE);

		ffp = addForFiscalPrincipal(bea, commune, dateSeparation, dateReconciliation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp.setModeImposition(ModeImposition.ORDINAIRE);

		// Ménage commun
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, momo, dateMariage, dateSeparation.getOneDayBefore());
		tiersService.addTiersToCouple(menage, bea, dateMariage, dateSeparation.getOneDayBefore());
		tiersService.addTiersToCouple(menage, momo, dateReconciliation, null);
		tiersService.addTiersToCouple(menage, bea, dateReconciliation, null);

		ffp = addForFiscalPrincipal(menage, commune, dateMariage, dateSeparation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
		ffp.setModeImposition(ModeImposition.ORDINAIRE);

		ffp = addForFiscalPrincipal(menage, commune, dateReconciliation, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
		ffp.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id=1, descr="Vérifie que les habitants ont chacun un For fermé et le For du ménage est ouvert")
	public void check1() {
		{
			PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " null");
			assertNotNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
		{
			PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " null");
			assertNotNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(2, mc.getForsFiscaux().size(), "Le ménage doit avoir deux fors fiscaux");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateReconciliation, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Le dernier for doit être ouvert");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomMajuscule());
		}
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation de Réconciliation")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_RECONCILIATION, noIndMomo, dateReconciliation, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le dernier For principal du ménage a été annulé et ceux de tiers rouverts")
	public void check2() {
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(2, mc.getForsFiscaux().size(), "Le ménage doit avoir deux fors fiscaux");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le for du ménage Maurice/Béatrice doit être fermé");
			assertEquals(dateMariage, ffp.getDateDebut(), "Le dernier for trouvé n'est pas le bon");
			assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin(), "Le dernier for trouvé n'est pas le bon");
			for (ForFiscal forFiscal : mc.getForsFiscaux()) {
				if (forFiscal.getDateFin() == null && dateReconciliation.equals(forFiscal.getDateDebut())) {
					assertEquals(true, forFiscal.isAnnule(), "Les fors fiscaux créés lors de la réconciliation doivent être annulés");
				}
			}
		}
		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabMomo), dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateReconciliation);
		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabBea), dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateReconciliation);
	}
}
