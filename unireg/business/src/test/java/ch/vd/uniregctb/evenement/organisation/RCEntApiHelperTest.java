package ch.vd.uniregctb.evenement.organisation;

import org.junit.Test;

import ch.vd.evd0022.v3.TypeOfNotice;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.evenement.RCEntApiHelper;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Raphaël Marmier, 2015-08-03
 */
public class RCEntApiHelperTest extends WithoutSpringTest {

	@Test
	public void testConvertTypeOfNotice() throws Exception {
		assertEquals(TypeEvenementOrganisation.values().length, TypeOfNotice.values().length);
		for (TypeOfNotice val : TypeOfNotice.values()) {
			assertNotNull(RCEntApiHelper.convertTypeOfNotice(val));
		}
	}
}