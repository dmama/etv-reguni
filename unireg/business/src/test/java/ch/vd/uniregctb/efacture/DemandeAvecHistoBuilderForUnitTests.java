package ch.vd.uniregctb.efacture;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0025.v1.RegistrationRequest;
import ch.vd.evd0025.v1.RegistrationRequestHistoryEntry;
import ch.vd.evd0025.v1.RegistrationRequestStatus;
import ch.vd.evd0025.v1.RegistrationRequestWithHistory;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DemandeAvecHisto;
import ch.vd.uniregctb.common.XmlUtils;

class DemandeAvecHistoBuilderForUnitTests extends DemandeBuilderForUnitTests {

	private List<RegistrationRequestHistoryEntry> listHistoryEntry = new ArrayList<RegistrationRequestHistoryEntry>();

	@Override
	DemandeAvecHisto build() {
		return new DemandeAvecHisto(buildRegistrationRequestWithHistory());
	}

	RegistrationRequestWithHistory buildRegistrationRequestWithHistory () {
		RegistrationRequest rr = super.buildRegistrationRequest();
		return new RegistrationRequestWithHistory(rr.getId(), rr.getBillerId(), rr.getProvider(),
				rr.getPayerBusinessId(), rr.getEBillAccountId(), rr.getLastName(), rr.getFirstName(), rr.getEmail(),
				rr.getRegistrationDate(), rr.getRegistrationMode(),rr.getRegistrationStatus(), rr.getAdditionalData(),listHistoryEntry);
	}

	DemandeAvecHistoBuilderForUnitTests addHistoryEntry(RegistrationRequestHistoryEntry entry) {
		listHistoryEntry.add(entry);
		return this;
	}

	DemandeAvecHistoBuilderForUnitTests addHistoryEntry(RegDate date, RegistrationRequestStatus status, @Nullable Integer reasonCode, String description, String customField) {
		addHistoryEntry(new RegistrationRequestHistoryEntry(XmlUtils.regdate2xmlcal(date), status, reasonCode, description, customField));
		return this;
	}

}
