package ch.vd.uniregctb.evenement.organisation.engine;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationCappingLevelProvider;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationErreur;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationService;
import ch.vd.uniregctb.evenement.organisation.engine.translator.EvenementOrganisationTranslatorImpl;
import ch.vd.uniregctb.evenement.organisation.engine.translator.NiveauCappingEtat;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.uniregctb.metier.MetierServicePM;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.regimefiscal.ServiceRegimeFiscal;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.rattrapage.appariement.AppariementService;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementErreur;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * Test de quelques cas de capping en erreur
 */
public class CappingEnErrorTest extends AbstractEvenementOrganisationProcessorTest {

	private EvenementFiscalDAO evtFiscalDAO;

	public CappingEnErrorTest() {
		setWantIndexationTiers(true);
	}

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	@Override
	protected boolean buildProcessorOnSetup() {
		return false;
	}

	@Test(timeout = 10000L)
	public void testCreationPM() throws Exception {

		// Mise en place service mock
		final long noOrganisation = 101202100L;
		final long noSitePrincipal = noOrganisation + 1000000L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(
						MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noSitePrincipal, "Synergy SA", RegDate.get(2015, 6, 27), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                 MockCommune.Lausanne));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 27), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// construction d'un translator cappé
		final EvenementOrganisationTranslatorImpl translator = new EvenementOrganisationTranslatorImpl();
		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setServiceRegimeFiscal(getBean(ServiceRegimeFiscal.class, "serviceRegimeFiscal"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementOrganisationService(getBean(EvenementOrganisationService.class, "evtOrganisationService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setCappingLevelProvider(new EvenementOrganisationCappingLevelProvider() {
			@Override
			public NiveauCappingEtat getNiveauCapping() {
				return NiveauCappingEtat.EN_ERREUR;
			}
		});
		translator.afterPropertiesSet();
		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du presque-traitement de l'événement (= tous les messages sont enregistrés, mais au final rien n'est fait !)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());
				                             Assert.assertEquals(10, evt.getErreurs().size());

				                             {
					                             final EvenementOrganisationErreur erreur = evt.getErreurs().get(0);
					                             Assert.assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
					                             Assert.assertEquals("Aucune entreprise identifiée pour le numéro civil " + noOrganisation + " ou les attributs civils [Synergy SA, IDE: CHE-999.999.996].", erreur.getMessage());
				                             }
				                             {
					                             final EvenementOrganisationErreur erreur = evt.getErreurs().get(1);
					                             Assert.assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
					                             Assert.assertEquals("Mutation : Création d'une entreprise vaudoise", erreur.getMessage());
				                             }

				                             final long idEntreprise;
				                             {
					                             final EvenementOrganisationErreur erreur = evt.getErreurs().get(2);
					                             Assert.assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());

					                             final Pattern pattern = Pattern.compile("Entreprise créée avec le numéro de contribuable ([0-9.]+) pour l'organisation n°" + noOrganisation);
					                             final Matcher matcher = pattern.matcher(erreur.getMessage());
					                             Assert.assertTrue(matcher.matches());

					                             idEntreprise = Long.parseLong(matcher.group(1).replaceAll("\\.", StringUtils.EMPTY));
				                             }
				                             {
					                             final EvenementOrganisationErreur erreur = evt.getErreurs().get(3);
					                             Assert.assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
					                             Assert.assertEquals("Réglage de l'état: Inscrite au RC.", erreur.getMessage());
				                             }
				                             {
					                             final EvenementOrganisationErreur erreur = evt.getErreurs().get(6);
					                             Assert.assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
					                             Assert.assertEquals("Régimes fiscaux par défaut [01 - Ordinaire] VD et CH ouverts pour l'entreprise n°" + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + " (civil: " + noOrganisation + ")", erreur.getMessage());
				                             }

				                             final long idEtablissementPrincipal;
				                             {
					                             final EvenementOrganisationErreur erreur = evt.getErreurs().get(4);
					                             Assert.assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());

					                             final Pattern pattern = Pattern.compile("Etablissement principal créé avec le numéro ([0-9.]+) pour le site " + noSitePrincipal + Pattern.quote(", domicile Lausanne (VD) (ofs: 5586), à partir du 24.06.2015"));
					                             final Matcher matcher = pattern.matcher(erreur.getMessage());
					                             Assert.assertTrue(matcher.matches());

					                             idEtablissementPrincipal = Long.parseLong(matcher.group(1).replaceAll("\\.", StringUtils.EMPTY));
				                             }
				                             {
					                             final EvenementOrganisationErreur erreur = evt.getErreurs().get(5);
					                             Assert.assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
					                             Assert.assertEquals("Application de la surcharge civile entre le 24.06.2015 et le 26.06.2015 avec les valeurs du 27.06.2015", erreur.getMessage());
				                             }
				                             {
					                             final EvenementOrganisationErreur erreur = evt.getErreurs().get(7);
					                             Assert.assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
					                             Assert.assertEquals("Ouverture d'un for fiscal principal à Lausanne (VD) à partir du 25.06.2015, motif ouverture Début d'activité, rattachement DOMICILE, pour l'entreprise n°" + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + " (civil: " + noOrganisation + ").", erreur.getMessage());
				                             }
				                             {
					                             final EvenementOrganisationErreur erreur = evt.getErreurs().get(8);
					                             Assert.assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
					                             Assert.assertEquals("Bouclement créé avec une périodicité de 12 mois à partir du 31.12.2015", erreur.getMessage());
				                             }
				                             {
					                             final EvenementOrganisationErreur erreur = evt.getErreurs().get(9);
					                             Assert.assertEquals(TypeEvenementErreur.ERROR, erreur.getType());
					                             Assert.assertEquals("Evénement explicitement placé 'en erreur' par configuration applicative. Toutes les modifications apportées pendant le traitement sont abandonnées.", erreur.getMessage());
					                             Assert.assertNull(erreur.getCallstack());
				                             }

				                             // mais en fait, rien n'a été créé...

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
				                             Assert.assertNull(entreprise);

				                             final Entreprise entrepriseFiscale = (Entreprise) tiersDAO.get(idEntreprise);
				                             Assert.assertNull(entrepriseFiscale);
				                             final Etablissement etablissementFiscal = (Etablissement) tiersDAO.get(idEtablissementPrincipal);
				                             Assert.assertNull(etablissementFiscal);

				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             return null;
			                             }
		                             }
		);
	}

}
