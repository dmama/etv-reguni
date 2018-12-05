package ch.vd.unireg.tiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.type.Type;

import ch.vd.unireg.hibernate.FillHoleGenerator;

/**
 * @author xciabi
 *
 */
@SuppressWarnings("unused")
public class TiersMultiSequenceGenerator implements Configurable, PersistentIdentifierGenerator {

	/**
	 * Un logger.
	 */
	//private static final Logger LOGGER = LoggerFactory.getLogger(MultiSequenceGenerator.class);

	private static final String CTB_SEQ_NAME = "S_CTB";
	private static final String DPI_SEQ_NAME = "S_DPI";
	private static final String CAAC_SEQ_NAME = "S_CAAC";
	private static final String PM_SEQ_NAME = "S_PM";
	private static final String ETB_SEQ_NAME = "S_ETB";

	private FillHoleGenerator ctbGenerator;
	private PersistentIdentifierGenerator dpiGenerator;
	private PersistentIdentifierGenerator caacGenerator;
	private PersistentIdentifierGenerator pmGenerator;
	private PersistentIdentifierGenerator etbGenerator;

	public TiersMultiSequenceGenerator() {
		super();
	}

	/**
	 * Instancie pour chaque type passé en paramètre un générator d'Id se basant
	 * sur une séquence. Le nom de la séquence est récupéré dans params.
	 *
	 * @see org.hibernate.id.SequenceHiLoGenerator#configure(org.hibernate.type.Type,
	 *      java.util.Properties, org.hibernate.dialect.Dialect)
	 */
	@Override
	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {

		dpiGenerator = createSequence(type, params, dialect, DebiteurPrestationImposable.FIRST_ID, DPI_SEQ_NAME);
		caacGenerator = createSequence(type, params, dialect, AutreCommunaute.CAAC_GEN_FIRST_ID, CAAC_SEQ_NAME);
		pmGenerator = createSequence(type, params, dialect, Entreprise.FIRST_ID, PM_SEQ_NAME);
		etbGenerator = createSequence(type, params, dialect, Etablissement.ETB_GEN_FIRST_ID, ETB_SEQ_NAME);

		ctbGenerator = new FillHoleGenerator("TIERS", CTB_SEQ_NAME, ContribuableImpositionPersonnesPhysiques.CTB_GEN_FIRST_ID, ContribuableImpositionPersonnesPhysiques.CTB_GEN_LAST_ID);
		ctbGenerator.configure(type, params, dialect);
	}

	private PersistentIdentifierGenerator createSequence(Type type, Properties params, Dialect dialect, int sequenceOffset, String seqName) {

		Properties properties = new Properties();
		properties.putAll(params);
		properties.put("sequence_name", seqName);
		properties.put("initial_value", String.valueOf(sequenceOffset));

		SequenceStyleGenerator generator = new SequenceStyleGenerator();
		generator.configure(type, properties, dialect);
		return generator;
	}

