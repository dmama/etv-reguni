package ch.vd.uniregctb.norentes.civil.annulationtutelle;

import java.util.Set;

import annotation.Check;
import annotation.Etape;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeTutelle;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class Ec_26001_01_AnnulationLeveeTutelle_Scenario extends EvenementCivilScenario {

	public static final String NAME = "26001_01_AnnulationLeveeTutelle";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_LEVEE_TUTELLE;
	}

	@Override
	public String getDescription() {
		return "Annulation de la levée de tutelle d'une personne avec tuteur";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndMomo = 54321;  // momo
	private final long noIndPierre = 6789; // pierre

	private MockIndividu indMomo;
	private MockIndividu indPierre;

	private long noHabMomo;
	private long noHabPierre;

	private final RegDate dateNaissance = RegDate.get(1990, 4, 19);
	private final RegDate dateTutelle = dateNaissance.addYears(12);
	private final RegDate dateLeveeTutelle = dateNaissance.addYears(18).addDays(3);
	private final Commune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new DefaultMockServiceCivil() {

			@Override
			protected void init() {

				indPierre = addIndividu(noIndPierre, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);
				indMomo = addIndividu(noIndMomo, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);

				addDefaultAdressesTo(indPierre);
				addDefaultAdressesTo(indMomo);

				setTutelle(indMomo, indPierre, EnumTypeTutelle.TUTELLE);

			}

		});
	}

	@Etape(id=1, descr="Chargement du pupille et son tuteur")
	public void step1() {
		PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();

		PersonnePhysique pierre = addHabitant(noIndPierre);
		noHabPierre = pierre.getNumero();

		RapportEntreTiers rapport = new Tutelle(dateTutelle, dateLeveeTutelle, momo, pierre);
		tiersDAO.save(rapport);
	}

	@Check(id=1, descr="Vérifie qu'un rapport tutelle existe avec Maurice comme pupille et Pierre comme tuteur")
	public void check1() {
		{
			PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			Set<RapportEntreTiers> rapportsEntreTiers = momo.getRapportsSujet();
			assertNotNull(rapportsEntreTiers, "Aucun rapport a été trouvé");
			int nombreRapportsTutelle = 0;
			RapportEntreTiers rapportTutelle = null;
			for (RapportEntreTiers rapportEntreTiers : rapportsEntreTiers) {
				if (TypeRapportEntreTiers.TUTELLE.equals(rapportEntreTiers.getType())) {
					nombreRapportsTutelle++;
					rapportTutelle = rapportEntreTiers;
				}
			}
			assertEquals(1, nombreRapportsTutelle, "Aucun rapport de tutelle (ou plus d'un) a été trouvé");
			assertNotNull(rapportTutelle.getDateFin(), "Le rapport tutelle est déjà ouvert");
		}
		{
			PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			Set<RapportEntreTiers> rapportsEntreTiers = pierre.getRapportsObjet();
			assertNotNull(rapportsEntreTiers, "Aucun rapport a été trouvé");
			int nombreRapportsTutelle = 0;
			RapportEntreTiers rapportTutelle = null;
			for (RapportEntreTiers rapportEntreTiers : rapportsEntreTiers) {
				if (TypeRapportEntreTiers.TUTELLE.equals(rapportEntreTiers.getType())) {
					nombreRapportsTutelle++;
					rapportTutelle = rapportEntreTiers;
				}
			}
			assertEquals(1, nombreRapportsTutelle, "Aucun rapport de tutelle (ou plus d'un) a été trouvé");
			assertNotNull(rapportTutelle.getDateFin(), "Le rapport tutelle est déjà ouvert");
		}
	}

	@Etape(id=2, descr="Envoi de l'événement d'annulation de levée de tutelle")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_LEVEE_TUTELLE, noIndMomo, dateLeveeTutelle, commune.getNoOFS());
		commitAndStartTransaction();
		regroupeEtTraiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que la levée de tutelle a bien été annulée")
	public void check2() {
		{
			PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			Set<RapportEntreTiers> rapportsEntreTiers = momo.getRapportsSujet();
			assertNotNull(rapportsEntreTiers, "Aucun rapport a été trouvé");
			int nombreRapportsTutelle = 0;
			RapportEntreTiers rapportTutelle = null;
			for (RapportEntreTiers rapportEntreTiers : rapportsEntreTiers) {
				if (TypeRapportEntreTiers.TUTELLE.equals(rapportEntreTiers.getType())) {
					nombreRapportsTutelle++;
					rapportTutelle = rapportEntreTiers;
				}
			}
			assertEquals(1, nombreRapportsTutelle, "Aucun rapport de tutelle (ou plus d'un) a été trouvé");
			assertNull(rapportTutelle.getDateFin(), "Le rapport tutelle est encore clos");
		}
		{
			PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			Set<RapportEntreTiers> rapportsEntreTiers = pierre.getRapportsObjet();
			assertNotNull(rapportsEntreTiers, "Aucun rapport a été trouvé");
			int nombreRapportsTutelle = 0;
			RapportEntreTiers rapportTutelle = null;
			for (RapportEntreTiers rapportEntreTiers : rapportsEntreTiers) {
				if (TypeRapportEntreTiers.TUTELLE.equals(rapportEntreTiers.getType())) {
					nombreRapportsTutelle++;
					rapportTutelle = rapportEntreTiers;
				}
			}
			assertEquals(1, nombreRapportsTutelle, "Aucun rapport de tutelle (ou plus d'un) a été trouvé");
			assertNull(rapportTutelle.getDateFin(), "Le rapport tutelle est encore clos");
		}
	}
}
