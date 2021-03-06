package ch.vd.unireg.norentes.civil.obtention.nationalite;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

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

	private static final long noIndJulie = 6789; // julie

	private MockIndividu indJulie;

	private long noHabJulie;

	private final RegDate dateNaissance = RegDate.get(1977, 4, 19);
	private final RegDate dateDebutSuisse = dateNaissance.addYears(22).addDays(48);
	private final RegDate dateObtentionPermis = RegDate.get(2003, 5, 9);
	private final RegDate dateObtentionNationalite = RegDate.get(2008, 11, 19);
	private final Commune commune = MockCommune.VillarsSousYens;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				super.init();

				indJulie = getIndividu(noIndJulie);
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
		addForFiscalPrincipal(julie, commune, dateDebutSuisse, dateObtentionPermis.getOneDayBefore(), MotifFor.DEBUT_EXPLOITATION, MotifFor.PERMIS_C_SUISSE, ModeImposition.SOURCE);
		addForFiscalPrincipal(julie, commune, dateObtentionPermis, null, MotifFor.PERMIS_C_SUISSE, null);
	}

	@Check(id=1, descr="Vérifie que l'habitant possède bien un for courant avec le mode d'imposition ORDINAIRE")
	public void check1() {
		PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
		ForFiscalPrincipalPP ffp = julie.getDernierForFiscalPrincipal();
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
		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabJulie);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement devrait être traité");
		assertEquals(0, evt.getErreurs().size(), "Il ne devrait pas y avoir aucune erreur");

		check1();
	}
}
