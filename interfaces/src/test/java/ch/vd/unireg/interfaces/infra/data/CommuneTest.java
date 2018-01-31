package ch.vd.unireg.interfaces.infra.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CommuneTest {

	@Test
	public void testLink2OfsId() throws Exception {
		assertNull(CommuneImpl.link2OfsId(" "));
		assertEquals(Integer.valueOf(124), CommuneImpl.link2OfsId("124"));
		assertEquals(Integer.valueOf(4), CommuneImpl.link2OfsId("districtFiscal/4"));
		assertEquals(Integer.valueOf(1), CommuneImpl.link2OfsId("regionFiscale/1"));
		assertEquals(Integer.valueOf(5871), CommuneImpl.link2OfsId("communeFiscale/5871I19600101"));
	}
}
