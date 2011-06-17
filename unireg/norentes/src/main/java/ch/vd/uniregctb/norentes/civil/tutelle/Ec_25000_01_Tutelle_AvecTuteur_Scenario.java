package ch.vd.uniregctb.norentes.civil.tutelle;

import java.util.Set;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.type.TypeTutelle;

public class Ec_25000_01_Tutelle_AvecTuteur_Scenario extends EvenementCivilScenario {

	public static final String NAME = "25000_01_Tutelle";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.MESURE_TUTELLE;
	}

	@Override
	public String getDescription() {
		return "Mesure de tutelle";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndJulie = 6789;	// julie
	private final long noIndBea = 23456;	// bea

	private MockIndividu indJulie;
	private MockIndividu indBea;

	private long noHabJulie;
	private long noHabBea;

	private final RegDate dateNaissance = RegDate.get(1995, 4, 19);
	private final RegDate dateTutelle = dateNaissance.addYears(12);
	private final Commune commune = MockCommune.Lausanne;

	@Override
	@SuppressWarnings("deprecation")
	protected void initServiceCivil() {
		serviceCivilService.setUp(new DefaultMockServiceCivil() {

			@Override
			protected void init() {

				indBea = addIndividu(noIndBea, RegDate.get(1963, 8, 20), "Duval", "Béatrice", false);
				indJulie = addIndividu(noIndJulie, RegDate.get(1977, 4, 19), "Goux", "Julie", false);

				addAdresse(indBea, TypeAdresseCivil.PRINCIPALE, "rue de Béa", "1Bis", MockLocalite.Bex.getNPA(), MockLocalite.Bex, new CasePostale(TexteCasePostale.CASE_POSTALE, 4848),
						RegDate.get(1980, 11, 2), null);
				addAdresse(indBea, TypeAdresseCivil.COURRIER, "rue de Béa", "1Bis", MockLocalite.Bex.getNPA(), MockLocalite.Bex, new CasePostale(TexteCasePostale.CASE_POSTALE, 4848),
						RegDate.get(1980, 11, 2), null);

				addAdresse(indJulie, TypeAdresseCivil.PRINCIPALE, "rue de Julie", "28", MockLocalite.Renens.getNPA(), MockLocalite.Renens, new CasePostale(TexteCasePostale.CASE_POSTALE, 5252),
						RegDate.get(1980, 11, 2), null);
				addAdresse(indJulie, TypeAdresseCivil.COURRIER, "rue de Julie", "28", MockLocalite.Renens.getNPA(), MockLocalite.Renens, new CasePostale(TexteCasePostale.CASE_POSTALE, 5252),
						RegDate.get(1980, 11, 2), null);

				//addDefaultAdressesTo(indBea);
				//addDefaultAdressesTo(indJulie);

				setTutelle(indJulie, indBea, null, TypeTutelle.TUTELLE);

			}
		});
	}

	@Etape(id=1, descr="Chargement du pupille et son tuteur")
	public void step1() {
		PersonnePhysique julie = addHabitant(noIndJulie);
		noHabJulie = julie.getNumero();

		PersonnePhysique bea = addHabitant(noIndBea);
		noHabBea = bea.getNumero();
	}

	@Check(id=1, descr="Vérifie qu'un rapport tutelle existe avec Julie comme pupille et Béatrice comme tuteur")
	public void check1() {
		{
			PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
			Set<RapportEntreTiers> rapportsEntreTiers = julie.getRapportsSujet();
			assertNotNull(rapportsEntreTiers, "Aucun rapport a été trouvé");
			int nombreRapportsTutelle = 0;
			for (RapportEntreTiers rapportEntreTiers : rapportsEntreTiers) {
				if (TypeRapportEntreTiers.TUTELLE == rapportEntreTiers.getType()) {
					nombreRapportsTutelle++;
				}
			}
			assertEquals(0, nombreRapportsTutelle, "Un ou plusieurs rapports de tutelle ont été trouvés");
		}
		{
			PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			Set<RapportEntreTiers> rapportsEntreTiers = bea.getRapportsObjet();
			assertNotNull(rapportsEntreTiers, "Aucun rapport a été trouvé");
			int nombreRapportsTutelle = 0;
			for (RapportEntreTiers rapportEntreTiers : rapportsEntreTiers) {
				if (TypeRapportEntreTiers.TUTELLE == rapportEntreTiers.getType()) {
					nombreRapportsTutelle++;
				}
			}
			assertEquals(0, nombreRapportsTutelle, "Un ou plusieurs rapports de tutelle ont été trouvés");
		}
	}

	@Etape(id=2, descr="Envoi de l'événement de tutelle")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.MESURE_TUTELLE, noIndJulie, dateTutelle, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le rapport tutelle est clos")
	public void check2() {
		{
			PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
			julie.getAdresseActive(TypeAdresseTiers.COURRIER, RegDate.get());
			Set<RapportEntreTiers> rapportsEntreTiers = julie.getRapportsSujet();
			assertNotNull(rapportsEntreTiers, "Aucun rapport a été trouvé");
			int nombreRapportsTutelle = 0;
			RapportEntreTiers rapportTutelle = null;
			for (RapportEntreTiers rapportEntreTiers : rapportsEntreTiers) {
				if (TypeRapportEntreTiers.TUTELLE == rapportEntreTiers.getType()) {
					nombreRapportsTutelle++;
					rapportTutelle = rapportEntreTiers;
				}
			}
			assertEquals(1, nombreRapportsTutelle, "Aucun rapport de tutelle (ou plus d'un) a été trouvé");
			assertNull(rapportTutelle.getDateFin(), "Le rapport tutelle est déjà clos");
		}
		{
			PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			Set<RapportEntreTiers> rapportsEntreTiers = bea.getRapportsObjet();
			assertNotNull(rapportsEntreTiers, "Aucun rapport a été trouvé");
			int nombreRapportsTutelle = 0;
			RapportEntreTiers rapportTutelle = null;
			for (RapportEntreTiers rapportEntreTiers : rapportsEntreTiers) {
				if (TypeRapportEntreTiers.TUTELLE == rapportEntreTiers.getType()) {
					nombreRapportsTutelle++;
					rapportTutelle = rapportEntreTiers;
				}
			}
			assertEquals(1, nombreRapportsTutelle, "Aucun rapport de tutelle (ou plus d'un) a été trouvé");
			assertNull(rapportTutelle.getDateFin(), "Le rapport tutelle est déjà clos");
		}
	}

	@Etape(id=3, descr="Envoi d'un deuxième événement de tutelle")
	public void step3() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.MESURE_TUTELLE, noIndJulie, dateTutelle, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Tout doit être comme après l'étape 2")
	public void check3() {
		check2();
	}
}
