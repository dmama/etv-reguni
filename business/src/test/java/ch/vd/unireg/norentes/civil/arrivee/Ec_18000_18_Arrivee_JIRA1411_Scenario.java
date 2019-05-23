package ch.vd.unireg.norentes.civil.arrivee;

import java.util.Collection;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.adresse.AdressesFiscalesHisto;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeEvenementCivil;

public class Ec_18000_18_Arrivee_JIRA1411_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_18000_18_Arrivee";

	private AdresseService adresseService;

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
		return "Arrivée d'un individu avec surcharge fiscale";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	private final RegDate dateArriveeLausanne = date(2009, 7, 15);
	private static final long noIndAntoine = 1020206L;
	private long evenementId;
	private MockIndividu indAntoine;
	private long noHabAntoine;


	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				indAntoine = addIndividu(noIndAntoine, date(1952, 2, 21) , "Lenormand", "Antoine", true);
				addAdresse(indAntoine, TypeAdresseCivil.COURRIER, MockRue.Bex.CheminDeLaForet, null, null, null);
				addAdresse(indAntoine, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, null, null);
			}
		});
	}

	@Etape(id = 1, descr = "Création d'une surcharge fiscale courrier")
	public void etape1() throws Exception {

		final PersonnePhysique antoine = addHabitant(noIndAntoine);
		noHabAntoine = antoine.getNumero();

		addForFiscalPrincipal(antoine, MockCommune.Bex, date(2000, 1, 1), null, MotifFor.INDETERMINE, null);

		// surcharge d'adresse permanente
		final AdresseSuisse adresse = new AdresseSuisse();
		adresse.setDateDebut(date(2009, 7, 8));
		adresse.setNumeroRue(MockRue.Vallorbe.GrandRue.getNoRue());
		adresse.setNumeroOrdrePoste(MockRue.Vallorbe.GrandRue.getNoLocalite());
		adresse.setNumeroMaison(Integer.toString(12));
		adresse.setPermanente(true);
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		antoine.addAdresseTiers(adresse);
	}

	@Check(id = 1, descr = "Vérifions que le couple a bien une adresse surchargée à Vallorbe")
	public void check1() throws Exception {
		final PersonnePhysique antoine = (PersonnePhysique) tiersDAO.get(noHabAntoine);
		assertNotNull(antoine, "On ne retrouve plus Antoine!");

		final AdresseEnvoiDetaillee adresseCourrier = adresseService.getAdresseEnvoi(antoine, null, TypeAdresseFiscale.COURRIER, true);
		assertTrue(adresseCourrier.getNpaEtLocalite().toString().contains("Vallorbe"), "L'adresse courrier devrait être à Vallorbe");

		final AdresseEnvoiDetaillee adresseDomicile = adresseService.getAdresseEnvoi(antoine, null, TypeAdresseFiscale.DOMICILE, true);
		assertTrue(adresseDomicile.getNpaEtLocalite().toString().contains("Bex"), "L'adresse domicile devrait être à Bex");
	}

	@Etape(id = 2, descr = "Arrivée principale vaudoise = correction dans l'adresse civile")
	public void etape2() throws Exception {

		addNouvelleAdresse(indAntoine);

		final long idEvtAntoine = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, noIndAntoine, dateArriveeLausanne, MockCommune.Lausanne.getNoOFS());

		commitAndStartTransaction();
		traiteEvenements(idEvtAntoine);

		evenementId = idEvtAntoine;
	}

	private void addNouvelleAdresse(MockIndividu individu) {
		final Collection<Adresse> adrs = individu.getAdresses();
		MockAdresse lastAdr = null;
		for (Adresse a : adrs) {
			lastAdr = (MockAdresse) a;
		}
		assertNotNull(lastAdr, "Aucune adresse connue!");
		lastAdr.setDateFinValidite(dateArriveeLausanne.getOneDayBefore());

		individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveeLausanne, null));
	}

	@Check(id = 2, descr = "Vérifions maintenant que la surcharge fiscale n'est pas effacée et que l'historique des adresses est correct")
	public void check2() throws Exception {

		final EvenementCivilRegPP evenement = evtExterneDAO.get(evenementId);
		assertEquals(EtatEvenementCivil.TRAITE, evenement.getEtat(), "L'événement civil devrait être en traité.");

		final PersonnePhysique antoine = (PersonnePhysique) tiersDAO.get(noHabAntoine);
		assertNotNull(antoine, "Où est passé Antoine?");

		// surcharge toujours permanente et pas fermée?
		{
			final List<AdresseTiers> adressesFiscales = antoine.getAdressesTiersSorted();
			assertNotNull(adressesFiscales, "Pas de trace de surcharges fiscales?");
			assertEquals(1, adressesFiscales.size(), "Il ne devrait y avoir qu'une seule surcharge fiscale");

			final AdresseTiers surcharge = adressesFiscales.get(0);
			assertNotNull(surcharge, "Surcharge nulle?");
			assertEquals(AdresseSuisse.class, surcharge.getClass(), "Mauvaise classe d'adresse");

			final AdresseSuisse surchargeSuisse = (AdresseSuisse) surcharge;
			assertTrue(surchargeSuisse.isPermanente(), "La surcharge a perdu son côté permanent!");
			assertNull(surchargeSuisse.getDateFin(), "La surcharge a été fermée!");
		}

		// on obtient la source de la liste affichée dans l'IHM
		final AdressesFiscalesHisto histo = adresseService.getAdressesFiscalHisto(antoine, false);

		// domicile
		{
			final List<AdresseGenerique> histoDomicile = histo.domicile;
			assertNotNull(histoDomicile, "Pas d'historique des adresses de domicile?");
			assertEquals(2, histoDomicile.size(), "Il devrait y avoir 2 adresses de domicile connues (une avant et une après le déménagement)");

			final AdresseGenerique domicileAvant = histoDomicile.get(0);
			assertNull(domicileAvant.getDateDebut(), "Qui a trouvé une date de début à l'adresse avant déménagement?");
			assertTrue(domicileAvant.getLocalite().contains("Bex"), "L'adresse de domicile avant déménagement devrait être à Bex");
			assertEquals(AdresseGenerique.SourceType.CIVILE_PERS, domicileAvant.getSource().getType(), "Adresse de domicile d'une autre source?");

			final AdresseGenerique domicileApres = histoDomicile.get(1);
			assertEquals(dateArriveeLausanne, domicileApres.getDateDebut(), "Mauvaise date de début");
			assertNull(domicileApres.getDateFin(), "Qui a trouvé une date de fin à l'adresse après déménagement?");
			assertTrue(domicileApres.getLocalite().contains("Lausanne"), "L'adresse de domicile avant déménagement devrait être à Lausanne");
			assertEquals(AdresseGenerique.SourceType.CIVILE_PERS, domicileApres.getSource().getType(), "Adresse de domicile d'une autre source?");
		}

		// courrier
		{
			final List<AdresseGenerique> histoCourrier = histo.courrier;
			assertNotNull(histoCourrier, "Pas d'historique des adresses de courrier?");
			assertEquals(2, histoCourrier.size(), "Il devrait y avoir 2 adresses de courrier connue (avant la surcharge, et après)");

			final AdresseGenerique courrierAvant = histoCourrier.get(0);
			assertNull(courrierAvant.getDateDebut(), "Qui a trouvé une date de début à l'adresse de courrier d'avant la surcharge?");
			assertEquals(date(2009, 7, 7), courrierAvant.getDateFin(), "Mauvaise date de fin");
			assertTrue(courrierAvant.getLocalite().contains("Bex"), "L'adresse de domicile avant déménagement devrait être à Bex");
			assertEquals(AdresseGenerique.SourceType.CIVILE_PERS, courrierAvant.getSource().getType(), "Adresse de courier d'une autre source?");

			final AdresseGenerique courrierSurcharge = histoCourrier.get(1);
			assertNull(courrierSurcharge.getDateFin(), "Qui a trouvé une date de fin à l'adresse de courrier surchargée?");
			assertEquals(date(2009, 7, 8), courrierSurcharge.getDateDebut(), "Mauvaise date de début de la surcharge");
			assertTrue(courrierSurcharge.getLocalite().contains("Vallorbe"), "L'adresse de courrier surchargée est à Vallorbe");
			assertEquals(AdresseGenerique.SourceType.FISCALE, courrierSurcharge.getSource().getType(), "Adresse de courier surchargée d'une autre source?");
		}
	}
}
