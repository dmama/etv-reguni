package ch.vd.unireg.norentes.civil.depart;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

public class Ec_19000_03_Depart_HS_Scenario extends DepartScenario {

	public static final String NAME = "19000_03_Depart";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {

		return TypeEvenementCivil.DEPART_COMMUNE;
	}

	@Override
	public String getDescription() {

		return "Départ d'un couple à l'étranger";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndCedric = 844770;
	private static final long noIndSandra = 844771;

	private MockIndividu indCedric;
	private MockIndividu indSandra;

	private long noHabCedric;
	private long noHabSandra;
	private long noMenage;

	private final int communeDepartBex = MockCommune.Bex.getNoOFS();
	private final RegDate dateArriveeBex = RegDate.get(2007, 4, 14);
	private final RegDate dateDepartBex = RegDate.get(2009, 6, 30);

	private final Pays paysDepart = MockPays.Danemark;
	private final int numOfsPaysDepart = paysDepart.getNoOFS();

	private class InternalIndividuConnector extends MockIndividuConnector {

		@Override
		protected void init() {

			final RegDate dateNaissanceCedric = RegDate.get(1973, 9, 24);
			indCedric = addIndividu(noIndCedric, dateNaissanceCedric, "Donzé", "Cédric", true);
			addOrigine(indCedric, MockCommune.Neuchatel);
			addNationalite(indCedric, MockPays.Suisse, dateNaissanceCedric, null);
			addAdresse(indCedric, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateArriveeBex, null);

			final RegDate dateNaissanceSandra = RegDate.get(1975, 2, 26);
			indSandra = addIndividu(noIndSandra, dateNaissanceSandra, "Donzé", "Sandra", false);
			addOrigine(indSandra, MockCommune.Peseux);
			addNationalite(indSandra, MockPays.Suisse, dateNaissanceSandra, null);
			addAdresse(indSandra, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateArriveeBex, null);

			marieIndividus(indCedric, indSandra, RegDate.get(2003, 7, 11));
		}

		private MockAdresse createAdresse(Individu individu, TypeAdresseCivil type, Rue rue, CasePostale casePostale, Localite localite, MockCommune commune, Pays pays, RegDate debutValidite, RegDate finValidite) {
			final MockAdresse adresse = new MockAdresse();
			adresse.setTypeAdresse(type);

			// localité
			if (rue != null) {
				adresse.setRue(rue.getDesignationCourrier());
				adresse.setNumeroRue(rue.getNoRue());
			}
			adresse.setCasePostale(casePostale);
			adresse.setCommuneAdresse(commune);
			if (localite != null) {
				adresse.setLocalite(localite.getNomAbrege());
				adresse.setNumeroPostal(localite.getNPA().toString());
				final Integer complementNPA = localite.getComplementNPA();
				adresse.setNumeroPostalComplementaire(complementNPA == null ? null : complementNPA.toString());
				adresse.setNumeroOrdrePostal(localite.getNoOrdre());
			}
			adresse.setPays(pays);

			// validité
			adresse.setDateDebutValidite(debutValidite);
			adresse.setDateFinValidite(finValidite);

			return adresse;
		}

		public MockAdresse addAdresse(MockIndividu individu, TypeAdresseCivil type, Rue rue, CasePostale casePostale, Localite localite, MockCommune commune, Pays pays, RegDate debutValidite, RegDate finValidite) {
			final MockAdresse adresse = createAdresse(individu, type, rue, casePostale, localite, commune, pays, debutValidite, finValidite);
			add(individu, adresse);
			return adresse;
		}
	}

	private InternalIndividuConnector internalServiceCivil;

	@Override
	protected void initServiceCivil() {
		internalServiceCivil = new InternalIndividuConnector();
		serviceCivilService.setUp(internalServiceCivil);
	}


	@Etape(id=1, descr="Chargement du couple")
	public void etape1() throws Exception {

		final PersonnePhysique cedric = addHabitant(noIndCedric);
		noHabCedric = cedric.getNumero();

		final PersonnePhysique sandra = addHabitant(noIndSandra);
		noHabSandra = sandra.getNumero();

		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun) tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, cedric, dateArriveeBex, null);
			tiersService.addTiersToCouple(menage, sandra, dateArriveeBex, null);
			// fors du ménage
			addForFiscalPrincipal(menage, MockCommune.Bex, dateArriveeBex, null, MotifFor.ARRIVEE_HC, null);
			menage.setBlocageRemboursementAutomatique(false);
		}
	}

	@Check(id=1, descr="Vérifie que le couple a son adresse et son For à Bex")
	public void check1() throws Exception {

		{
			final PersonnePhysique cedric = (PersonnePhysique) tiersDAO.get(noHabCedric);
			assertNotNull(cedric, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabCedric) + " non existant");
			assertNull(cedric.getDernierForFiscalPrincipal(), "For principal de l'Habitant " + FormatNumeroHelper.numeroCTBToDisplay(cedric.getNumero()) + " non null");

			// vérification que les adresses civiles sont a Bex
			assertEquals(MockCommune.Bex.getNomOfficiel(), serviceCivilService.getAdresses(noIndCedric, dateArriveeBex, false).principale.getLocalite(), "L'adresse principale n'est pas à Bex");
		}

		{
			final PersonnePhysique sandra = (PersonnePhysique) tiersDAO.get(noHabSandra);
			assertNotNull(sandra, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabSandra) + " non existant");
			assertNull(sandra.getDernierForFiscalPrincipal(), "For principal de l'Habitant " + FormatNumeroHelper.numeroCTBToDisplay(sandra.getNumero()) + " non null");

			// vérification que les adresses civiles sont a Bex
			assertEquals(MockCommune.Bex.getNomOfficiel(), serviceCivilService.getAdresses(noIndSandra, dateArriveeBex, false).principale.getLocalite(), "L'adresse principale n'est pas à Bex");
		}

		{
			final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
			assertNotNull(menage, "Ménage non trouvé");
			final ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + FormatNumeroHelper.numeroCTBToDisplay(menage.getNumero()) + " null");
			assertNull( ffp.getDateFin(), "Date de fin du dernier for l'Habitant " + FormatNumeroHelper.numeroCTBToDisplay(menage.getNumero()) + " fausse");
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	@Etape(id=2, descr="Départ des individus vers le Danemark")
	public void etape2() throws Exception {
		fermerAdresse(indCedric, dateDepartBex);
		fermerAdresse(indSandra, dateDepartBex);

		ouvrirAdresses(indCedric);
		ouvrirAdresses(indSandra);
	}

	@Check(id=2, descr="Vérifie que les habitants ont toujours leurs For à Bex mais leurs adresse au Danemark")
	public void check2() throws Exception {

		{
			final PersonnePhysique cedric = (PersonnePhysique) tiersDAO.get(noHabCedric);
			assertNotNull(cedric, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabCedric) + " non existant");
			assertNull(cedric.getDernierForFiscalPrincipal(), "For principal de l'Habitant " + FormatNumeroHelper.numeroCTBToDisplay(cedric.getNumero()) + " non null");

			// vérification que les adresses civiles sont au Danemark
			assertEquals(numOfsPaysDepart, serviceCivilService.getAdresses(noIndCedric, dateDepartBex.addDays(1), false).principale.getNoOfsPays(), "L'adresse principale n'est pas fermée");
		}

		{
			final PersonnePhysique sandra = (PersonnePhysique) tiersDAO.get(noHabSandra);
			assertNotNull(sandra, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabSandra) + " non existant");
			assertNull(sandra.getDernierForFiscalPrincipal(), "For principal de l'Habitant " + FormatNumeroHelper.numeroCTBToDisplay(sandra.getNumero()) + " non null");

			// vérification que les adresses civiles sont à Danemark
			assertEquals(numOfsPaysDepart, serviceCivilService.getAdresses(noIndSandra, dateDepartBex.addDays(1), false).principale.getNoOfsPays(), "L'adresse principale n'est pas fermée");
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}


	@Etape(id=3, descr="Envoi de l'événement de départ des individus")
	public void etape3() throws Exception {

		long id1 = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndCedric, dateDepartBex, communeDepartBex);
		long id2 = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndSandra, dateDepartBex, communeDepartBex);

		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id1);
		traiteEvenements(id2);
	}

	@Check(id=3, descr="Vérifie que le couple n'a plus son for sur Bex mais sur Danemark")
	public void check3() throws Exception {

		// On check que le couple est parti
		{
			final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabCedric);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "Etat invalide");

			final PersonnePhysique cedric = (PersonnePhysique) tiersDAO.get(noHabCedric);
			assertTrue(cedric.getForsFiscaux() == null || cedric.getForsFiscaux().isEmpty(), "");

			final PersonnePhysique sandra = (PersonnePhysique) tiersDAO.get(noHabSandra);
			assertTrue(sandra.getForsFiscaux() == null || sandra.getForsFiscaux().isEmpty(), "");

			final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
			final List<ForFiscal> list = menage.getForsFiscauxSorted();


			// For fermé sur Bex
			final ForFiscalPrincipal ffpFerme = (ForFiscalPrincipal) list.get(list.size()-2);
			assertEquals(dateDepartBex, ffpFerme.getDateFin(), "Le for sur Bex n'est pas fermé à la bonne date");

			// For ouvert sur Danemark
			final ForFiscalPrincipalPP ffpOuvert = (ForFiscalPrincipalPP) list.get(list.size()-1);
			assertEquals(dateDepartBex.addDays(1), ffpOuvert.getDateDebut(), "Le for sur Danemark n'est pas ouvert à la bonne date");
			assertEquals(numOfsPaysDepart, ffpOuvert.getNumeroOfsAutoriteFiscale(), "Le for ouvert n'est pas sur Zurich");
			assertEquals(MotifRattachement.DOMICILE, ffpOuvert.getMotifRattachement(), "Le MotifRattachement du for est faux");
			assertEquals(GenreImpot.REVENU_FORTUNE, ffpOuvert.getGenreImpot(), "Le GenreImpot du for est faux");
			assertEquals(ModeImposition.ORDINAIRE, ffpOuvert.getModeImposition(), "Le ModeImposition du for est faux");
		}

		assertBlocageRemboursementAutomatique(true, true, true);
	}

	private void ouvrirAdresses(MockIndividu individu) {
		internalServiceCivil.addAdresse(individu, TypeAdresseCivil.PRINCIPALE, null, null, null, null, paysDepart, dateDepartBex.getOneDayAfter(), null);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduCedric, boolean blocageAttenduSandra, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduCedric, tiersDAO.get(noHabCedric));
		assertBlocageRemboursementAutomatique(blocageAttenduSandra, tiersDAO.get(noHabSandra));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}
