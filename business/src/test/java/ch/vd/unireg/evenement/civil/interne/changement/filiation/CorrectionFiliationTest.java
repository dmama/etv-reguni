package ch.vd.unireg.evenement.civil.interne.changement.filiation;

import java.util.Collections;

import net.sf.ehcache.CacheManager;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.cache.UniregCacheManager;
import ch.vd.unireg.data.CivilDataEventNotifier;
import ch.vd.unireg.data.CivilDataEventNotifierImpl;
import ch.vd.unireg.data.PluggableCivilDataEventNotifier;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.interfaces.civil.cache.IndividuConnectorCache;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CorrectionFiliationTest extends AbstractEvenementCivilInterneTest {

	private PluggableCivilDataEventNotifier pluggableCivilDataEventNotifier;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.pluggableCivilDataEventNotifier = getBean(PluggableCivilDataEventNotifier.class, "civilDataEventNotifier");
	}

	@Override
	public void onTearDown() throws Exception {
		this.pluggableCivilDataEventNotifier.setTarget(null);
		super.onTearDown();
	}

	@Override
	protected EvenementCivilOptions buildOptions() {
		return new EvenementCivilOptions(true);
	}

	/**
	 * Ce test vérifie que l'arrivée d'un événement civil de correction de filiation provoque bien l'invalidation de l'individu correspondant et de ses
	 * parents dans le cache du service civil.
	 */
	@Test
	public void testInvalidationDuCache() throws Exception {

		/*
		 * Préparation
		 */

		final CacheManager cacheManager = getBean(CacheManager.class, "ehCacheManager");
		assertNotNull(cacheManager);

		final CivilDataEventNotifier civilDataEventNotifier = getBean(CivilDataEventNotifier.class, "civilDataEventNotifier");
		assertNotNull(civilDataEventNotifier);

		final UniregCacheManager uniregCacheManager = getBean(UniregCacheManager.class, "uniregCacheManager");
		assertNotNull(uniregCacheManager);

		// Initialisation du service civil avec un cache
		final IndividuConnectorCache cache = new IndividuConnectorCache();
		cache.setCache(cacheManager.getCache("serviceCivil"));
		cache.setUniregCacheManager(uniregCacheManager);
		cache.afterPropertiesSet();
		cache.reset();
		pluggableCivilDataEventNotifier.setTarget(new CivilDataEventNotifierImpl(Collections.singletonList(cache)));

		try {
			serviceCivil.setUp(cache);

			final long jeanNoInd = 1234;
			final long veroNoInd = 1235;
			final long jacquesNoInd = 1233;
			final long martineNoInd = 1232;
			final RegDate datesNaissanceEnfants = date(1975, 3, 2);

			// Création de l'individu
			cache.setTarget(new MockIndividuConnector() {
				@Override
				protected void init() {
					final MockIndividu enfant = addIndividu(jeanNoInd, datesNaissanceEnfants, "Jacquouille", "Jean", true);
					final MockIndividu enfant2 = addIndividu(veroNoInd, datesNaissanceEnfants, "Jacquouille", "Véronique", false);       // soeur jumelle

					final MockIndividu pere = addIndividu(jacquesNoInd, date(1948, 1, 26), "Jacquouille", "Jacques", true);
					final MockIndividu mere = addIndividu(martineNoInd, date(1948, 9, 4), "Jacquouille", "Martine", false);

					addLiensFiliation(enfant, pere, mere, datesNaissanceEnfants, null);
					addLiensFiliation(enfant2, pere, mere, datesNaissanceEnfants, null);
				}
			});

			// Crée le contribuable correspondant
			final Long jeanId = doInNewTransaction(status -> {
				final PersonnePhysique jean = addHabitant(jeanNoInd);
				addForPrincipal(jean, date(1993, 3, 2), MotifFor.MAJORITE, MockCommune.Lausanne);
				return jean.getNumero();
			});

			// On vérifie que les individus sont bien présents dans le cache
			assertNomIndividu("Jacquouille", "Jean", cache, jeanNoInd);
			assertNomIndividu("Jacquouille", "Véronique", cache, veroNoInd);
			assertNomIndividu("Jacquouille", "Jacques", cache, jacquesNoInd);
			assertNomIndividu("Jacquouille", "Martine", cache, martineNoInd);

			// on change les noms dans le service civil (le cache ne doit pas encore avoir bougé)
			doModificationIndividu(jeanNoInd, individu -> individu.setNom("Jacquard"));
			doModificationIndividu(veroNoInd, individu -> individu.setNom("Jacquard"));
			doModificationIndividu(jacquesNoInd, individu -> individu.setNom("Jacquard"));
			doModificationIndividu(martineNoInd, individu -> individu.setNom("Jacquard"));

			// On vérifie que les individus sont toujours bien présents dans le cache avec l'ancien nom
			assertNomIndividu("Jacquouille", "Jean", cache, jeanNoInd);
			assertNomIndividu("Jacquouille", "Véronique", cache, veroNoInd);
			assertNomIndividu("Jacquouille", "Jacques", cache, jacquesNoInd);
			assertNomIndividu("Jacquouille", "Martine", cache, martineNoInd);

			/*
			 * Traitement d'un événement de correction de filiation
			 */
			doInNewTransactionAndSession(status -> {
				final Individu jean = serviceCivil.getIndividu(jeanNoInd, null, AttributeIndividu.PARENTS);
				final CorrectionFiliation correction = new CorrectionFiliation(jean, null, date(2009, 1, 1), MockCommune.Lausanne.getNoOFS(), context);
				assertSansErreurNiWarning(correction);
				return null;
			});

			// On vérifie que les individus ont maintenant le nouveau nom (ce qui prouve que le cache des trois individus a été nettoyé)
			// (sauf pour Véronique, évidemment, puisqu'elle n'est pas concernées par la correction de filiation)
			assertNomIndividu("Jacquard", "Jean", cache, jeanNoInd);
			assertNomIndividu("Jacquouille", "Véronique", cache, veroNoInd);
			assertNomIndividu("Jacquard", "Jacques", cache, jacquesNoInd);
			assertNomIndividu("Jacquard", "Martine", cache, martineNoInd);
		}
		finally {
			cache.destroy();
		}
	}

	private static void assertNomIndividu(String nom, String prenom, IndividuConnectorCache cache, final long noIndividu) {
		final Individu individu = cache.getIndividu(noIndividu, AttributeIndividu.PARENTS); // on demande toujours la même part que pour le traitement de l'événement, pour éviter des effers de bord au niveau du cache
		assertNotNull(individu);
		assertEquals(prenom, individu.getPrenomUsuel());
		assertEquals(nom, individu.getNom());
	}
}
