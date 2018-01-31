package ch.vd.uniregctb.evenement.reqdes.reception;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.xml.event.reqdes.v1.InscriptionMode;
import ch.vd.uniregctb.common.WithoutSpringTest;

public class ModeInscriptionDansActeTest extends WithoutSpringTest {

	@Test
	public void testMapping() throws Exception {
		Assert.assertNull(ModeInscriptionDansActe.valueOf((InscriptionMode) null));
		for (InscriptionMode mode : InscriptionMode.values()) {
			final ModeInscriptionDansActe mida = ModeInscriptionDansActe.valueOf(mode);
			Assert.assertNotNull(mode.name(), mida);
		}
	}
}
