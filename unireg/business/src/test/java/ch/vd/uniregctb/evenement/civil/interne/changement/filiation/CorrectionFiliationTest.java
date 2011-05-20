package ch.vd.uniregctb.evenement.civil.interne.changement.filiation;

import net.sf.ehcache.CacheManager;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockHistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilCache;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class CorrectionFiliationTest extends AbstractEvenementCivilInterneTest {

	@Override
	protected EvenementCivilOptions buildOptions() {
		return new EvenementCivilOptions(true);
	}

	/**
	 * Ce test vérifie que l'arrivée d'un événement civil de correction de filiation provoque bien l'invalidation de l'individu correspondant et de ses
	 * parents dans le cache du service civil.
	 */
	@NotTransactional
	@Test
	public void testInvalidationDuCache() throws Exception {

		/*
		 * Préparation
		 */

		final CacheManager cacheManager = getBean(CacheManager.class, "ehCacheManager");
		assertNotNull(cacheManager);

		final DataEventService dataEventService = getBean(DataEventService.class, "dataEventService");
		assertNotNull(dataEventService);

		final UniregCacheManager uniregCacheManager = getBean(UniregCacheManager.class, "uniregCacheManager");
		assertNotNull(uniregCacheManager);

		// Initialisation du service civil avec un cache
		final ServiceCivilCache cache = new ServiceCivilCache();
		cache.setCacheManager(cacheManager);
		cache.setCacheName("serviceCivil");
		cache.setUniregCacheManager(uniregCacheManager);
		cache.setDataEventService(dataEventService);
		cache.afterPropertiesSet();
		cache.reset();
		try {
			serviceCivil.setUp(cache);

			final long jeanNoInd = 1234;
			final long veroNoInd = 1235;
			final long jacquesNoInd = 1233;
			final long martineNoInd = 1232;

			// Création de l'individu
			cache.setTarget(new MockServiceCivil() {
				@Override
				protected void init() {
					final MockIndividu enfant = addIndividu(jeanNoInd, date(1975, 3, 2), "Jacquouille", "Jean", true);
					final MockIndividu enfant2 = addIndividu(veroNoInd, date(1975, 3, 2), "Jacquouille", "Véronique", false);       // soeur jumelle

					final MockIndividu pere = addIndividu(jacquesNoInd, date(1948, 1, 26), "Jacquouille", "Jacques", true);
					enfant.setPere(pere);
					enfant2.setPere(pere);

					final MockIndividu mere = addIndividu(martineNoInd, date(1948, 9, 4), "Jacquouille", "Martine", false);
					enfant.setMere(mere);
					enfant2.setMere(mere);
				}
			});

			// Crée le contribuable correspondant
			final Long jeanId = doInNewTransaction(new TxCallback<Long>() {
				@Override
				public Long execute(TransactionStatus status) throws Exception {
					final PersonnePhysique jean = addHabitant(jeanNoInd);
					addForPrincipal(jean, date(1993, 3, 2), MotifFor.MAJORITE, MockCommune.Lausanne);
					return jean.getNumero();
				}
			});

			// On vérifie que les individus sont bien présents dans le cache
			assertNomIndividu("Jacquouille", "Jean", cache, jeanNoInd);
			assertNomIndividu("Jacquouille", "Véronique", cache, veroNoInd);
			assertNomIndividu("Jacquouille", "Jacques", cache, jacquesNoInd);
			assertNomIndividu("Jacquouille", "Martine", cache, martineNoInd);

			// on change les noms dans le service civil (le cache ne doit pas encore avoir bougé)
			doModificationIndividu(jeanNoInd, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					final HistoriqueIndividu h = new MockHistoriqueIndividu(RegDate.get(2009, 1, 1), "Jacquard", "Jean");
					individu.addHistoriqueIndividu(h);
				}
			});
			doModificationIndividu(veroNoInd, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					final HistoriqueIndividu h = new MockHistoriqueIndividu(RegDate.get(2009, 1, 1), "Jacquard", "Véronique");
					individu.addHistoriqueIndividu(h);
				}
			});
			doModificationIndividu(jacquesNoInd, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					final HistoriqueIndividu h = new MockHistoriqueIndividu(RegDate.get(2009, 1, 1), "Jacquard", "Jacques");
					individu.addHistoriqueIndividu(h);
				}
			});
			doModificationIndividu(martineNoInd, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					final HistoriqueIndividu h = new MockHistoriqueIndividu(RegDate.get(2009, 1, 1), "Jacquard", "Martine");
					individu.addHistoriqueIndividu(h);
				}
			});

			// On vérifie que les individus sont toujours bien présents dans le cache avec l'ancien nom
			assertNomIndividu("Jacquouille", "Jean", cache, jeanNoInd);
			assertNomIndividu("Jacquouille", "Véronique", cache, veroNoInd);
			assertNomIndividu("Jacquouille", "Jacques", cache, jacquesNoInd);
			assertNomIndividu("Jacquouille", "Martine", cache, martineNoInd);

			/*
			 * Traitement d'un événement de correction de filiation
			 */
			final Individu jean = serviceCivil.getIndividu(jeanNoInd, 2400, AttributeIndividu.PARENTS);
			final CorrectionFiliation correction = new CorrectionFiliation(jean, jeanId, null, null, date(2009, 1, 1), MockCommune.Lausanne.getNoOFSEtendu(), context);
			assertSansErreurNiWarning(correction);

			// On vérifie que les individus ont maintenant le nouveau nom (ce qui prouve que le cache des trois individus a été nettoyé)
			// (sauf pour Véronique, évidemment, puisqu'elle n'est pas concernées par la correction de filiation)
			assertNomIndividu("Jacquard", "Jean", cache, jeanNoInd);
			assertNomIndividu("Jacquouille", "Véronique", cache, veroNoInd);
			assertNomIndividu("Jacquard", "Jacques", cache, jacquesNoInd);
			assertNomIndividu("Jacquard", "Martine", cache, martineNoInd);
		}
		finally {
			serviceCivil.tearDown();
			cache.destroy();
		}
	}

	private static void assertNomIndividu(String nom, String prenom, ServiceCivilCache cache, final long noIndividu) {
		final Individu individu = cache.getIndividu(noIndividu, 2400);
		assertNotNull(individu);
		assertEquals(prenom, individu.getDernierHistoriqueIndividu().getPrenom());
		assertEquals(nom, individu.getDernierHistoriqueIndividu().getNom());
	}
}
