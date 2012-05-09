package ch.vd.uniregctb.norentes.civil.mariage;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Scénario de mariage d'un couple arrivé marié de hors Suisse.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4000_01_Mariage_CoupleArriveHorsSuisse_Scenario extends EvenementCivilScenario {

	public static final String NAME = "4000_01_Mariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Mariage d'un couple arrivé marié de hors Suisse.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private ServiceInfrastructureService infraService;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	private static final long noIndRafa = 3913648; // rafa
	private static final long noIndMaria = 3913649; // maria

	private MockIndividu indRafa;
	private MockIndividu indMaria;

	private long noHabRafa;

	private final RegDate dateArrivee = RegDate.get(2008, 11, 1);
	private final RegDate dateMariage = RegDate.get(2000, 6, 21);
	private final MockCommune communeArrivee = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				RegDate dateNaissanceRafa = RegDate.get(1974, 6, 25);
				indRafa = addIndividu(noIndRafa, dateNaissanceRafa, "Nadalino", "Rafa", true);

				RegDate dateNaissanceMaria = RegDate.get(1975, 7, 31);
				indMaria = addIndividu(noIndMaria, dateNaissanceMaria, "Nadalino", "Maria", false);

				marieIndividus(indRafa, indMaria, dateMariage);

				addOrigine(indRafa, MockPays.Espagne.getNomMinuscule());
				addNationalite(indRafa, MockPays.Espagne, dateNaissanceRafa, null);
				addPermis(indRafa, TypePermis.ETABLISSEMENT, RegDate.get(2008, 10, 1), null, false);
				addAdresse(indRafa, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, dateArrivee, null);

				addOrigine(indMaria, MockPays.Espagne.getNomMinuscule());
				addNationalite(indMaria, MockPays.Espagne, dateNaissanceMaria, null);
				addPermis(indMaria, TypePermis.ETABLISSEMENT, RegDate.get(2008, 10, 1), null, false);
				addAdresse(indMaria, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, dateArrivee, null);
			}
		});
	}

	@Etape(id=1, descr="Envoi des événements d'arrivée hors Suisse")
	public void etape1() throws Exception {
		long id1 = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, noIndRafa, dateArrivee, communeArrivee.getNoOFS());
		long id2 = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, noIndMaria, dateArrivee, communeArrivee.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id1);
		traiteEvenements(id2);
	}

	@Check(id=1, descr="Vérifie que les habitants ont bien été créés ainsi que le ménage commun")
	public void check1() throws Exception {
		// rafa
		PersonnePhysique rafa = tiersDAO.getHabitantByNumeroIndividu(noIndRafa);
		assertNotNull(rafa, "L'habitant Rafa n'a pas été créé");
		noHabRafa = rafa.getNumero();
		{
			ForFiscalPrincipal ffp = rafa.getDernierForFiscalPrincipal();
			assertNull(ffp, "L'habitant n°" + FormatNumeroHelper.numeroCTBToDisplay(rafa.getNumero()) + " ne devrait avoir aucun for principal");
		}
		// maria
		PersonnePhysique maria = tiersDAO.getHabitantByNumeroIndividu(noIndMaria);
		assertNotNull(maria, "L'habitant Maria n'a pas été créé");
		{
			ForFiscalPrincipal ffp = maria.getDernierForFiscalPrincipal();
			assertNull(ffp, "L'habitant n° " + FormatNumeroHelper.numeroCTBToDisplay(maria.getNumero()) + " ne devrait avoir aucun for principal");
		}
		// ménage
		EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(rafa, dateArrivee);
		assertNotNull(couple, "Le ménage n'a pas été créé");
		MenageCommun menage = couple.getMenage();
		assertNotNull(menage, "Le ménage n'a pas été créé");
		{
			ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le for principal du ménage n'a pas été trouvé");
		}
	}

	@Etape(id=2, descr="Envoi de l'événement de Mariage (redondant)")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.MARIAGE, noIndRafa, dateMariage, 0);
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que l'événement civil est dans l'état REDONDANT")
	public void check2() throws Exception {

		final List<EvenementCivilRegPP> evts = getEvenementsCivils(noIndRafa, TypeEvenementCivil.MARIAGE);
		assertNotNull(evts, "Pas d'événement de mariage ?");
		assertEquals(1, evts.size(), "Rafa ne s'est marié qu'une seule fois!");
		final EvenementCivilRegPP evt = evts.get(0);

		assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat(), "L'événement de mariage devrait être dans l'état REDONDANT car le couple est arrivé marié");
		assertEquals(0, evt.getErreurs().size(), "Il ne devrait pas y avoir exactement d'erreurs");
	}
}
