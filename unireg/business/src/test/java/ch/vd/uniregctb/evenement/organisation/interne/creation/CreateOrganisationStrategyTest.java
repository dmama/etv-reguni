package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractEvenementOrganisationInterneTest;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.MessageSuiviPreExecution;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MockTiersDAO;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static ch.vd.unireg.interfaces.infra.mock.MockCommune.Lausanne;
import static ch.vd.unireg.interfaces.infra.mock.MockCommune.Zurich;
import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;
import static ch.vd.uniregctb.type.TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION;

/**
 * @author Raphaël Marmier, 2015-09-07
 */
public class CreateOrganisationStrategyTest extends AbstractEvenementOrganisationInterneTest {

	public CreateOrganisationStrategyTest() {
		setWantIndexationTiers(true);
	}

	private CreateOrganisationStrategy strategy;

	/**
	 * Version du service mock ou on peut ajouter des organisations à la demande.
	 */
	public static class OpenMockServiceOrganisation extends MockServiceOrganisation {
		@Override
		protected void init() {}

		public void addOrganisation(MockOrganisation org) {
			super.addOrganisation(org);
		}
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		// Mise en place services mock
		this.serviceOrganisation.setUp(new OpenMockServiceOrganisation());

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
		createAddOrg(101220101L, RegDate.get(2015, 9, 7), FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE, Lausanne);
		tryCreationEventAndCheckResult(101220101L, MessageSuiviPreExecution.class);

		createAddOrg(101220103L, RegDate.get(2015, 9, 7), FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF, Lausanne);
		tryCreationEventAndCheckResult(101220103L, CreateEntrepriseSP.class);

		createAddOrg(101220104L, RegDate.get(2015, 9, 7), FormeLegale.N_0104_SOCIETE_EN_COMMANDITE, Lausanne);
		tryCreationEventAndCheckResult(101220104L, CreateEntrepriseSP.class);

		createAddOrg(101220105L, RegDate.get(2015, 9, 7), FormeLegale.N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS, Lausanne);
		tryCreationEventAndCheckResult(101220105L, CreateEntreprisePM.class);

		createAddOrg(101220106L, RegDate.get(2015, 9, 7), FormeLegale.N_0106_SOCIETE_ANONYME, Lausanne);
		tryCreationEventAndCheckResult(101220106L, CreateEntreprisePM.class);

		createAddOrg(101220107L, RegDate.get(2015, 9, 7), FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, Lausanne);
		tryCreationEventAndCheckResult(101220107L, CreateEntreprisePM.class);

		createAddOrg(101220108L, RegDate.get(2015, 9, 7), FormeLegale.N_0108_SOCIETE_COOPERATIVE, Lausanne);
		tryCreationEventAndCheckResult(101220108L, CreateEntreprisePM.class);

		createAddOrg(101220109L, RegDate.get(2015, 9, 7), FormeLegale.N_0109_ASSOCIATION, Lausanne);
		tryCreationEventAndCheckResult(101220109L, CreateEntrepriseAPMAuRC.class);

		createAddOrg(101220110L, RegDate.get(2015, 9, 7), FormeLegale.N_0110_FONDATION, Lausanne);
		tryCreationEventAndCheckResult(101220110L, CreateEntrepriseAPMAuRC.class);

		createAddOrg(101220111L, RegDate.get(2015, 9, 7), FormeLegale.N_0111_FILIALE_ETRANGERE_AU_RC, Lausanne);
		tryCreationEventAndCheckResult(101220111L, TraitementManuel.class);

		createAddOrg(101220113L, RegDate.get(2015, 9, 7), FormeLegale.N_0113_FORME_JURIDIQUE_PARTICULIERE, Lausanne);
		tryCreationEventAndCheckResult(101220113L, TraitementManuel.class);

		createAddOrg(101220114L, RegDate.get(2015, 9, 7), FormeLegale.N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX, Lausanne);
		tryCreationEventAndCheckResult(101220114L, CreateEntrepriseFDSPLAC.class);

		createAddOrg(101220115L, RegDate.get(2015, 9, 7), FormeLegale.N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE, Lausanne);
		tryCreationEventAndCheckResult(101220115L, CreateEntrepriseFDSPLAC.class);

		createAddOrg(101220116L, RegDate.get(2015, 9, 7), FormeLegale.N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE, Lausanne);
		tryCreationEventAndCheckResult(101220116L, CreateEntrepriseFDSPLAC.class);

		createAddOrg(101220117L, RegDate.get(2015, 9, 7), FormeLegale.N_0117_INSTITUT_DE_DROIT_PUBLIC, Lausanne);
		tryCreationEventAndCheckResult(101220117L, TraitementManuel.class);

		createAddOrg(101220118L, RegDate.get(2015, 9, 7), FormeLegale.N_0118_PROCURATIONS_NON_COMMERCIALES, Lausanne);
		tryCreationEventAndCheckResult(101220118L, TraitementManuel.class);

