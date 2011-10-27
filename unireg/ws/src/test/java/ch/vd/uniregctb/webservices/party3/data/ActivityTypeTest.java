package ch.vd.uniregctb.webservices.party3.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.relation.v1.ActivityType;
import ch.vd.uniregctb.webservices.party3.EnumTest;
import ch.vd.uniregctb.webservices.party3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class ActivityTypeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(ActivityType.class, ch.vd.uniregctb.type.TypeActivite.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeActivite) null));
		assertEquals(ActivityType.MAIN, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeActivite.PRINCIPALE));
		assertEquals(ActivityType.ACCESSORY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeActivite.ACCESSOIRE));
		assertEquals(ActivityType.COMPLEMENTARY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeActivite.COMPLEMENTAIRE));
	}
}
