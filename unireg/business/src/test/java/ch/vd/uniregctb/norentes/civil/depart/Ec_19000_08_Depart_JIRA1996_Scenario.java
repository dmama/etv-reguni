package ch.vd.uniregctb.norentes.civil.depart;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_19000_08_Depart_JIRA1996_Scenario extends DepartScenario {

	private AdresseService adresseService;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public static final String NAME = "19000_08_Depart";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DEPART_COMMUNE;
	}

	@Override
	public String getDescription() {
		return "Départ Hors canton de la commune de chamblon à Enney";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndCharles = 782551;
	private final long noIndGeorgette = 782552;

	private MockIndividu indCharles;
	private MockIndividu indGorgette;

	private long noHabCharles;
	private long noHabGeorgette;
	private long noMenage;

	private final RegDate dateMariage = RegDate.get(1977, 1, 6);
	private final RegDate dateDebutForChamblon = RegDate.get(1981,2, 1);
	private final RegDate dateDepart = RegDate.get(2009, 8, 31);
	private final RegDate dateArrivee = dateDepart.getOneDayAfter();
	private final MockCommune communeDepart = MockCommune.Chamblon;
	private final MockCommune communeArrivee = MockCommune.Enney;
	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "deparHC26012004.xml";

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				final RegDate dateAmenagement = RegDate.get(1977, 1, 6);


				final RegDate dateNaissanceCharles = RegDate.get(1944, 8, 2);
				indCharles = addIndividu(noIndCharles, dateNaissanceCharles, "CHABOUDEZ", "Charles", true);
				addOrigine(indCharles, MockCommune.Neuchatel);
				addNationalite(indCharles, MockPays.Suisse, dateNaissanceCharles, null);
				addAdresse(indCharles, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, dateAmenagement, null);
				addAdresse(indCharles, TypeAdresseCivil.COURRIER,  MockRue.Chamblon.RueDesUttins, null, dateAmenagement, null);

				final RegDate dateNaissanceGeorgette = RegDate.get(1946, 5, 14);
				indGorgette = addIndividu(noIndGeorgette, dateNaissanceGeorgette, "CHABOUDEZ", "Georgette", false);
				addOrigine(indGorgette, MockCommune.Neuchatel);
				addNationalite(indGorgette, MockPays.Suisse, dateNaissanceGeorgette, null);
				addAdresse(indGorgette, TypeAdresseCivil.PRINCIPALE,  MockRue.Chamblon.RueDesUttins, null, dateAmenagement, null);
				addAdresse(indGorgette, TypeAdresseCivil.COURRIER,  MockRue.Chamblon.RueDesUttins, null, dateAmenagement, null);

				marieIndividus(indCharles, indGorgette, dateMariage);
			}

		});


	}

	@Etape(id=1, descr="Chargement du couple et son for")
	public void etape1() throws Exception {

		final PersonnePhysique charles = addHabitant(noIndCharles);
		noHabCharles = charles.getNumero();

		final PersonnePhysique georgette = addHabitant(noIndGeorgette);
		noHabGeorgette = georgette.getNumero();

		final MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		{
			noMenage = menage.getNumero();

			tiersService.addTiersToCouple(menage, charles, dateMariage, null);
			tiersService.addTiersToCouple(menage, georgette, dateMariage, null);

			addForFiscalPrincipal(menage, communeDepart, dateDebutForChamblon, null, MotifFor.DEMENAGEMENT_VD, null);
		}
	}

	@Check(id=1, descr="Vérifie que le couple a son adresse et son For à Chamblon")
	public void check1() throws Exception {

		{
			final PersonnePhysique charles = (PersonnePhysique) tiersDAO.get(noHabCharles);
			assertNotNull(charles, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabCharles) + " non existant");
			final ForFiscalPrincipal ffp = charles.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + charles.getNumero() + " non null");
		}

		{
			final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(noHabGeorgette);
			assertNotNull(georgette, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabGeorgette) + " non existant");
			final ForFiscalPrincipal ffp = georgette.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + georgette.getNumero() + " non null");
		}

		{
			final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);

			final ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du ménage est null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for du ménage fausse");
			assertEquals(communeDepart.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");

			// vérification que les adresses civiles sont à Chamblon
			assertEquals(communeDepart.getNomMinuscule(),
					serviceCivilService.getAdresses(noIndCharles, RegDate.get(), false).principale.getLocalite(),
					"L'adresse principale n'est pas à " + communeDepart.getNomMinuscule());
		}


	}

	@Etape(id=2, descr="Départ hors Canton du contribuable principal du couple")
	public void etape2() throws Exception {
		fermerAdresses(indCharles, dateDepart);
		ouvrirAdresseEnney(indCharles, dateArrivee);


		fermerAdresses(indGorgette, dateDepart);
		ouvrirAdresseEnney(indGorgette, dateArrivee);
	}

	@Check(id=2, descr="Vérifie que l'habitant a toujours son For à Chamblon mais l'adresse hors canton")
	public void check2() throws Exception {

		{
			final PersonnePhysique charles = (PersonnePhysique) tiersDAO.get(noHabCharles);
			assertNotNull(charles, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabCharles) + " non existant");
			final ForFiscalPrincipal ffp = charles.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + charles.getNumero() + " non null");
		}

		{
			final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(noHabGeorgette);
			assertNotNull(georgette, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabGeorgette) + " non existant");
			final ForFiscalPrincipal ffp = georgette.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + georgette.getNumero() + " non null");
		}

		{
			final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
			final ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabCharles) + " null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for de Sebastien fausse");
			assertEquals(communeDepart.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");

			// vérification que les adresses civiles sont au A Enney
			assertEquals(MockLocalite.Enney.getNomCompletMinuscule(),
					serviceCivilService.getAdresses(noIndCharles, dateArrivee, false).principale.getLocalite(),
					"L'adresse principale n'est pas à Enney");
			assertEquals(MockLocalite.Enney.getNomCompletMinuscule(),
					serviceCivilService.getAdresses(noIndGeorgette, dateArrivee, false).principale.getLocalite(),
			"L'adresse principale n'est pas à Enney");
		}


	}



	@Etape(id=3, descr="Envoi de l'événement de départ")
	public void etape3() throws Exception {
		final long idCharles = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndCharles, dateDepart, communeDepart.getNoOFS());
		final long idGeorgette = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndGeorgette, dateDepart, communeDepart.getNoOFS());

		final long idEvenementAncien = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndCharles, noIndGeorgette, dateDepart, communeDepart.getNoOFS());
		commitAndStartTransaction();
		traiteEvenementsAnciens(idEvenementAncien);


	}

	@Check(id=3, descr="Vérifie les fors et adresse d'envoi du couple")
	public void check3() throws Exception {


		final PersonnePhysique charles = (PersonnePhysique)tiersDAO.get(noHabCharles);
		final PersonnePhysique georgette = (PersonnePhysique)tiersDAO.get(noHabGeorgette);

		assertFalse(charles.isHabitantVD(),"Charles aurait du passé non Habitant");
		assertFalse(georgette.isHabitantVD(),"georgette aurait du passé non Habitant");

		final MenageCommun menageCommun = (MenageCommun) tiersDAO.get(noMenage);
		final ForFiscalPrincipal ffp = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull(ffp, "Le couple n'est plus asujetti");
		assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale(), "Le for n'est pas Hors canton");
		assertEquals(communeArrivee.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le for n'est pas dans la bonne commune");

	}


}
