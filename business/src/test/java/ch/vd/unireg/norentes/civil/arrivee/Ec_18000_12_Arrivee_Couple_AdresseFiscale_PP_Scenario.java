package ch.vd.unireg.norentes.civil.arrivee;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.AdresseSuisse;
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
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeEvenementCivil;

public class Ec_18000_12_Arrivee_Couple_AdresseFiscale_PP_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_18000_12_Arrivee_Couple_AdresseFiscale_PP";

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
		return "Arrivée d'un couple dont l'un des membre possédait une surcharge d'adresse fiscale non permanente";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	private final RegDate dateMariage = date(1970, 1, 1);
	private final RegDate dateArriveeBex = date(1980, 1, 1);
	private final RegDate dateArriveeLausanne = date(2008, 11, 13);
	private static final long noIndAntoine = 1020206L;
	private static final long noIndCleo = 1020207L;
	private long evenementId;
	private MockIndividu indAntoine;
	private MockIndividu indCleo;
	private long noHabAntoine;
	private long noHabCleo;
	private long noMenage;


	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				indAntoine = addIndividu(noIndAntoine, date(1952, 2, 21) , "Lenormand", "Antoine", true);
				indCleo = addIndividu(noIndCleo, date(1953, 1, 1), "D'Egypte", "Cléo", false);

				marieIndividus(indAntoine, indCleo, date(1972, 11, 9));

				addAdresse(indAntoine, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateArriveeBex, null);
				addAdresse(indCleo, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateArriveeBex, null);
			}
		});
	}

	@Etape(id = 1, descr = "Création d'une surcharge fiscale à l'adresse d'Antoine")
	public void etape1() throws Exception {

		final PersonnePhysique antoine = addHabitant(noIndAntoine);
		noHabAntoine = antoine.getNumero();

		final PersonnePhysique cleo = addHabitant(noIndCleo);
		noHabCleo = cleo.getNumero();

		// ménage
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun)tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, antoine, dateMariage, null);
			tiersService.addTiersToCouple(menage, cleo, dateMariage, null);

			addForFiscalPrincipal(menage, MockCommune.Bex, date(1990, 1, 1), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
		}

		// surcharge d'adresse sur un des membres du couple
		final AdresseSuisse adresse = new AdresseSuisse();
		adresse.setDateDebut(date(2005, 12, 3));
		adresse.setNumeroRue(MockRue.Vallorbe.GrandRue.getNoRue());
		adresse.setNumeroOrdrePoste(MockRue.Vallorbe.GrandRue.getNoLocalite());
		adresse.setNumeroMaison(Integer.toString(12));
		adresse.setPermanente(false);
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		antoine.addAdresseTiers(adresse);
	}

	@Check(id = 1, descr = "Vérifions que le couple a bien une adresse surchargée à Vallorbe")
	public void check1() throws Exception {
		final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
		assertNotNull(menage, "On ne retrouve plus le ménage commun!");
		final PersonnePhysique antoine = (PersonnePhysique) tiersDAO.get(noHabAntoine);
		assertNotNull(antoine, "On ne retrouve plus Antoine!");
		final PersonnePhysique cleo = (PersonnePhysique) tiersDAO.get(noHabCleo);
		assertNotNull(cleo, "On ne retrouve plus Cléo!");

		final AdresseEnvoiDetaillee adresseMenage = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, true);
		assertTrue(adresseMenage.getNpaEtLocalite().toString().contains("Vallorbe"), "L'adresse d'envoi du ménage devrait être à Vallorbe");
		final AdresseEnvoiDetaillee adresseAntoine = adresseService.getAdresseEnvoi(antoine, null, TypeAdresseFiscale.COURRIER, true);
		assertTrue(adresseAntoine.getNpaEtLocalite().toString().contains("Vallorbe"), "L'adresse d'envoi d'Antoine devrait être à Vallorbe");
		final AdresseEnvoiDetaillee adresseCleo = adresseService.getAdresseEnvoi(cleo, null, TypeAdresseFiscale.COURRIER, true);
		assertTrue(adresseCleo.getNpaEtLocalite().toString().contains("Bex"), "L'adresse d'envoi de Cléo devrait être à Bex");
	}

	@Etape(id = 2, descr = "Arrivée principale vaudoise = correction dans l'adresse civile")
	public void etape2() throws Exception {

		addNouvelleAdresse(indAntoine);
		addNouvelleAdresse(indCleo);

		final long idEvtAntoine = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, noIndAntoine, dateArriveeLausanne, MockCommune.Lausanne.getNoOFS());
		final long idEvtCleo = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, noIndCleo, dateArriveeLausanne, MockCommune.Lausanne.getNoOFS());

		commitAndStartTransaction();
		traiteEvenements(idEvtAntoine);
		traiteEvenements(idEvtCleo);

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

	@Check(id = 2, descr = "Vérifions maintenant que le couple (ainsi qu'Antoine, qui était à la source de la surcharge d'adresse) sont bien passés sur Lausanne")
	public void check2() throws Exception {
		final EvenementCivilRegPP evenement = evtExterneDAO.get(evenementId);
		assertEquals(EtatEvenementCivil.TRAITE, evenement.getEtat(), "L'événement civil devrait être en traité.");

		final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
		assertNotNull(menage, "On ne retrouve plus le ménage commun!");
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menage, null);
		assertNotNull(ensemble, "On ne retrouve plus l'ensemble couple!");
		assertNotNull(ensemble.getPrincipal(), "Pas de membre principal sur le couple!");
		assertNotNull(ensemble.getConjoint(), "Pas de conjoint sur le couple!");

		final AdresseEnvoiDetaillee adresseMenage = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, true);
		assertTrue(adresseMenage.getNpaEtLocalite().toString().contains("Lausanne"), "L'adresse d'envoi du ménage devrait être à Lausanne");
		final AdresseEnvoiDetaillee adresseAntoine = adresseService.getAdresseEnvoi(ensemble.getPrincipal(), null, TypeAdresseFiscale.COURRIER, true);
		assertTrue(adresseAntoine.getNpaEtLocalite().toString().contains("Lausanne"), "L'adresse d'envoi du principal devrait être à Lausanne");
		final AdresseEnvoiDetaillee adresseCleo = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, true);
		assertTrue(adresseCleo.getNpaEtLocalite().toString().contains("Lausanne"), "L'adresse d'envoi du conjoint devrait être à Lausanne");
	}
}
