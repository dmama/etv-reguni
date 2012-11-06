package ch.vd.unireg.interfaces.efacture.data;

import java.math.BigInteger;
import java.util.Collections;

import org.junit.Test;

import ch.vd.evd0025.v1.Map;
import ch.vd.evd0025.v1.MapEntry;
import ch.vd.evd0025.v1.Provider;
import ch.vd.evd0025.v1.RegistrationMode;
import ch.vd.evd0025.v1.RegistrationRequest;
import ch.vd.evd0025.v1.RegistrationRequestStatus;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.common.XmlUtils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class DemandeTest extends WithoutSpringTest {

	@Test
	public void testEmailValidator () {
		assertEquals(TypeRefusDemande.EMAIL_INVALIDE, newDemande("toto").performBasicValidation());
		assertEquals(TypeRefusDemande.EMAIL_INVALIDE, newDemande("toto@").performBasicValidation());
		assertEquals(TypeRefusDemande.EMAIL_INVALIDE, newDemande("toto@vd.h").performBasicValidation());
		assertEquals(TypeRefusDemande.EMAIL_INVALIDE, newDemande("toto@vd..ch").performBasicValidation());
		assertNull(newDemande("toto@d.ch").performBasicValidation());
		assertNull(newDemande("agachet@carrard-associes.ch").performBasicValidation()); // SIFISC-6629 - test non-régression
		assertNull(newDemande("toto+efacture@gmail.com").performBasicValidation());

	}

	private Demande newDemande (String email) {
		Map additionalDatas =  new Map(Collections.singletonList(new MapEntry(Demande.AVS13, "7564329665386")));
		return new Demande(
				new RegistrationRequest(
						"request-id", "biller-id", new Provider("provider-id"), "1", BigInteger.ZERO,"lastName","firstName",
						email,
						XmlUtils.regdate2xmlcal(RegDate.get()), RegistrationMode.STANDARD, RegistrationRequestStatus.A_TRAITER, additionalDatas));
	}
}
