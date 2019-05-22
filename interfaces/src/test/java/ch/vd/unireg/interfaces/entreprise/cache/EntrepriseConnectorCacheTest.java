package ch.vd.unireg.interfaces.entreprise.cache;


import java.util.Map;

import net.sf.ehcache.CacheManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.cache.UniregCacheManagerImpl;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.data.CivilDataEventListener;
import ch.vd.unireg.data.CivilDataEventService;
import ch.vd.unireg.interfaces.entreprise.EntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.EntrepriseConnectorException;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivileEvent;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.stats.MockStatsService;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;

public class EntrepriseConnectorCacheTest extends WithoutSpringTest {

	private CallCounterEntrepriseConnector target;
	private EntrepriseConnectorCache cache;

	private final CivilDataEventService dataEventService = new CivilDataEventService() {
		@Override
		public void register(CivilDataEventListener listener) {
		}

		@Override
		public void unregister(CivilDataEventListener listener) {
		}

		@Override
		public void onIndividuChange(long id) {
		}

		@Override
		public void onEntrepriseChange(long id) {
		}
	};

	private class CallCounterEntrepriseConnector implements EntrepriseConnector {

		private int historyCounter = 0;
		private final EntrepriseConnector target;

		public CallCounterEntrepriseConnector(EntrepriseConnector target) {
			this.target = target;
		}

		@Override
		public EntrepriseCivile getEntrepriseHistory(long noEntreprise) throws EntrepriseConnectorException {
			historyCounter++;
			return target.getEntrepriseHistory(noEntreprise);
		}

		@Override
		public Long getNoEntrepriseFromNoEtablissement(Long noEtablissementCivil) throws EntrepriseConnectorException {
			return target.getNoEntrepriseFromNoEtablissement(noEtablissementCivil);
		}

		@Override
		public Identifiers getEntrepriseByNoIde(String noide) throws EntrepriseConnectorException {
			return target.getEntrepriseByNoIde(noide);
		}

		@Override
		public Map<Long, EntrepriseCivileEvent> getEntrepriseEvent(long noEvenement) throws EntrepriseConnectorException {
			return target.getEntrepriseEvent(noEvenement);
		}

		@Override
		public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE modele) throws EntrepriseConnectorException {
			return target.validerAnnonceIDE(modele);
		}

		@NotNull
		@Override
		public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws EntrepriseConnectorException {
			return target.findAnnoncesIDE(query, order, pageNumber, resultsPerPage);
		}

		@Override
		public void ping() throws EntrepriseConnectorException {
			target.ping();
		}
	}

	@Before
	public void setup() throws Exception {

		target = new CallCounterEntrepriseConnector(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				 addEntreprise(
						 MockEntrepriseFactory.createEntreprise(101202100L, 872394879L, "Les gentils joueurs de belotte", RegDate.get(2000, 5, 14), null, FormeLegale.N_0109_ASSOCIATION,
						                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, RegDate.get(2000, 5, 16), StatusRegistreIDE.DEFINITIF,
						                                        TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999995"));
				 addEntreprise(
						 MockEntrepriseFactory.createEntreprise(101202101L, 872394812L, "Le Tarot, c'est rigolo", RegDate.get(2005, 1, 11), null, FormeLegale.N_0109_ASSOCIATION,
						                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, RegDate.get(2000, 5, 16), StatusRegistreIDE.DEFINITIF,
						                                        TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996"));
			}
		});

		final CacheManager manager = CacheManager.create(ResourceUtils.getFile("classpath:ut/ehcache.xml").getPath());
		manager.clearAll(); // Manager is a singleton, and may exist already

		cache = new EntrepriseConnectorCache();
		cache.setCacheManager(manager);
		cache.setCacheName("entrepriseConnector");
		cache.setUniregCacheManager(new UniregCacheManagerImpl());
		cache.setStatsService(new MockStatsService());
		cache.setTarget(target);
		cache.setDataEventService(dataEventService);
		cache.afterPropertiesSet();
	}

	@Test
	public void testCacheReturnsCorrectOrganisation() {
		long id = 101202100L;
		final EntrepriseCivile entrepriseCivileFromService = target.getEntrepriseHistory(id);
		final EntrepriseCivile entrepriseCivileFromCache = cache.getEntrepriseHistory(id);
		assertEquals(entrepriseCivileFromService.getNumeroEntreprise(), entrepriseCivileFromCache.getNumeroEntreprise());
	}

	@Test
	public void testCallTwiceHitServiceOnce() {
		Assert.assertEquals(0, target.historyCounter);

		cache.getEntrepriseHistory(101202100L);
		Assert.assertEquals(1, target.historyCounter);
		cache.getEntrepriseHistory(101202100L);
		Assert.assertEquals(1, target.historyCounter);

		cache.getEntrepriseHistory(101202101L);
		Assert.assertEquals(2, target.historyCounter);
		cache.getEntrepriseHistory(101202101L);
		Assert.assertEquals(2, target.historyCounter);

		cache.getEntrepriseHistory(101202100L);
		Assert.assertEquals(2, target.historyCounter);
	}

	@Test
	public void testCallTwiceOrganisationChangesInBetween() {
		long id = 101202100L;
		Assert.assertEquals(0, target.historyCounter);
		cache.getEntrepriseHistory(id);
		Assert.assertEquals(1, target.historyCounter);
		cache.onEntrepriseChange(id);
		cache.getEntrepriseHistory(id);
		Assert.assertEquals(2, target.historyCounter);
	}

}
