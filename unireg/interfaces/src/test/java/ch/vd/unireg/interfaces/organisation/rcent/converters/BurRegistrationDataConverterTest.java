package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.evd0022.v3.BurLocalUnitStatus;
import ch.vd.unireg.interfaces.organisation.data.StatusREE;

public class BurRegistrationDataConverterTest {

	@Test
	public void testMapStatus() throws Exception {
		Assert.assertNull(BurRegistrationDataConverter.mapStatus(null));
		for (BurLocalUnitStatus src : BurLocalUnitStatus.values()) {
			final StatusREE dest = BurRegistrationDataConverter.mapStatus(src);
			Assert.assertNotNull("Valeur " + src + " non-mappée!", dest);
		}
	}
}
