package ch.vd.uniregctb.evenement.organisation;

import org.junit.Test;

import ch.vd.evd0022.v1.SenderIdentification;
import ch.vd.evd0022.v1.TypeOfNotice;
import ch.vd.uniregctb.type.EmetteurEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Raphaël Marmier, 2015-08-03
 */
public class EvenementOrganisationConversionHelperTest {

	@Test
	public void testConvertSenderIdentification() throws Exception {
		assertEquals(EmetteurEvenementOrganisation.values().length, SenderIdentification.values().length);
		for (SenderIdentification val : SenderIdentification.values()) {
			assertNotNull(EvenementOrganisationConversionHelper.convertSenderIdentification(val));
		}
	}

	@Test
	public void testConvertTypeOfNotice() throws Exception {
		assertEquals(TypeEvenementOrganisation.values().length, TypeOfNotice.values().length);
		for (SenderIdentification val : SenderIdentification.values()) {
			assertNotNull(EvenementOrganisationConversionHelper.convertSenderIdentification(val));
		}
	}
}