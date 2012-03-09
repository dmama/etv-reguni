package ch.vd.uniregctb.norentes.civil.annulation.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement annulation de mariage d'une personne mariée seule.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4001_02_AnnulationMariage_MarieSeul_Scenario extends EvenementCivilScenario {

	public static final String NAME = "4001_02_AnnulationMariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de mariage d'une personne mariée seule.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private DefaultMockServiceCivil localMockServiceCivil;

	@Override
	protected void initServiceCivil() {
		localMockServiceCivil = new DefaultMockServiceCivil();
		serviceCivilService.setUp(localMockServiceCivil);
	}

	private static final long noIndPierre = 12345; // Pierre

	private long noHabPierre;
	private long noMenage;

	private final RegDate dateDebutSuisse = RegDate.get(1980, 3, 1);
	private final RegDate dateMariage = RegDate.get(2005, 4, 8);
	private final MockCommune commune = MockCommune.Lausanne;

	@Etape(id=1, descr="Chargement de l'habitant marié seul")
	public void step1() {
		// Pierre
		PersonnePhysique pierre = addHabitant(noIndPierre);
		noHabPierre = pierre.getNumero();

		ForFiscalPrincipal f = addForFiscalPrincipal(pierre, commune, dateDebutSuisse, dateMariage.getOneDayBefore(), MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		f.setModeImposition(ModeImposition.SOURCE);

		// Ménage
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, pierre, dateMariage, null);

		f = addForFiscalPrincipal(menage, commune, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
		f.setModeImposition(ModeImposition.SOURCE);
	}

	@Check(id=1, descr="Vérifie que l'habitant Pierre n'a aucun For fiscal principal ouvert et que le ménage a un For fiscal principal ouvert")
	public void check1() {
		{
			PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			ForFiscalPrincipal ffp = pierre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " null");
			assertNotNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.SOURCE, ffp.getModeImposition(), "Le mode d'imposition n'est pas SOURCE");
		}
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.SOURCE, ffp.getModeImposition(), "Le mode d'imposition n'est pas SOURCE");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomMajuscule());
		}
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation de Mariage")
	public void step2() throws Exception {
		// annulation dans le civil
		MockServiceCivil.annuleMariage(localMockServiceCivil.getIndividu(noIndPierre));
		// envoi de l'événement
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_MARIAGE, noIndPierre, dateMariage, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le For principal du ménage a été fermé et celui de Pierre rouvert")
	public void check2() {
		{
			PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			ForFiscalPrincipal ffp = pierre.getForFiscalPrincipalAt(null);
			assertNotNull(ffp, "Pierre doit avoir un for principal actif après l'annulation de mariage");
			assertEquals(dateDebutSuisse, ffp.getDateDebut(), "Le for de l'habitant " + pierre.getNumero() + " devrait commencer le " + dateDebutSuisse);
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + pierre.getNumero() + " est fermé");
			assertEquals(MotifFor.ARRIVEE_HC, ffp.getMotifOuverture(), "Le motif de fermeture n'est pas ARRIVEE_HC");
			assertNull(ffp.getMotifFermeture(), "Le motif de fermeture devrait être null");
			// Vérification des fors fiscaux
			for (ForFiscal forFiscal : pierre.getForsFiscaux()) {
				if (forFiscal.getDateFin() != null && dateMariage.getOneDayBefore().equals(forFiscal.getDateFin()) &&
						(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
					assertEquals(true, forFiscal.isAnnule(), "Les fors fiscaux fermés lors du mariage doivent être annulés");
				}
			}
		}
	}

}
