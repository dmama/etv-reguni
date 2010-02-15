package ch.vd.uniregctb.migreg;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.common.CoreDAOTest;

public class MigrationErrorDAOTest extends CoreDAOTest {

	private MigrationErrorDAO dao;

	@Override
	public void onSetUp() throws Exception {
		dao = getBean(MigrationErrorDAO.class, "migrationErrorDAO");
		super.onSetUp();
	}

	@Override
	protected void doLoadDatabase() throws Exception {
		super.doLoadDatabase();

		{
			MigrationError error = new MigrationError();
			error.setNoContribuable(1234L);
			dao.save(error);
		}
		{
			MigrationError error = new MigrationError();
			error.setNoContribuable(1235L);
			dao.save(error);
		}
	}

	@Test
	@NotTransactional
	public void testGetErrorForContribuable() throws Exception {

		final long id = 12345678;

		saveError(id);

		MigrationError error = dao.getErrorForContribuable(id);
		Assert.assertEquals(new Long(id), error.getNoContribuable());
	}

	@Test
	@NotTransactional
	public void testExistsForContribuable() throws Exception {

		final long id = 12345678;

		Assert.assertEquals(2, dao.getAll().size());
		Assert.assertFalse(dao.existsForContribuable(id));

		saveError(id);

		Assert.assertEquals(3, dao.getAll().size());
		Assert.assertTrue(dao.existsForContribuable(id));
	}

	@Test
	@NotTransactional
	public void testUniqueNumeroContribuable() throws Exception {

		final long id = 12345678;

		Assert.assertEquals(2, dao.getAll().size());

		saveError(id);

		Assert.assertEquals(3, dao.getAll().size());

		try {
			saveError(id);
			Assert.fail();
		}
		catch (Exception ignored) {
			ignored = null;
		}

		Assert.assertEquals(3, dao.getAll().size());
	}

	public void testRemoveForContribuable() throws Exception {

		final long id1 = 12345678;
		final long id2 = 12345679;

		saveError(id1);
		saveError(id2);

		List<?> list = dao.getAll();
		Assert.assertEquals(2, list.size());

		dao.removeForContribuable(id1);

		list = dao.getAll();
		Assert.assertEquals(1, list.size());
	}

	private void saveError(final long id) throws Exception {
		doExecuteInTransaction(Propagation.REQUIRES_NEW, new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {

				{
					MigrationError error = new MigrationError();
					error.setNoContribuable(id);
					dao.save(error);
				}
				return null;
			}
		});
	}

	@Test
	public void getAllNoCtb() {
		List<Long> list = dao.getAllNoCtb();
		Assert.assertEquals(2, list.size());
	}

}
