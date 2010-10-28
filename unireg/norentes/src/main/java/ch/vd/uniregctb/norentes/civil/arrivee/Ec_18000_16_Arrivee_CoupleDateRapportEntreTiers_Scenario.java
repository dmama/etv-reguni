package ch.vd.uniregctb.norentes.civil.arrivee;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_18000_16_Arrivee_CoupleDateRapportEntreTiers_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_18000_16_Arrivee_CoupleDateRapportEntreTiers";

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

	private final long noIndAntonio = 250797;
	private final long noIndAnneLaure = 250798;

	private MockIndividu indAntonio;
	private MockIndividu indAnneLaure;

	private final RegDate dateMariage = date(1995, 1, 6);
	private final RegDate dateArriveeLausanne = date(2009, 6, 1);

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil(serviceInfrastructureService) {
			@Override
			protected void init() {
				indAntonio = addIndividu(noIndAntonio, date(1976, 4, 25) , "Lauria", "Antonio", true);
				indAnneLaure = addIndividu(noIndAnneLaure, date(1976, 8, 6), "Lauria", "Anne-Laure", false);

				addPermis(indAntonio, EnumTypePermis.ETABLLISSEMENT, date(2005, 1, 11), null, 1, false);

				marieIndividus(indAntonio, indAnneLaure, dateMariage);
				addAdresse(indAntonio, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateArriveeLausanne, null);
				addAdresse(indAnneLaure, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateArriveeLausanne, null);
			}
		});
	}

	@Etape(id = 1, descr = "Envoi de l'événement d'arrivée")
	public void etape1() throws Exception {
		final long idAntonio = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndAntonio, dateArriveeLausanne, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(idAntonio);

		final long idAnneLaure = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndAnneLaure, dateArriveeLausanne, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(idAnneLaure);
	}

	@Check(id = 1, descr = "Vérification de l'existence d'un ménage commun dont la date d'ouverture est la date de mariage (et pas la date d'arrivée)")
	public void check1() throws Exception {
		
		final PersonnePhysique antonio = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndAntonio);
		assertNotNull(antonio, "L'habitant Antonio n'a pas été créé ?");

		final PersonnePhysique anneLaure = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndAnneLaure);
		assertNotNull(anneLaure, "L'habitant Anne-Laure n'a pas été créé ?");

		final EvenementCivilData evenementAntonio = getEvenementCivilRegoupeForHabitant(antonio.getNumero());
		assertNotNull(evenementAntonio, "Où est l'événement civil d'arrivée d'Antonio ?");
		assertEquals(EtatEvenementCivil.TRAITE, evenementAntonio.getEtat(), "L'événement civil devrait être en traité.");

		final EvenementCivilData evenementAnneLaure = getEvenementCivilRegoupeForHabitant(anneLaure.getNumero());
		assertNotNull(evenementAnneLaure, "Où est l'événement civil d'arrivée d'Anne-Laure ?");
		assertEquals(EtatEvenementCivil.TRAITE, evenementAnneLaure.getEtat(), "L'événement civil devrait être en traité.");

		// recherche du couple créé pour ces habitant
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(antonio, dateMariage);
		assertNotNull(ensemble, "Pas de couple existant à la date du mariage ?");

		final PersonnePhysique conjoint = ensemble.getConjoint(antonio);
		assertNotNull(conjoint, "Antonio n'est pas marié seul!");
		assertEquals(anneLaure.getNumero(), conjoint.getNumero(), "Le conjoint d'Antonio n'est plus Anne-Laure ?");
	}
}
