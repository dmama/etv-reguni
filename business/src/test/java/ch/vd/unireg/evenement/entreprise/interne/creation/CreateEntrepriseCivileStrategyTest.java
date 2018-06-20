package ch.vd.unireg.evenement.entreprise.interne.creation;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.interne.AbstractEvenementEntrepriseCivileInterneTest;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.entreprise.interne.MessageSuiviPreExecution;
import ch.vd.unireg.evenement.entreprise.interne.TraitementManuel;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.regimefiscal.RegimeFiscalServiceImpl;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.MockTiersDAO;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static ch.vd.unireg.interfaces.infra.mock.MockCommune.Lausanne;
import static ch.vd.unireg.interfaces.infra.mock.MockCommune.Zurich;
import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;
import static ch.vd.unireg.type.TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION;

/**
 * @author Raphaël Marmier, 2015-09-07
 */
public class CreateEntrepriseCivileStrategyTest extends AbstractEvenementEntrepriseCivileInterneTest {

	public CreateEntrepriseCivileStrategyTest() {
		setWantIndexationTiers(true);
	}

	private CreateEntrepriseStrategy strategy;

	/**
	 * Version du service mock ou on peut ajouter des entreprises à la demande.
	 */
	public static class OpenMockServiceEntreprise extends MockServiceEntreprise {
		@Override
		protected void init() {}

		public void addEntreprise(MockEntrepriseCivile entreprise) {
			super.addEntreprise(entreprise);
		}
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		// Mise en place services mock
		this.serviceEntreprise.setUp(new OpenMockServiceEntreprise());

		MockTiersDAO tiersDAO = new MockTiersDAO() {
			@Override
			public Entreprise getEntrepriseByNoEntrepriseCivile(long numeroEntrepriseCivile) {
				if (numeroEntrepriseCivile == 222222001L) {
					Entreprise entreprise = new Entreprise();
					entreprise.setNumeroEntreprise(222222001L);
					return entreprise;
				}
				return null;
			}
		};

		final RegimeFiscalServiceImpl regimeFiscalService = new RegimeFiscalServiceImpl();
		regimeFiscalService.setServiceInfra(serviceInfra);
		context = new EvenementEntrepriseContext(this.serviceEntreprise, serviceInfra, regimeFiscalService, tiersDAO);

		// La stratégie à tester
		strategy = new CreateEntrepriseStrategy(context, new EvenementEntrepriseOptions());
	}

