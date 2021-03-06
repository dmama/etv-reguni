package ch.vd.unireg.norentes.civil.arrivee;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockPermis;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

public class Ec_18000_10_Arrivee_HC_PermisC_Scenario extends EvenementCivilScenario {

	public static final String NAME = "18000_10_Arrivee_HC_PermisC";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Arrivée hors canton d'un individu et obtention permis C.";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_DANS_COMMUNE;
	}

	private static final long noIndAntoine = 1020206L;

	private MockIndividu indAntoine;

	private final int communeArriveeBex = MockCommune.Bex.getNoOFS();
	private final RegDate dateArriveeZurich = RegDate.get(1974, 3, 3);
	private final RegDate dateNaissance = RegDate.get(1952, 2, 21);
	private final RegDate dateArriveeBex = RegDate.get(2008, 11, 13);
	
	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {

				indAntoine = addIndividu(noIndAntoine,dateNaissance , "Lenormand", "Antoine", true);
				addNationalite(indAntoine, MockPays.France, dateNaissance, null);
				addAdresse(indAntoine, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null,
						dateArriveeZurich, dateArriveeBex.getOneDayBefore());
				addAdresse(indAntoine, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null,
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
		assertEquals(MockCommune.Bex.getNomOfficiel(), serviceCivilService.getAdresses(noIndAntoine, dateArriveeBex, false).principale.getLocalite(),
			"l'adresse principale n'est pas à Bex");
	}

	@Etape(id = 2, descr = "Envoi de l'événement d'arrivée d'Antoine")
	public void etape2() throws Exception {

		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndAntoine, dateArriveeBex, communeArriveeBex);

		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérifie que l'evenement d'arrivée est au statut traité et qu'Antoine a un for sur Bex sourcier")
	public void check2() throws Exception {

		PersonnePhysique habAntoine = tiersDAO.getHabitantByNumeroIndividu(noIndAntoine);
		assertNotNull(habAntoine,"Le tiers Antoine n'as pas été créé dans le registre fiscal");
		{
			List<EvenementCivilRegPP> list = evtExterneDAO.getAll();
			for (EvenementCivilRegPP evt : list) {
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");
			}
		}

		ForFiscalPrincipalPP ffp = habAntoine.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'Habitant " + habAntoine.getNumero() + " null");
		assertEquals(dateArriveeBex, ffp.getDateDebut(), "Date de début du  for sur Bex fausse");
		assertEquals(ModeImposition.SOURCE, ffp.getModeImposition(), "mode d'imposition faux");
				
	}
	
	@Etape(id = 3, descr = "Obtention permis C d'Antoine")
	public void etape3() throws Exception {
		doModificationIndividu(noIndAntoine, individu -> {
			final MockPermis permis = new MockPermis();
			permis.setTypePermis(TypePermis.ETABLISSEMENT);
			permis.setDateDebutValidite(dateArriveeBex);
			permis.setDateFinValidite(null);
			individu.getPermis().add(permis);
		});
	}

	@Check(id = 3, descr = "Vérifie qu'Antoine a le permis C")
	public void check3() throws Exception {

		// vérification que les adresses civiles sont a Bex
		final Individu ind = serviceCivilService.getIndividu(noIndAntoine, dateArriveeBex, AttributeIndividu.PERMIS);
		assertEquals(TypePermis.ETABLISSEMENT, ind.getPermis().getPermisActif(dateArriveeBex).getTypePermis(),
				"pas de permis C");
	}

	@Etape(id = 4, descr = "Envoi de l'événement d'obtention de permis d'Antoine")
	public void etape4() throws Exception {

		long id = addEvenementCivil(TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, noIndAntoine, dateArriveeBex, 0);

		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id);
	}

	@Check(id = 4, descr = "Vérifie que l'evenement d'obtention de permis est au statut traité et qu'Antoine a un for sur Bex ordinaire")
	public void check4() throws Exception {

		PersonnePhysique habAntoine = tiersDAO.getHabitantByNumeroIndividu(noIndAntoine);
		assertNotNull(habAntoine,"Le tiers Antoine n'as pas été créé dans le registre fiscal");
		{
			List<EvenementCivilRegPP> list = evtExterneDAO.getAll();
			for (EvenementCivilRegPP evt : list) {
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");
			}
		}

		ForFiscalPrincipalPP ffp = habAntoine.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'Habitant " + habAntoine.getNumero() + " null");
		assertEquals(dateArriveeBex.getOneDayAfter(), ffp.getDateDebut(), "Date de début du  for sur Bex fausse");
		assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "mode d'imposition faux");
				
	}
}
