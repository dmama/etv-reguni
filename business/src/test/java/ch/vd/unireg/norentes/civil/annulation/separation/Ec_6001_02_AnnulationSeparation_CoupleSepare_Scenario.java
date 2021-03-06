package ch.vd.unireg.norentes.civil.annulation.separation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEvenementCivil;

/**
 * Scénario d'un événement annulation de séparation avec un couple de mariés.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_6001_02_AnnulationSeparation_CoupleSepare_Scenario extends AbstractAnnulationSeparationScenario {

	public static final String NAME = "6001_02_AnnulationSeparation";

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de séparation avec un couple de mariés.";
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

	private static final long noIndMomo = 54321; // Maurice
	private static final long noIndBea = 23456; // Béatrice

	private long noHabMomo;
	private long noHabBea;
	private long noMenage;

	private void assertBlocageRemboursementAutomatique(boolean flagMomo, boolean flagBea, boolean flagMenage) {
		assertBlocageRemboursementAutomatique(flagMomo, tiersDAO.get(noHabMomo));
		assertBlocageRemboursementAutomatique(flagBea, tiersDAO.get(noHabBea));
		assertBlocageRemboursementAutomatique(flagMenage, tiersDAO.get(noMenage));
	}

	@Etape(id=1, descr="Chargement du couple Maurice-Béatrice")
	public void step1() {
		// Maurice
		PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();
		addForFiscalPrincipal(momo, commune, dateDebutMomo, dateMariage.getOneDayBefore(), MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		addForFiscalPrincipal(momo, commune, dateSeparation, null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null);
		momo.setBlocageRemboursementAutomatique(false);

		// Béatrice
		PersonnePhysique bea = addHabitant(noIndBea);
		noHabBea = bea.getNumero();
		addForFiscalPrincipal(bea, commune, dateDebutBea, dateMariage.getOneDayBefore(), MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		addForFiscalPrincipal(bea, commune, dateSeparation, null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null);
		bea.setBlocageRemboursementAutomatique(false);

		// Ménage commun
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, momo, dateMariage, dateSeparation.getOneDayBefore());
		tiersService.addTiersToCouple(menage, bea, dateMariage, dateSeparation.getOneDayBefore());
		addForFiscalPrincipal(menage, commune, dateMariage, dateSeparation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
		                      MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
	}

	@Check(id=1, descr="Vérifie que les contribuables ont leur For principal ouvert et celui du ménage est fermé")
	public void check1() {
		{
			PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			ForFiscalPrincipalPP ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " n'a pas pu être trouvé");
			assertEquals(dateSeparation, ffp.getDateDebut(), "Le dernier for n'est pas le bon");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture(), "Le motif d'ouverture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
		{
			PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			ForFiscalPrincipalPP ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " n'a pas pu être trouvé");
			assertEquals(dateSeparation, ffp.getDateDebut(), "Le dernier for n'est pas le bon");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture(), "Le motif d'ouverture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage doit avoir un for fiscal");
			ForFiscalPrincipalPP ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " n'a pas pu être trouvé");
			assertEquals(dateMariage, ffp.getDateDebut(), "Le dernier for n'est pas le bon");
			assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture(), "Le motif d'ouverture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifFermeture(), "Le motif d'ouverture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}

		assertBlocageRemboursementAutomatique(false, false, true);
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation de Séparation")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_SEPARATION, noIndBea, dateSeparation, commune.getNoOFS());
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
			assertNull(ffp.getDateFin(), "Le dernier for trouvé n'est pas le bon");
			for (ForFiscal forFiscal : mc.getForsFiscaux()) {
				// recherche des fors fermés avec date de fin égal à celle de la séparation
				// ces fors doivent être annulés
				if (forFiscal.getDateFin() != null && dateSeparation.equals(forFiscal.getDateFin())) {
					assertEquals(true, forFiscal.isAnnule(), "Les fors fiscaux créés lors de la séparation doivent être annulés");
				}
			}
		}
		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabMomo), dateSeparation);
		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabBea), dateSeparation);

		assertBlocageRemboursementAutomatique(true, true, false);
	}
}