	@Test
	public void testCasEntrepriseVD() throws Exception {

		// Création et contrôle pour chaque type d'entreprise
		createAddEnt(101220101L, RegDate.get(2015, 9, 7), FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE, Lausanne);
		tryCreationEventAndCheckResult(101220101L, MessageSuiviPreExecution.class);

		createAddEnt(101220103L, RegDate.get(2015, 9, 7), FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF, Lausanne);
		tryCreationEventAndCheckResult(101220103L, CreateEntrepriseVD.class);

		createAddEnt(101220104L, RegDate.get(2015, 9, 7), FormeLegale.N_0104_SOCIETE_EN_COMMANDITE, Lausanne);
		tryCreationEventAndCheckResult(101220104L, CreateEntrepriseVD.class);

		createAddEnt(101220105L, RegDate.get(2015, 9, 7), FormeLegale.N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS, Lausanne);
		tryCreationEventAndCheckResult(101220105L, CreateEntrepriseVD.class);

		createAddEnt(101220106L, RegDate.get(2015, 9, 7), FormeLegale.N_0106_SOCIETE_ANONYME, Lausanne);
		tryCreationEventAndCheckResult(101220106L, CreateEntrepriseVD.class);

		createAddEnt(101220107L, RegDate.get(2015, 9, 7), FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, Lausanne);
		tryCreationEventAndCheckResult(101220107L, CreateEntrepriseVD.class);

		createAddEnt(101220108L, RegDate.get(2015, 9, 7), FormeLegale.N_0108_SOCIETE_COOPERATIVE, Lausanne);
		tryCreationEventAndCheckResult(101220108L, CreateEntrepriseVD.class);

		createAddEnt(101220109L, RegDate.get(2015, 9, 7), FormeLegale.N_0109_ASSOCIATION, Lausanne);
		tryCreationEventAndCheckResult(101220109L, CreateEntrepriseVD.class);

		createAddEnt(101220110L, RegDate.get(2015, 9, 7), FormeLegale.N_0110_FONDATION, Lausanne);
		tryCreationEventAndCheckResult(101220110L, CreateEntrepriseVD.class);

		createAddEnt(101220111L, RegDate.get(2015, 9, 7), FormeLegale.N_0111_FILIALE_ETRANGERE_AU_RC, Lausanne);
		tryCreationEventAndCheckResult(101220111L, CreateEntrepriseVD.class);

		createAddEnt(101220113L, RegDate.get(2015, 9, 7), FormeLegale.N_0113_FORME_JURIDIQUE_PARTICULIERE, Lausanne);
		tryCreationEventAndCheckResult(101220113L, CreateEntrepriseVD.class);

		createAddEnt(101220114L, RegDate.get(2015, 9, 7), FormeLegale.N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX, Lausanne);
		tryCreationEventAndCheckResult(101220114L, CreateEntrepriseVD.class);

		createAddEnt(101220115L, RegDate.get(2015, 9, 7), FormeLegale.N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE, Lausanne);
		tryCreationEventAndCheckResult(101220115L, CreateEntrepriseVD.class);

		createAddEnt(101220116L, RegDate.get(2015, 9, 7), FormeLegale.N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE, Lausanne);
		tryCreationEventAndCheckResult(101220116L, CreateEntrepriseVD.class);

		createAddEnt(101220117L, RegDate.get(2015, 9, 7), FormeLegale.N_0117_INSTITUT_DE_DROIT_PUBLIC, Lausanne);
		tryCreationEventAndCheckResult(101220117L, CreateEntrepriseVD.class);

		createAddEnt(101220118L, RegDate.get(2015, 9, 7), FormeLegale.N_0118_PROCURATIONS_NON_COMMERCIALES, Lausanne);
		tryCreationEventAndCheckResult(101220118L, CreateEntrepriseVD.class);

		createAddEnt(101220119L, RegDate.get(2015, 9, 7), FormeLegale.N_0119_CHEF_INDIVISION, Lausanne);
		tryCreationEventAndCheckResult(101220119L, CreateEntrepriseVD.class);

		createAddEnt(101220151L, RegDate.get(2015, 9, 7), FormeLegale.N_0151_SUCCURSALE_SUISSE_AU_RC, Lausanne);
		tryCreationEventAndCheckResult(101220151L, CreateEntrepriseVD.class);

		createAddEnt(101220220L, RegDate.get(2015, 9, 7), FormeLegale.N_0220_ADMINISTRATION_CONFEDERATION, Lausanne);
		tryCreationEventAndCheckResult(101220220L, CreateEntrepriseVD.class);

		createAddEnt(101220221L, RegDate.get(2015, 9, 7), FormeLegale.N_0221_ADMINISTRATION_CANTON, Lausanne);
		tryCreationEventAndCheckResult(101220221L, CreateEntrepriseVD.class);

		createAddEnt(101220222L, RegDate.get(2015, 9, 7), FormeLegale.N_0222_ADMINISTRATION_DISTRICT, Lausanne);
		tryCreationEventAndCheckResult(101220222L, CreateEntrepriseVD.class);

		createAddEnt(101220223L, RegDate.get(2015, 9, 7), FormeLegale.N_0223_ADMINISTRATION_COMMUNE, Lausanne);
		tryCreationEventAndCheckResult(101220223L, CreateEntrepriseVD.class);

		createAddEnt(101220224L, RegDate.get(2015, 9, 7), FormeLegale.N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION, Lausanne);
		tryCreationEventAndCheckResult(101220224L, CreateEntrepriseVD.class);

		createAddEnt(101220230L, RegDate.get(2015, 9, 7), FormeLegale.N_0230_ENTREPRISE_CONFEDERATION, Lausanne);
		tryCreationEventAndCheckResult(101220230L, CreateEntrepriseVD.class);

		createAddEnt(101220231L, RegDate.get(2015, 9, 7), FormeLegale.N_0231_ENTREPRISE_CANTON, Lausanne);
		tryCreationEventAndCheckResult(101220231L, CreateEntrepriseVD.class);

		createAddEnt(101220232L, RegDate.get(2015, 9, 7), FormeLegale.N_0232_ENTREPRISE_DISTRICT, Lausanne);
		tryCreationEventAndCheckResult(101220232L, CreateEntrepriseVD.class);

		createAddEnt(101220233L, RegDate.get(2015, 9, 7), FormeLegale.N_0233_ENTREPRISE_COMMUNE, Lausanne);
		tryCreationEventAndCheckResult(101220233L, CreateEntrepriseVD.class);

		createAddEnt(101220234L, RegDate.get(2015, 9, 7), FormeLegale.N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE, Lausanne);
		tryCreationEventAndCheckResult(101220234L, CreateEntrepriseVD.class);

		createAddEnt(101220302L, RegDate.get(2015, 9, 7), FormeLegale.N_0302_SOCIETE_SIMPLE, Lausanne);
		tryCreationEventAndCheckResult(101220302L, MessageSuiviPreExecution.class);

		createAddEnt(101220312L, RegDate.get(2015, 9, 7), FormeLegale.N_0312_FILIALE_ETRANGERE_NON_AU_RC, Lausanne);
		tryCreationEventAndCheckResult(101220312L, CreateEntrepriseVD.class);

		createAddEnt(101220327L, RegDate.get(2015, 9, 7), FormeLegale.N_0327_ENTREPRISE_PUBLIQUE_ETRANGERE, Lausanne);
		tryCreationEventAndCheckResult(101220327L, CreateEntrepriseVD.class);

		createAddEnt(101220328L, RegDate.get(2015, 9, 7), FormeLegale.N_0328_ADMINISTRATION_PUBLIQUE_ETRANGERE, Lausanne);
		tryCreationEventAndCheckResult(101220328L, CreateEntrepriseVD.class);

		createAddEnt(101220329L, RegDate.get(2015, 9, 7), FormeLegale.N_0329_ORGANISATION_INTERNATIONALE, Lausanne);
		tryCreationEventAndCheckResult(101220329L, CreateEntrepriseVD.class);

		createAddEnt(101220441L, RegDate.get(2015, 9, 7), FormeLegale.N_0441_ENTREPRISE_ETRANGERE, Lausanne);
		tryCreationEventAndCheckResult(101220441L, CreateEntrepriseVD.class);

		createAddEnt(101220441L, RegDate.get(2015, 9, 7), null, Lausanne);
		tryCreationAndExpectNull(101220441L);
	}

