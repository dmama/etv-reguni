package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.evd0022.v3.CommercialRegisterStatus;
import ch.vd.evd0022.v3.DissolutionReason;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeDissolutionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.model.RCRegistrationData;

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

	@Test
	public void testDates() throws Exception {
		final CommercialRegisterRegistrationDataConverter converter = new CommercialRegisterRegistrationDataConverter();
		final RegDate dateInscriptionVD = RegDate.get(2015, 3, 12);
		final RegDate dateInscriptionCH = RegDate.get(2010, 3, 12);
		final RegDate dateRadiationVD = RegDate.get(2016, 1, 5);
		final RegDate dateRadiationCH = RegDate.get(2016, 7, 31);
		final RCRegistrationData source = new RCRegistrationData(CommercialRegisterStatus.EN_LIQUIDATION, dateInscriptionCH, dateRadiationCH, dateInscriptionVD, dateRadiationVD, null);
		final InscriptionRC converted = converter.convert(source);
		Assert.assertNotNull(converted);
		Assert.assertEquals(dateInscriptionVD, converted.getDateInscriptionVD());
		Assert.assertEquals(dateRadiationVD, converted.getDateRadiationVD());
		Assert.assertEquals(dateInscriptionCH, converted.getDateInscriptionCH());
		Assert.assertEquals(dateRadiationCH, converted.getDateRadiationCH());
		Assert.assertEquals(StatusInscriptionRC.EN_LIQUIDATION, converted.getStatus());
	}
}
