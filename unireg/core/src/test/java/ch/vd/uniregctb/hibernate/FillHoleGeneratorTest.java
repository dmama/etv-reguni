package ch.vd.uniregctb.hibernate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Properties;

import org.hibernate.Hibernate;
import org.hibernate.impl.AbstractSessionImpl;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.migreg.MigrationError;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;

import static junit.framework.Assert.assertEquals;

public class FillHoleGeneratorTest extends CoreDAOTest {

	private static final long firstId = Contribuable.CTB_GEN_FIRST_ID;
	private HibernateTemplate hibernateTemplate;
	private FillHoleGenerator generator;

	private DataSource rawDataSource;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		rawDataSource = getBean(DataSource.class, "rawDataSource");

		generator = new FillHoleGenerator("TIERS", "S_CTB", Contribuable.CTB_GEN_FIRST_ID, Contribuable.CTB_GEN_LAST_ID);
		generator.configure(Hibernate.LONG, new Properties(), dialect);

		resetSequence();
	}

	private void resetSequence() throws Exception {

		String[] drops = generator.sqlDropStrings(dialect);
		String[] creates = generator.sqlCreateStrings(dialect);

		Connection con = null;
		try {
			con = rawDataSource.getConnection();

			for (String d : drops) {
				final PreparedStatement st = con.prepareStatement(d);
				try {
					st.execute();
				}
				finally {
					st.close();
				}
			}

			for (String c : creates) {
				final PreparedStatement st = con.prepareStatement(c);
				try {
					st.execute();
				}
				finally {
					st.close();
				}
			}
		}
		finally {
			if (con != null) {
				con.close();
			}
		}
	}

	@Test
	public void testGenerateEmptyDb() {
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
	private void addIds(final Long... ids) throws Exception {
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				for (Long id : ids) {
					final PersonnePhysique pp = new PersonnePhysique(false);
					pp.setNumero(id);
					pp.setNom(buildNomTiers(id));
					hibernateTemplate.save(pp);
				}
				return null;
			}
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
	private void addErrorIds(final Long... ids) throws Exception {
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				for (Long id : ids) {
					MigrationError e = new MigrationError();
					e.setNoContribuable(id);
					e.setMessage("PP-" + id);
					hibernateTemplate.save(e);
				}
				return null;
			}
		});
	}

	private Long nextId() {
		AbstractSessionImpl session = (AbstractSessionImpl) hibernateTemplate.getSessionFactory().getCurrentSession();
		Long id = (Long) generator.generate(session, new PersonnePhysique());
		return id;
	}
}
