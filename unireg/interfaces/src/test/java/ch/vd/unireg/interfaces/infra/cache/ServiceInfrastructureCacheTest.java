package ch.vd.unireg.interfaces.infra.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheManager;
import org.apache.commons.lang.mutable.MutableInt;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeEtatPM;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.cache.UniregCacheManagerImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class ServiceInfrastructureCacheTest {

	private ServiceInfrastructureCache cache;
	private TestService target;

	@Before
	public void setup() throws Exception {
		cache = new ServiceInfrastructureCache();
		final CacheManager manager = CacheManager.create(ResourceUtils.getFile("classpath:ut/ehcache.xml").getPath());
		cache.setCacheManager(manager);
		cache.setCacheName("serviceInfra");
		cache.setShortLivedCacheName("serviceInfraShortLived");
		cache.setUniregCacheManager(new UniregCacheManagerImpl());
		target = new TestService();
		cache.setTarget(target);
		cache.afterPropertiesSet();
	}

	@Test
	public void testGetCommuneHistoByNumeroOfsMonoThread() {
		final List<Commune> list = cache.getCommuneHistoByNumeroOfs(1);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(1, list.get(0).getNoOFSEtendu());
	}

	private static final int ITERATIONS = 2; // à augmenter pour stresser plus le cache si nécessaire
	private static final int TAILLE_CACHE = 5000; // ne doit pas dépasser la taille du cache 'serviceInfra' configuré dans 'ut/ehcache.xml'
	private static final int NB_THREADS = 4;

	/**
	 * Ce test à pour but de tester le fonctionnement correct du cache du service infrastructure, et plus particulièrement dans le cas où la taille maximum d'éléments en mémoire n'est pas dépassée, de
	 * s'assurer que le cache enregistre bien toutes les données (= tous les appels subséquents doivent retourner la même instance que le premier appel).
	 */
	@Test
	public void testGetCommuneHistoByNumeroOfsMultiThreads() throws Exception {

		// plusieurs listes (une par thread) de numéros Ofs de commune à demander
		final int tailleList = TAILLE_CACHE / NB_THREADS;
		final List<List<Integer>> lists = new ArrayList<List<Integer>>(NB_THREADS);
		int c = 0;
		List<Integer> last = null;
		for (int i = 0; i < NB_THREADS; ++i) {
			List<Integer> list = new ArrayList<Integer>(tailleList);
			for (int j = 0; j < tailleList; j++) {
				list.add(c++);
			}
			lists.add(list);
			last = list;
		}
		assert last != null;
		for (; c < TAILLE_CACHE; ++c) { // pour compenser l'erreur d'arrondi lors du calcul de TAILLE_LIST en cas de division non-entière
			last.add(c);
		}

		for (int t = 0; t < ITERATIONS; ++t) {

			if (t % 100 == 0) {
				System.out.println("Iteration #" + t);
			}

			// des threads qui chacun vont :
			//  - appeler la méthode 'getCommuneHistoByNumeroOfs' sur tous les numéros Ofs de la première liste
			//  - attendre que les autres threads aient fini de processer leurs premières listes
			//  - appeler la méthode 'getCommuneHistoByNumeroOfs' sur tous les autres numéros Ofs
			final List<List<Commune>> results = new ArrayList<List<Commune>>(NB_THREADS);
			final List<GetCommuneHistoByNumeroOfsThread> threads = new ArrayList<GetCommuneHistoByNumeroOfsThread>(NB_THREADS);
			final MutableInt rendezvous = new MutableInt(NB_THREADS);

			for (int i = 0; i < NB_THREADS; ++i) {
				final List<Integer> first = lists.get(i);
				final List<Integer> others = new ArrayList<Integer>();
				for (int j = 0; j < NB_THREADS; ++j) {
					if (j != i) {
						others.addAll(lists.get(j));
					}
				}
				final ArrayList<Commune> res = new ArrayList<Commune>(TAILLE_CACHE);
				results.add(res);
				threads.add(new GetCommuneHistoByNumeroOfsThread(first, others, res, rendezvous));
			}

			cache.reset();
			target.resetCalls();

			for (GetCommuneHistoByNumeroOfsThread thread : threads) {
				thread.start();
			}
			for (GetCommuneHistoByNumeroOfsThread thread : threads) {
				thread.join();
			}

			assertEquals(TAILLE_CACHE, target.getCalls());

			for (List<Commune> r : results) {
				Collections.sort(r, new TestCommuneComparator());
			}

			// on s'assure que toutes les listes :
			//  - sont de tailles identiques
			//  - possèdent des communes équivalentes (= égalité)
			//  - possèdent des instances de communes identiques (= identité)
			// ce dernier point permet de vérifier que le cache fonctionne correctement
			for (int i = 0; i < results.size(); i++) {
				List<Commune> resi = results.get(i);
				for (int j = 0; j < results.size(); j++) {
					List<Commune> resj = results.get(j);
					if (i != j) {
						assertIdentical(resi, resj);
					}
				}
			}
		}
	}

	private static void assertIdentical(List<Commune> results1, List<Commune> results2) {
		assertEquals(results1.size(), results2.size());
		for (int k = 0; k < results1.size(); k++) {
			final Commune commune1 = results1.get(k);
			final Commune commune2 = results2.get(k);
			assertEquals(commune1.getNoOFSEtendu(), commune2.getNoOFSEtendu());
			assertEquals(commune1.hashCode(), commune2.hashCode());
		}
	}

	private class GetCommuneHistoByNumeroOfsThread extends Thread {

		private final List<Integer> listA;
		private final List<Integer> listB;
		private final List<Commune> results;
		private final MutableInt rendezvous;

		public GetCommuneHistoByNumeroOfsThread(List<Integer> listA, List<Integer> listB, List<Commune> results, MutableInt rendezvous) {
			this.listA = listA;
			this.listB = listB;
			this.results = results;
			this.rendezvous = rendezvous;
		}

		@Override
		public void run() {

			for (Integer noOfs : listA) {

				final List<Commune> list = cache.getCommuneHistoByNumeroOfs(noOfs);
				assertNotNull(list);
				assertEquals(1, list.size());

				final Commune commune = list.get(0);
				assertEquals(noOfs.intValue(), commune.getNoOFSEtendu());
				results.add(commune);
			}

			// on attend que les autres threads aient processé leurs premières listes avant de passer à la deuxième
			synchronized (rendezvous) {
				rendezvous.decrement();
				rendezvous.notifyAll();
				while (rendezvous.intValue() > 0) {
					try {
						rendezvous.wait();
					}
					catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}

			for (Integer noOfs : listB) {

				final List<Commune> list = cache.getCommuneHistoByNumeroOfs(noOfs);
				assertNotNull(list);
				assertEquals(1, list.size());

				final Commune commune = list.get(0);
				assertEquals(noOfs.intValue(), commune.getNoOFSEtendu());
				results.add(commune);
			}
		}
	}

	private static class TestService implements ServiceInfrastructureRaw {

		private AtomicInteger calls = new AtomicInteger(0);

		public void resetCalls() {
			calls.set(0);
		}

		public int getCalls() {
			return calls.get();
		}

		@Override
		public List<Pays> getPays() throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public Pays getPays(int numeroOFS) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public Pays getPays(String codePays) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<Canton> getAllCantons() throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<Commune> getCommunes() throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<Localite> getLocalites() throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public Localite getLocaliteByONRP(int onrp) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<Rue> getRues(Canton canton) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public Rue getRueByNumero(int numero) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
			calls.incrementAndGet();
//			System.out.println(String.format("[thread %d] getCommuneHistoByNumeroOfs(%d)", Thread.currentThread().getId(), noOfsCommune));
			List<Commune> list = new ArrayList<Commune>(1);
			list.add(new TestCommune(noOfsCommune));
			return list;
		}

		@Override
		public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public Localite getLocaliteByNPA(int npa) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public List<TypeEtatPM> getTypesEtatsPM() throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public TypeEtatPM getTypeEtatPM(String code) throws ServiceInfrastructureException {
			throw new NotImplementedException();
		}

		@Override
		public String getUrlVers(ApplicationFiscale application, Long tiersId, Integer oid) {
			throw new NotImplementedException();
		}

		@Override
		public Logiciel getLogiciel(Long id) {
			throw new NotImplementedException();
		}

		@Override
		public List<Logiciel> getTousLesLogiciels() {
			throw new NotImplementedException();
		}

	}

	private static class TestCommune implements Commune {
		private final int noOfsCommune;

		public TestCommune(int noOfsCommune) {
			this.noOfsCommune = noOfsCommune;
		}

		@Override
		public RegDate getDateDebutValidite() {
			throw new NotImplementedException();
		}

		@Override
		public RegDate getDateFinValidite() {
			throw new NotImplementedException();
		}

		@Override
		public int getNoOFSEtendu() {
			return noOfsCommune;
		}

		@Override
		public int getNumTechMere() {
			return 0;
		}

		@Override
		public int getNumeroTechnique() {
			return noOfsCommune;
		}

		@Override
		public String getSigleCanton() {
			throw new NotImplementedException();
		}

		@Override
		public boolean isVaudoise() {
			return true;
		}

		@Override
		public boolean isFraction() {
			return false;
		}

		@Override
		public boolean isPrincipale() {
			return false;
		}

		@Override
		public District getDistrict() {
			throw new NotImplementedException();
		}

		@Override
		public String getNomMajuscule() {
			throw new NotImplementedException();
		}

		@Override
		public String getNomMinuscule() {
			throw new NotImplementedException();
		}

		@Override
		public int getNoOFS() {
			return noOfsCommune;
		}

		@Override
		public String getSigleOFS() {
			throw new NotImplementedException();
		}
	}

	private static class TestCommuneComparator implements Comparator<Commune> {
		@Override
		public int compare(Commune o1, Commune o2) {
			return o1.getNoOFSEtendu() - o2.getNoOFSEtendu();
		}
	}
}
