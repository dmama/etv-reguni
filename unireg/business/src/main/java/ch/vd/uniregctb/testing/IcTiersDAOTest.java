package ch.vd.uniregctb.testing;

import java.util.List;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

public class IcTiersDAOTest extends InContainerTest {

	@Test
	@Rollback
	public void execute() throws Exception {

		long id;
		{
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Bla");
			nh.setPrenom("Bli");
			nh = (PersonnePhysique)getTiersDAO().save(nh);
			id = nh.getNumero();
		}

		{
			List<Tiers> list = getTiersDAO().getAll();
			Assert.isEqual(1, list.size());
			Assert.isEqual(id, list.get(0).getNumero());
		}

		{
			Assert.isTrue(getTiersDAO().exists(id));
		}
	}

}
