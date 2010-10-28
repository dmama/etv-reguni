package ch.vd.uniregctb.norentes.civil.arrivee;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementCivilData;
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
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class Ec_18000_17_Arrivee_JIRA1677_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_18000_17_Arrivee_JIRA1677";

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
		return "Contrôle de la date d'ouverture du rapport d'appartenance ménage créé lors de l'arrivée d'un couple sur le sol cantonal";
	}

	private final long noIndOlivier = 960713;
	private final long noIndAlexandra = 960714;

	private MockIndividu indOlivier;
	private MockIndividu indAlexandra;

	private long noCtbOlivier;
	private long noCtbMenage;

	private final RegDate dateMariage = date(2009, 9, 25);
	private final RegDate dateArriveeLausanne = date(2009, 9, 28);

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

				addAdresse(indOlivier, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateArriveeLausanne, null);
				addAdresse(indAlexandra, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateArriveeLausanne, null);

				addNationalite(indOlivier, MockPays.Suisse, naissanceOlivier, null, 1);
				addNationalite(indAlexandra, MockPays.Suisse, naissanceAlex, null, 1);

				marieIndividus(indOlivier, indAlexandra, dateMariage);
			}
		});
	}

	@Etape(id = 1, descr = "Création du couple marié seul non habitant propriétaire d'immeuble")
	public void etape1() throws Exception {
		final PersonnePhysique pp = addNonHabitant("Bouchet", "Olivier", RegDate.get(1969, 1, 18), Sexe.MASCULIN);
		final EnsembleTiersCouple couple = tiersService.createEnsembleTiersCouple(pp, null, dateMariage, null);
		final MenageCommun mc = couple.getMenage();
		assertNotNull(mc, "Erreur lors de la création du ménage commun");

		addForFiscalPrincipal(mc, MockCommune.Bern, dateMariage, null, MotifFor.ACHAT_IMMOBILIER, null);
		addForFiscalSecondaire(mc, MockCommune.Lausanne.getNoOFSEtendu(), dateMariage, null);

		noCtbOlivier = pp.getNumero();
		noCtbMenage = mc.getNumero();
	}

	@Check(id = 1, descr = "Vérification de l'existence des fors")
	public void check1() throws Exception {
		final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noCtbOlivier);
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
		assertNotNull(couple, "Pas de couple trouvé à la date de mariage");
		assertNotNull(couple.getMenage(), "Pas de ménage dans le couple?");
		assertNull(couple.getConjoint(), "Il devrait être marié seul");
		assertEquals(false, pp.isHabitantVD(), "Olivier n'est pas habitant!");
		assertNull(pp.getNumeroIndividu(), "Olivier n'est pas encore connu du registre civil!");

		final MenageCommun mc = couple.getMenage();
		final ForFiscalPrincipal ffp = mc.getForFiscalPrincipalAt(dateMariage);
		assertNotNull(ffp, "Pas de for principal?");
		assertEquals(dateMariage, ffp.getDateDebut(), "Mauvaise date de début pour le for principal");

		final AppartenanceMenage am = (AppartenanceMenage) pp.getRapportSujetValidAt(dateMariage, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		assertNotNull(am, "Pas de rapport d'appartenance ménage à la date du mariage");
		assertEquals(dateMariage, am.getDateDebut(), "Mauvaise date de début pour le rapport d'appartenance ménage");
		assertEquals(mc.getNumero(), am.getObjetId(), "Mauvais ménage commun de l'autre côté du rapport d'appartenance ménage");
	}


	@Etape(id = 2, descr = "Envoi de l'événement d'arrivée de Monsieur et Madame")
	public void etape2() throws Exception {

		final long idEvtOlivier = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndOlivier, dateArriveeLausanne, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		globalIndexer.sync();
		traiteEvenements(idEvtOlivier);

		final long idEvtAlexandra = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndAlexandra, dateArriveeLausanne, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		globalIndexer.sync();
		traiteEvenements(idEvtAlexandra);
	}

	@Check(id = 2, descr = "Vérification de l'existence d'un ménage commun dont la date d'ouverture est la date de mariage (et pas la date d'arrivée)")
	public void check2() throws Exception {

		final PersonnePhysique alex = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndAlexandra);
		assertNotNull(alex, "L'habitante Alexandra n'a pas été créée");
		assertTrue(alex.isHabitantVD(), "Alexandra devrait être habitante!");

		final PersonnePhysique olivier = (PersonnePhysique) tiersDAO.get(noCtbOlivier);
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(olivier, dateMariage);
		assertNotNull(couple, "Pas de couple trouvé à la date de mariage");
		assertNotNull(couple.getMenage(), "Pas de ménage dans le couple?");
		assertNotNull(couple.getConjoint(), "Madame est maintenant connue!");
		assertEquals(alex.getNumero(), couple.getConjoint().getNumero(), "Pas marié avec Madame?");
		assertTrue(olivier.isHabitantVD(), "Olivier est maintenant arrivé!");
		assertEquals(noIndOlivier, olivier.getNumeroIndividu(), "Olivier est maintenant connu du registre civil!");

		final EvenementCivilData evenementOlivier = getEvenementCivilRegoupeForHabitant(olivier.getNumero());
		assertNotNull(evenementOlivier, "Où est l'événement civil d'arrivée d'Olivier ?");
		assertEquals(EtatEvenementCivil.TRAITE, evenementOlivier.getEtat(), "L'événement civil devrait être en traité.");

		final EvenementCivilData evenementAlex = getEvenementCivilRegoupeForHabitant(alex.getNumero());
		assertNotNull(evenementAlex, "Où est l'événement civil d'arrivée d'Alexandra ?");
		assertEquals(EtatEvenementCivil.TRAITE, evenementAlex.getEtat(), "L'événement civil devrait être en traité.");

		final MenageCommun mc = couple.getMenage();
		final ForFiscalPrincipal ffp = mc.getForFiscalPrincipalAt(dateMariage);
		assertNotNull(ffp, "Pas de for principal?");
		assertEquals(dateMariage, ffp.getDateDebut(), "Mauvaise date de début pour le for principal");

		final AppartenanceMenage amOlivier = (AppartenanceMenage) olivier.getRapportSujetValidAt(dateMariage, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		assertNotNull(amOlivier, "Pas de rapport d'appartenance ménage à la date du mariage");
		assertEquals(dateMariage, amOlivier.getDateDebut(), "Mauvaise date de début pour le rapport d'appartenance ménage");
		assertEquals(mc.getNumero(), amOlivier.getObjetId(), "Mauvais ménage commun de l'autre côté du rapport d'appartenance ménage");

		final AppartenanceMenage amAlex = (AppartenanceMenage) alex.getRapportSujetValidAt(dateMariage, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
		assertNotNull(amAlex, "Pas de rapport d'appartenance ménage à la date du mariage");
		assertEquals(dateMariage, amAlex.getDateDebut(), "Mauvaise date de début pour le rapport d'appartenance ménage");
		assertEquals(mc.getNumero(), amAlex.getObjetId(), "Mauvais ménage commun de l'autre côté du rapport d'appartenance ménage");
	}
}
