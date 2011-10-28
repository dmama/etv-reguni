package ch.vd.uniregctb.norentes.civil.annulationpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Scénario d'un événement annulation de permis d'une personne celibataire.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_16001_01_AnnulationPermis_Celibataire_Scenario extends AnnulationPermisNorentesScenario {

	public static final String NAME = "16001_01_AnnulationPermis";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER;
	}

	@Override
	public String getDescription() {
		return "Annulation du Permis C d'un habitant célibataire";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndJulie = 6789; // julie

	private MockIndividu indJulie;

	private long noHabJulie;

	private final RegDate dateNaissance = RegDate.get(1977, 4, 19);
	private final RegDate dateDebutSuisse = dateNaissance.addYears(22).addDays(48);
	private final RegDate dateObtentionPermis = RegDate.get(2007, 12, 6);
	private final RegDate dateAnnulationPermis = RegDate.get(2008, 3, 26);
	private final Commune commune = MockCommune.Cossonay;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				indJulie = addIndividu(noIndJulie, dateNaissance, "Goux", "Julie", false);
				addOrigine(indJulie, MockPays.France.getNomMinuscule());
				addNationalite(indJulie, MockPays.France, RegDate.get(1961, 3, 12), null);
				addPermis(indJulie, TypePermis.ETABLISSEMENT, dateObtentionPermis, null, false);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant célibataire")
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

	@Etape(id=2, descr="Envoi de l'événement Annulation Permis")
	public void etape2() throws Exception {

		{
			// annulation du permis
			searchPermis(noIndJulie, TypePermis.ETABLISSEMENT, dateAnnulationPermis.year()).setDateAnnulation(dateAnnulationPermis);
		}

		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER, noIndJulie, dateObtentionPermis, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des fors fiscaux")
	public void check2() {
		PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
		ForFiscalPrincipal ffp = julie.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'habitant " + julie.getNumero() + " null");
		assertEquals(ffp.getDateDebut(), dateDebutSuisse, "Date de début for fausse");
		assertNull(ffp.getDateFin(), "Le for de l'habitant " + julie.getNumero() + " est fermé");
		assertEquals(ffp.getModeImposition(), ModeImposition.SOURCE, "Le mode d'imposition n'est pas SOURCE");
	}
}
