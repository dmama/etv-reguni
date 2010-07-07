package ch.vd.uniregctb.norentes.civil.annulation.veuvage;

import annotation.Check;
import annotation.Etape;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement annulation de veuvage d'une personne non veuve.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_10001_02_AnnulationVeuvage_NonMarie_Scenario extends EvenementCivilScenario {

	public static final String NAME = "10001_02_AnnulationVeuvage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_VEUVAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de veuvage d'une personne non veuve (cas d'erreur).";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndMomo = 54321; // momo
	private final long noIndBea = 23456; // bea

	private long noHabMomo;
	private long noHabBea;
	private long noMenage;

	private final RegDate dateNaissanceBea = RegDate.get(1963, 8, 20);
	private final RegDate dateMajorite = dateNaissanceBea.addYears(18);
	private final RegDate dateArriveeVillars = RegDate.get(1974, 3, 3);
	private final RegDate dateMariage = RegDate.get(1986, 4, 27);
	private final RegDate dateAvantMariage = dateMariage.getOneDayBefore();
	private final RegDate dateVeuvage = RegDate.get(2008, 1, 1);
	private final Commune commune = MockCommune.VillarsSousYens;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new DefaultMockServiceCivil());
	}

	@Etape(id=1, descr="Chargement de l'habitant et son conjoint")
	public void step1() {
		// momo
		PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();
		ForFiscalPrincipal f = addForFiscalPrincipal(momo, MockCommune.VillarsSousYens, dateArriveeVillars, dateAvantMariage, MotifFor.ARRIVEE_HS, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		f.setModeImposition(ModeImposition.SOURCE);
		// bea
		PersonnePhysique bea = addHabitant(noIndBea);
		noHabBea = bea.getNumero();
		addForFiscalPrincipal(bea, MockCommune.Lausanne, dateMajorite, dateAvantMariage, MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		// ménage
		MenageCommun menage = new MenageCommun();
		menage = (MenageCommun) tiersDAO.save(menage);
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, momo, dateMariage, null);
		tiersService.addTiersToCouple(menage, bea, dateMariage, null);
		f = addForFiscalPrincipal(menage, commune, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
		f.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id=1, descr="Vérifie que l'habitant Maurice est marié a Béatrice et le For du menage existe")
	public void check1() {
		{
			PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " null");
			assertEquals(dateAvantMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}
		{
			PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " null");
			assertEquals(dateAvantMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomMajuscule());
		}
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation Veuvage")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_VEUVAGE, noIndMomo, dateVeuvage, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérifie que l'événement civil est en erreur")
	public void check2() {
		final EvenementCivilData evt = getEvenementCivilRegoupeForHabitant(noHabMomo);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(), "L'événement d'annulation de veuvage devrait être en erreur car l'individu n'est pas veuf");
		assertEquals(1, evt.getErreurs().size(), "Il devrait y avoir exactement une erreur");

		final EvenementCivilErreur erreur = evt.getErreurs().iterator().next();
		assertEquals("L'événement d'annulation veuvage ne peut pas s'appliquer à une personne mariée.", erreur.getMessage(), "L'erreur n'est pas la bonne");
	}

}
