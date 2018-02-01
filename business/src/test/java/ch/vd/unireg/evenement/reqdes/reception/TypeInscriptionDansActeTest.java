package ch.vd.uniregctb.evenement.reqdes.reception;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.xml.event.reqdes.v1.InscriptionType;
import ch.vd.uniregctb.common.WithoutSpringTest;

public class TypeInscriptionDansActeTest extends WithoutSpringTest {

	@Test
	public void testMapping() throws Exception {
		Assert.assertNull(TypeInscriptionDansActe.valueOf((InscriptionType) null));
		for (InscriptionType mode : InscriptionType.values()) {
			final TypeInscriptionDansActe tida = TypeInscriptionDansActe.valueOf(mode);
			Assert.assertNotNull(mode.name(), tida);
		}
	}
}