	@Test
	public void testCasEntrepriseHCAvecSiegeSecondaireVaud() throws EvenementEntrepriseException {
		// Cas hors Vaud avec un établissement civil secondaire sur Vaud
		addEnt(MockEntrepriseFactory.createEntrepriseAvecEtablissementSecondaire(101220106L, 101220106L + 1000000, 101220106L + 2000000, "abcdef", RegDate.get(2015, 9, 7), null, FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                                         TypeAutoriteFiscale.COMMUNE_HC, Zurich.getNoOFS(),
		                                                                         TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 1, 2),
		                                                                         StatusInscriptionRC.ACTIF, date(2015, 9, 4), StatusRegistreIDE.DEFINITIF,
		                                                                         StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999992", "CHE999999993"));
		tryCreationEventAndCheckResult(101220106L, CreateEntrepriseHorsVD.class);
	}


	@Test
	public void testCasSpeciaux() throws Exception {

		// Avec une entreprise existante
		createAddEnt(222222001L, RegDate.get(2010, 1, 1), FormeLegale.N_0106_SOCIETE_ANONYME, Lausanne);
		tryCreationAndExpectNull(222222001L);

		// Avec commune forme juridique inconnue
		addEnt(MockEntrepriseFactory
				       .createEntreprise(222222003L, 222222003L + 1000000, "abcdef", RegDate.get(2015, 9, 7), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 06, 24), null, null, "CHE999999994"));
		tryCreationAndExpectNull(222222003L);

		// TODO: A bien vérifier ce qu'on doit faire lorsque le siege n'est pas connu.
		// Avec commune inconnue (pas de no ofs dans le siege)
		addEnt(MockEntrepriseFactory.createEntreprise(222222002L, 222222002L + 1000000, "abcdef", RegDate.get(2015, 9, 7), null, FormeLegale.N_0106_SOCIETE_ANONYME, null, null, null, null, null, null, "CHE999999995"));
		tryCreationEventAndCheckResult(222222002L, TraitementManuel.class);

		// Cas hors Vaud aucun établissement civil sur Vaud
		addEnt(MockEntrepriseFactory.createEntrepriseAvecEtablissementSecondaire(222222004L, 222222004L + 1000000, 222222004L + 2000000, "abcdef", RegDate.get(2015, 9, 7), null, FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                                         TypeAutoriteFiscale.COMMUNE_HC, Zurich.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, Zurich.getNoOFS(),
		                                                                         null, null, null, null, null, null, null, null, "CHE999999996", "CHE999999997"));
		tryCreationEventAndCheckResult(222222004L, MessageSuiviPreExecution.class);

	}

