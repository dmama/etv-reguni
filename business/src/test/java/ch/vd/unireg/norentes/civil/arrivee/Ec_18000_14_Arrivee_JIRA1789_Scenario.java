package ch.vd.unireg.norentes.civil.arrivee;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

public class Ec_18000_14_Arrivee_JIRA1789_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_18000_14_Arrivee_JIRA1789_Scenario";

	private AdresseService adresseService;

	private ServiceInfrastructureService serviceInfrastructureService;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Déménagement vaudois d'un seul conjoint avec déménagement ultérieur de l'autre vers une troisième commune";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE;
	}

	private static final long noIndAntonio = 250797;
	private static final long noIndAnneLaure = 250798;

	private MockIndividu indAntonio;
	private MockIndividu indAnneLaure;

	private long noHabAntonio;
	private long noHabAnneLaure;
	private long noMenage;

	private final RegDate dateMariage = date(1995, 1, 6);
	private final RegDate dateArriveeBex = date(2007, 7, 1);
	private final RegDate dateArriveeLausanne = date(2009, 6, 1);
	private final RegDate dateArriveeBussigny = date(2009, 10, 4);

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				indAntonio = addIndividu(noIndAntonio, date(1976, 4, 25) , "Lauria", "Antonio", true);
				indAnneLaure = addIndividu(noIndAnneLaure, date(1976, 8, 6), "Lauria", "Anne-Laure", false);

				addPermis(indAntonio, TypePermis.ETABLISSEMENT, date(2005, 1, 11), null, false);

				marieIndividus(indAntonio, indAnneLaure, dateMariage);

				addAdresse(indAntonio, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateArriveeBex, null);
				addAdresse(indAnneLaure, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateArriveeBex, null);
			}
		});
	}

	@Etape(id = 1, descr = "Chargement des habitants et du couple")
	public void etape1() throws Exception {

		final PersonnePhysique antonio = addHabitant(noIndAntonio);
		noHabAntonio = antonio.getNumero();

		final PersonnePhysique anneLaure = addHabitant(noIndAnneLaure);
		noHabAnneLaure = anneLaure.getNumero();

		// ménage
		{
			final MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, antonio, dateMariage, null);
			tiersService.addTiersToCouple(menage, anneLaure, dateMariage, null);

			addForFiscalPrincipal(menage, MockCommune.Bex, dateArriveeBex, null, MotifFor.DEMENAGEMENT_VD, null);
		}
	}

	@Check(id = 1, descr = "Vérifie que les contribuables ont bien été créés et leurs adresses sont à Bex")
	public void check1() throws Exception {
		// Antonio
		final PersonnePhysique antonio = (PersonnePhysique) tiersDAO.get(noHabAntonio);
		{
			assertNotNull(antonio, "Le non habitant Antonio n'a pas été créé");
			assertNull(antonio.getForFiscalPrincipalAt(null), "Antonio ne devrait avoir aucun for fiscal principal");
		}

		// Anne-Laure
		final PersonnePhysique anneLaure = (PersonnePhysique) tiersDAO.get(noHabAnneLaure);
		{
			assertNotNull(anneLaure, "L'habitant Anne-Laure n'a pas été créé");
			assertNull(antonio.getForFiscalPrincipalAt(null), "Anne-Laure ne devrait avoir aucun for fiscal principal");
		}

		// ménage
		final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
		{
			assertNotNull(menage, "Le ménage commun n'a pas été créé");

		}

		assertAdresses(menage, "Bex");
	}

	@Etape(id = 2, descr = "Déménagement d'Antonio à Lausanne")
	public void etape2() throws Exception {

		addNouvelleAdresse(indAntonio, MockRue.Lausanne.AvenueDeBeaulieu, dateArriveeLausanne);

		final long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, noIndAntonio, dateArriveeLausanne, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	private void addNouvelleAdresse(MockIndividu individu, MockRue nouvelleRue, RegDate dateArrivee) {

		final Collection<Adresse> adrs = individu.getAdresses();
		MockAdresse lastAdr = null;
		for (Adresse a : adrs) {
			lastAdr = (MockAdresse) a;
		}
		assertNotNull(lastAdr, "Aucune adresse connue!");
		lastAdr.setDateFinValidite(dateArrivee.getOneDayBefore());

		individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, nouvelleRue, null, dateArrivee, null));
	}

	@Check(id = 2, descr = "Vérifions maintenant que le couple est bien resté à Bex (un seul des deux conjoints a déménagé pour le moment)")
	public void check2() throws Exception {

		final EvenementCivilRegPP evenement = getEvenementCivilRegoupeForHabitant(noHabAntonio);
		assertEquals(EtatEvenementCivil.TRAITE, evenement.getEtat(), "L'événement civil devrait être en traité.");

		final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
		assertNotNull(menage, "On ne retrouve plus le ménage commun!");
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menage, null);
		assertNotNull(ensemble, "On ne retrouve plus l'ensemble couple!");
		assertNotNull(ensemble.getPrincipal(), "Pas de membre principal sur le couple!");
		assertNotNull(ensemble.getConjoint(), "Pas de conjoint sur le couple!");

		final ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "Plus de for principal sur le ménage ?");
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale(), "Mauvais type d'autorité fiscale");
		assertEquals(MockCommune.Bex.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le for du couple aurait dû rester à Bex");

		// l'adresse du couple suit le principal s'il est vaudois
		assertAdresses(menage, "Lausanne");
	}

	@Etape(id = 3, descr = "Déménagement d'Anne-Laure aussi, mais vers Bussigny")
	public void etape3() throws Exception {

		addNouvelleAdresse(indAnneLaure, MockRue.Bussigny.RueDeLIndustrie, dateArriveeBussigny);

		final long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, noIndAnneLaure, dateArriveeBussigny, MockCommune.Bussigny.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id = 3, descr = "Vérifions maintenant que le couple est bien passé sur Lausanne (les deux membres ont déménagé, et le principal est maintenant sur Lausanne)")
	public void check3() throws Exception {
		final EvenementCivilRegPP evenement = getEvenementCivilRegoupeForHabitant(noHabAnneLaure);
		assertNotNull(evenement, "Pas d'événement pour Anne-Laure?");
		assertEquals(EtatEvenementCivil.TRAITE, evenement.getEtat(), "L'événement civil devrait être en traité.");

		final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
		assertNotNull(menage, "On ne retrouve plus le ménage commun!");
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menage, null);
		assertNotNull(ensemble, "On ne retrouve plus l'ensemble couple!");
		assertNotNull(ensemble.getPrincipal(), "Pas de membre principal sur le couple!");
		assertNotNull(ensemble.getConjoint(), "Pas de conjoint sur le couple!");

		final ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "Plus de for principal sur le ménage ?");
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale(), "Mauvais type d'autorité fiscale");
		assertEquals(MockCommune.Lausanne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le for du couple aurait dû venir à Lausanne");

		assertAdresses(menage, "Lausanne");
	}

	private void assertAdresses(final MenageCommun menage, final String nomCommune) throws AdresseException {
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menage, null);
		final AdresseEnvoiDetaillee adresseMenage = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, true);
		assertTrue(adresseMenage.getNpaEtLocalite().toString().contains(nomCommune), String.format("L'adresse d'envoi du ménage devrait être à %s", nomCommune));
		final AdresseEnvoiDetaillee adresseAntoine = adresseService.getAdresseEnvoi(ensemble.getPrincipal(), null, TypeAdresseFiscale.COURRIER, true);
		assertTrue(adresseAntoine.getNpaEtLocalite().toString().contains(nomCommune), String.format("L'adresse d'envoi du principal devrait être à %s", nomCommune));
		final AdresseEnvoiDetaillee adresseCleo = adresseService.getAdresseEnvoi(menage, null, TypeAdresseFiscale.COURRIER, true);
		assertTrue(adresseCleo.getNpaEtLocalite().toString().contains(nomCommune), String.format("L'adresse d'envoi du conjoint devrait être à %s", nomCommune));
	}
}
