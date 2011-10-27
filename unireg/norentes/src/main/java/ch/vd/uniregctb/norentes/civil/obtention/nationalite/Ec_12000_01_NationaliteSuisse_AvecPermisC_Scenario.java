package ch.vd.uniregctb.norentes.civil.obtention.nationalite;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Scénario d'un événement obtention de nationalité suisse d'un individu avec permis C.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_12000_01_NationaliteSuisse_AvecPermisC_Scenario extends EvenementCivilScenario {

	public static final String NAME = "12000_01_NationaliteSuisse";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Obtention de nationalité suisse d'un individu avec permis C";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.NATIONALITE_SUISSE;
	}

	private final long noIndJulie = 6789; // julie

	private MockIndividu indJulie;

	private long noHabJulie;

	private final RegDate dateNaissance = RegDate.get(1977, 4, 19);
	private final RegDate dateDebutSuisse = dateNaissance.addYears(22).addDays(48);
	private final RegDate dateObtentionPermis = RegDate.get(2003, 5, 9);
	private final RegDate dateObtentionNationalite = RegDate.get(2008, 11, 19);
	private final Commune commune = MockCommune.VillarsSousYens;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				super.init();

				indJulie = getIndividu(noIndJulie);
				addOrigine(indJulie, MockPays.France.getNomMinuscule());
				addNationalite(indJulie, MockPays.France, dateNaissance, null);
				addNationalite(indJulie, MockPays.Suisse, dateObtentionNationalite, null);
				addPermis(indJulie, TypePermis.ETABLISSEMENT, dateObtentionPermis, dateObtentionNationalite, false);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant")
	public void etape1() {
		PersonnePhysique julie = addHabitant(noIndJulie);
		noHabJulie = julie.getNumero();
		ForFiscalPrincipal f = addForFiscalPrincipal(julie, commune, dateDebutSuisse, dateObtentionPermis.getOneDayBefore(), MotifFor.DEBUT_EXPLOITATION, MotifFor.PERMIS_C_SUISSE);
		f.setModeImposition(ModeImposition.SOURCE);

		f = addForFiscalPrincipal(julie, commune, dateObtentionPermis, null, MotifFor.PERMIS_C_SUISSE, null);
		f.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id=1, descr="Vérifie que l'habitant possède bien un for courant avec le mode d'imposition ORDINAIRE")
	public void check1() {
		PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
		ForFiscalPrincipal ffp = julie.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'habitant " + julie.getNumero() + " null");
		assertNull(ffp.getDateFin(), "Le for principal l'habitant " + julie.getNumero() + " est fermé");
		assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
	}

	@Etape(id=2, descr="Envoi de l'événement Obtention de Nationalité Suisse")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.NATIONALITE_SUISSE, noIndJulie, dateObtentionNationalite, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des fors fiscaux & que l'événement civil est traité")
	public void check2() {
		final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabJulie);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement devrait être traité");
		assertEquals(0, evt.getErreurs().size(), "Il ne devrait pas y avoir aucune erreur");

		check1();
	}
}
