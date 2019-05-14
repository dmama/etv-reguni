package ch.vd.unireg.migreg;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.CoreDAOTest;

public class MigrationErrorDAOTest extends CoreDAOTest {

	private MigrationErrorDAO dao;

	@Override
	public void onSetUp() throws Exception {
		dao = getBean(MigrationErrorDAO.class, "migrationErrorDAO");
		super.onSetUp();

		doInNewTransaction(status -> {
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
			return null;
		});
	}

	@Test
	public void testGetErrorForContribuable() throws Exception {

		final long id = 12345678;

		saveError(id);

		doInNewTransaction(status -> {
			MigrationError error = dao.getErrorForContribuable(id);
			Assert.assertEquals(Long.valueOf(id), error.getNoContribuable());
			return null;
		});
	}

	@Test
	public void testExistsForContribuable() throws Exception {

		final long id = 12345678;

		doInNewTransaction(status -> {
			Assert.assertEquals(2, dao.getAll().size());
			Assert.assertFalse(dao.existsForContribuable(id));
			return null;
		});

		saveError(id);

		doInNewTransaction(status -> {
			Assert.assertEquals(3, dao.getAll().size());
			Assert.assertTrue(dao.existsForContribuable(id));
			return null;
		});
	}

	@Test
	public void testUniqueNumeroContribuable() throws Exception {

		final long id = 12345678;

		doInNewTransaction(status -> {
			Assert.assertEquals(2, dao.getAll().size());
			return null;
		});

		saveError(id);

		doInNewTransaction(status -> {
			Assert.assertEquals(3, dao.getAll().size());
			return null;
		});

		try {
			saveError(id);
			Assert.fail();
		}
		catch (Exception ignored) {
		}

		doInNewTransaction(status -> {
			status.setRollbackOnly();
			Assert.assertEquals(3, dao.getAll().size());
			return null;
		});
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
		doInNewTransaction(status -> {
			MigrationError error = new MigrationError();
			error.setNoContribuable(id);
			dao.save(error);
			return null;
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void getAllNoCtb() {
		List<Long> list = dao.getAllNoCtb();
		Assert.assertEquals(2, list.size());
	}

}
