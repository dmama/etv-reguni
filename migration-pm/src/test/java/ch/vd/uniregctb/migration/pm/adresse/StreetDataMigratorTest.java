package ch.vd.uniregctb.migration.pm.adresse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.mutable.MutableInt;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.migration.pm.AbstractSpringTest;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAdresseEntreprise;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClient;

@ContextConfiguration(locations = {
		"classpath:spring/regpm.xml",
		"classpath:spring/database.xml",
		"classpath:spring/validation.xml",
		"classpath:spring/interfaces.xml",
		"classpath:spring/migration.xml",
		"classpath:spring/ut-properties.xml"
})
public class StreetDataMigratorTest extends AbstractSpringTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(StreetDataMigratorTest.class);

	private static final String adressesEntreprisesFilename = "adressesEntreprises.data";

	private StreetDataMigratorImpl migrator;

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();
		this.migrator = new StreetDataMigratorImpl();
		this.migrator.setFidorClient(getBean(FidorClient.class, "fidorClient"));
		//noinspection unchecked
		this.migrator.setMappingNosOrdrePoste(getBean(Map.class, "noOrdrePosteProvider"));
	}

	@Test
	@Ignore("Ne doit être activé que pour reconstituer le fichier des adresses...")
	public void testAdresseExtractor() throws Exception {

		// on va chercher toutes les adresses d'entreprises
		final List<RegpmAdresseEntreprise> adresses = doInRegpmTransaction(new TransactionCallback<List<RegpmAdresseEntreprise>>() {
			@Override
			public List<RegpmAdresseEntreprise> doInTransaction(TransactionStatus status) {
				final Session session = getRegpmSessionFactory().getCurrentSession();
				final Query query = session.createQuery("select a from RegpmAdresseEntreprise a");
				//noinspection unchecked
				return query.list();
			}
		});

		serialize(adresses, adressesEntreprisesFilename);
	}

	@Test
	public void testAdressesEntreprise() throws Exception {

		final List<RegpmAdresseEntreprise> adresses;
		final boolean serializedSource = true;
		if (serializedSource) {
			adresses = unserialize(List.class, adressesEntreprisesFilename);
		}
		else {
			// on va chercher toutes les adresses d'entreprises
			adresses = doInRegpmTransaction(new TransactionCallback<List<RegpmAdresseEntreprise>>() {
				@Override
				public List<RegpmAdresseEntreprise> doInTransaction(TransactionStatus status) {
					final Session session = getRegpmSessionFactory().getCurrentSession();
					final Query query = session.createQuery("select a from RegpmAdresseEntreprise a");
					//noinspection unchecked
					return query.list();
				}
			});
		}

		Assert.assertNotNull(adresses);

		final class Data {
			final StreetData streetData;
			final RegpmAdresseEntreprise.PK adresseId;

			Data(StreetData streetData, RegpmAdresseEntreprise.PK adresseId) {
				this.streetData = streetData;
				this.adresseId = adresseId;
			}
		}

		// nombre de réponse par classe...
		final Map<Class<? extends StreetData>, MutableInt> nbParClasse = new HashMap<>();

		final int nbThreads = 4;
		final ExecutorService executorService = Executors.newFixedThreadPool(nbThreads);
		try {
			final CompletionService<Data> completionService = new ExecutorCompletionService<>(executorService);

			// et on boucle !
			int envoyees = 0;
//			for (final RegpmAdresseEntreprise adresse : adresses.subList(0, 10000)) {
			for (final RegpmAdresseEntreprise adresse : adresses) {
				completionService.submit(() -> new Data(migrator.migrate(adresse), adresse.getId()));
				++ envoyees;
			}

			// on a fini de remplir
			executorService.shutdown();

			// et on attend les résultats
			int nbRemaining = envoyees;
			int nbAdressesHorsSuisse = 0;
			while (nbRemaining > 0) {
				final Future<Data> future = completionService.poll(1, TimeUnit.SECONDS);
				if (future != null) {
					-- nbRemaining;

					final Data data = future.get();
					if (data.streetData == null) {
						++ nbAdressesHorsSuisse;
					}
					else {
						if (data.streetData.getEstrid() == null) {
							LOGGER.warn("Adresse " + toString(data.adresseId) + " sans estrid trouvé (" + data.streetData + ")");
						}
						else {
							LOGGER.info("Adresse " + toString(data.adresseId) + " avec estrid trouvé : " + data.streetData.getEstrid());
						}

						final MutableInt nb = nbParClasse.get(data.streetData.getClass());
						if (nb == null) {
							nbParClasse.put(data.streetData.getClass(), new MutableInt(1));
						}
						else {
							nb.increment();
						}
					}
				}
			}

			LOGGER.info(String.format("HS -> %d", nbAdressesHorsSuisse));
			for (Map.Entry<Class<? extends StreetData>, MutableInt> entry : nbParClasse.entrySet()) {
				LOGGER.info(String.format("%s -> %d", entry.getKey().getSimpleName(), entry.getValue().intValue()));
			}
		}
		finally {
			executorService.shutdownNow();
			while (!executorService.isTerminated()) {
				executorService.awaitTermination(1, TimeUnit.SECONDS);
			}
		}
	}

	private static String toString(RegpmAdresseEntreprise.PK pk) {
		return String.format("{idEntreprise=%d, type=%s}", pk.getIdEntreprise(), pk.getTypeAdresse());
	}
}
