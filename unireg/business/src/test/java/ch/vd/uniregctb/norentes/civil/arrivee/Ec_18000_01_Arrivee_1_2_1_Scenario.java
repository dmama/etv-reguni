package ch.vd.uniregctb.norentes.civil.arrivee;

import java.util.Collection;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_18000_01_Arrivee_1_2_1_Scenario extends EvenementCivilScenario {

	public static final String NAME = "18000_01_Arrivee";

	private ServiceInfrastructureService serviceInfrastructureService;

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	@Override
	public String getName() {
		return NAME;
	}
	@Override
	public String getDescription() {
		return "Arrivée d'une famille de 4 personnes. D'abord le fils, puis le père, puis la mère et la fille";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_DANS_COMMUNE;
	}


	private final long noIndAlain = 123456L;
	private final long noIndJanine = 223456L;
	private final long noIndJulien = 423456L;
	private final long noIndFanny = 323456L;

	private MockIndividu indAlain;
	private MockIndividu indJanine;
	private MockIndividu indJulien;
	private final RegDate dateNaissanceJulien = RegDate.get(1988, 2, 24);
	private final RegDate dateMajoriteJulien = dateNaissanceJulien.addYears(18);
	private MockIndividu indFanny;
	private final RegDate dateNaissanceFanny = RegDate.get(1998, 11, 2);

	private long noHabAlain;
	private long noHabJanine;
	private long noMenage;
	private long noHabJulien;
	private long noHabFanny;

	private final RegDate avantDateMariage = RegDate.get(1986, 4, 27);
	private final RegDate dateMariage = avantDateMariage.addDays(1);
	private final int communeDepartBex = MockCommune.Bex.getNoOFS();
	private final RegDate dateArriveeVillars = RegDate.get(1974, 3, 3);
	private final RegDate dateArriveeLausanne = RegDate.get(1980, 5, 19);
	private final RegDate dateArriveeBex = dateMariage;
	private final RegDate dateDepartBex = RegDate.get(2006, 4, 11);
	private final int communeArriveeOrbe = MockCommune.Orbe.getNoOFS();
	private final RegDate dateArriveeOrbe = dateDepartBex.addDays(1);


	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil(serviceInfrastructureService) {
			@Override
			protected void init() {

				indAlain = addIndividu(noIndAlain, RegDate.get(1952, 2, 21), "Baschung", "Alain", true);
				addAdresse(indAlain, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.CheminDuCollege, null, dateArriveeVillars, avantDateMariage);
				addAdresse(indAlain, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, dateArriveeBex, null);

				indJanine = addIndividu(noIndJanine, RegDate.get(1957, 4, 12), "Baschung-Maurer", "Janine", false);
				addAdresse(indJanine, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeLausanne, avantDateMariage);
				addAdresse(indJanine, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, dateArriveeBex, null);

				indJulien = addIndividu(noIndJulien, dateNaissanceJulien, "Baschung", "Julien", true);
				addAdresse(indJulien, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, dateNaissanceJulien, null);

				indFanny = addIndividu(noIndFanny, dateNaissanceFanny, "Baschung", "Fanny", false);
				addAdresse(indFanny, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, dateNaissanceFanny, null);

				marieIndividus(indAlain, indJanine, dateMariage);
			}
		});
	}

	@Etape(id=1, descr="Chargement de 4 Habitant à Bex")
	public void etape1() throws Exception {

		PersonnePhysique alain = addHabitant(noIndAlain);
		noHabAlain = alain.getNumero();
		addForFiscalPrincipal(alain, MockCommune.VillarsSousYens, dateArriveeVillars, avantDateMariage, MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		PersonnePhysique janine = addHabitant(noIndJanine);
		noHabJanine = janine.getNumero();
		addForFiscalPrincipal(janine, MockCommune.Lausanne, dateArriveeLausanne, avantDateMariage, MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		PersonnePhysique julien = addHabitant(noIndJulien);
		noHabJulien = julien.getNumero();
		addForFiscalPrincipal(julien, MockCommune.Bex, dateMajoriteJulien, null, MotifFor.MAJORITE, null);

		PersonnePhysique fanny = addHabitant(noIndFanny);
		noHabFanny = fanny.getNumero();

		// Menage
		MenageCommun menage = new MenageCommun();
		menage = (MenageCommun)tiersDAO.save(menage);
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, alain, dateMariage, null);
		tiersService.addTiersToCouple(menage, janine, dateMariage, null);
		addForFiscalPrincipal(menage, MockCommune.Bex, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
	}

	@Check(id=1, descr="Vérifie que les 4 Habitant ont leur adresse à Bex et leur For à Bex")
	public void check1() throws Exception {

		{
			PersonnePhysique alain = (PersonnePhysique)tiersDAO.get(noHabAlain);
			ForFiscalPrincipal ffp = alain.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant "+alain.getNumero()+" null");
			assertEquals(avantDateMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}
		{
			PersonnePhysique janine = (PersonnePhysique)tiersDAO.get(noHabJanine);
			ForFiscalPrincipal ffp = janine.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant "+janine.getNumero()+" null");
			assertEquals(avantDateMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}
		{
			PersonnePhysique julien = (PersonnePhysique)tiersDAO.get(noHabJulien);
			ForFiscalPrincipal ffp = julien.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant "+julien.getNumero()+" null");
			assertEquals(dateMajoriteJulien, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(MockCommune.Bex.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "le for n'est pas à Bex");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
		}
		{
			PersonnePhysique fanny = (PersonnePhysique)tiersDAO.get(noHabFanny);
			assertEquals(0, fanny.getForsFiscaux().size(), "Fanny a des fors fiscaux alors qu'elle est mineure");
		}
		{
			MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage "+mc.getNumero()+" null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(MockCommune.Bex.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "le for n'est pas à Bex");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		// vérification que les adresses civiles sont a Bex
		assertEquals(MockCommune.Bex.getNomMinuscule(), serviceCivilService.getAdresses(noIndAlain, dateDepartBex, false).principale.getLocalite(),
				"l'adresse principale n'est pas à Bex");
		assertEquals(MockCommune.Bex.getNomMinuscule(), serviceCivilService.getAdresses(noIndJanine, dateDepartBex, false).principale.getLocalite(),
			"l'adresse principale n'est pas à Bex");
		assertEquals(MockCommune.Bex.getNomMinuscule(), serviceCivilService.getAdresses(noIndFanny, dateDepartBex, false).principale.getLocalite(),
			"l'adresse principale n'est pas à Bex");
		assertEquals(MockCommune.Bex.getNomMinuscule(), serviceCivilService.getAdresses(noIndJulien, dateDepartBex, false).principale.getLocalite(),
			"l'adresse principale n'est pas à Bex");
	}

	@Etape(id=2, descr="Déménagement civile des individus à Orbe")
	public void etape2() throws Exception {
		addAdresseOrbe(indAlain);
		addAdresseOrbe(indJanine);
		addAdresseOrbe(indJulien);
		addAdresseOrbe(indFanny);
	}
	private void addAdresseOrbe(MockIndividu ind) {
		Collection<Adresse> adrs = ind.getAdresses();
		MockAdresse last = null;
		for (Adresse a : adrs) {
			last = (MockAdresse)a;
		}
		last.setDateFinValidite(dateDepartBex);
		Adresse aa = MockServiceCivil.newAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Orbe.RueDavall, null, dateArriveeOrbe, null);
		adrs.add(aa);
	}

	@Check(id=2, descr="Vérifie que les Habitant ont toujours leur For à Bex mais leur adresse a Orbe")
	public void check2() throws Exception {

		{
			PersonnePhysique alain = (PersonnePhysique)tiersDAO.get(noHabAlain);
			ForFiscalPrincipal ffp = alain.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant "+alain.getNumero()+" null");
			assertEquals(avantDateMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}
		{
			PersonnePhysique janine = (PersonnePhysique)tiersDAO.get(noHabJanine);
			ForFiscalPrincipal ffp = janine.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant "+janine.getNumero()+" null");
			assertEquals(avantDateMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}
		{
			PersonnePhysique julien = (PersonnePhysique)tiersDAO.get(noHabJulien);
			ForFiscalPrincipal ffp = julien.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant "+julien.getNumero()+" null");
			assertEquals(dateMajoriteJulien, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
		}
		{
			PersonnePhysique fanny = (PersonnePhysique)tiersDAO.get(noHabFanny);
			assertEquals(0, fanny.getForsFiscaux().size(), "Fanny a des fors fiscaux alors qu'elle est mineure");
		}
		{
			MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage "+mc.getNumero()+" null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		// vérification que les adresses civiles sont a Orbe
		assertEquals(MockCommune.Orbe.getNomMinuscule(), serviceCivilService.getAdresses(noIndAlain, dateArriveeOrbe, false).principale.getLocalite(),
			"l'adresse principale n'est pas à Orbe");
		assertEquals(MockCommune.Orbe.getNomMinuscule(), serviceCivilService.getAdresses(noIndJanine, dateArriveeOrbe, false).principale.getLocalite(),
			"l'adresse principale n'est pas à Orbe");
		assertEquals(MockCommune.Orbe.getNomMinuscule(), serviceCivilService.getAdresses(noIndFanny, dateArriveeOrbe, false).principale.getLocalite(),
			"l'adresse principale n'est pas à Orbe");
		assertEquals(MockCommune.Orbe.getNomMinuscule(), serviceCivilService.getAdresses(noIndJulien, dateArriveeOrbe, false).principale.getLocalite(),
			"l'adresse principale n'est pas à Orbe");
	}

	@Etape(id=3, descr="Envoi de l'événement de déménagement du père et du fils")
	public void etape3() throws Exception {

		long id1 = addEvenementCivil(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, noIndAlain, dateArriveeOrbe, communeArriveeOrbe);
		long id2 = addEvenementCivil(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, noIndJulien, dateArriveeOrbe, communeArriveeOrbe);
		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id1);
		traiteEvenements(id2);
	}

	@Check(id=3, descr="Vérifie que le père a toujours son for a Bex mais que le fils a son for a Orbe")
	public void check3() throws Exception {

		// On check que l'evenement de Alain est traité et qu'il a bien déménagé
		{
			EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabAlain);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");

		}

		{
			PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHabAlain);
			ForFiscalPrincipal ffp = hab.getDernierForFiscalPrincipal();
			assertEquals(avantDateMariage, ffp.getDateFin(), "Le for sur Villars n'est pas fermé à la bonne date");
		}

		{
			MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertEquals(dateArriveeOrbe, ffp.getDateDebut(), "Le for sur Orbe n'est pas ouvert à la bonne date");
			assertNull(ffp.getDateFin(), "Le for sur Bex est fermé");
			assertEquals(communeArriveeOrbe, ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur Orbe");
			assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement(), "Le MotifRattachement du for est faux");
			assertEquals(GenreImpot.REVENU_FORTUNE, ffp.getGenreImpot(), "Le GenreImpot du for est faux");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le ModeImposition du for est faux");
		}

		// On check que Julien a déménagé
		{
			EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabJulien);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");

			PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHabJulien);
			List<ForFiscal> list = hab.getForsFiscauxSorted();

			// For fermé sur Bex
			ForFiscalPrincipal ffpFerme = (ForFiscalPrincipal)list.get(list.size()-2);
			assertEquals(RegDate.get(2006, 4, 11), ffpFerme.getDateFin(), "Le for sur Bex n'est pas fermé à la bonne date");

			// For ouvert sur Orbe
			ForFiscalPrincipal ffpOuvert = (ForFiscalPrincipal)list.get(list.size()-1);
			assertEquals(RegDate.get(2006, 4, 12), ffpOuvert.getDateDebut(), "Le for sur Orbe n'est pas ouvert à la bonne date");
			assertEquals(communeArriveeOrbe, ffpOuvert.getNumeroOfsAutoriteFiscale(), "Le for ouvert n'est pas sur Orbe");
			assertEquals(MotifRattachement.DOMICILE, ffpOuvert.getMotifRattachement(), "Le MotifRattachement du for est faux");
			assertEquals(GenreImpot.REVENU_FORTUNE, ffpOuvert.getGenreImpot(), "Le GenreImpot du for est faux");
			assertEquals(ModeImposition.ORDINAIRE, ffpOuvert.getModeImposition(), "Le ModeImposition du for est faux");
		}
	}

	@Etape(id=4, descr="Envoi de l'événement de déménagement de la fille")
	public void etape4() throws Exception {

		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, noIndFanny, dateArriveeOrbe, communeArriveeOrbe);
		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id);
	}

	@Check(id=4, descr="Vérifie que la fille a déménagé, mais n'a pas de fors")
	public void check4() throws Exception {

		// On check que Fanny n'a tjrs pas de fors
		{
			EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabFanny);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");

			PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHabFanny);
			List<ForFiscal> list = hab.getForsFiscauxSorted();
			assertEquals(0, list.size(), "Fanny a des fors alors qu'elle est mineure");
		}
	}

	@Etape(id=5, descr="Envoi de l'événement de déménagement de la mère")
	public void etape5() throws Exception {

		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, noIndJanine, dateArriveeOrbe, communeArriveeOrbe);
		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id);
	}

	@Check(id=5, descr="Vérifie que tout le monde a déménagé")
	public void check5() throws Exception {

		// Check que tous les evt sont OK
		{
			List<EvenementCivilExterne> list = evtExterneDAO.getAll();
			for (EvenementCivilExterne evt : list) {
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");
			}
		}

		{
			PersonnePhysique alain = (PersonnePhysique)tiersDAO.get(noHabAlain);
			ForFiscalPrincipal ffp = alain.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant "+alain.getNumero()+" null");
			assertEquals(avantDateMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}
		{
			PersonnePhysique janine = (PersonnePhysique)tiersDAO.get(noHabJanine);
			ForFiscalPrincipal ffp = janine.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant "+janine.getNumero()+" null");
			assertEquals(avantDateMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}
		{
			PersonnePhysique julien = (PersonnePhysique)tiersDAO.get(noHabJulien);
			ForFiscalPrincipal ffp = julien.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant "+julien.getNumero()+" null");
			assertEquals(dateArriveeOrbe, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
		}
		{
			PersonnePhysique fanny = (PersonnePhysique)tiersDAO.get(noHabFanny);
			assertEquals(0, fanny.getForsFiscaux().size(), "Fanny a des fors fiscaux alors qu'elle est mineure");
		}
		{
			MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			assertEquals(2, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			List<ForFiscal> fors = mc.getForsFiscauxSorted();

			ForFiscalPrincipal ffpFerme = (ForFiscalPrincipal)fors.get(0);
			assertNotNull(ffpFerme, "For principal du Ménage "+mc.getNumero()+" null");
			assertEquals(communeDepartBex, ffpFerme.getNumeroOfsAutoriteFiscale(), "Commune du for fausse");
			assertEquals(dateMariage, ffpFerme.getDateDebut(), "Date de début du for fausse");
			assertEquals(dateDepartBex, ffpFerme.getDateFin(), "Date de fin du for fausse");

			ForFiscalPrincipal ffpOuvert = (ForFiscalPrincipal)fors.get(1);
			assertNotNull(ffpOuvert, "For principal du Ménage "+mc.getNumero()+" null");
			assertEquals(communeArriveeOrbe, ffpOuvert.getNumeroOfsAutoriteFiscale(), "Date de début du dernier for fausse");
			assertEquals(dateArriveeOrbe, ffpOuvert.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffpOuvert.getDateFin(), "Dernier for fermé");
		}
	}


}
