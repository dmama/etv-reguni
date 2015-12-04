package ch.vd.uniregctb.evenement.reqdes.reception;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.xml.event.reqdes.v1.StakeholderRole;
import ch.vd.uniregctb.common.WithoutSpringTest;

public class RoleDansActeTest extends WithoutSpringTest {

	@Test
	public void testMapping() throws Exception {
		Assert.assertNull(RoleDansActe.valueOf((StakeholderRole) null));
		for (StakeholderRole role : StakeholderRole.values()) {
			final RoleDansActe rda = RoleDansActe.valueOf(role);
			Assert.assertNotNull(role.name(), rda);
		}
	}
}
