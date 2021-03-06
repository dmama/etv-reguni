package ch.vd.unireg.norentes.civil.arrivee;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

/**
 * Un événement d'obtention de permis B part d'abord en erreur car le tiers est inconnu,
 * puis un événement d'arrivée HS arrive, qui crée le for principal (mais apparemment pas à
 * la bonne date)
 */
public class Ec_18000_21_Arrivee_JIRA3133_DecalageUnJour_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_18000_21_Arrivee";

	private ServiceInfrastructureService serviceInfrastructureService;

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Contrôle de la date de création du for lors d'une arrivée HS d'un sourcier sans permis au moment de l'arrivée";
	}

	private static final long numeroIndividu = 960713;
	private final RegDate dateArrivee = date(2010, 10, 30);
	private final RegDate datePermis = dateArrivee.addMonths(1);
	private final RegDate dateNaissance = date(1956, 3, 15);
	private final Commune commune = MockCommune.Lausanne;

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(numeroIndividu, dateNaissance, "Bouchet", "Olivier", true);
				individu.setDateNaissance(dateNaissance);
				addAdresse(individu, TypeAdresseCivil.COURRIER, "Rue du Lac", "76", 1003, MockLocalite.Lausanne, null, dateArrivee, null);
				addNationalite(individu, MockPays.France, dateArrivee, null);
				addPermis(individu, TypePermis.SEJOUR, datePermis, null, false);
			}
		});
	}

	@Etape(id = 1, descr = "Envoi de l'événement de changement de catégorie d'étranger")
	public void etape1() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, numeroIndividu, datePermis, commune.getNoOFS());
		commitAndStartTransaction();
		globalIndexer.sync();
		traiteEvenements(id);
	}

	@Check(id = 1, descr = "Vérification de l'erreur due au tiers inconnu")
	public void check1() throws Exception {

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(numeroIndividu);
		assertNull(pp, "Pourquoi devrait-on trouver un tiers ? Pourquoi a-t-il été créé ?");

		final List<EvenementCivilRegPP> evts = getEvenementsCivils(numeroIndividu, TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER);
		assertNotNull(evts, "Pas d'événement civil ?");
		assertEquals(1, evts.size(), "Où est passé l'événement civil d'obtention du permis ?");
		assertEquals(EtatEvenementCivil.EN_ERREUR, evts.get(0).getEtat(), "Pourquoi pas parti en erreur (le tiers n'existe pas) ?");
	}

	@Etape(id = 2, descr = "Envoi de l'événement d'arrivée")
	public void etape2() throws Exception {

		doModificationIndividu(numeroIndividu, individu -> {
			final MockAdresse adresse = new MockAdresse("Rue du Lac", "76", "1003", "Lausanne");
			adresse.setDateDebutValidite(dateArrivee);
			adresse.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
			adresse.setCommuneAdresse(MockCommune.Lausanne);
			individu.addAdresse(adresse);
		});

		final long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, numeroIndividu, dateArrivee, commune.getNoOFS());
		commitAndStartTransaction();
		globalIndexer.sync();
		traiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérification du traitement correct de l'événement d'arrivée")
	public void check2() throws Exception {

		// si l'événement a été traité, c'est qu'un tiers a été créé
		final List<EvenementCivilRegPP> arrivees = getEvenementsCivils(numeroIndividu, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS);
		assertNotNull(arrivees, "Pas d'événements civils ?");
		assertEquals(1, arrivees.size(), "Où est passé l'événement civil d'arrivée ?");
		assertEquals(EtatEvenementCivil.TRAITE, arrivees.get(0).getEtat(), "Aurait dû être traité, non ?");

		// quel est ce tiers ?
		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(numeroIndividu);
		assertNotNull(pp, "On n'a pas créé de tiers ?");
		assertTrue(pp.isHabitantVD(), "Aurait dû être habitant !");

		// quel est son for principal ?
		final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "Pas de for ?");
		assertEquals(dateArrivee, ffp.getDateDebut(), "Mauvaise date de début du for");

		final ForFiscalPrincipal ffpAvantArrivee = pp.getForFiscalPrincipalAt(dateArrivee.getOneDayBefore());
		assertNull(ffpAvantArrivee, "Pourquoi y a-t-il déjà un for avant l'arrivée ?");

		// vérification que l'événement de permis a également été traité (en fait, ignoré!)
		final List<EvenementCivilRegPP> obtentionPermis = getEvenementsCivils(numeroIndividu, TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER);
		assertNotNull(obtentionPermis, "Pas d'événements civils ?");
		assertEquals(1, obtentionPermis.size(), "Où est passé l'événement civil d'obtention de permis ?");
		assertEquals(EtatEvenementCivil.TRAITE, obtentionPermis.get(0).getEtat(), "Aurait dû être traité, non ?");
	}
}
