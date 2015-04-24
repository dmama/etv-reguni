package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.migration.pm.AbstractSpringTest;
import ch.vd.uniregctb.migration.pm.MigrationResultCollector;
import ch.vd.uniregctb.migration.pm.MigrationResultMessage;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.adresse.StreetDataMigrator;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCanton;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntity;
import ch.vd.uniregctb.migration.pm.utils.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.utils.IdMapper;
import ch.vd.uniregctb.tiers.TiersDAO;

@ContextConfiguration(locations = {
		"classpath:spring/regpm.xml",
		"classpath:spring/database.xml",
		"classpath:spring/validation.xml",
		"classpath:spring/interfaces.xml",
		"classpath:spring/migration.xml",
		"classpath:spring/ut-database.xml",
		"classpath:spring/ut-properties.xml"
})
public abstract class AbstractEntityMigratorTest extends AbstractSpringTest {

	protected static final Iterator<Long> ID_GENERATOR = new Iterator<Long>() {
		private final AtomicLong seqNext = new AtomicLong(0);

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public Long next() {
			return seqNext.incrementAndGet();
		}
	};

	public static final RegpmCommune LAUSANNE = buildCommune(RegpmCanton.VD, "Lausanne", MockCommune.Lausanne.getNoOFS());
	public static final RegpmCommune MORGES = buildCommune(RegpmCanton.VD, "Morges", MockCommune.Morges.getNoOFS());
	public static final RegpmCommune ECHALLENS = buildCommune(RegpmCanton.VD, "Echallens", MockCommune.Echallens.getNoOFS());
	public static final RegpmCommune BERN = buildCommune(RegpmCanton.BE, "Bern", MockCommune.Bern.getNoOFS());
	public static final RegpmCommune BALE = buildCommune(RegpmCanton.BS, "Bâle", MockCommune.Bale.getNoOFS());
	public static final RegpmCommune ZURICH = buildCommune(RegpmCanton.ZH, "Zürich", MockCommune.Zurich.getNoOFS());

	private StreetDataMigrator streetDataMigrator;
	private TiersDAO tiersDAO;

	private static RegpmCommune buildCommune(RegpmCanton canton, String nom, int noOfs) {
		final RegpmCommune commune = new RegpmCommune();
		commune.setId(ID_GENERATOR.next());
		commune.setCanton(canton);
		commune.setNom(nom);
		commune.setNoOfs(noOfs);
		return commune;
	}

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
	protected <T extends RegpmEntity> void migrate(T entity, EntityMigrator<T> migrator, MigrationResultProduction mr, EntityLinkCollector linkCollector, IdMapper idMapper) {
		doInUniregTransaction(status -> {
			migrator.migrate(entity, mr, linkCollector, idMapper);
			return null;
		});
	}

	/**
	 * Calcul de numéro de séquence pour un nouvel élément dans une collection (-> max + 1)
	 * @param elements collection dans laquelle on souhaite ajouter un nouvel élément
	 * @param seqNoExtractor extracteur des numéros de séquence des éléments existants
	 * @param <T> type des éléments dans la collection
	 * @return le prochain numéro de séquence disponible
	 */
	protected static <T> int computeNewSeqNo(Collection<T> elements, Function<? super T, Integer> seqNoExtractor) {
		final int biggestSoFar = elements.stream()
				.map(seqNoExtractor)
				.max(Comparator.<Integer>naturalOrder())
				.orElse(0);
		return biggestSoFar + 1;
	}

	/**
	 * Ajoute une période fiscale en base de données Unireg
	 * @param annee l'année de la PF
	 * @return la période fiscale
	 */
	protected PeriodeFiscale addPeriodeFiscale(int annee) {
		final PeriodeFiscale pf = new PeriodeFiscale();
		pf.setAnnee(annee);
		return (PeriodeFiscale) getUniregSessionFactory().getCurrentSession().merge(pf);
	}

	/**
	 * Vérifie qu'il y a au moins un message contenant la chaîne de caractères donnée dans la catégorie donnée
	 * @param mr collecteur de messages
	 * @param cat catégorie visée
	 * @param regex expression régulière (potentiellement partielle) recherchée
	 */
	protected static void assertExistMessageWithContent(MigrationResultCollector mr, MigrationResultMessage.CategorieListe cat, String regex) {
		// s'il n'y a aucun message pour la catégorie, c'est forcément faux
		final List<MigrationResultMessage> messages = mr.getMessages().get(cat);
		Assert.assertNotNull(messages);

		final Pattern pattern = Pattern.compile(regex);
		final MigrationResultMessage candidate = messages.stream()
				.filter(msg -> msg.getTexte() != null && pattern.matcher(msg.getTexte()).find())
				.findAny()
				.orElse(null);

		// on va faire un joli message pour voir tous les messages
		if (candidate == null) {
			final String msgs = messages.stream()
					.map(MigrationResultMessage::getTexte)
					.collect(Collectors.joining(System.lineSeparator()));
			Assert.fail("Aucun message ne correspond à la regex '" + regex + "' dans la catégorie " + cat + " : \n" + msgs);
		}
	}
}
