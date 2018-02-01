package ch.vd.uniregctb.efacture;

import java.math.BigInteger;
import java.util.Collections;

import ch.vd.evd0025.v1.Map;
import ch.vd.evd0025.v1.MapEntry;
import ch.vd.evd0025.v1.Provider;
import ch.vd.evd0025.v1.RegistrationMode;
import ch.vd.evd0025.v1.RegistrationRequest;
import ch.vd.evd0025.v1.RegistrationRequestStatus;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.Demande;
import ch.vd.uniregctb.common.XmlUtils;

@SuppressWarnings("UnusedDeclaration")
class DemandeBuilderForUnitTests {

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
	private RegistrationRequestStatus status = RegistrationRequestStatus.VALIDATION_EN_COURS;

	Demande build () {
		return new Demande(buildRegistrationRequest());
	}

	RegistrationRequest buildRegistrationRequest () {
		return new RegistrationRequest(
				id, billerId, new Provider(providerId), businessPayerId, eBillAccountId,
				lastName, firstName, email, XmlUtils.regdate2xmlcal(dateDemande), registrationMode,
				status, new Map(Collections.<MapEntry>singletonList(new MapEntry("AVS13", noAvs))));
	}

	DemandeBuilderForUnitTests id(String id) {
		this.id = id;
		return this;
	}

	DemandeBuilderForUnitTests billerId(String billerId) {
		this.billerId = billerId;
		return this;
	}

	DemandeBuilderForUnitTests providerId(String providerId) {
		this.providerId = providerId;
		return this;
	}

	DemandeBuilderForUnitTests businessPayerId(String businessPayerId) {
		this.businessPayerId = businessPayerId;
		return this;
	}

	DemandeBuilderForUnitTests eBillAccountId(BigInteger eBillAccountId) {
		this.eBillAccountId = eBillAccountId;
		return this;
	}

	DemandeBuilderForUnitTests lastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	DemandeBuilderForUnitTests firstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	DemandeBuilderForUnitTests dateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
		return this;
	}

	DemandeBuilderForUnitTests email(String email) {
		this.email = email;
		return this;
	}

	DemandeBuilderForUnitTests registrationMode(RegistrationMode registrationMode) {
		this.registrationMode = registrationMode;
		return this;
	}

	DemandeBuilderForUnitTests status(RegistrationRequestStatus status) {
		this.status= status;
		return this;
	}

	DemandeBuilderForUnitTests noAvs(String noAvs) {
		this.noAvs = noAvs;
		return this;
	}
}
