package ch.vd.uniregctb.evenement.organisation.interne;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockServiceInfrastructureService;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationBuilder;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.interne.creation.CreateEntrepriseDPPM;
import ch.vd.uniregctb.evenement.organisation.interne.creation.CreateEntrepriseFDSPLAC;
import ch.vd.uniregctb.evenement.organisation.interne.creation.CreateEntreprisePMAPM;
import ch.vd.uniregctb.evenement.organisation.interne.creation.CreateEntrepriseSP;
import ch.vd.uniregctb.evenement.organisation.interne.creation.CreateOrganisationStrategy;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MockTiersDAO;

import static ch.vd.unireg.interfaces.infra.mock.MockCommune.Lausanne;
import static ch.vd.unireg.interfaces.infra.mock.MockCommune.Zurich;
import static ch.vd.unireg.interfaces.organisation.data.FormeLegale.*;
import static ch.vd.uniregctb.type.EmetteurEvenementOrganisation.IDE;
import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;
import static ch.vd.uniregctb.type.TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION_DANS_REGISTRE;

/**
 * @author Raphaël Marmier, 2015-09-07
 */
public class CreateOrganisationStrategyTest extends AbstractEvenementOrganisationInterneTest {

	private CreateOrganisationStrategy strategy;

	/**
	 * Version du service mock ou on peut ajouter des organisations à la demande.
	 */
	public static class OpenMockServiceOrganisation extends MockServiceOrganisation {
		@Override
		protected void init() {}

		public void addOrganisation(Organisation org) {
			super.addOrganisation(org);
		}
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		// Mise en place services mock
		this.serviceOrganisation.setUp(new OpenMockServiceOrganisation());
		this.serviceInfra.setUp(new MockServiceInfrastructureService() {

			@Override
			protected void init() {
				communesVaud.add(Lausanne);
				communesHorsCanton.add(MockCommune.Zurich);
			}
		});

		MockTiersDAO tiersDAO = new MockTiersDAO() {
			@Override
			public Entreprise getEntrepriseByNumeroOrganisation(long numeroOrganisation) {
				if (numeroOrganisation == 222222001L) {
					Entreprise entreprise = new Entreprise();
					entreprise.setNumeroEntreprise(222222001L);
					return entreprise;
				}
				return null;
			}
		};
		context = new EvenementOrganisationContext(this.serviceOrganisation, serviceInfra, tiersDAO);

		// La stratégie à tester
		strategy = new CreateOrganisationStrategy();
	}

