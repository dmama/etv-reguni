package ch.vd.unireg.norentes.civil.arrivee;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.ForsParType;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivil;

/**
 * Le principe de ce test est le suivant ; dans le cas jira UNIREG-2730, une arrivée HS au 1.01.2007
 * trouvait deux candidats non-habitant, dont un qui, bien qu'ayant la bonne date de naissance,
 * n'avait rien en commun dans le nom (cela était dû au fait que l'année de référence dans la
 * classe {@link ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne} était l'année de la veille
 * de la date de l'événement (= 2006, pas d'historique individu connu dans le host avant 2007, donc
 * impossible de trouver le nom de l'individu, donc recherche dans l'indexeur avec une date de naissance
 * seulement, pas de nom...))
 */
public class Ec_18000_20_Arrivee_JIRA2730_ArriveeJourDeLAn_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_18000_20_Arrivee";

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
		return "Contrôle de la récupération d'un ancien non-habitant lors d'une arrivée le jour de l'an (UNIREG-2730)";
	}

	private static final long numeroIndividu = 960713;
	private final RegDate dateArrivee = date(2007, 1, 1);
	private final RegDate dateNaissance = date(1956, 3, 15);

	private Long idMauvaisArrivant;
	private Long idBonArrivant;

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
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Orbe.CheminDeLaTranchee, null, dateArrivee, null);
				addNationalite(individu, MockPays.France, dateArrivee, null);
			}
		});
	}

	@Etape(id = 1, descr = "Mise en place fiscale : création des non-habitants avec la même date de naissance")
	public void etape1() throws Exception {

		// il n'ont en commun que la date de naissance et le fait qu'ils sont non-habitants
		final PersonnePhysique pp1 = addNonHabitant("Tartempion", "Bidule", dateNaissance, Sexe.MASCULIN);
		addForFiscalPrincipal(pp1, MockCommune.Bern, date(2000, 1, 2), null, MotifFor.ACHAT_IMMOBILIER, null);
		addForFiscalSecondaire(pp1, MockCommune.Lausanne.getNoOFS(), date(2000, 1, 2), null);
		idMauvaisArrivant = pp1.getNumero();

		final PersonnePhysique pp2 = addNonHabitant("Bouchet", "Olivier", dateNaissance, Sexe.MASCULIN);
		addForFiscalPrincipal(pp2, MockCommune.Bale, date(2001, 5, 12), null, MotifFor.ACHAT_IMMOBILIER, null);
		addForFiscalSecondaire(pp2, MockCommune.Bussigny.getNoOFS(), date(2001, 5, 12), null);
		idBonArrivant = pp2.getNumero();
	}

	@Check(id = 1, descr = "Vérification de l'existence des fors")
	public void check1() throws Exception {

		{
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idMauvaisArrivant);
			assertFalse(pp.isHabitantVD(), "Déjà habitant ?");

			final ForsParType forsParType = pp.getForsParType(false);

			final List<ForFiscalPrincipalPP> ffps = forsParType.principauxPP;
			assertNotNull(ffps, "Pas de fors principaux du tout ?");
			assertEquals(1, ffps.size(), "Pas le bon nombre de fors principaux");

			final ForFiscalPrincipal ffp = ffps.get(0);
			assertNotNull(ffp, "Bizarre, comme collection de fors...");
			assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale(), "Mauvais type d'autorité fiscale");
			assertEquals(MockCommune.Bern.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Pas à Berne ?");
			assertTrue(ffp.isValidAt(null), "For fermé ou annulé");

			final List<ForFiscalSecondaire> ffss = forsParType.secondaires;
			assertNotNull(ffss, "Pas de fors secondaires du tout ?");
			assertEquals(1, ffss.size(), "Pas le bon nombre de fors secondaires");

			final ForFiscalSecondaire ffs = ffss.get(0);
			assertNotNull(ffs, "Bizarre, comme collection de fors...");
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale(), "Mauvais type d'autorité fiscale");
			assertEquals(MockCommune.Lausanne.getNoOFS(), ffs.getNumeroOfsAutoriteFiscale(), "Pas à Lausanne ?");
			assertTrue(ffs.isValidAt(null), "For fermé ou annulé");
		}

		{
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idBonArrivant);
			assertFalse(pp.isHabitantVD(), "Déjà habitant ?");

			final ForsParType forsParType = pp.getForsParType(false);

			final List<ForFiscalPrincipalPP> ffps = forsParType.principauxPP;
			assertNotNull(ffps, "Pas de fors principaux du tout ?");
			assertEquals(1, ffps.size(), "Pas le bon nombre de fors principaux");

			final ForFiscalPrincipal ffp = ffps.get(0);
			assertNotNull(ffp, "Bizarre, comme collection de fors...");
			assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale(), "Mauvais type d'autorité fiscale");
			assertEquals(MockCommune.Bale.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Pas à Bâle ?");
			assertTrue(ffp.isValidAt(null), "For fermé ou annulé");

			final List<ForFiscalSecondaire> ffss = forsParType.secondaires;
			assertNotNull(ffss, "Pas de fors secondaires du tout ?");
			assertEquals(1, ffss.size(), "Pas le bon nombre de fors secondaires");

			final ForFiscalSecondaire ffs = ffss.get(0);
			assertNotNull(ffs, "Bizarre, comme collection de fors...");
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale(), "Mauvais type d'autorité fiscale");
			assertEquals(MockCommune.Bussigny.getNoOFS(), ffs.getNumeroOfsAutoriteFiscale(), "Pas à Bussigny ?");
			assertTrue(ffs.isValidAt(null), "For fermé ou annulé");
		}
	}

	@Etape(id = 2, descr = "Envoi de l'événement d'arrivée")
	public void etape2() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, numeroIndividu, dateArrivee, MockCommune.Orbe.getNoOFS());
		commitAndStartTransaction();
		globalIndexer.sync();
		traiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérification du traitement correct de l'événement d'arrivée")
	public void check2() throws Exception {

		// si l'événement a été traité, c'est qu'il n'y avait qu'un seul candidat (ou aucun)
		final List<EvenementCivilRegPP> evts = getEvenementsCivils(numeroIndividu, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS);
		assertNotNull(evts, "Pas d'événements civils ?");
		assertEquals(1, evts.size(), "Où est passé l'événement civil d'arrivée ?");
		assertEquals(EtatEvenementCivil.TRAITE, evts.get(0).getEtat(), "Aurait dû être traité, non ?");

		// on vérifie ici que c'est bien l'hypothèse "1 candidat" qui est la bonne, puisqu'aucun nouveau tiers n'a été créé
		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(numeroIndividu);
		assertNotNull(pp, "L'habitant Olivier n'a pas été trouvé");
		assertTrue(pp.isHabitantVD(), "Olivier devrait être habitant!");
		assertEquals(idBonArrivant, pp.getNumero(), "Nouvel habitant créé ?");
	}
}
