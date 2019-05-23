package ch.vd.unireg.norentes.civil.arrivee;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypeRapportEntreTiers;

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

	private static final long noIndOlivier = 960713;
	private static final long noIndAlexandra = 960714;

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
		serviceCivilService.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final RegDate naissanceOlivier = date(1969, 1, 18);
				final RegDate naissanceAlex = date(1972, 5, 12);

				indOlivier = addIndividu(noIndOlivier, naissanceOlivier, "Bouchet", "Olivier", true);
				indAlexandra = addIndividu(noIndAlexandra, naissanceAlex, "Bouchet", "Alexandra", false);

				addAdresse(indOlivier, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateArriveeMonsieur, null);
				addAdresse(indAlexandra, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateArriveeMadame, null);

				addNationalite(indOlivier, MockPays.Suisse, naissanceOlivier, null);
				addNationalite(indAlexandra, MockPays.Suisse, naissanceAlex, null);

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

	@Check(id = 2, descr = "Vérification de l'absence de création de contribuable pour Olivier")
	public void check2() throws Exception {

		final PersonnePhysique olivier = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndOlivier);
		assertNull(olivier, "L'habitant Olivier a été créé par erreur");

	}
}
