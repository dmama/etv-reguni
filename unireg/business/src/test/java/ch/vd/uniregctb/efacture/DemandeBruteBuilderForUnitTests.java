package ch.vd.uniregctb.efacture;

import java.math.BigInteger;
import java.util.Collections;

import ch.vd.evd0025.v1.Map;
import ch.vd.evd0025.v1.MapEntry;
import ch.vd.evd0025.v1.Provider;
import ch.vd.evd0025.v1.RegistrationMode;
import ch.vd.evd0025.v1.RegistrationRequest;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DemandeBrute;
import ch.vd.uniregctb.common.XmlUtils;

@SuppressWarnings("UnusedDeclaration")
class DemandeBruteBuilderForUnitTests {

	// Les valeurs par défaut permettent une validation sans problème

	private String id = "id";
	private String billerId = EFactureService.ACI_BILLER_ID;
	private String providerId = "provider_id";
	private String businessPayerId = "123456678";
	private BigInteger eBillAccountId = BigInteger.valueOf(1234567890L);
	private String lastName = "Lepaon";
	private String firstName = "Leon";
	private RegDate dateDemande = RegDate.get(2012, 6, 25);
	private String email = "leon@lepaon.com";
	private RegistrationMode registrationMode = RegistrationMode.STANDARD;
	private String noAvs = "7565817249033";

	DemandeBrute build () {
		return new DemandeBrute(buildRegistrationRequest());
	}

	RegistrationRequest buildRegistrationRequest () {
		return new RegistrationRequest(
				id, billerId, new Provider(providerId), businessPayerId, eBillAccountId,
				lastName, firstName, email, XmlUtils.regdate2xmlcal(dateDemande), registrationMode,
				new Map(Collections.<MapEntry>singletonList(new MapEntry("AVS13", noAvs))));
	}

	DemandeBruteBuilderForUnitTests id(String id) {
		this.id = id;
		return this;
	}

	DemandeBruteBuilderForUnitTests setBillerId(String billerId) {
		this.billerId = billerId;
		return this;
	}

	DemandeBruteBuilderForUnitTests setProviderId(String providerId) {
		this.providerId = providerId;
		return this;
	}

	DemandeBruteBuilderForUnitTests setBusinessPayerId(String businessPayerId) {
		this.businessPayerId = businessPayerId;
		return this;
	}

	DemandeBruteBuilderForUnitTests seteBillAccountId(BigInteger eBillAccountId) {
		this.eBillAccountId = eBillAccountId;
		return this;
	}

	DemandeBruteBuilderForUnitTests setLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	DemandeBruteBuilderForUnitTests setFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	DemandeBruteBuilderForUnitTests setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
		return this;
	}

	DemandeBruteBuilderForUnitTests setEmail(String email) {
		this.email = email;
		return this;
	}

	DemandeBruteBuilderForUnitTests setRegistrationMode(RegistrationMode registrationMode) {
		this.registrationMode = registrationMode;
		return this;
	}

	DemandeBruteBuilderForUnitTests setNoAvs(String noAvs) {
		this.noAvs = noAvs;
		return this;
	}
}
