package ch.vd.unireg.interfaces.efacture.data;

import java.util.ArrayList;
import java.util.List;

import ch.vd.evd0025.v1.RegistrationRequestHistoryEntry;
import ch.vd.evd0025.v1.RegistrationRequestWithHistory;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;

public class HistoriqueDemandeWrapper {

	private String id;
	private RegDate dateInscription;
	//TODO rajouter les autres attributs en cas de besoin
	private List<EtatDemandeWrapper> historiqueEtatDemandeWrapper;


	public RegDate getDateInscription() {
		return dateInscription;
	}

	public List<EtatDemandeWrapper> getHistoriqueEtatDemandeWrapper() {
		return historiqueEtatDemandeWrapper;
	}

	public String getId() {
		return id;
	}

	public HistoriqueDemandeWrapper(RegistrationRequestWithHistory registrationRequestWithHistory) {
		this.historiqueEtatDemandeWrapper = new ArrayList<EtatDemandeWrapper>();
		this.id = registrationRequestWithHistory.getId();
		this.dateInscription = XmlUtils.xmlcal2regdate(registrationRequestWithHistory.getRegistrationDate());
		for (RegistrationRequestHistoryEntry registrationRequestHistoryEntry : registrationRequestWithHistory.getRegistrationRequestHistoryEntry()) {
			historiqueEtatDemandeWrapper.add(new EtatDemandeWrapper(registrationRequestHistoryEntry));
		}
	}
}
