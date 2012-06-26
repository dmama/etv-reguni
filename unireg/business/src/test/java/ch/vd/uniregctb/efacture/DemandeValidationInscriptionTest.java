package ch.vd.uniregctb.efacture;

import java.math.BigInteger;
import java.util.Collections;

import org.junit.Test;

import ch.vd.evd0025.v1.Map;
import ch.vd.evd0025.v1.MapEntry;
import ch.vd.evd0025.v1.Provider;
import ch.vd.evd0025.v1.RegistrationMode;
import ch.vd.evd0025.v1.RegistrationRequest;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.common.XmlUtils;

import static ch.vd.uniregctb.efacture.TypeRefusEFacture.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DemandeValidationInscriptionTest extends WithoutSpringTest {

	DemandeValidationInscription demande;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
	}

	@Test
	public void testPerformBasicValidation (){

		demande = new Builder().build();
		assertPerformBasicValidationOK();

		demande = new Builder().setNoAvs("756.581.724.90.33").build();
		assertPerformBasicValidationOK();

		demande = new Builder().setNoAvs(null).build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		demande = new Builder().setNoAvs("").build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		demande = new Builder().setNoAvs("XXXXXXXXXXXXX").build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		demande = new Builder().setNoAvs("75658172490").build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		demande = new Builder().setNoAvs("75658172490330").build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		demande = new Builder().setNoAvs("07565817249033").build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		demande = new Builder().setNoAvs("7565817249034").build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		demande = new Builder().setEmail(null).build();
		assertPerformBasicValidationFailWith(EMAIL_INVALIDE);

		demande = new Builder().setEmail("").build();
		assertPerformBasicValidationFailWith(EMAIL_INVALIDE);

		demande = new Builder().setEmail("toto").build();
		assertPerformBasicValidationFailWith(EMAIL_INVALIDE);

		demande = new Builder().setEmail("toto@").build();
		assertPerformBasicValidationFailWith(EMAIL_INVALIDE);

		demande = new Builder().setEmail("toto@tutu").build();
		assertPerformBasicValidationFailWith(EMAIL_INVALIDE);

		demande = new Builder().setEmail("toto@@gmail.com").build();
		assertPerformBasicValidationFailWith(EMAIL_INVALIDE);
	}

	private void assertPerformBasicValidationOK() {
		assertNull(demande.performBasicValidation());
	}

	private void assertPerformBasicValidationFailWith(TypeRefusEFacture typeRefusEFacture) {
		assertEquals(typeRefusEFacture, demande.performBasicValidation());
	}

	private static class Builder {

		// Les valeurs par défaut permettent une validation sans problème

		private String id = "id";
		private String billerId = EFactureEvent.ACI_BILLER_ID;
		private String providerId = "provider_id";
		private String businessPayerId = "123456678";
		private BigInteger eBillAccountId = BigInteger.valueOf(1234567890L);
		private String lastName = "Lepaon";
		private String firstName = "Leon";
		private RegDate dateDemande = RegDate.get(2012, 6, 25);
		private String email = "leon@lepaon.com";
		private RegistrationMode registrationMode = RegistrationMode.STANDARD;
		private String noAvs = "7565817249033";

		DemandeValidationInscription build () {
			return new DemandeValidationInscription (new RegistrationRequest(
					id, billerId, new Provider(providerId), businessPayerId, eBillAccountId,
					lastName, firstName, email, XmlUtils.regdate2xmlcal(dateDemande), registrationMode,
					new Map(Collections.< MapEntry>singletonList(new MapEntry("AVS13", noAvs)))));
		}

		Builder id(String id) {
			this.id = id;
			return this;
		}

		Builder setBillerId(String billerId) {
			this.billerId = billerId;
			return this;
		}

		Builder setProviderId(String providerId) {
			this.providerId = providerId;
			return this;
		}

		Builder setBusinessPayerId(String businessPayerId) {
			this.businessPayerId = businessPayerId;
			return this;
		}

		Builder seteBillAccountId(BigInteger eBillAccountId) {
			this.eBillAccountId = eBillAccountId;
			return this;
		}

		Builder setLastName(String lastName) {
			this.lastName = lastName;
			return this;
		}

		Builder setFirstName(String firstName) {
			this.firstName = firstName;
			return this;
		}

		Builder setDateDemande(RegDate dateDemande) {
			this.dateDemande = dateDemande;
			return this;
		}

		Builder setEmail(String email) {
			this.email = email;
			return this;
		}

		Builder setRegistrationMode(RegistrationMode registrationMode) {
			this.registrationMode = registrationMode;
			return this;
		}

		Builder setNoAvs(String noAvs) {
			this.noAvs = noAvs;
			return this;
		}
	}
}
