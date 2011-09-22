package ch.vd.uniregctb.norentes.civil.annulation.mariage;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement annulation de mariage d'une personne célibataire (cas d'erreur).
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4001_01_AnnulationMariage_Celibataire_Scenario extends EvenementCivilScenario {

	public static final String NAME = "4001_01_AnnulationMariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de mariage d'une personne célibataire (cas d'erreur).";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndJulie = 6789;

	private Long noHabJulie;

	private final MockCommune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new DefaultMockServiceCivil());
	}

	@Etape(id=1, descr="Chargement de l'habitant célibataire")
	public void step1() {
		PersonnePhysique julie = addHabitant(noIndJulie);
		noHabJulie = julie.getNumero();

		ForFiscalPrincipal f = addForFiscalPrincipal(julie, commune, RegDate.get(1995, 4, 19), null, MotifFor.ARRIVEE_HC, null);
		f.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id=1, descr="Vérifie que l'habitant Julie a un For ouvert")
	public void check1() {
		PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
		ForFiscalPrincipal ffp = julie.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'habitant " + julie.getNumero() + " null");
		assertNull(ffp.getDateFin(), "Le for principal l'habitant " + julie.getNumero() + " est fermé");
		assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation de Mariage")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_MARIAGE, noIndJulie, RegDate.get(2008, 10, 24), commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que l'événement civil est en erreur")
	public void check2() {
		final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabJulie);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(), "L'événement d'annulation de mariage devrait être en erreur car le tiers n'est pas marié");
		assertEquals(1, evt.getErreurs().size(), "Il devrait y avoir exactement une erreur");

		final EvenementCivilExterneErreur erreur = evt.getErreurs().iterator().next();
		assertEquals("Le tiers ménage commun n'a pu être trouvé", erreur.getMessage(), "L'erreur n'est pas la bonne");
	}

}
