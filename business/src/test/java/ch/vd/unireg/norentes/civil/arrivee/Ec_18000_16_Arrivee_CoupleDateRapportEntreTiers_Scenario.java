package ch.vd.unireg.norentes.civil.arrivee;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

public class Ec_18000_16_Arrivee_CoupleDateRapportEntreTiers_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_18000_16_Arrivee_CoupleDateRapportEntreTiers";

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

	private static final long noIndAntonio = 250797;
	private static final long noIndAnneLaure = 250798;

	private MockIndividu indAntonio;
	private MockIndividu indAnneLaure;

	private final RegDate dateMariage = date(1995, 1, 6);
	private final RegDate dateArriveeLausanne = date(2009, 6, 1);

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				indAntonio = addIndividu(noIndAntonio, date(1976, 4, 25) , "Lauria", "Antonio", true);
				indAnneLaure = addIndividu(noIndAnneLaure, date(1976, 8, 6), "Lauria", "Anne-Laure", false);

				addPermis(indAntonio, TypePermis.ETABLISSEMENT, date(2005, 1, 11), null, false);

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

		final EvenementCivilRegPP evenementAntonio = getEvenementCivilRegoupeForHabitant(antonio.getNumero());
		assertNotNull(evenementAntonio, "Où est l'événement civil d'arrivée d'Antonio ?");
		assertEquals(EtatEvenementCivil.TRAITE, evenementAntonio.getEtat(), "L'événement civil devrait être en traité.");

		final EvenementCivilRegPP evenementAnneLaure = getEvenementCivilRegoupeForHabitant(anneLaure.getNumero());
		assertNotNull(evenementAnneLaure, "Où est l'événement civil d'arrivée d'Anne-Laure ?");
		assertEquals(EtatEvenementCivil.REDONDANT, evenementAnneLaure.getEtat(), "L'événement civil devrait être considéré comme redondant.");

		// recherche du couple créé pour ces habitant
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(antonio, dateMariage);
		assertNotNull(ensemble, "Pas de couple existant à la date du mariage ?");

		final PersonnePhysique conjoint = ensemble.getConjoint(antonio);
		assertNotNull(conjoint, "Antonio n'est pas marié seul!");
		assertEquals(anneLaure.getNumero(), conjoint.getNumero(), "Le conjoint d'Antonio n'est plus Anne-Laure ?");
	}
}
