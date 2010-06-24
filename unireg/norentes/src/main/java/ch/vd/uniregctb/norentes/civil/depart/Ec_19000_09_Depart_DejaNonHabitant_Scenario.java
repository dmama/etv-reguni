package ch.vd.uniregctb.norentes.civil.depart;

import annotation.Check;
import annotation.Etape;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_19000_09_Depart_DejaNonHabitant_Scenario extends DepartScenario {

	public static final String NAME = "19000_09_Depart";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DEPART_COMMUNE;
	}

	@Override
	public String getDescription() {
		return "Départ Hors canton de la commune de chamblon à Enney (d'une personne qui est déjà notée comme non-habitant)";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndCharles = 782551;
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
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				final RegDate dateAmenagement = RegDate.get(1977, 1, 6);

				indCharles = addIndividu(noIndCharles, dateNaissanceCharles, "CHABOUDEZ", "Charles", true);
				addOrigine(indCharles, MockPays.Suisse, MockCommune.Neuchatel, dateNaissanceCharles);
				addNationalite(indCharles, MockPays.Suisse, dateNaissanceCharles, null, 1);
				addAdresse(indCharles, EnumTypeAdresse.PRINCIPALE, MockRue.Chamblon.GrandRue, null, dateAmenagement, null);
				addAdresse(indCharles, EnumTypeAdresse.COURRIER,  MockRue.Chamblon.GrandRue, null, dateAmenagement, null);
			}

		});


	}

	@Etape(id=1, descr="Chargement de la personne physique non-habitante et de son for")
	public void etape1() throws Exception {

		final PersonnePhysique charles = addNonHabitant("CHABOUDEZ", "Charles", dateNaissanceCharles, Sexe.MASCULIN);
		charles.setNumeroIndividu(noIndCharles);
		noHabCharles = charles.getNumero();

		addForFiscalPrincipal(charles, communeDepart, dateDebutForChamblon, null, MotifFor.DEMENAGEMENT_VD, null);
	}

	@Check(id=1, descr="Vérifie que Charles a son adresse et son For à Chamblon")
	public void check1() throws Exception {

		final PersonnePhysique charles = (PersonnePhysique) tiersDAO.get(noHabCharles);
		assertNotNull(charles, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabCharles) + " non existant");
		final ForFiscalPrincipal ffp = charles.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de Charles est null");
		assertNull(ffp.getDateFin(), "Date de fin du dernier for de Charles fausse");
		assertEquals(communeDepart.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");

		// vérification que les adresses civiles sont à Chamblon
		assertEquals(communeDepart.getNomMinuscule(),
				serviceCivilService.getAdresses(noIndCharles, RegDate.get(), false).principale.getLocalite(),
				"L'adresse principale n'est pas à " + communeDepart.getNomMinuscule());
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
		assertEquals(communeDepart.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");

		// vérification que les adresses civiles sont au A Enney
		assertEquals(MockLocalite.Enney.getNomCompletMinuscule(),
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

		final EvenementCivilData evt = getEvenementCivilRegoupeForHabitant(noHabCharles);
		assertNotNull(evt, "Où est l'événement civil d'arrivée de Charles ?");
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement civil devrait être en traité.");

		final PersonnePhysique charles = (PersonnePhysique) tiersDAO.get(noHabCharles);
		assertFalse(charles.isHabitant(),"Charles aurait dû rester non Habitant");

		final ForFiscalPrincipal ffp = charles.getForFiscalPrincipalAt(null);
		assertNotNull(ffp, "Charles n'a plus de for fiscal principal");
		assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale(), "Le for n'est pas Hors canton");
		assertEquals(communeArrivee.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le for n'est pas dans la bonne commune");

	}


}