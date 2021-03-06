package ch.vd.unireg.norentes.civil.depart;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.adresse.AdresseSupplementaire;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivil;

public class Ec_19000_07_Depart_JIRA1703_Scenario extends DepartScenario {

	private AdresseService adresseService;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public static final String NAME = "19000_07_Depart";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DEPART_COMMUNE;
	}

	@Override
	public String getDescription() {
		return "Départ hors Suisse d'un couple dont l'adresse fiscale surchargée était permanente";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndSebastien = 844770;
	private static final long noIndGloria = 646404;

	private MockIndividu indSebastien;
	private MockIndividu indGloria;

	private long noHabSebastien;
	private long noHabGloria;
	private long noMenage;

	private final RegDate dateMariage = RegDate.get(2005, 6, 29);
	private final RegDate dateDepart = RegDate.get(2008, 6, 30);
	private final RegDate dateArrivee = dateDepart.getOneDayAfter();
	private final MockCommune communeDepart = MockCommune.VillarsSousYens;
	private final MockPays paysArrivee = MockPays.Danemark;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {

				final RegDate dateAmenagement = RegDate.get(1971, 6, 27);

				final RegDate dateNaissanceSebastien = RegDate.get(1971, 6, 27);
				indSebastien = addIndividu(noIndSebastien, dateNaissanceSebastien, "Fournier", "Sebastien", true);
				addOrigine(indSebastien, MockCommune.Neuchatel);
				addNationalite(indSebastien, MockPays.Suisse, dateNaissanceSebastien, null);
				addAdresse(indSebastien, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null, dateAmenagement, null);
				addAdresse(indSebastien, TypeAdresseCivil.COURRIER, MockRue.VillarsSousYens.CheminDuCollege, null, dateAmenagement, null);

				final RegDate dateNaissanceGloria = RegDate.get(1973, 3, 7);
				indGloria = addIndividu(noIndGloria, dateNaissanceGloria, "Fournier", "Gloria", false);
				addOrigine(indGloria, MockCommune.Neuchatel);
				addNationalite(indGloria, MockPays.Suisse, dateNaissanceGloria, null);
				addAdresse(indGloria, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null, dateAmenagement, null);
				addAdresse(indGloria, TypeAdresseCivil.COURRIER, MockRue.VillarsSousYens.CheminDuCollege, null, dateAmenagement, null);

				marieIndividus(indSebastien, indGloria, dateMariage);
			}

		});
	}

	@Etape(id=1, descr="Chargement du couple et son for")
	public void etape1() throws Exception {

		final PersonnePhysique sebastien = addHabitant(noIndSebastien);
		noHabSebastien = sebastien.getNumero();

		final PersonnePhysique gloria = addHabitant(noIndGloria);
		noHabGloria = gloria.getNumero();

		final MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		{
			noMenage = menage.getNumero();

			tiersService.addTiersToCouple(menage, sebastien, dateMariage, null);
			tiersService.addTiersToCouple(menage, gloria, dateMariage, null);

			addForFiscalPrincipal(menage, communeDepart, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
			menage.setBlocageRemboursementAutomatique(false);
		}
	}

	@Check(id=1, descr="Vérifie que le couple a son adresse et son For à Villars-sous-Yens")
	public void check1() throws Exception {

		{
			final PersonnePhysique sebastien = (PersonnePhysique) tiersDAO.get(noHabSebastien);
			assertNotNull(sebastien, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabSebastien) + " non existant");
			final ForFiscalPrincipal ffp = sebastien.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + sebastien.getNumero() + " non null");
		}

		{
			final PersonnePhysique gloria = (PersonnePhysique) tiersDAO.get(noHabGloria);
			assertNotNull(gloria, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabGloria) + " non existant");
			final ForFiscalPrincipal ffp = gloria.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + gloria.getNumero() + " non null");
		}

		{
			final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);

			final ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du ménage est null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for du ménage fausse");
			assertEquals(communeDepart.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");

			// vérification que les adresses civiles sont à Villars-sous-Yens
			assertEquals(communeDepart.getNomOfficiel(),
					serviceCivilService.getAdresses(noIndSebastien, RegDate.get(), false).principale.getLocalite(),
					"L'adresse principale n'est pas à " + communeDepart.getNomOfficiel());
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	@Etape(id=2, descr="Départ hors Suisse du contribuable principal du couple")
	public void etape2() throws Exception {
		fermerAdresses(indSebastien, dateDepart);
		ouvrirAdresseEtranger(indSebastien, dateArrivee, paysArrivee);

		fermerAdresses(indGloria, dateDepart);
		ouvrirAdresseEtranger(indGloria, dateArrivee, paysArrivee);
	}

	@Check(id=2, descr="Vérifie que l'habitant a toujours son For à Villars-sous-Yens mais l'adresse hors Suisse")
	public void check2() throws Exception {

		{
			final PersonnePhysique sebastien = (PersonnePhysique) tiersDAO.get(noHabSebastien);
			assertNotNull(sebastien, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabSebastien) + " non existant");
			final ForFiscalPrincipal ffp = sebastien.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + sebastien.getNumero() + " non null");
		}

		{
			final PersonnePhysique gloria = (PersonnePhysique) tiersDAO.get(noHabGloria);
			assertNotNull(gloria, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabGloria) + " non existant");
			final ForFiscalPrincipal ffp = gloria.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + gloria.getNumero() + " non null");
		}

		{
			final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
			final ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabSebastien) + " null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for de Sebastien fausse");
			assertEquals(communeDepart.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");

			// vérification que les adresses civiles sont au Danemark
			assertEquals(paysArrivee.getNoOFS(),
					serviceCivilService.getAdresses(noIndSebastien, dateArrivee, false).principale.getNoOfsPays(),
					"L'adresse principale n'est pas dans le pays " + paysArrivee.getNomCourt());
			assertEquals(paysArrivee.getNoOFS(),
					serviceCivilService.getAdresses(noIndGloria, dateArrivee, false).principale.getNoOfsPays(),
					"L'adresse principale n'est pas dans le pays " + paysArrivee.getNomCourt());
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	@Etape(id=3, descr="Surcharge de l'adresse fiscale (en Suisse) avec une annulée")
	public void etape3() throws Exception {

		final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);

		final AdresseSuisse adresseAnnulee = new AdresseSuisse();
		adresseAnnulee.setDateDebut(dateDepart.addDays(1));
		adresseAnnulee.setNumeroMaison(Integer.toString(26));
		adresseAnnulee.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
		adresseAnnulee.setNumeroOrdrePoste(MockRue.Lausanne.AvenueDeBeaulieu.getNoLocalite());
		adresseAnnulee.setUsage(TypeAdresseTiers.COURRIER);
		adresseAnnulee.setPermanente(true);
		adresseAnnulee.setAnnule(true);
		menage.addAdresseTiers(adresseAnnulee);

		final AdresseSuisse adresse = new AdresseSuisse();
		adresse.setDateDebut(dateDepart.addDays(1));
		adresse.setNumeroMaison(Integer.toString(26));
		adresse.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
		adresse.setNumeroOrdrePoste(MockRue.Lausanne.AvenueDeBeaulieu.getNoLocalite());
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setPermanente(true);
		menage.addAdresseTiers(adresse);
	}

	@Check(id=3, descr = "Vérification de l'adresse d'envoi sur Lausanne")
	public void check3() throws Exception {

		final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(menage, dateArrivee, TypeAdresseFiscale.COURRIER, true);
		assertTrue(adresseEnvoi.getNpaEtLocalite().toString().contains(MockCommune.Lausanne.getNomOfficiel()), "Surcharge non prise en compte");

		final Set<AdresseTiers> adresses = menage.getAdressesTiers();
		for (AdresseTiers adresse : adresses) {
			assertTrue(((AdresseSupplementaire) adresse).isPermanente(), "Adresse non permanente !");
		}
	}

	@Etape(id=4, descr="Envoi de l'événement de départ")
	public void etape4() throws Exception {
		final long idSeb = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndSebastien, dateDepart, communeDepart.getNoOFS());
		final long idGloria = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndGloria, dateDepart, communeDepart.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(idSeb);
		traiteEvenements(idGloria);
	}

	@Check(id=4, descr="Vérifie les fors et adresse d'envoi du couple")
	public void check4() throws Exception {

		final PersonnePhysique sebastien = (PersonnePhysique) tiersDAO.get(noHabSebastien);
		assertFalse(sebastien.isHabitantVD(), "Le contribuable" + FormatNumeroHelper.numeroCTBToDisplay(noHabSebastien) + " aurait dû devenir non-habitant");

		final PersonnePhysique gloria = (PersonnePhysique) tiersDAO.get(noHabGloria);
		assertFalse(gloria.isHabitantVD(), "Le contribuable " + FormatNumeroHelper.numeroCTBToDisplay(noHabGloria) + " aurait dû devenir non-habitant");

		final MenageCommun menageCommun = (MenageCommun) tiersDAO.get(noMenage);
		final ForFiscalPrincipal ffp = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull(ffp, "Le couple n'est plus asujetti");
		assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale(), "Le for n'est pas HS");
		assertEquals(paysArrivee.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le for n'est pas dans le bon pays");

		final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(menageCommun, dateArrivee, TypeAdresseFiscale.COURRIER, true);
		assertTrue(adresseEnvoi.getNpaEtLocalite().toString().contains(MockCommune.Lausanne.getNomOfficiel()), "Surcharge non prise en compte");

		final Set<AdresseTiers> adresses = menageCommun.getAdressesTiers();
		for (AdresseTiers adresse : adresses) {
			assertTrue(((AdresseSupplementaire) adresse).isPermanente(), "Adresse passée à non permanente !");
		}

		assertBlocageRemboursementAutomatique(true, true, true);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduSebastien, boolean blocageAttenduGloria, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduSebastien, tiersDAO.get(noHabSebastien));
		assertBlocageRemboursementAutomatique(blocageAttenduGloria, tiersDAO.get(noHabGloria));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}
