package ch.vd.unireg.norentes.civil.annulationpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

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

	private static final long noIndJulie = 6789; // julie

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
				addNationalite(indJulie, MockPays.France, RegDate.get(1961, 3, 12), null);
				addPermis(indJulie, TypePermis.ETABLISSEMENT, dateObtentionPermis, null, false);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant célibataire")
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

	@Etape(id=2, descr="Envoi de l'événement Annulation Permis")
	public void etape2() throws Exception {

		{
			// annulation du permis
			searchPermis(noIndJulie, TypePermis.ETABLISSEMENT, dateAnnulationPermis).setDateAnnulation(dateAnnulationPermis);
		}

		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER, noIndJulie, dateObtentionPermis, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des fors fiscaux")
	public void check2() {
		PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
		ForFiscalPrincipalPP ffp = julie.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'habitant " + julie.getNumero() + " null");
		assertEquals(ffp.getDateDebut(), dateDebutSuisse, "Date de début for fausse");
		assertNull(ffp.getDateFin(), "Le for de l'habitant " + julie.getNumero() + " est fermé");
		assertEquals(ffp.getModeImposition(), ModeImposition.SOURCE, "Le mode d'imposition n'est pas SOURCE");
	}
}
