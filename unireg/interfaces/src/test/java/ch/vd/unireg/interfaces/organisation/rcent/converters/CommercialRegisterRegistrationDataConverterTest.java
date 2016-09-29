package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.evd0022.v3.CommercialRegisterStatus;
import ch.vd.evd0022.v3.DissolutionReason;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeDissolutionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;

public class CommercialRegisterRegistrationDataConverterTest {

	@Test
	public void testMapStatus() throws Exception {
		Assert.assertNull(CommercialRegisterRegistrationDataConverter.mapStatus(null));
		for (CommercialRegisterStatus src : CommercialRegisterStatus.values()) {
			final StatusInscriptionRC dest = CommercialRegisterRegistrationDataConverter.mapStatus(src);
			Assert.assertNotNull("Valeur " + src + " non-mappée!", dest);
		}
	}

	@Test
	public void testMapRaisonDissolution() throws Exception {
		Assert.assertNull(CommercialRegisterRegistrationDataConverter.mapRaisonDissolution(null));
		for (DissolutionReason src : DissolutionReason.values()) {
			final RaisonDeDissolutionRC dest = CommercialRegisterRegistrationDataConverter.mapRaisonDissolution(src);
			Assert.assertNotNull("Valeur " + src + " non-mappée!", dest);
		}
	}
}