	private void createAddEnt(long cantonalId, RegDate dateDebut, FormeLegale formeLegale, MockCommune commune) {
		addEnt(MockEntrepriseFactory.createSimpleEntrepriseRC(cantonalId, cantonalId + 1000000, "XYZ", dateDebut, null, formeLegale, commune));
	}

	private void createAddEnt(long cantonalId, RegDate dateDebut, FormeLegale formeLegale, MockPays pays) {
		addEnt(MockEntrepriseFactory.createSimpleEntrepriseRC(cantonalId, cantonalId + 1000000, "XYZ", dateDebut, null, formeLegale, pays));
	}

	private void addEnt(MockEntrepriseCivile ent) {
		((OpenMockServiceEntreprise) serviceEntreprise.getUltimateTarget())
				.addEntreprise(ent);
	}

	private void tryCreationAndExpectNull(long noEntreprise) throws EvenementEntrepriseException {
		final long noEvenement = 12121212L;
		final EvenementEntreprise event = new EvenementEntreprise(noEvenement, IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 9, 7), noEntreprise, A_TRAITER);
		EntrepriseCivile entrepriseCivile = serviceEntreprise.getEntrepriseHistory(noEntreprise);
		final Entreprise entreprise = context.getTiersDAO().getEntrepriseByNoEntrepriseCivile(entrepriseCivile.getNumeroEntreprise());

		EvenementEntrepriseInterne interne = strategy.matchAndCreate(event, entrepriseCivile, entreprise);

		Assert.assertNull(interne);
	}

	private void tryCreationEventAndCheckResult(long noEntreprise, Class<? extends EvenementEntrepriseInterne> resultClass) throws EvenementEntrepriseException {
		final long noEvenement = 12121212L;
		final EvenementEntreprise event = new EvenementEntreprise(noEvenement, IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 9, 7), noEntreprise, A_TRAITER);
		EntrepriseCivile entrepriseCivile = serviceEntreprise.getEntrepriseHistory(noEntreprise);
		final Entreprise entreprise = context.getTiersDAO().getEntrepriseByNoEntrepriseCivile(entrepriseCivile.getNumeroEntreprise());

		EvenementEntrepriseInterne interne = strategy.matchAndCreate(event, entrepriseCivile, entreprise);

		Assert.assertNotNull(interne);
		Assert.assertEquals(event.getNoEvenement(), interne.getNumeroEvenement().longValue());
		Assert.assertEquals(event.getNoEntrepriseCivile(), interne.getNoEntrepriseCivile());
		Assert.assertEquals(event.getDateEvenement(), interne.getDateEvt());
		Assert.assertEquals(resultClass, interne.getClass());
	}
}