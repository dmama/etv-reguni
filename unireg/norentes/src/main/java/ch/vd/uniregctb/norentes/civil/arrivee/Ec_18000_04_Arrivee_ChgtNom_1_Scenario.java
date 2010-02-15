package ch.vd.uniregctb.norentes.civil.arrivee;

import java.util.List;

import annotation.Check;
import annotation.Etape;
import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_18000_04_Arrivee_ChgtNom_1_Scenario extends EvenementCivilScenario {

	public static final String NAME = "18000_04_Arrivee_ChgtNom";
			
	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC;
	}

	@Override
	public String getDescription() {
		return "arrivée dans le canton + changement de nom";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndAlain = 122456L;

	private MockIndividu indAlain;

	private final RegDate dateNaissance = RegDate.get(1952, 2, 21);
	private final RegDate dateArriveeZurich = RegDate.get(2003, 3, 3);
	private final RegDate dateArriveeBex = RegDate.get(2008, 7, 5);
	
	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				indAlain = addIndividu(noIndAlain, RegDate.get(1952, 2, 21), "Gregoire", "Alain", true);
				setNationalite(indAlain, dateNaissance, null, MockPays.Suisse);
				addAdresse(indAlain, EnumTypeAdresse.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null,
						MockLocalite.Zurich, dateArriveeZurich, dateArriveeBex.getOneDayBefore());
				addAdresse(indAlain, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null,
						MockLocalite.Bex, dateArriveeBex, null);

			}
		});
	}


	@Etape(id=1, descr="Chargement d'un habitant à Lausanne")
	public void etape1() throws Exception {

	}

	@Check(id=1, descr="Vérifie que l'habitant Alain est inconnu")
	public void check1() throws Exception {
		PersonnePhysique alain = tiersDAO.getHabitantByNumeroIndividu(noIndAlain);
		assertNull(alain, "Alain ne devrait pas être dans le registre");
	}
	
	@Etape(id=2, descr="Envoi de l'événement de changement de nom de l'individu Alain")
	public void etape2() throws Exception {

		long id = addEvenementCivil(TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, noIndAlain, dateArriveeBex, MockCommune.Bex.getNoOFS());

		commitAndStartTransaction();

		// On traite les evenements
		regroupeEtTraiteEvenements(id);
	}
	
	@Check(id=2, descr="Vérifie que l'évènement est en erreur")
	public void check2() throws Exception {
		List<EvenementCivilRegroupe> list = evtRegroupeDAO.getAll();
		for (EvenementCivilRegroupe evt : list) {
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(), "");
		}
	}
	
	@Etape(id=3, descr="Envoi de l'événement d'arrivée de l'individu Alain")
	public void etape3() throws Exception {

		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndAlain, dateArriveeBex, MockCommune.Bex.getNoOFS());

		commitAndStartTransaction();

		// On traite les evenements
		regroupeEtTraiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que l'habitant Alain a été créé et que tous les événements ont été traité")
	public void check3() throws Exception {
		PersonnePhysique habAlain = tiersDAO.getHabitantByNumeroIndividu(noIndAlain);
		assertNotNull(habAlain,"Le tiers Alain n'as pas été créé dans le registre fiscal");
		{
			List<EvenementCivilRegroupe> list = evtRegroupeDAO.getAll();
			for (EvenementCivilRegroupe evt : list) {
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");
			}
		}

		ForFiscalPrincipal ffp = habAlain.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'Habitant " + habAlain.getNumero() + " null");
		assertEquals(dateArriveeBex, ffp.getDateDebut(), "Date de début du for sur Bex fausse");

	}
	
}