	/**
	 * Génère un ID pour l'objet passé en paramètre. Se base sur la classe de
	 * l'objet pour récupérér le SequenceGenerator adéquat.
	 *
	 * @see org.hibernate.id.SequenceHiLoGenerator#generate(org.hibernate.engine.spi.SessionImplementor, java.lang.Object)
	 */
	@Override
	public synchronized Serializable generate(SessionImplementor session, Object object) throws HibernateException {

		Tiers tiers = (Tiers) object;
		Long numeroTiers = tiers.getNumero();
		final Serializable sequenceNumber;
		if (numeroTiers != null && numeroTiers > 0) {
			// De 1 à 999'999
			if (object instanceof Entreprise) {
				assertIdBetween(Entreprise.FIRST_ID, Entreprise.LAST_ID, numeroTiers, object);
			}
			// De 1'500'000 à 1'999'999 (Note : les DPI migrés ont des numéros de 1'000'000 à 1'500'000)
			else if (object instanceof DebiteurPrestationImposable) {
				assertIdBetween(DebiteurPrestationImposable.FIRST_MIGRATION_ID, DebiteurPrestationImposable.LAST_ID, numeroTiers, object);
			}
			// De 2'000'000 à 2'999'999
			else if (object instanceof CollectiviteAdministrative || object instanceof AutreCommunaute) {
				assertIdBetween(AutreCommunaute.CAAC_GEN_FIRST_ID, AutreCommunaute.CAAC_GEN_LAST_ID, numeroTiers, object);
			}
			// De 3'000'000 à 3'999'999
			else if (object instanceof Etablissement) {
				assertIdBetween(Etablissement.ETB_GEN_FIRST_ID, Etablissement.ETB_GEN_LAST_ID, numeroTiers, object);
			}
			// De 10'000'000 à 99'999'999
			else if (object instanceof ContribuableImpositionPersonnesPhysiques) {
				assertIdBetween(ContribuableImpositionPersonnesPhysiques.CTB_GEN_FIRST_ID, ContribuableImpositionPersonnesPhysiques.CTB_GEN_LAST_ID, numeroTiers, object);
			}
			else {
				throw new IllegalArgumentException("Classe " + object.getClass().getSimpleName() + " inconnue");
			}
			sequenceNumber = numeroTiers;
		}
		else {
			// Les Entreprise migrées ont un numéro de 1 à 999'999
			if (object instanceof Entreprise) {
				sequenceNumber = pmGenerator.generate(session, object);
			}
			//
			// Les DPI migrés ont des numéros de 1'000'000 à 1'500'000
			//
			// De 1'500'000 à 1'999'999
			else if (object instanceof DebiteurPrestationImposable) {
				sequenceNumber = dpiGenerator.generate(session, object);
			}
			// De 2'000'000 à 2'999'999
			else if (object instanceof CollectiviteAdministrative || object instanceof AutreCommunaute) {
				sequenceNumber = caacGenerator.generate(session, object);
			}
			// De 3'000'000 à 3'999'999
			else if (object instanceof Etablissement) {
				sequenceNumber = etbGenerator.generate(session, object);
			}
			// De 10'000'000 à 99'999'999
			else if (object instanceof PersonnePhysique || object instanceof MenageCommun) {
				sequenceNumber = ctbGenerator.generate(session, object);
			}
			else {
				throw new IllegalArgumentException("Classe " + object.getClass().getSimpleName() + " inconnue");
			}
		}
		return sequenceNumber;
	}

	private static void assertIdBetween(long firstId, long lastId, long id, Object object) {
		if (id < firstId || id > lastId) {
			final String tiersType = object.getClass().getSimpleName();
			final String message = String.format(
					"L'id %d pour le type de tiers de type %s doit impérativement être compris dans la plage de %d à %d.", id, tiersType,
					firstId, lastId);
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * The SQL required to remove the underlying database objects.
	 *
	 * @param dialect
	 * @return String
	 * @throws HibernateException
	 */
	@Override
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {

		final List<String> sqlDropStrings = new ArrayList<>();
		sqlDropStrings.addAll(Arrays.asList(pmGenerator.sqlDropStrings(dialect)));
		sqlDropStrings.addAll(Arrays.asList(caacGenerator.sqlDropStrings(dialect)));
		sqlDropStrings.addAll(Arrays.asList(dpiGenerator.sqlDropStrings(dialect)));
		sqlDropStrings.addAll(Arrays.asList(ctbGenerator.sqlDropStrings(dialect)));
		sqlDropStrings.addAll(Arrays.asList(etbGenerator.sqlDropStrings(dialect)));

		return sqlDropStrings.toArray(new String[0]);
	}

	/**
	 * The SQL required to create the underlying database objects.
	 *
	 * @param dialect
	 * @return String[]
	 * @throws HibernateException
	 */
	@Override
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {

		List<String> sqlCreateStrings = new ArrayList<>();
		sqlCreateStrings.addAll(Arrays.asList(pmGenerator.sqlCreateStrings(dialect)));
		sqlCreateStrings.addAll(Arrays.asList(caacGenerator.sqlCreateStrings(dialect)));
		sqlCreateStrings.addAll(Arrays.asList(dpiGenerator.sqlCreateStrings(dialect)));
		sqlCreateStrings.addAll(Arrays.asList(ctbGenerator.sqlCreateStrings(dialect)));
		sqlCreateStrings.addAll(Arrays.asList(etbGenerator.sqlCreateStrings(dialect)));

		return sqlCreateStrings.toArray(new String[0]);
	}

	@Override
	public Object generatorKey() {
		return CTB_SEQ_NAME;
	}

}
