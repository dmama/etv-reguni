package ch.vd.uniregctb.migration.pm.engine;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Assert;

import ch.vd.uniregctb.migration.pm.MigrationResultCollector;
import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.adresse.StreetDataMigrator;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.mapping.IdMapper;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntity;
import ch.vd.uniregctb.tiers.TiersDAO;

public abstract class AbstractEntityMigratorTest extends AbstractMigrationEngineTest {

	private StreetDataMigrator streetDataMigrator;
	private TiersDAO tiersDAO;

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();
		this.tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		this.streetDataMigrator = getBean(StreetDataMigrator.class, "streetDataMigrator");
	}

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public StreetDataMigrator getStreetDataMigrator() {
		return streetDataMigrator;
	}

	/**
	 * Lance une migration d'une entité (à l'intérieur d'une nouvelle transaction)
	 * @param entity entité à migrer
	 * @param migrator migrateur à utiliser
	 * @param <T> type de l'entité à migrer
	 */
	protected <T extends RegpmEntity> void migrate(T entity, EntityMigrator<T> migrator, MigrationResultContextManipulation mr, EntityLinkCollector linkCollector, IdMapper idMapper) {
		doInUniregTransaction(status -> {
			migrator.migrate(entity, mr, linkCollector, idMapper);
			return null;
		});
	}

	/**
	 * Vérifie qu'il y a au moins un message contenant la chaîne de caractères donnée dans la catégorie donnée
	 * @param mr collecteur de messages
	 * @param cat catégorie visée
	 * @param regex expression régulière (potentiellement partielle) recherchée
	 */
	protected static void assertExistMessageWithContent(MigrationResultCollector mr, LogCategory cat, String regex) {
		// s'il n'y a aucun message pour la catégorie, c'est forcément faux
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(cat);
		Assert.assertNotNull(messages);

		final Pattern pattern = Pattern.compile(regex);
		final MigrationResultCollector.Message candidate = messages.stream()
				.filter(msg -> msg.text != null && pattern.matcher(msg.text).find())
				.findAny()
				.orElse(null);

		// on va faire un joli message pour voir tous les messages
		if (candidate == null) {
			final String msgs = messages.stream()
					.map(m -> m.text)
					.collect(Collectors.joining(System.lineSeparator()));
			Assert.fail("Aucun message ne correspond à la regex '" + regex + "' dans la catégorie " + cat + " : \n" + msgs);
		}
	}
}
