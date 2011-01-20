package ch.vd.uniregctb.dao.jdbc;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JdbcTiersDaoTest extends CoreDAOTest {

	private JdbcTiersDao dao = new JdbcTiersDaoImpl();

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	@Test
	public void testGet() throws Exception {

		final Long id = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique tiers = addNonHabitant("Raoul", "Laplanche", date(1967, 3, 4), Sexe.MASCULIN);
				return tiers.getNumero();
			}
		});

		JdbcTemplate template = new JdbcTemplate(dataSource);
		final PersonnePhysique tiers = (PersonnePhysique) dao.get(id, template);
		assertNotNull(tiers);
		assertEquals(id, tiers.getNumero());
		assertEquals("Raoul", tiers.getPrenom());
		assertEquals("Laplanche", tiers.getNom());
		assertEquals(date(1967, 3, 4), tiers.getDateNaissance());
		assertEquals(Sexe.MASCULIN, tiers.getSexe());
	}

	@Test
	public void testGetList() throws Exception {

		class Ids {
			long raoul;
			long samantha;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique raoul = addNonHabitant("Raoul", "Laplanche", date(1967, 3, 4), Sexe.MASCULIN);
				ids.raoul = raoul.getNumero();
				PersonnePhysique samantha = addNonHabitant("Samantha", "Declanché", date(1971, 11, 23), Sexe.FEMININ);
				ids.samantha = samantha.getNumero();
				return null;
			}
		});

		JdbcTemplate template = new JdbcTemplate(dataSource);
		final List<Tiers> list = dao.getList(Arrays.asList(ids.raoul, ids.samantha), template);
		assertNotNull(list);
		assertEquals(2, list.size());

		final PersonnePhysique raoul = (PersonnePhysique) list.get(0);
		assertNotNull(raoul);
		assertEquals(ids.raoul, raoul.getNumero().longValue());
		assertEquals("Raoul", raoul.getPrenom());
		assertEquals("Laplanche", raoul.getNom());
		assertEquals(date(1967, 3, 4), raoul.getDateNaissance());
		assertEquals(Sexe.MASCULIN, raoul.getSexe());

		final PersonnePhysique samantha = (PersonnePhysique) list.get(1);
		assertNotNull(samantha);
		assertEquals(ids.samantha, samantha.getNumero().longValue());
		assertEquals("Samantha", samantha.getPrenom());
		assertEquals("Declanché", samantha.getNom());
		assertEquals(date(1971, 11, 23), samantha.getDateNaissance());
		assertEquals(Sexe.FEMININ, samantha.getSexe());
	}
}
