package ch.vd.uniregctb.norentes.civil.arrivee;

import java.util.List;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

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

	private final long numeroIndividu = 960713;
	private final RegDate dateArrivee = date(2010, 10, 30);
	private final RegDate datePermis = dateArrivee.addMonths(1);
	private final RegDate dateNaissance = date(1956, 3, 15);
	private final Commune commune = MockCommune.Lausanne;

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil(serviceInfrastructureService) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(numeroIndividu, dateNaissance, "Bouchet", "Olivier", true, dateArrivee);
				individu.setDateNaissance(dateNaissance);
				addAdresse(individu, TypeAdresseCivil.COURRIER, "Rue du Lac", "76", 1003, MockLocalite.Lausanne, null, dateArrivee, null);
				addNationalite(individu, MockPays.France, dateArrivee, null, 1);
				addPermis(individu, TypePermis.ANNUEL, datePermis, null, 1, false);
			}
		});
	}

	@Etape(id = 1, descr = "Envoi de l'événement de changement de catégorie d'étranger")
	public void etape1() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, numeroIndividu, datePermis, commune.getNoOFSEtendu());
		commitAndStartTransaction();
		globalIndexer.sync();
		traiteEvenements(id);
	}

	@Check(id = 1, descr = "Vérification de l'erreur due au tiers inconnu")
	public void check1() throws Exception {

		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(numeroIndividu);
		assertNull(pp, "Pourquoi devrait-on trouver un tiers ? Pourquoi a-t-il été créé ?");

		final List<EvenementCivilExterne> evts = getEvenementsCivils(numeroIndividu, TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER);
		assertNotNull(evts, "Pas d'événement civil ?");
		assertEquals(1, evts.size(), "Où est passé l'événement civil d'obtention du permis ?");
		assertEquals(EtatEvenementCivil.EN_ERREUR, evts.get(0).getEtat(), "Pourquoi pas parti en erreur (le tiers n'existe pas) ?");
	}

	@Etape(id = 2, descr = "Envoi de l'événement d'arrivée")
	public void etape2() throws Exception {

		doModificationIndividu(numeroIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final MockAdresse adresse = new MockAdresse("Rue du Lac", "76", "1003", "Lausanne");
				adresse.setDateDebutValidite(dateArrivee);
				adresse.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
				individu.getAdresses().add(adresse);
			}
		});

		final long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, numeroIndividu, dateArrivee, commune.getNoOFSEtendu());
		commitAndStartTransaction();
		globalIndexer.sync();
		traiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérification du traitement correct de l'événement d'arrivée")
	public void check2() throws Exception {

		// si l'événement a été traité, c'est qu'un tiers a été créé
		final List<EvenementCivilExterne> arrivees = getEvenementsCivils(numeroIndividu, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS);
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
		final List<EvenementCivilExterne> obtentionPermis = getEvenementsCivils(numeroIndividu, TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER);
		assertNotNull(obtentionPermis, "Pas d'événements civils ?");
		assertEquals(1, obtentionPermis.size(), "Où est passé l'événement civil d'obtention de permis ?");
		assertEquals(EtatEvenementCivil.TRAITE, obtentionPermis.get(0).getEtat(), "Aurait dû être traité, non ?");
	}
}
