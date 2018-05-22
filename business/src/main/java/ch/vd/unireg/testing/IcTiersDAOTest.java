package ch.vd.unireg.testing;

import java.util.List;
import java.util.Objects;

import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;

public class IcTiersDAOTest extends InContainerTest {

	@Test
	@Rollback
	public void execute() throws Exception {

		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom("Bla");
		nh.setPrenomUsuel("Bli");
		nh = (PersonnePhysique) getTiersDAO().save(nh);
		long id = nh.getNumero();

		List<Tiers> list = getTiersDAO().getAll();
		if (list.size() != 1) {
			throw new IllegalArgumentException();
		}
		if (!Objects.equals(id, list.get(0).getNumero())) {
			throw new IllegalArgumentException();
		}
		if (!getTiersDAO().exists(id)) {
			throw new IllegalArgumentException();
		}
	}

}