	@Test
	public void testCasEntrepriseVD() throws Exception {

		// Création et contrôle pour chaque type d'entreprise
		createAddOrg(101220000L, RegDate.get(2015, 9, 7), N_00_AUTRE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220000L, TraitementManuel.class);

		createAddOrg(101220100L, RegDate.get(2015, 9, 7), N_01_FORMES_JUR_DE_DROIT_PRIVE_UTILISEES_DANS_RC, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220100L, TraitementManuel.class);

		createAddOrg(101220101L, RegDate.get(2015, 9, 7), N_0101_ENTREPRISE_INDIVIDUELLE, Lausanne.getNoOFS());
		tryCreationAndExpectNull(101220101L);

		createAddOrg(101220103L, RegDate.get(2015, 9, 7), N_0103_SOCIETE_NOM_COLLECIF, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220103L, CreateEntrepriseSP.class);

		createAddOrg(101220104L, RegDate.get(2015, 9, 7), N_0104_SOCIETE_EN_COMMANDITE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220104L, CreateEntrepriseSP.class);

		createAddOrg(101220105L, RegDate.get(2015, 9, 7), N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220105L, CreateEntreprisePMAPM.class);

		createAddOrg(101220106L, RegDate.get(2015, 9, 7), N_0106_SOCIETE_ANONYME, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220106L, CreateEntreprisePMAPM.class);

		createAddOrg(101220107L, RegDate.get(2015, 9, 7), N_0107_SOCIETE_A_RESPONSABILITE_LIMITE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220107L, CreateEntreprisePMAPM.class);

		createAddOrg(101220108L, RegDate.get(2015, 9, 7), N_0108_SOCIETE_COOPERATIVE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220108L, CreateEntreprisePMAPM.class);

		createAddOrg(101220109L, RegDate.get(2015, 9, 7), N_0109_ASSOCIATION, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220109L, CreateEntreprisePMAPM.class);

		createAddOrg(101220110L, RegDate.get(2015, 9, 7), N_0110_FONDATION, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220110L, CreateEntreprisePMAPM.class);

		createAddOrg(101220111L, RegDate.get(2015, 9, 7), N_0111_FILIALE_ETRANGERE_AU_RC, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220111L, TraitementManuel.class);

		createAddOrg(101220113L, RegDate.get(2015, 9, 7), N_0113_FORME_JURIDIQUE_PARTICULIERE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220113L, TraitementManuel.class);

		createAddOrg(101220114L, RegDate.get(2015, 9, 7), N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220114L, CreateEntrepriseFDSPLAC.class);

		createAddOrg(101220115L, RegDate.get(2015, 9, 7), N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220115L, CreateEntrepriseFDSPLAC.class);

		createAddOrg(101220116L, RegDate.get(2015, 9, 7), N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220116L, CreateEntrepriseFDSPLAC.class);

		createAddOrg(101220117L, RegDate.get(2015, 9, 7), N_0117_INSTITUT_DE_DROIT_PUBLIC, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220117L, TraitementManuel.class);

		createAddOrg(101220118L, RegDate.get(2015, 9, 7), N_0118_PROCURATIONS_NON_COMMERCIALES, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220118L, TraitementManuel.class);

		createAddOrg(101220119L, RegDate.get(2015, 9, 7), N_0119_CHEF_INDIVISION, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220119L, TraitementManuel.class);

		createAddOrg(101220151L, RegDate.get(2015, 9, 7), N_0151_SUCCURSALE_SUISSE_AU_RC, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220151L, TraitementManuel.class);

		createAddOrg(101220200L, RegDate.get(2015, 9, 7), N_02_FORMES_JUR_DE_DROIT_PUBLIC_NON_UTILISEES_DANS_RC, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220200L, TraitementManuel.class);

		createAddOrg(101220220L, RegDate.get(2015, 9, 7), N_0220_ADMINISTRATION_CONFEDERATION, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220220L, TraitementManuel.class);

		createAddOrg(101220221L, RegDate.get(2015, 9, 7), N_0221_ADMINISTRATION_CANTON, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220221L, TraitementManuel.class);

		createAddOrg(101220222L, RegDate.get(2015, 9, 7), N_0222_ADMINISTRATION_DISTRICT, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220222L, TraitementManuel.class);

		createAddOrg(101220223L, RegDate.get(2015, 9, 7), N_0223_ADMINISTRATION_COMMUNE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220223L, TraitementManuel.class);

		createAddOrg(101220224L, RegDate.get(2015, 9, 7), N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220224L, TraitementManuel.class);

		createAddOrg(101220230L, RegDate.get(2015, 9, 7), N_0230_ENTREPRISE_CONFEDERATION, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220230L, TraitementManuel.class);

		createAddOrg(101220231L, RegDate.get(2015, 9, 7), N_0231_ENTREPRISE_CANTON, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220231L, TraitementManuel.class);

		createAddOrg(101220232L, RegDate.get(2015, 9, 7), N_0232_ENTREPRISE_DISTRICT, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220232L, TraitementManuel.class);

		createAddOrg(101220233L, RegDate.get(2015, 9, 7), N_0233_ENTREPRISE_COMMUNE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220233L, TraitementManuel.class);

		createAddOrg(101220234L, RegDate.get(2015, 9, 7), N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220234L, CreateEntrepriseDPPM.class);

		createAddOrg(101220300L, RegDate.get(2015, 9, 7), N_03_AUTRES_FORMES_JUR_NON_UTILISEES_DANS_RC, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220300L, TraitementManuel.class);

		createAddOrg(101220302L, RegDate.get(2015, 9, 7), N_0302_SOCIETE_SIMPLE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220302L, TraitementManuel.class);

		createAddOrg(101220312L, RegDate.get(2015, 9, 7), N_0312_FILIALE_ETRANGERE_NON_AU_RC, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220312L, TraitementManuel.class);

		createAddOrg(101220327L, RegDate.get(2015, 9, 7), N_0327_ENTREPRISE_PUBLIQUE_ETRANGERE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220327L, TraitementManuel.class);

		createAddOrg(101220328L, RegDate.get(2015, 9, 7), N_0328_ADMINISTRATION_PUBLIQUE_ETRANGERE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220328L, TraitementManuel.class);

		createAddOrg(101220329L, RegDate.get(2015, 9, 7), N_0329_ORGANISATION_INTERNATIONALE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220329L, TraitementManuel.class);

		createAddOrg(101220400L, RegDate.get(2015, 9, 7), N_04_ENTREPRISE_ETRANGERE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220400L, TraitementManuel.class);

		createAddOrg(101220441L, RegDate.get(2015, 9, 7), N_0441_ENTREPRISE_ETRANGERE, Lausanne.getNoOFS());
		tryCreationEventAndCheckResult(101220441L, TraitementManuel.class);

	}

