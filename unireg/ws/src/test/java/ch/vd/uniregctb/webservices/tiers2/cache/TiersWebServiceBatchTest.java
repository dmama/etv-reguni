package ch.vd.uniregctb.webservices.tiers2.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersDAOImpl;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.impl.TiersWebServiceImpl;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class TiersWebServiceBatchTest extends WebserviceTest {

	public static final Logger LOGGER = Logger.getLogger(TiersWebServiceBatchTest.class);

	private TiersWebServiceCache cache;
	private CrashingTiersDAO crashingTiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final CacheManager manager = CacheManager.create();
		Cache ehcache = new Cache("webServiceTiers2", 1000, false, false, 5, 5);
		manager.addCache(ehcache);

		crashingTiersDAO = new CrashingTiersDAO();
		crashingTiersDAO.setDialect(getBean(Dialect.class, "hibernateDialect"));
		crashingTiersDAO.setSessionFactory(getBean(SessionFactory.class, "sessionFactory"));

		final TiersWebServiceImpl webService = new TiersWebServiceImpl();
		webService.setAdresseService(getBean(AdresseService.class, "adresseService"));
		webService.setAssujettissementService(getBean(AssujettissementService.class, "assujettissementService"));
		webService.setBamMessageSender(getBean(BamMessageSender.class, "bamMessageSender"));
		webService.setDiService(getBean(DeclarationImpotService.class, "diService"));
		webService.setHibernateTemplate(hibernateTemplate);
		webService.setIbanValidator(getBean(IbanValidator.class, "ibanValidator"));
		webService.setInfraService(getBean(ServiceInfrastructureService.class, "serviceInfrastructureService"));
		webService.setLrService(getBean(ListeRecapService.class, "lrService"));
		webService.setParametreService(getBean(ParametreAppService.class, "parametreAppService"));
		webService.setPeriodeImpositionService(getBean(PeriodeImpositionService.class, "periodeImpositionService"));
		webService.setServiceCivil(serviceCivil);
		webService.setServicePM(servicePM);
		webService.setSituationService(getBean(SituationFamilleService.class, "situationFamilleService"));
		webService.setThreadPool(getBean(ExecutorService.class, "wsThreadPool"));
		webService.setTiersDAO(crashingTiersDAO);
		webService.setTiersSearcher(globalTiersSearcher);
		webService.setTiersService(tiersService);
		webService.setTransactionManager(transactionManager);

		cache = new TiersWebServiceCache();
		cache.setCacheManager(manager);
		cache.setTarget(webService);
		cache.setCacheName("webServiceTiers2");
	}

	/**
	 * [SIFISC-6494] Ce test vérifie que le crash d'un thread de mapping de la méthode getBatchTiers ne provoque pas de corruption du cache du web-service.
	 */
	@Test
	public void testCrashMappingThread() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil());

		final List<Long> ids = doInNewTransaction(new TxCallback<List<Long>>() {
			@Override
			public List<Long> execute(TransactionStatus status) throws Exception {
				List<Long> ids = new ArrayList<>();
				for (int i = 0; i < 40; ++i) {
					DebiteurPrestationImposable deb = addDebiteur();
					ids.add(deb.getId());
				}
				return ids;
			}
		});

		// on exécute une requête batch qui va provoquer le crash du thread de mapping
		final GetBatchTiers req = new GetBatchTiers();
		req.login = new UserLogin("[TiersWebServiceCacheTest]", 21);
		req.parts = new HashSet<>();
		req.parts.add(TiersPart.ADRESSES);
		req.parts.add(TiersPart.FORS_FISCAUX);
		req.parts.add(TiersPart.SITUATIONS_FAMILLE);
		req.parts.add(TiersPart.COMPOSANTS_MENAGE);
		req.parts.add(TiersPart.ASSUJETTISSEMENTS);
		req.parts.add(TiersPart.FORS_GESTION);
		req.parts.add(TiersPart.COMPTES_BANCAIRES);
		req.parts.add(TiersPart.ETATS_PM);
		req.parts.add(TiersPart.CAPITAUX);
		req.parts.add(TiersPart.FORMES_JURIDIQUES);
		req.parts.add(TiersPart.REGIMES_FISCAUX);
		req.parts.add(TiersPart.SIEGES);
		req.tiersNumbers = new HashSet<>(ids);

		try {
			crashingTiersDAO.setIdToCrash(ids.get(0));
			// le crash du thread de mapping doit provoquer une exception sur l'appel lui-même
			cache.getBatchTiers(req);
			fail();
		}
		catch (Exception e) {
			assertNotNull(e);
			assertEquals("Exception [Exception de test] dans le thread de mapping du getBatchTiers", e.getMessage());
		}

		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				// on vérifie que le crash de l'appel batch n'a pas corrompu le cache et que les requêtes unitaires passent toujours bien
				for (Long id : ids) {
					final GetTiers r = new GetTiers();
					r.login = new UserLogin("[TiersWebServiceCacheTest]", 21);
					r.parts = new HashSet<>();
					r.parts.add(TiersPart.FORS_FISCAUX_VIRTUELS);
					r.parts.add(TiersPart.DECLARATIONS);
					r.tiersNumber = id;

					final Tiers party = cache.getTiers(r);
					assertNotNull("Le tiers n°" + id + " est null !", party);
					assertEquals(id, party.numero);
				}
			}
		});
	}

	private static class CrashingTiersDAO extends TiersDAOImpl {

		private Long idToCrash;

		public void setIdToCrash(Long idToCrash) {
			this.idToCrash = idToCrash;
		}

		@Override
		public List<ch.vd.uniregctb.tiers.Tiers> getBatch(Collection<Long> ids, Set<Parts> parts) {
			if (ids.contains(idToCrash)) {
				throw new RuntimeException("Exception de test");
			}
			return super.getBatch(ids, parts);
		}
	}
}