		createAddOrg(101220119L, RegDate.get(2015, 9, 7), FormeLegale.N_0119_CHEF_INDIVISION, Lausanne);
		tryCreationEventAndCheckResult(101220119L, TraitementManuel.class);

		createAddOrg(101220151L, RegDate.get(2015, 9, 7), FormeLegale.N_0151_SUCCURSALE_SUISSE_AU_RC, Lausanne);
		tryCreationEventAndCheckResult(101220151L, TraitementManuel.class);

		createAddOrg(101220220L, RegDate.get(2015, 9, 7), FormeLegale.N_0220_ADMINISTRATION_CONFEDERATION, Lausanne);
		tryCreationEventAndCheckResult(101220220L, TraitementManuel.class);

		createAddOrg(101220221L, RegDate.get(2015, 9, 7), FormeLegale.N_0221_ADMINISTRATION_CANTON, Lausanne);
		tryCreationEventAndCheckResult(101220221L, TraitementManuel.class);

		createAddOrg(101220222L, RegDate.get(2015, 9, 7), FormeLegale.N_0222_ADMINISTRATION_DISTRICT, Lausanne);
		tryCreationEventAndCheckResult(101220222L, TraitementManuel.class);

		createAddOrg(101220223L, RegDate.get(2015, 9, 7), FormeLegale.N_0223_ADMINISTRATION_COMMUNE, Lausanne);
		tryCreationEventAndCheckResult(101220223L, TraitementManuel.class);

		createAddOrg(101220224L, RegDate.get(2015, 9, 7), FormeLegale.N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION, Lausanne);
		tryCreationEventAndCheckResult(101220224L, TraitementManuel.class);

		createAddOrg(101220230L, RegDate.get(2015, 9, 7), FormeLegale.N_0230_ENTREPRISE_CONFEDERATION, Lausanne);
		tryCreationEventAndCheckResult(101220230L, TraitementManuel.class);

		createAddOrg(101220231L, RegDate.get(2015, 9, 7), FormeLegale.N_0231_ENTREPRISE_CANTON, Lausanne);
		tryCreationEventAndCheckResult(101220231L, TraitementManuel.class);

		createAddOrg(101220232L, RegDate.get(2015, 9, 7), FormeLegale.N_0232_ENTREPRISE_DISTRICT, Lausanne);
		tryCreationEventAndCheckResult(101220232L, TraitementManuel.class);

		createAddOrg(101220233L, RegDate.get(2015, 9, 7), FormeLegale.N_0233_ENTREPRISE_COMMUNE, Lausanne);
		tryCreationEventAndCheckResult(101220233L, TraitementManuel.class);

		createAddOrg(101220234L, RegDate.get(2015, 9, 7), FormeLegale.N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE, Lausanne);
		tryCreationEventAndCheckResult(101220234L, CreateEntrepriseDPPM.class);

		createAddOrg(101220302L, RegDate.get(2015, 9, 7), FormeLegale.N_0302_SOCIETE_SIMPLE, Lausanne);
		tryCreationEventAndCheckResult(101220302L, MessageSuiviPreExecution.class);

		createAddOrg(101220312L, RegDate.get(2015, 9, 7), FormeLegale.N_0312_FILIALE_ETRANGERE_NON_AU_RC, Lausanne);
		tryCreationEventAndCheckResult(101220312L, TraitementManuel.class);

		createAddOrg(101220327L, RegDate.get(2015, 9, 7), FormeLegale.N_0327_ENTREPRISE_PUBLIQUE_ETRANGERE, Lausanne);
		tryCreationEventAndCheckResult(101220327L, TraitementManuel.class);

		createAddOrg(101220328L, RegDate.get(2015, 9, 7), FormeLegale.N_0328_ADMINISTRATION_PUBLIQUE_ETRANGERE, Lausanne);
		tryCreationEventAndCheckResult(101220328L, TraitementManuel.class);

		createAddOrg(101220329L, RegDate.get(2015, 9, 7), FormeLegale.N_0329_ORGANISATION_INTERNATIONALE, Lausanne);
		tryCreationEventAndCheckResult(101220329L, TraitementManuel.class);

		createAddOrg(101220441L, RegDate.get(2015, 9, 7), FormeLegale.N_0441_ENTREPRISE_ETRANGERE, Lausanne);
		tryCreationEventAndCheckResult(101220441L, TraitementManuel.class);

		createAddOrg(101220441L, RegDate.get(2015, 9, 7), null, Lausanne);
		tryCreationEventAndCheckResult(101220441L, TraitementManuel.class);
	}

