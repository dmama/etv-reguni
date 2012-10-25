package ch.vd.uniregctb.norentes.civil.annulation.reconciliation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPErreur;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement annulation de réconciliation avec un habitant célibataire (cas d'erreur).
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_7001_03_AnnulationReconciliation_Celibataire_Scenario extends AbstractAnnulationReconciliationScenario {

	public static final String NAME = "7001_03_AnnulationReconciliation";

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de réconciliation avec un habitant célibataire (cas d'erreur).";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndJulie = 6789;
	private final RegDate dateDebut = RegDate.get(1995, 4, 19);
	private final RegDate dateFictive = RegDate.get(2008, 1, 1);
	private final MockCommune commune = MockCommune.Lausanne;

	private long noHabJulie;

	@Etape(id=1, descr="Chargement de l'habitant")
	public void step1() {
		// Julie
		PersonnePhysique julie = addHabitant(noIndJulie);
		noHabJulie = julie.getNumero();
		ForFiscalPrincipal ffp = addForFiscalPrincipal(julie, commune, dateDebut, null, MotifFor.ARRIVEE_HC, null);
		ffp.setModeImposition(ModeImposition.SOURCE);
	}

	@Check(id=1, descr="Vérifie que l'habitant Julie a bien un for ouvert")
	public void check1() {
		{
			PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabJulie);
			ForFiscalPrincipal ffp = pierre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " inexistant");
			assertEquals(dateDebut, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.SOURCE, ffp.getModeImposition(), "Le mode d'imposition n'est pas SOURCE");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomMajuscule());
		}
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation de Réconciliation")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_RECONCILIATION, noIndJulie, dateFictive, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que l'événement civil est en erreur")
	public void check2() {
		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabJulie);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(), "L'événement d'annulation de réconciliation devrait être en erreur car l'habitant n'est pas marié");
		assertEquals(1, evt.getErreurs().size(), "Il devrait y avoir exactement une erreur");

		final EvenementCivilRegPPErreur erreur = evt.getErreurs().iterator().next();
		assertEquals("Le tiers ménage commun n'a pu être trouvé", erreur.getMessage(), "L'erreur n'est pas la bonne");
	}
}
