package ch.vd.uniregctb.norentes.civil.arrivee;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_18000_02_Arrivee_HC_1_Scenario extends EvenementCivilScenario {

	public static final String NAME = "18000_02_Arrivee_HC";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Arrivée hors canton d'un individu.";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_DANS_COMMUNE;
	}

	private final long noIndAntoine = 1020206L;

	private MockIndividu indAntoine;

	private final int communeArriveeBex = MockCommune.Bex.getNoOFS();
	private final RegDate dateArriveeZurich = RegDate.get(1974, 3, 3);
	private final RegDate dateNaissance = RegDate.get(1952, 2, 21);
	private final RegDate dateArriveeBex = RegDate.get(2008, 12, 27);

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				indAntoine = addIndividu(noIndAntoine,dateNaissance , "Lenormand", "Antoine", true);
				setNationalite(indAntoine, dateNaissance, null, MockPays.Suisse);
				addAdresse(indAntoine, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null,
						dateArriveeZurich, dateArriveeBex.getOneDayBefore());
				addAdresse(indAntoine, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null,
						dateArriveeBex, null);


			}
		});
	}

	@Etape(id = 1, descr = "Déménagement civil d'Antoine à Bex")
	public void etape1() throws Exception {

		//addAdresseBex(indAntoine);
	}

	@Check(id = 1, descr = "Vérifie qu'Antoine est inconnue dans le registre fiscal")
	public void check1() throws Exception {

		{
			PersonnePhysique antoine = tiersDAO.getHabitantByNumeroIndividu(noIndAntoine);
			assertNull(antoine, "Atoine ne devrait pas être dans le registre");

		}

		// vérification que les adresses civiles sont a Bex
		assertEquals(MockCommune.Bex.getNomMinuscule(), serviceCivilService.getAdresses(noIndAntoine, dateArriveeBex, false).principale.getLocalite(),
			"l'adresse principale n'est pas à Bex");
	}

	@Etape(id = 2, descr = "Envoi de l'événement d'arrivée d'Antoine")
	public void etape2() throws Exception {

		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndAntoine, dateArriveeBex, communeArriveeBex);

		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérifie que l'evenement d'arrivée est au statut traité et qu'Antoine a un for ouvert sur Bex")
	public void check2() throws Exception {

		PersonnePhysique habAntoine = tiersDAO.getHabitantByNumeroIndividu(noIndAntoine);
		assertNotNull(habAntoine,"Le tiers Antoine n'as pas été créé dans le registre fiscal");
		{
			List<EvenementCivilExterne> list = evtExterneDAO.getAll();
			for (EvenementCivilExterne evt : list) {
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");
			}
		}

		ForFiscalPrincipal ffp = habAntoine.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'Habitant " + habAntoine.getNumero() + " null");
		assertEquals(dateArriveeBex, ffp.getDateDebut(), "Date de début du  for sur Bex fausse");
		assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "mode d'imposition faux");

	}

/*	private void addAdresseBex(MockIndividu ind) {
		Collection<Adresse> adrs = ind.getAdresses();
		MockAdresse last = null;
		for (Adresse a : adrs) {
			last = (MockAdresse) a;
		}
		last.setDateFinValidite(dateArriveeBex.getOneDayBefore());
		Adresse aa = MockServiceCivil.newAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, MockLocalite.Bex,
				dateArriveeBex, null);
		adrs.add(aa);
	}*/

}
