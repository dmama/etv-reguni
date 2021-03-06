package ch.vd.unireg.norentes.civil.depart;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPPErreur;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

public class Ec_19000_10_Depart_DejaNonHabitantDejaForLoin_Scenario extends DepartScenario {

	private AdresseService adresseService;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public static final String NAME = "19000_10_Depart";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DEPART_COMMUNE;
	}

	@Override
	public String getDescription() {
		return "Départ Hors canton de la commune de chamblon à Enney (d'une personne qui est déjà notée comme non-habitant) et dont les fors sont déjà HC";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndCharles = 782551;
	private MockIndividu indCharles;
	private long noHabCharles;

	private final RegDate dateNaissanceCharles = RegDate.get(1944, 8, 2);
	private final RegDate dateDebutForChamblon = RegDate.get(1981,2, 1);
	private final RegDate dateDepart = RegDate.get(2009, 8, 31);
	private final RegDate dateArrivee = dateDepart.getOneDayAfter();
	private final MockCommune communeDepart = MockCommune.Chamblon;
	private final MockCommune communeArrivee = MockCommune.Enney;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {

				final RegDate dateAmenagement = RegDate.get(1977, 1, 6);

				indCharles = addIndividu(noIndCharles, dateNaissanceCharles, "CHABOUDEZ", "Charles", true);
				addOrigine(indCharles, MockCommune.Neuchatel);
				addNationalite(indCharles, MockPays.Suisse, dateNaissanceCharles, null);
				addAdresse(indCharles, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, dateAmenagement, null);
				addAdresse(indCharles, TypeAdresseCivil.COURRIER,  MockRue.Chamblon.RueDesUttins, null, dateAmenagement, null);
			}

		});


	}

	@Etape(id=1, descr="Chargement de la personne physique non-habitante et de son for")
	public void etape1() throws Exception {

		final PersonnePhysique charles = addNonHabitant("CHABOUDEZ", "Charles", dateNaissanceCharles, Sexe.MASCULIN);
		charles.setNumeroIndividu(noIndCharles);
		noHabCharles = charles.getNumero();

		final RegDate debutForHC = dateArrivee.addMonths(-3);
		addForFiscalPrincipal(charles, communeDepart, dateDebutForChamblon, debutForHC.addDays(-1), MotifFor.DEMENAGEMENT_VD, MotifFor.DEPART_HC);
		addForFiscalPrincipal(charles, communeArrivee, debutForHC, null, MotifFor.DEPART_HC, null);
	}

	@Check(id=1, descr="Vérifie que Charles a son adresse et son For à Chamblon")
	public void check1() throws Exception {

		final PersonnePhysique charles = (PersonnePhysique) tiersDAO.get(noHabCharles);
		assertNotNull(charles, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabCharles) + " non existant");
		final ForFiscalPrincipal ffp = charles.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de Charles est null");
		assertNull(ffp.getDateFin(), "Date de fin du dernier for de Charles fausse");
		assertEquals(communeArrivee.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");

		// vérification que les adresses civiles sont à Chamblon
		assertEquals(communeDepart.getNomOfficiel(),
				serviceCivilService.getAdresses(noIndCharles, RegDate.get(), false).principale.getLocalite(),
				"L'adresse principale n'est pas à " + communeDepart.getNomOfficiel());
	}

	@Etape(id=2, descr="Départ hors Canton du contribuable")
	public void etape2() throws Exception {
		fermerAdresses(indCharles, dateDepart);
		ouvrirAdresseEnney(indCharles, dateArrivee);
	}

	@Check(id=2, descr="Vérifie que Charles a toujours son For à Chamblon mais l'adresse hors canton")
	public void check2() throws Exception {

		final PersonnePhysique charles = (PersonnePhysique) tiersDAO.get(noHabCharles);
		assertNotNull(charles, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabCharles) + " non existant");
		final ForFiscalPrincipal ffp = charles.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de Charles est null");
		assertNull(ffp.getDateFin(), "Date de fin du dernier for de Charles fausse");
		assertEquals(communeArrivee.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");

		// vérification que les adresses civiles sont au A Enney
		assertEquals(MockLocalite.Enney.getNom(),
				serviceCivilService.getAdresses(noIndCharles, dateArrivee, false).principale.getLocalite(),
				"L'adresse principale n'est pas à Enney");
	}



	@Etape(id=3, descr="Envoi de l'événement de départ")
	public void etape3() throws Exception {
		final long idCharles = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndCharles, dateDepart, communeDepart.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(idCharles);
	}

	@Check(id=3, descr="Vérifie les fors et adresse d'envoi")
	public void check3() throws Exception {

		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabCharles);
		assertNotNull(evt, "Où est l'événement civil d'arrivée de Charles ?");
		assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(), "L'événement civil devrait être en erreur (for déjà HC).");

		final Set<EvenementCivilRegPPErreur> erreurs = evt.getErreurs();
		assertNotNull(erreurs, "Evénement en erreur sans erreur?");
		assertEquals(1, erreurs.size(), "Mauvais nombre d'erreurs");

		final EvenementCivilRegPPErreur erreur = erreurs.iterator().next();
		assertNotNull(erreur, "Evénement en erreur sans erreur?");
		assertEquals("Le for du contribuable est déjà hors du canton", erreur.getMessage(), "Mauvaise erreur");
	}


}
