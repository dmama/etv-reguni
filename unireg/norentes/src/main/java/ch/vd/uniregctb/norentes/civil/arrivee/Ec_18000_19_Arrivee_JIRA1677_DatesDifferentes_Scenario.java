package ch.vd.uniregctb.norentes.civil.arrivee;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class Ec_18000_19_Arrivee_JIRA1677_DatesDifferentes_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_18000_19_Arrivee";

	private ServiceInfrastructureService serviceInfrastructureService;

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Contrôle de la date d'ouverture du rapport d'appartenance ménage créé lors de l'arrivée d'un couple (à deux dates différentes) sur le sol cantonal";
	}

	private final long noIndOlivier = 960713;
	private final long noIndAlexandra = 960714;

	private MockIndividu indOlivier;
	private MockIndividu indAlexandra;

	private long noCtbAlex;

	private final RegDate dateMariage = date(1995, 9, 25);
	private final RegDate dateArriveeMadame = date(2007, 3, 1);
	private final RegDate dateArriveeMonsieur = date(2007, 3, 5);
	private final RegDate dateDebutForExistantSurCouple = date(2009, 6, 2);

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil(serviceInfrastructureService) {
			@Override
			protected void init() {
				final RegDate naissanceOlivier = date(1969, 1, 18);
				final RegDate naissanceAlex = date(1972, 5, 12);

				indOlivier = addIndividu(noIndOlivier, naissanceOlivier, "Bouchet", "Olivier", true);
				indAlexandra = addIndividu(noIndAlexandra, naissanceAlex, "Bouchet", "Alexandra", false);

				addAdresse(indOlivier, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateArriveeMonsieur, null);
				addAdresse(indAlexandra, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateArriveeMadame, null);

				addNationalite(indOlivier, MockPays.Suisse, naissanceOlivier, null, 1);
				addNationalite(indAlexandra, MockPays.Suisse, naissanceAlex, null, 1);

				marieIndividus(indOlivier, indAlexandra, dateMariage);
			}
		});
	}

	@Etape(id = 1, descr = "Création du couple marié seul habitant (Madame, date de début d'appartenance ménage = arrivée de madame) avec for débutant encore plus tard sur le couple")
	public void etape1() throws Exception {
		final PersonnePhysique pp = addHabitant(noIndAlexandra);
		final EnsembleTiersCouple couple = tiersService.createEnsembleTiersCouple(pp, null, dateArriveeMadame, null);
		final MenageCommun mc = couple.getMenage();
		assertNotNull(mc, "Erreur lors de la création du ménage commun");

		addForFiscalPrincipal(mc, MockCommune.Lausanne, dateDebutForExistantSurCouple, null, MotifFor.PERMIS_C_SUISSE, null);

		noCtbAlex = pp.getNumero();
	}

	@Check(id = 1, descr = "Vérification de l'existence des fors")
	public void check1() throws Exception {
		final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noCtbAlex);
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateArriveeMadame);
		assertNotNull(couple, "Pas de couple trouvé à la date d'arrivée de Madame");
		assertNotNull(couple.getMenage(), "Pas de ménage dans le couple?");
		assertNull(couple.getConjoint(), "Il devrait être marié seul");
		assertEquals(true, pp.isHabitantVD(), "Alex est habitante!");

		final MenageCommun mc = couple.getMenage();
		final ForFiscalPrincipal ffpDateMariage = mc.getForFiscalPrincipalAt(dateMariage);
		assertNull(ffpDateMariage, "Le for du ménage commun ne débute pas au mariage, dans ce cas d'espèce");
		final ForFiscalPrincipal ffp = mc.getForFiscalPrincipalAt(dateDebutForExistantSurCouple);
		assertNotNull(ffp, "Le for du ménage commun est attendu au " + dateDebutForExistantSurCouple);
		assertEquals(dateDebutForExistantSurCouple, ffp.getDateDebut(), "Mauvaise date de début pour le for principal");

		final AppartenanceMenage amDateMariage = (AppartenanceMenage) pp.getRapportSujetValidAt(dateMariage, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		assertNull(amDateMariage, "Rapport d'appartenance ménage ouvert trop tôt par rapport aux données du test!");

		final AppartenanceMenage am = (AppartenanceMenage) pp.getRapportSujetValidAt(dateArriveeMadame, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		assertNotNull(am, "Pas de rapport d'appartenance ménage à la date d'arrivée de Madame");
		assertEquals(dateArriveeMadame, am.getDateDebut(), "Mauvaise date de début pour le rapport d'appartenance ménage");
		assertEquals(mc.getNumero(), am.getObjetId(), "Mauvais ménage commun de l'autre côté du rapport d'appartenance ménage");
	}


	@Etape(id = 2, descr = "Envoi de l'événement d'arrivée de Monsieur")
	public void etape2() throws Exception {

		final long idEvtOlivier = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndOlivier, dateArriveeMonsieur, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(idEvtOlivier);
	}

	@Check(id = 2, descr = "Vérification de l'existence d'un ménage commun dont la date d'ouverture est la date de mariage (et pas la date d'arrivée)")
	public void check2() throws Exception {

		final PersonnePhysique olivier = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndOlivier);
		assertNotNull(olivier, "L'habitant Olivier n'a pas été créé");
		assertTrue(olivier.isHabitantVD(), "Olivier devrait être habitant!");

		final PersonnePhysique alex = (PersonnePhysique) tiersDAO.get(noCtbAlex);
		assertNotNull(alex, "L'habitante Alexandra n'existe plus ?");
		assertTrue(alex.isHabitantVD(), "Alexandra devrait être habitante!");

		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(olivier, dateArriveeMonsieur);
		assertNotNull(couple, "Pas de couple trouvé à la date d'arrivée de monsieur");
		assertNotNull(couple.getMenage(), "Pas de ménage dans le couple?");
		assertNotNull(couple.getConjoint(), "Le couple n'a pas été reconstitué ?");
		assertEquals(alex.getNumero(), couple.getConjoint().getNumero(), "Le couple n'a pas été reconstitué entre les bons contribuable");

		final EvenementCivilExterne evenementOlivier = getEvenementCivilRegoupeForHabitant(olivier.getNumero());
		assertNotNull(evenementOlivier, "Où est l'événement civil d'arrivée d'Olivier ?");
		assertEquals(EtatEvenementCivil.TRAITE, evenementOlivier.getEtat(), "L'événement civil devrait être en traité.");

		final MenageCommun mc = couple.getMenage();
		final ForFiscalPrincipal ffpDateMariage = mc.getForFiscalPrincipalAt(dateMariage);
		assertNull(ffpDateMariage, "Le for du ménage commun ne débute pas au mariage, dans ce cas d'espèce");
		final ForFiscalPrincipal ffp = mc.getForFiscalPrincipalAt(dateDebutForExistantSurCouple);
		assertNotNull(ffp, "Le for du ménage commun est attendu au " + dateDebutForExistantSurCouple);
		assertEquals(dateDebutForExistantSurCouple, ffp.getDateDebut(), "Mauvaise date de début pour le for principal");

		final AppartenanceMenage amAlex = (AppartenanceMenage) alex.getRapportSujetValidAt(dateArriveeMadame, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		assertNotNull(amAlex, "Pas de rapport d'appartenance ménage à la date d'arrivée");
		assertEquals(dateArriveeMadame, amAlex.getDateDebut(), "Mauvaise date de début pour le rapport d'appartenance ménage");
		assertEquals(mc.getNumero(), amAlex.getObjetId(), "Mauvais ménage commun de l'autre côté du rapport d'appartenance ménage");

		// comme il s'agit d'un rajout à un marié seul, la date du début du rapport entre tiers créé pour monsieur
		// doit être la même que celle utilisée pour madame, même si ce n'est pas la date du mariage
		final AppartenanceMenage amOlivier = (AppartenanceMenage) olivier.getRapportSujetValidAt(dateArriveeMonsieur, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		assertNotNull(amOlivier, "Pas de rapport d'appartenance ménage à la date d'arrivée");
		assertEquals(dateArriveeMadame, amOlivier.getDateDebut(), "Mauvaise date de début pour le rapport d'appartenance ménage");
		assertEquals(mc.getNumero(), amOlivier.getObjetId(), "Mauvais ménage commun de l'autre côté du rapport d'appartenance ménage");
	}
}
