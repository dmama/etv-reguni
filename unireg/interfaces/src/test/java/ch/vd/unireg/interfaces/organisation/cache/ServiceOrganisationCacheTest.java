package ch.vd.unireg.interfaces.organisation.cache;


import java.util.Map;

import net.sf.ehcache.CacheManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.uniregctb.cache.UniregCacheManagerImpl;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.data.CivilDataEventListener;
import ch.vd.uniregctb.data.CivilDataEventService;
import ch.vd.uniregctb.stats.MockStatsService;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;

public class ServiceOrganisationCacheTest extends WithoutSpringTest {

	private CallCounterServiceOrganisation target;
	private ServiceOrganisationCache cache;

	private final CivilDataEventService dataEventService = new CivilDataEventService() {
		@Override
		public void register(CivilDataEventListener listener) {
		}

		@Override
		public void onIndividuChange(long id) {
		}

		@Override
		public void onOrganisationChange(long id) {
		}
	};

	private class CallCounterServiceOrganisation implements ServiceOrganisationRaw {

		private int historyCounter = 0;
		private final ServiceOrganisationRaw target;

		public CallCounterServiceOrganisation(ServiceOrganisationRaw target) {
			this.target = target;
		}

		@Override
		public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
			historyCounter++;
			return target.getOrganisationHistory(noOrganisation);
		}

		@Override
		public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
			return target.getOrganisationPourSite(noSite);
		}

		@Override
		public Identifiers getOrganisationByNoIde(String noide) throws ServiceOrganisationException {
			return target.getOrganisationByNoIde(noide);
		}

		@Override
		public Map<Long, ServiceOrganisationEvent> getOrganisationEvent(long noEvenement) throws ServiceOrganisationException {
			return target.getOrganisationEvent(noEvenement);
		}

		@Override
		public AnnonceIDE getAnnonceIDE(long numero) {
			return target.getAnnonceIDE(numero);
		}

		@Override
		public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE modele) throws ServiceOrganisationException {
			return target.validerAnnonceIDE(modele);
		}

		@NotNull
		@Override
		public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceOrganisationException {
			return target.findAnnoncesIDE(query, order, pageNumber, resultsPerPage);
		}

		@Override
		public void ping() throws ServiceOrganisationException {
			target.ping();
		}
	}

	@Before
	public void setup() throws Exception {

		target = new CallCounterServiceOrganisation(new MockServiceOrganisation() {
			@Override
			protected void init() {
				 addOrganisation(
						 MockOrganisationFactory.createOrganisation(101202100L, 872394879L, "Les gentils joueurs de belotte", RegDate.get(2000, 5, 14), null, FormeLegale.N_0109_ASSOCIATION,
						                                            TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, RegDate.get(2000, 5, 16), StatusRegistreIDE.DEFINITIF,
						                                            TypeOrganisationRegistreIDE.ASSOCIATION, "CHE999999995"));
				 addOrganisation(
				                 MockOrganisationFactory.createOrganisation(101202101L, 872394812L, "Le Tarot, c'est rigolo", RegDate.get(2005, 1, 11), null, FormeLegale.N_0109_ASSOCIATION,
				                                                            TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, RegDate.get(2000, 5, 16), StatusRegistreIDE.DEFINITIF,
				                                                            TypeOrganisationRegistreIDE.ASSOCIATION, "CHE999999996"));
			}
		});

		final CacheManager manager = CacheManager.create(ResourceUtils.getFile("classpath:ut/ehcache.xml").getPath());
		manager.clearAll(); // Manager is a singleton, and may exist already

		cache = new ServiceOrganisationCache();
		cache.setCacheManager(manager);
		cache.setCacheName("serviceOrganisation");
		cache.setUniregCacheManager(new UniregCacheManagerImpl());
		cache.setStatsService(new MockStatsService());
		cache.setTarget(target);
		cache.setDataEventService(dataEventService);
		cache.afterPropertiesSet();
	}

	@Test
	public void testCacheReturnsCorrectOrganisation() {
		long id = 101202100L;
		final Organisation organisationFromService = target.getOrganisationHistory(id);
		final Organisation organisationFromCache = cache.getOrganisationHistory(id);
		assertEquals(organisationFromService.getNumeroOrganisation(), organisationFromCache.getNumeroOrganisation());
	}

	@Test
	public void testCallTwiceHitServiceOnce() {
		assertEquals(0, target.historyCounter);

		cache.getOrganisationHistory(101202100L);
		assertEquals(1, target.historyCounter);
		cache.getOrganisationHistory(101202100L);
		assertEquals(1, target.historyCounter);

		cache.getOrganisationHistory(101202101L);
		assertEquals(2, target.historyCounter);
		cache.getOrganisationHistory(101202101L);
		assertEquals(2, target.historyCounter);

		cache.getOrganisationHistory(101202100L);
		assertEquals(2, target.historyCounter);
	}

	@Test
	public void testCallTwiceOrganisationChangesInBetween() {
		long id = 101202100L;
		assertEquals(0, target.historyCounter);
		cache.getOrganisationHistory(id);
		assertEquals(1, target.historyCounter);
		cache.onOrganisationChange(id);
		cache.getOrganisationHistory(id);
		assertEquals(2, target.historyCounter);
	}

}