	@Test
	public void testCasEntrepriseHCAvecSiegeSecondaireVaud() throws EvenementOrganisationException {
		// Cas hors Vaud avec un site secondaire sur Vaud
		addOrg(MockOrganisationBuilder.createOrganisationAvecSiteSecondaire(101220106L, 101220106L + 1000000, 101220106L + 2000000,  "abcdef", RegDate.get(2015, 9, 7), N_0106_SOCIETE_ANONYME, Zurich.getNoOFS(), Lausanne.getNoOFS(), null, null,
		                                                                    null, null, null, null, null, null));
		tryCreationEventAndCheckResult(101220106L, CreateEntreprisePMAPM.class);
	}


	@Test
	public void testCasSpeciaux() throws Exception {

		// Avec une entreprise existante
		createAddOrg(222222001L, RegDate.get(2010, 1, 1), N_0106_SOCIETE_ANONYME, Lausanne.getNoOFS());
		tryCreationAndExpectNull(222222001L);

		// Avec commune forme juridique inconnue
		addOrg(MockOrganisationBuilder.createOrganisation(222222003L, 222222003L + 1000000, "abcdef", RegDate.get(2015, 9, 7), null, Lausanne.getNoOFS(), null, null, null, null));
		tryCreationEventAndCheckResult(222222003L, TraitementManuel.class);

		// TODO: A bien vérifier ce qu'on doit faire lorsque le siege n'est pas connu.
		// Avec commune inconnue (pas de no ofs dans le siege)
		addOrg(MockOrganisationBuilder.createOrganisation(222222002L, 222222002L + 1000000, "abcdef", RegDate.get(2015, 9, 7), N_0106_SOCIETE_ANONYME, null, null, null, null, null));
		tryCreationAndExpectNull(222222002L);

		// Cas hors Vaud aucun site sur Vaud
		addOrg(MockOrganisationBuilder.createOrganisationAvecSiteSecondaire(222222004L, 222222004L + 1000000, 222222004L + 2000000, "abcdef", RegDate.get(2015, 9, 7), N_0106_SOCIETE_ANONYME, Zurich.getNoOFS(), Zurich.getNoOFS(), null, null, null, null, null, null, null, null));
		tryCreationAndExpectNull(222222004L);

	}

	private void createAddOrg(long cantonalId, RegDate dateDebut, FormeLegale formeLegale, Integer noOfsSiege) {
		addOrg(MockOrganisationBuilder.createSimpleEntrepriseRC(cantonalId, cantonalId + 1000000, "XYZ", dateDebut, formeLegale, noOfsSiege));
	}

	private void addOrg(Organisation org) {
		((OpenMockServiceOrganisation) serviceOrganisation.getUltimateTarget())
				.addOrganisation(org);
	}

	private void tryCreationAndExpectNull(long noOrganisation) throws EvenementOrganisationException {
		final long evtId = 12121212L;
		final EvenementOrganisation event = new EvenementOrganisation(evtId, IDE, "abcdefg", IDE_NOUVELLE_INSCRIPTION_DANS_REGISTRE, RegDate.get(2015, 9, 7), noOrganisation, A_TRAITER);
		EvenementOrganisationInterne interne = strategy.matchAndCreate(event, serviceOrganisation.getOrganisationHistory(noOrganisation), context, options);
		Assert.assertNull(interne);
	}

	private void tryCreationEventAndCheckResult(long noOrganisation, Class<? extends EvenementOrganisationInterne> resultClass) throws EvenementOrganisationException {
		final long evtId = 12121212L;
		final EvenementOrganisation event = new EvenementOrganisation(evtId, IDE, "abcdefg", IDE_NOUVELLE_INSCRIPTION_DANS_REGISTRE, RegDate.get(2015, 9, 7), noOrganisation, A_TRAITER);
		EvenementOrganisationInterne interne = strategy.matchAndCreate(event, serviceOrganisation.getOrganisationHistory(noOrganisation), context, options);
		Assert.assertNotNull(interne);
		Assert.assertEquals(event.getId(), interne.getNumeroEvenement().longValue());
		Assert.assertEquals(event.getNoOrganisation(), interne.getNoOrganisation());
		Assert.assertEquals(event.getDateEvenement(), interne.getDate());
		Assert.assertEquals(resultClass, interne.getClass());
	}
}