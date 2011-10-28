package ch.vd.uniregctb.norentes.civil.annulation.deces;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement d'annulation de décès d'un célibataire.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_2001_01_AnnulationDeces_Celibataire_Scenario extends EvenementCivilScenario {

	public static final String NAME = "2001_01_AnnulationDeces";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_DECES;
	}

	@Override
	public String getDescription() {
		return "Scénario d'un événement d'annulation de décès d'un célibataire.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndJulie = 6789;

	private long noHabJulie;

	private final RegDate dateNaissance = RegDate.get(1977, 4, 19);
	private final RegDate dateDebutSuisse = dateNaissance.addYears(22).addDays(48);
	private final RegDate dateObtentionPermis = RegDate.get(2007, 12, 6);
	private final RegDate dateDeces = RegDate.get(2008, 10, 5);
	private final Commune commune = MockCommune.Cossonay;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new DefaultMockServiceCivil());
	}

	@Etape(id=1, descr="Chargement de l'habitant célibataire")
	public void step1() {
		PersonnePhysique julie = addHabitant(noIndJulie);
		noHabJulie = julie.getNumero();
		ForFiscalPrincipal f = addForFiscalPrincipal(julie, commune, dateDebutSuisse, dateObtentionPermis.getOneDayBefore(), MotifFor.DEBUT_EXPLOITATION, MotifFor.PERMIS_C_SUISSE);
		f.setModeImposition(ModeImposition.SOURCE);

		f = addForFiscalPrincipal(julie, commune, dateObtentionPermis, dateDeces, MotifFor.PERMIS_C_SUISSE, MotifFor.VEUVAGE_DECES);
		f.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id=1, descr="Vérifie que les fors de l'habitant sont fermés car il est sensé être décédé")
	public void check1() {
		PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
		ForFiscalPrincipal ffp = julie.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'habitant " + julie.getNumero() + " null");
		assertNotNull(ffp.getDateFin(), "Le for principal l'habitant " + julie.getNumero() + " est ouvert");
		assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture(), "Le motif de fermeture n'est pas VEUVAGE_DECES");
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation Décès")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_DECES, noIndJulie, dateDeces, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des fors fiscaux")
	public void check2() {
		PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
		ForFiscalPrincipal ffp = julie.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'habitant " + julie.getNumero() + " null");
		assertEquals(dateObtentionPermis, ffp.getDateDebut(), "Date de début for fausse");
		assertNull(ffp.getDateFin(), "Le for de l'habitant " + julie.getNumero() + " est fermé");
		assertNull(ffp.getMotifFermeture(), "Le for de l'habitant " + julie.getNumero() + " est fermé");
		assertEquals(ffp.getModeImposition(), ModeImposition.ORDINAIRE, "Le mode d'imposition n'est pas ORDINAIRE");
	}
}
