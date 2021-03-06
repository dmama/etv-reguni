package ch.vd.unireg.norentes.civil.deces;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

public class Ec_2000_10_Deces_CelibataireTuteur_Scenario extends EvenementCivilScenario {

	public static final String NAME = "2000_10_Deces_CelibataireTuteur";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DECES;
	}

	@Override
	public String getDescription() {
		return "Décès d'un habitant célibataire avec un rapport de tutelle actif lors du déces";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndCharles = 124095;
	private static final long noIndNora = 128157;

	private MockIndividu indCharles;
	private MockIndividu indNora;

	private long noHabCharles;
	private long noHabNora;

	private final RegDate dateArriveeVD = RegDate.get(1980, 2, 1);
	private final RegDate dateTutelle = dateArriveeVD.addYears(12);

	private final RegDate dateDeces = RegDate.get(2009, 8, 1);

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {

				final RegDate dateNaissanceCharles = RegDate.get(1947, 7, 26);
				indCharles = addIndividu(noIndCharles, dateNaissanceCharles, "Coutaz", "Charles", true);
				addAdresse(indCharles, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveeVD, null);

				indNora = addIndividu(noIndNora, RegDate.get(1968, 10, 15), "Von Ballmoos", "Nora", false);
				addAdresse(indNora, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArriveeVD, null);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant et son pupille")
	public void etape1() {
		final PersonnePhysique charles = addHabitant(noIndCharles);
		noHabCharles = charles.getNumero();
		addForFiscalPrincipal(charles, MockCommune.Lausanne, dateArriveeVD, null, MotifFor.DEMENAGEMENT_VD, null);

		final PersonnePhysique nora = addHabitant(noIndNora);
		noHabNora = nora.getNumero();
		addForFiscalPrincipal(nora, MockCommune.Lausanne, dateArriveeVD, null, MotifFor.DEMENAGEMENT_VD, null);

		RapportEntreTiers rapportTutelle = new ch.vd.unireg.tiers.Tutelle(dateTutelle, null, nora, charles, null);
		tiersDAO.save(rapportTutelle);

		charles.setBlocageRemboursementAutomatique(false);
		nora.setBlocageRemboursementAutomatique(false);
	}

	@Check(id = 1, descr = "Vérification que l'habitant a un rapport tutelle actif")
	public void check1() throws Exception {

		final PersonnePhysique nora = (PersonnePhysique) tiersDAO.get(noHabNora);

		final PersonnePhysique charles = (PersonnePhysique) tiersDAO.get(noHabCharles);

		final ForFiscalPrincipal ffp = charles.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'Habitant " + charles.getNumero() + " null");
		assertNull(ffp.getDateFin(), "Date de fin du dernier for de Charles fausse");

		final Set<RapportEntreTiers> rapports = charles.getRapportsObjet();
		assertNotNull(rapports, "Rapport tutelle inexistant");
		assertEquals(1, rapports.size(), "Nombre de rapports incorrect");
		RapportEntreTiers tutelle = rapports.iterator().next();
		assertEquals(nora.getId(), tutelle.getSujetId(), "Mauvais rapport tutelle");
		assertEquals(dateTutelle, tutelle.getDateDebut(), "Date de début tutelle fausse");
		assertNull(tutelle.getDateFin(), "Date de fin tutelle fausse");

		// vérification que les adresses civiles sont a Bex
		assertEquals(MockCommune.Lausanne.getNomOfficiel(), serviceCivilService.getAdresses(noIndCharles, dateArriveeVD, false).principale.getLocalite(), "l'adresse principale n'est pas à Lausanne");

		assertBlocageRemboursementAutomatique(false, false);
	}

	@Etape(id = 2, descr = "Déclaration de décès")
	public void etape2() throws Exception {

		doModificationIndividu(noIndCharles, individu -> individu.setDateDeces(dateDeces));

		long id = addEvenementCivil(TypeEvenementCivil.DECES, noIndCharles, dateDeces, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérification que le for est bien fermé sur Lausanne après le décès, et que les remboursements automatiques sont bien bloqués")
	public void check2() {

		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabCharles);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");

		final PersonnePhysique charles = (PersonnePhysique) tiersDAO.get(noHabCharles);
		final List<ForFiscal> list = charles.getForsFiscauxSorted();
		assertEquals(1, list.size(), "Plusieurs for?: " + list.size());

		// For fermé sur Lausanne pour cause de décès
		final ForFiscalPrincipal ffpFerme = (ForFiscalPrincipal)list.get(0);
		assertEquals(dateDeces, ffpFerme.getDateFin(), "Le for sur Lausanne n'est pas fermé à la bonne date");
		assertEquals(MotifFor.VEUVAGE_DECES, ffpFerme.getMotifFermeture(), "Le for sur Lausanne n'est pas fermé pour cause de décès");

		final Set<RapportEntreTiers> rapports = charles.getRapportsObjet();
		assertNotNull(rapports, "Rapport tutelle inexistant");
		assertEquals(1, rapports.size(), "Nombre de rapports incorrect");

		final PersonnePhysique nora = (PersonnePhysique) tiersDAO.get(noHabNora);

		RapportEntreTiers tutelle = rapports.iterator().next();
		assertEquals(nora.getId(), tutelle.getSujetId(), "Mauvais rapport tutelle");
		assertEquals(dateTutelle, tutelle.getDateDebut(), "Date de début tutelle fausse");
		assertEquals(dateDeces, tutelle.getDateFin(), "Date de fin tutelle fausse");

		assertBlocageRemboursementAutomatique(true, false);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduCharles, boolean blocageAttenduNora) {
		assertBlocageRemboursementAutomatique(blocageAttenduCharles, tiersDAO.get(noHabCharles));
		assertBlocageRemboursementAutomatique(blocageAttenduNora, tiersDAO.get(noHabNora));
	}
}
