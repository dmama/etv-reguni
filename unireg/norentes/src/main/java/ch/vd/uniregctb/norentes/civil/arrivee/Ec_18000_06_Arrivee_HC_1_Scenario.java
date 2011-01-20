package ch.vd.uniregctb.norentes.civil.arrivee;

import java.util.List;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_18000_06_Arrivee_HC_1_Scenario extends EvenementCivilScenario {

	public static final String NAME = "18000_06_Arrivee_HC";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Arrivée hors canton d'un individu sur une fraction de commune.";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_DANS_COMMUNE;
	}

	private final long noIndAntoine = 122456L;

	private MockIndividu indAntoine;

	private final int communeArriveeLeSentier = MockCommune.LeChenit.getNoOFS();
	private final RegDate dateArriveeZurich = RegDate.get(1974, 3, 3);
	private final RegDate dateNaissance = RegDate.get(1952, 2, 21);
	private final RegDate dateArriveeLeSentier = RegDate.get(2008, 4, 27);

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				indAntoine = addIndividu(noIndAntoine,dateNaissance , "Lenormand", "Antoine", true);
				setNationalite(indAntoine, dateNaissance, null, MockPays.Suisse);
				addAdresse(indAntoine, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, dateArriveeZurich, dateArriveeLeSentier.getOneDayBefore());
				addAdresse(indAntoine, TypeAdresseCivil.PRINCIPALE, MockRue.LeSentier.GrandRue, null, dateArriveeLeSentier, null);
			}
		});
	}

	@Etape(id = 1, descr = "Déménagement civil d'Antoine Sur Le Sentier")
	public void etape1() throws Exception {

	}

	@Check(id = 1, descr = "Vérifie qu'Antoine est inconnue dans le registre fiscal")
	public void check1() throws Exception {

		{
			PersonnePhysique antoine = tiersDAO.getHabitantByNumeroIndividu(noIndAntoine);
			assertNull(antoine, "Atoine ne devrait pas être dans le registre");

		}

		// vérification que les adresses civiles sont sur le sentier
		assertEquals(MockCommune.Fraction.LeSentier.getNomMinuscule(), serviceCivilService.getAdresses(noIndAntoine, dateArriveeLeSentier, false).principale.getLocalite(),
			"l'adresse principale n'est pas sur le Sentier");
	}

	@Etape(id = 2, descr = "Envoi de l'événement d'arrivée d'Antoine")
	public void etape2() throws Exception {

		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndAntoine, dateArriveeLeSentier, communeArriveeLeSentier);

		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id);
		//commitAndStartTransaction();
	}

	@Check(id = 2, descr = "Vérifie que l'evenement d'arrivée est au statut a verifier et qu'Antoine a un for ouvert sur ")
	public void check2() throws Exception {

		PersonnePhysique habAntoine = tiersDAO.getHabitantByNumeroIndividu(noIndAntoine);
		assertNotNull(habAntoine,"Le tiers Antoine n'as pas été créé dans le registre fiscal");
		{
			List<EvenementCivilData> list = evtDAO.getAll();
			for (EvenementCivilData evt : list) {
				assertEquals(EtatEvenementCivil.A_VERIFIER, evt.getEtat(), "");
			}
		}

		ForFiscalPrincipal ffp = habAntoine.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'Habitant " + habAntoine.getNumero() + " null");
		assertEquals(dateArriveeLeSentier, ffp.getDateDebut(), "Date de début du for sur Le Sentier fausse");

	}


}
