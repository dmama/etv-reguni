package ch.vd.unireg.webservices.party3.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import net.sf.ehcache.CacheManager;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.WebserviceTest;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.declaration.source.ListeRecapService;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.jms.BamMessageSender;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.situationfamille.SituationFamilleService;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAOImpl;
import ch.vd.unireg.webservices.party3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.webservices.party3.impl.PartyWebServiceImpl;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.v1.Party;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class PartyWebServiceBatchTest extends WebserviceTest {

	public static final Logger LOGGER = LoggerFactory.getLogger(PartyWebServiceBatchTest.class);

	private PartyWebServiceCache cache;
	private CrashingTiersDAO crashingTiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final CacheManager manager = getBean(CacheManager.class, "ehCacheManager");

		crashingTiersDAO = new CrashingTiersDAO();
		crashingTiersDAO.setDialect(getBean(Dialect.class, "hibernateDialect"));
		crashingTiersDAO.setSessionFactory(getBean(SessionFactory.class, "sessionFactory"));

		final PartyWebServiceImpl webService = new PartyWebServiceImpl();
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
		webService.setServiceEntreprise(serviceEntreprise);
		webService.setSituationService(getBean(SituationFamilleService.class, "situationFamilleService"));
		webService.setThreadPool(getBean(ExecutorService.class, "wsThreadPoolParty3"));
		webService.setTiersDAO(crashingTiersDAO);
		webService.setTiersSearcher(globalTiersSearcher);
		webService.setTiersService(tiersService);
		webService.setTransactionManager(transactionManager);
		webService.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));

		cache = new PartyWebServiceCache();
		cache.setCacheManager(manager);
		cache.setTarget(webService);
		cache.setCacheName("webServiceParty3");
	}

	/**
	 * [SIFISC-6494] Ce test vérifie que le crash d'un thread de mapping de la méthode getBatchParty ne provoque pas de corruption du cache du web-service.
	 */
	@Test
	public void testCrashMappingThread() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil());

		final List<Integer> ids = doInNewTransaction(status -> {
			List<Integer> ids1 = new ArrayList<>();
			for (int i = 0; i < 40; ++i) {
				DebiteurPrestationImposable deb = addDebiteur();
				ids1.add(deb.getId().intValue());
			}
			return ids1;
		});

		// on exécute une requête batch qui va provoquer le crash du thread de mapping
		final GetBatchPartyRequest req = new GetBatchPartyRequest();
		req.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		req.getParts().add(PartyPart.ADDRESSES);
		req.getParts().add(PartyPart.TAX_RESIDENCES);
		req.getParts().add(PartyPart.FAMILY_STATUSES);
		req.getParts().add(PartyPart.HOUSEHOLD_MEMBERS);
		req.getParts().add(PartyPart.SIMPLIFIED_TAX_LIABILITIES);
		req.getParts().add(PartyPart.MANAGING_TAX_RESIDENCES);
		req.getParts().add(PartyPart.BANK_ACCOUNTS);
		req.getParts().add(PartyPart.CORPORATION_STATUSES);
		req.getParts().add(PartyPart.CAPITALS);
		req.getParts().add(PartyPart.LEGAL_FORMS);
		req.getParts().add(PartyPart.TAX_SYSTEMS);
		req.getParts().add(PartyPart.LEGAL_SEATS);
		req.getPartyNumbers().addAll(ids);

		try {
			crashingTiersDAO.setIdToCrash(ids.get(0).longValue());
			// le crash du thread de mapping doit provoquer une exception sur l'appel lui-même
			cache.getBatchParty(req);
			fail();
		}
		catch (WebServiceException e) {
			assertNotNull(e);
			assertEquals("Exception [Exception de test] dans le thread de mapping du getBatchParty", e.getMessage());
		}

		doInNewTransaction(status -> {
			// on vérifie que le crash de l'appel batch n'a pas corrompu le cache et que les requêtes unitaires passent toujours bien
			for (Integer id : ids) {
				final GetPartyRequest r = new GetPartyRequest();
				r.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
				r.getParts().add(PartyPart.VIRTUAL_TAX_RESIDENCES);
				r.getParts().add(PartyPart.TAX_DECLARATIONS);
				r.setPartyNumber(id);

				final Party party;
				try {
					party = cache.getParty(r);
				}
				catch (WebServiceException e) {
					throw new RuntimeException(e);
				}
				assertNotNull("Le tiers n°" + id + " est null !", party);
				assertEquals(id.intValue(), party.getNumber());
			}
			return null;
		});
	}

	private static class CrashingTiersDAO extends TiersDAOImpl {

		private Long idToCrash;

		public void setIdToCrash(Long idToCrash) {
			this.idToCrash = idToCrash;
		}

		@Override
		public List<Tiers> getBatch(Collection<Long> ids, Set<Parts> parts) {
			if (ids.contains(idToCrash)) {
				throw new RuntimeException("Exception de test");
			}
			return super.getBatch(ids, parts);
		}
	}
}
