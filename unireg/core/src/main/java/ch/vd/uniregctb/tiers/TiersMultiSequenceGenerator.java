package ch.vd.uniregctb.tiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.type.Type;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.hibernate.FillHoleGenerator;

/**
 * @author xciabi
 *
 */
public class TiersMultiSequenceGenerator implements Configurable, PersistentIdentifierGenerator {

	/**
	 * Un logger.
	 */
	//private static final Logger LOGGER = Logger.getLogger(MultiSequenceGenerator.class);

	private static final String CTB_SEQ_NAME = "S_CTB";
	private static final String DPI_SEQ_NAME = "S_DPI";
	private static final String PM_SEQ_NAME = "S_PM";

	private FillHoleGenerator ctbGenerator;
	private PersistentIdentifierGenerator dpiGenerator;
	private PersistentIdentifierGenerator pmGenerator;

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
	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {

		dpiGenerator = createSequence(type, params, dialect, DebiteurPrestationImposable.FIRST_ID, DPI_SEQ_NAME);
		pmGenerator = createSequence(type, params, dialect, Entreprise.PM_GEN_FIRST_ID, PM_SEQ_NAME);

		ctbGenerator = new FillHoleGenerator("TIERS", CTB_SEQ_NAME, Contribuable.CTB_GEN_FIRST_ID, Contribuable.CTB_GEN_LAST_ID);
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
	 * @see org.hibernate.id.SequenceHiLoGenerator#generate(org.hibernate.engine.SessionImplementor,
	 *      java.lang.Object)
	 */
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
			else if (object instanceof CollectiviteAdministrative || object instanceof AutreCommunaute || object instanceof Etablissement) {
				assertIdBetween(Entreprise.PM_GEN_FIRST_ID, Entreprise.PM_GEN_LAST_ID, numeroTiers, object);
			}
			// De 10'000'000 à 99'999'999
			else if (object instanceof PersonnePhysique || object instanceof MenageCommun) {
				assertIdBetween(Contribuable.CTB_GEN_FIRST_ID, Contribuable.CTB_GEN_LAST_ID, numeroTiers, object);
			}
			else {
				Assert.fail("Classe " + object.getClass().getSimpleName() + " inconnue");
			}
			sequenceNumber = numeroTiers;
		}
		else {
			// Les Entreprise migrées ont un numéro de 1 à 999'999
			// Ce numéro vient de SIMPA-PM
			if (object instanceof Entreprise) {
				Assert.fail("Pas de génération de numéros pour les Entreprise");
				sequenceNumber = null;
			}
			//
			// Les DPI migrés ont des numéros de 1'000'000 à 1'500'000
			//
			// De 1'500'000 à 1'999'999
			else if (object instanceof DebiteurPrestationImposable) {
				sequenceNumber = dpiGenerator.generate(session, object);
			}
			// De 2'000'000 à 2'999'999
			else if (object instanceof CollectiviteAdministrative || object instanceof AutreCommunaute || object instanceof Etablissement) {
				sequenceNumber = pmGenerator.generate(session, object);
			}
			// De 10'000'000 à 99'999'999
			else if (object instanceof PersonnePhysique || object instanceof MenageCommun) {
				sequenceNumber = ctbGenerator.generate(session, object);
			}
			else {
				Assert.fail("Classe " + object.getClass().getSimpleName() + " inconnue");
				sequenceNumber = null;
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
			Assert.fail(message);
		}
	}

	/**
	 * The SQL required to remove the underlying database objects.
	 *
	 * @param dialect
	 * @return String
	 * @throws HibernateException
	 */
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {

		List<String> sqlDropStrings = new ArrayList<String>();
		sqlDropStrings.addAll(Arrays.asList(pmGenerator.sqlDropStrings(dialect)));
		sqlDropStrings.addAll(Arrays.asList(dpiGenerator.sqlDropStrings(dialect)));
		sqlDropStrings.addAll(Arrays.asList(ctbGenerator.sqlDropStrings(dialect)));

		return sqlDropStrings.toArray(new String[sqlDropStrings.size()]);
	}

	/**
	 * The SQL required to create the underlying database objects.
	 *
	 * @param dialect
	 * @return String[]
	 * @throws HibernateException
	 */
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {

		List<String> sqlCreateStrings = new ArrayList<String>();
		sqlCreateStrings.addAll(Arrays.asList(pmGenerator.sqlCreateStrings(dialect)));
		sqlCreateStrings.addAll(Arrays.asList(dpiGenerator.sqlCreateStrings(dialect)));
		sqlCreateStrings.addAll(Arrays.asList(ctbGenerator.sqlCreateStrings(dialect)));

		return sqlCreateStrings.toArray(new String[sqlCreateStrings.size()]);
	}

	public Object generatorKey() {
		return CTB_SEQ_NAME;
	}

}
