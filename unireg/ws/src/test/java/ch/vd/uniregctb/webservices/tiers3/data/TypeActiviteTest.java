package ch.vd.uniregctb.webservices.tiers3.data;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers3.EnumTest;
import ch.vd.uniregctb.webservices.tiers3.TypeActivite;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class TypeActiviteTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(TypeActivite.class, ch.vd.uniregctb.type.TypeActivite.class);
		assertEnumConstantsEqual(TypeActivite.class, ch.vd.uniregctb.type.TypeActivite.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeActivite) null));
		assertEquals(TypeActivite.PRINCIPALE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeActivite.PRINCIPALE));
		assertEquals(TypeActivite.ACCESSOIRE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeActivite.ACCESSOIRE));
		assertEquals(TypeActivite.COMPLEMENTAIRE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeActivite.COMPLEMENTAIRE));
	}
}
