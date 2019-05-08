package ch.vd.unireg.hibernate;

import java.sql.Connection;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionImpl;
import org.hibernate.type.StandardBasicTypes;
import org.junit.Test;

import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.migreg.MigrationError;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.PersonnePhysique;

import static org.junit.Assert.assertEquals;

public class FillHoleGeneratorTest extends CoreDAOTest {

	private static final long firstId = ContribuableImpositionPersonnesPhysiques.CTB_GEN_FIRST_ID;
	private SessionFactory sessionFactory;
	private FillHoleGenerator generator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		sessionFactory = getBean(SessionFactory.class, "sessionFactory");

		generator = new FillHoleGenerator("TIERS", "S_CTB", ContribuableImpositionPersonnesPhysiques.CTB_GEN_FIRST_ID, ContribuableImpositionPersonnesPhysiques.CTB_GEN_LAST_ID);
		generator.configure(StandardBasicTypes.LONG, new Properties(), sessionFactory.getSessionFactoryOptions().getServiceRegistry());

		resetSequence();
	}

	private void resetSequence() throws Exception {
		try (Connection con = dataSource.getConnection()) {
			generator.dropAndCreateSequence(con);
		}
	}

	@Test
	public void testGenerateEmptyDb() throws Exception {
		assertEquals(firstId + 1, nextId().longValue());
		assertEquals(firstId + 2, nextId().longValue());
		assertEquals(firstId + 3, nextId().longValue());
		assertEquals(firstId + 4, nextId().longValue());
		assertEquals(firstId + 5, nextId().longValue());
	}

	@Test
	public void testGeneratePartialFilledDb() throws Exception {
		addIds(firstId + 1, firstId + 2, firstId + 5);

		assertEquals(firstId + 3, nextId().longValue());
		assertEquals(firstId + 4, nextId().longValue());
		assertEquals(firstId + 6, nextId().longValue());
		assertEquals(firstId + 7, nextId().longValue());
		assertEquals(firstId + 8, nextId().longValue());
	}

	@Test
	public void testGenerateAvecErreursMigration1() throws Exception {
		addIds(firstId + 1, firstId + 2, firstId + 5, firstId + 20);
		addErrorIds(firstId + 7, firstId + 9);

		assertEquals(firstId + 3, nextId().longValue());
		assertEquals(firstId + 4, nextId().longValue());
		assertEquals(firstId + 6, nextId().longValue());
		assertEquals(firstId + 8, nextId().longValue());
		assertEquals(firstId + 10, nextId().longValue());
	}

	/**
	 * [UNIREG-726] vérifie que les ids des ctbs non-migrés ne puisse pas être utilisés pour de nouveaux ctbs
	 */
	@Test
	public void testGenerateAvecErreursMigration2() throws Exception {
		addIds(firstId + 4, firstId + 5, firstId + 6, firstId + 7, firstId + 8, firstId + 10, firstId + 12);
		addErrorIds(firstId + 1, firstId + 2, firstId + 3, firstId + 9);

		assertEquals(firstId + 11, nextId().longValue());
		assertEquals(firstId + 13, nextId().longValue());
		assertEquals(firstId + 14, nextId().longValue());
		assertEquals(firstId + 15, nextId().longValue());
		assertEquals(firstId + 16, nextId().longValue());
	}

	@Test
	public void testGenerateAvecUniquementErreursMigration() throws Exception {
		addErrorIds(firstId + 4, firstId + 5, firstId + 7);

		assertEquals(firstId + 1, nextId().longValue());
		assertEquals(firstId + 2, nextId().longValue());
		assertEquals(firstId + 3, nextId().longValue());
		assertEquals(firstId + 6, nextId().longValue());
		assertEquals(firstId + 8, nextId().longValue());
	}

	/**
	 * Cas particulier où l'id d'un ctb non-migré est <b>plus grand</b> que le plus grand id des ctb migrés.
	 */
	@Test
	public void testGenerateAvecErreursMigrationCasParticulier() throws Exception {
		addIds(firstId + 1, firstId + 2, firstId + 5);
		addErrorIds(firstId + 7, firstId + 9);

		assertEquals(firstId + 3, nextId().longValue());
		assertEquals(firstId + 4, nextId().longValue());
		assertEquals(firstId + 6, nextId().longValue());
		assertEquals(firstId + 8, nextId().longValue());
		assertEquals(firstId + 10, nextId().longValue());
	}

	/**
	 * Ajoute les ids spécifié dans la table TIERs
	 */
	private void addIds(final Long... ids) {
		doInNewTransaction(status -> {
			for (Long id : ids) {
				final PersonnePhysique pp = new PersonnePhysique(false);
				pp.setNumero(id);
				pp.setNom(buildNomTiers(id));
				hibernateTemplate.merge(pp);
			}
			return null;
		});
	}

	private static String buildNomTiers(long index) {
		final StringBuilder b = new StringBuilder("pp-");
		final String indexStr = Long.toString(index);
		for (char c : indexStr.toCharArray()) {
			b.append((char) ((c - '0') + 'A'));
		}
		return b.toString();
	}

	/**
	 * Ajoute les ids spécifié dans la table MIGREG_ERROR
	 */
	private void addErrorIds(final Long... ids) {
		doInNewTransaction(status -> {
			for (Long id : ids) {
				MigrationError e = new MigrationError();
				e.setNoContribuable(id);
				e.setMessage("PP-" + id);
				hibernateTemplate.merge(e);
			}
			return null;
		});
	}

	private Long nextId() {
		return doInNewTransaction(status -> {
			final SessionImpl session = (SessionImpl) sessionFactory.getCurrentSession();
			return (Long) generator.generate(session, new PersonnePhysique());
		});
	}
}
