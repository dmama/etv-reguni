package ch.vd.uniregctb.evenement.organisation.engine;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.engine.translator.EvenementOrganisationTranslatorImpl;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.uniregctb.metier.MetierServicePM;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.type.EmetteurEvenementOrganisation.FOSC;
import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class RadiationProcessorTest extends AbstractEvenementOrganisationProcessorTest {

	private EvenementFiscalDAO evtFiscalDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	protected boolean buildProcessorOnSetup() {
		return false;
	}

	@Test(timeout = 10000L)
	public void testRadiationOrganisation() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 24), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusRC.INSCRIT, StatusInscriptionRC.ACTIF,
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, BigDecimal.valueOf(50000), "CHF");
				MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.changeStatusInscription(RegDate.get(2015, 7, 5), StatusInscriptionRC.RADIE);
				MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) organisation.getDonneesSites().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(RegDate.get(2015, 7, 5), StatusRegistreIDE.RADIE);
				addOrganisation(organisation);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noOrganisation);
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER, FOSC, "rcent-ut");
				return hibernateTemplate.merge(event).getId();
			}
		});


		// Configuration du translator
		EvenementOrganisationTranslatorImpl translator = new EvenementOrganisationTranslatorImpl();

		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAssujettissementService(getBean(AssujettissementService.class, "assujettissementService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.A_VERIFIER, evt.getEtat());

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testRadiationAvecAssujetissement() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 24), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusRC.INSCRIT, StatusInscriptionRC.ACTIF,
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, BigDecimal.valueOf(50000), "CHF");
				MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.changeStatusInscription(RegDate.get(2015, 7, 5), StatusInscriptionRC.RADIE);
				MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) organisation.getDonneesSites().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(RegDate.get(2015, 7, 5), StatusRegistreIDE.RADIE);
				addOrganisation(organisation);

			}
		});

		// Création de l'entreprise

		final Entreprise entreprise = doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noOrganisation);
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER, FOSC, "rcent-ut");
				return hibernateTemplate.merge(event).getId();
			}
		});


		// Configuration du translator
		EvenementOrganisationTranslatorImpl translator = new EvenementOrganisationTranslatorImpl();

		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAssujettissementService(new AssujettissementService() {
			@Override
			public List<Assujettissement> determine(Contribuable ctb) throws AssujettissementException {
				return Collections.singletonList((Assujettissement) new VaudoisOrdinaire(entreprise, RegDate.get(2010, 6, 24), null, MotifFor.DEBUT_EXPLOITATION, null));
			}

			@Override
			public List<Assujettissement> determineRole(ContribuableImpositionPersonnesPhysiques ctb) throws AssujettissementException {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<SourcierPur> determineSource(ContribuableImpositionPersonnesPhysiques ctb) throws AssujettissementException {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<Assujettissement> determinePourCommunes(Contribuable ctb, Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<Assujettissement> determine(Contribuable contribuable, int annee) throws AssujettissementException {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<Assujettissement> determine(Contribuable contribuable, @Nullable DateRange range, boolean collate) throws AssujettissementException {
				throw new UnsupportedOperationException();
			}
		});
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testRadiationIDEPasRC() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 24), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusRC.INSCRIT, StatusInscriptionRC.ACTIF,
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, BigDecimal.valueOf(50000), "CHF");
				MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) organisation.getDonneesSites().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(RegDate.get(2015, 7, 5), StatusRegistreIDE.RADIE);
				addOrganisation(organisation);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noOrganisation);
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER, FOSC, "rcent-ut");
				return hibernateTemplate.merge(event).getId();
			}
		});


		// Configuration du translator
		EvenementOrganisationTranslatorImpl translator = new EvenementOrganisationTranslatorImpl();

		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAssujettissementService(getBean(AssujettissementService.class, "assujettissementService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testRadiationRCPasIDE() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 24), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusRC.INSCRIT, StatusInscriptionRC.ACTIF,
						                                           null,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, BigDecimal.valueOf(50000), "CHF");
				MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.changeStatusInscription(RegDate.get(2015, 7, 5), StatusInscriptionRC.RADIE);
				addOrganisation(organisation);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noOrganisation);
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER, FOSC, "rcent-ut");
				return hibernateTemplate.merge(event).getId();
			}
		});


		// Configuration du translator
		EvenementOrganisationTranslatorImpl translator = new EvenementOrganisationTranslatorImpl();

		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAssujettissementService(getBean(AssujettissementService.class, "assujettissementService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             return null;
			                             }
		                             }
		);
	}
}