	@Test
	public void testCasEntrepriseHCAvecSiegeSecondaireVaud() throws EvenementOrganisationException {
		// Cas hors Vaud avec un site secondaire sur Vaud
		addOrg(MockOrganisationFactory.createOrganisationAvecSiteSecondaire(101220106L, 101220106L + 1000000, 101220106L + 2000000, "abcdef", RegDate.get(2015, 9, 7), null, FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                                    TypeAutoriteFiscale.COMMUNE_HC, Zurich.getNoOFS(),
		                                                                    TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 1, 2),
		                                                                    StatusInscriptionRC.ACTIF, date(2015, 9, 4), StatusRegistreIDE.DEFINITIF,
		                                                                    StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999992", "CHE999999993"));
		tryCreationEventAndCheckResult(101220106L, CreateEntrepriseHorsVD.class);
	}


	@Test
	public void testCasSpeciaux() throws Exception {

		// Avec une entreprise existante
		createAddOrg(222222001L, RegDate.get(2010, 1, 1), FormeLegale.N_0106_SOCIETE_ANONYME, Lausanne);
		tryCreationAndExpectNull(222222001L);

		// Avec commune forme juridique inconnue
		addOrg(MockOrganisationFactory.createOrganisation(222222003L, 222222003L + 1000000, "abcdef", RegDate.get(2015, 9, 7), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, Lausanne.getNoOFS(), null, null, null, null, "CHE999999994"));
		tryCreationEventAndCheckResult(222222003L, TraitementManuel.class);

		// TODO: A bien vérifier ce qu'on doit faire lorsque le siege n'est pas connu.
		// Avec commune inconnue (pas de no ofs dans le siege)
		addOrg(MockOrganisationFactory.createOrganisation(222222002L, 222222002L + 1000000, "abcdef", RegDate.get(2015, 9, 7), null, FormeLegale.N_0106_SOCIETE_ANONYME, null, null, null, null, null, null, "CHE999999995"));
		tryCreationEventAndCheckResult(222222002L, TraitementManuel.class);

		// Cas hors Vaud aucun site sur Vaud
		addOrg(MockOrganisationFactory.createOrganisationAvecSiteSecondaire(222222004L, 222222004L + 1000000, 222222004L + 2000000, "abcdef", RegDate.get(2015, 9, 7), null, FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                                    TypeAutoriteFiscale.COMMUNE_HC, Zurich.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, Zurich.getNoOFS(),
		                                                                    null, null, null, null, null, null, null, null, "CHE999999996", "CHE999999997"));
		tryCreationEventAndCheckResult(222222004L, MessageSuiviPreExecution.class);

	}

	private void createAddOrg(long cantonalId, RegDate dateDebut, FormeLegale formeLegale, MockCommune commune) {
		addOrg(MockOrganisationFactory.createSimpleEntrepriseRC(cantonalId, cantonalId + 1000000, "XYZ", dateDebut, null, formeLegale, commune));
	}

	private void createAddOrg(long cantonalId, RegDate dateDebut, FormeLegale formeLegale, MockPays pays) {
		addOrg(MockOrganisationFactory.createSimpleEntrepriseRC(cantonalId, cantonalId + 1000000, "XYZ", dateDebut, null, formeLegale, pays));
	}

	private void addOrg(MockOrganisation org) {
		((OpenMockServiceOrganisation) serviceOrganisation.getUltimateTarget())
				.addOrganisation(org);
	}

	private void tryCreationAndExpectNull(long noOrganisation) throws EvenementOrganisationException {
		final long noEvenement = 12121212L;
		final EvenementOrganisation event = new EvenementOrganisation(noEvenement, IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 9, 7), noOrganisation, A_TRAITER);
		Organisation organisation = serviceOrganisation.getOrganisationHistory(noOrganisation);
		final Entreprise entreprise = context.getTiersDAO().getEntrepriseByNumeroOrganisation(organisation.getNumeroOrganisation());

		EvenementOrganisationInterne interne = strategy.matchAndCreate(event, organisation, entreprise, context, options);

		Assert.assertNull(interne);
	}

	private void tryCreationEventAndCheckResult(long noOrganisation, Class<? extends EvenementOrganisationInterne> resultClass) throws EvenementOrganisationException {
		final long noEvenement = 12121212L;
		final EvenementOrganisation event = new EvenementOrganisation(noEvenement, IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 9, 7), noOrganisation, A_TRAITER);
		Organisation organisation = serviceOrganisation.getOrganisationHistory(noOrganisation);
		final Entreprise entreprise = context.getTiersDAO().getEntrepriseByNumeroOrganisation(organisation.getNumeroOrganisation());

		EvenementOrganisationInterne interne = strategy.matchAndCreate(event, organisation, entreprise, context, options);

		Assert.assertNotNull(interne);
		Assert.assertEquals(event.getNoEvenement(), interne.getNumeroEvenement().longValue());
		Assert.assertEquals(event.getNoOrganisation(), interne.getNoOrganisation());
		Assert.assertEquals(event.getDateEvenement(), interne.getDateEvt());
		Assert.assertEquals(resultClass, interne.getClass());
	}
}