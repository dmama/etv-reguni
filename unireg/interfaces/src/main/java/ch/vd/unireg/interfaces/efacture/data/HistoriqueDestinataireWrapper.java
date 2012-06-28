package ch.vd.unireg.interfaces.efacture.data;

import java.util.ArrayList;
import java.util.List;

import ch.vd.evd0025.v1.PayerSituationHistoryEntry;
import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.evd0025.v1.RegistrationRequestWithHistory;

public class HistoriqueDestinataireWrapper {
	private long ctbId;
	private List<HistoriqueDemandeWrapper> historiqueDemandeWrapper;
	private List<EtatDestinataireWrapper> etats;

	public List<HistoriqueDemandeWrapper> getHistoriqueDemandeWrapper() {
		return historiqueDemandeWrapper;
	}

	public List<EtatDestinataireWrapper> getEtats() {
		return etats;
	}

	public HistoriqueDestinataireWrapper(PayerWithHistory payerWithHistory, long ctbId) {
		this.ctbId = ctbId;
		this.historiqueDemandeWrapper = new ArrayList<HistoriqueDemandeWrapper>();
		PayerWithHistory.HistoryOfRequests historyOfRequests = payerWithHistory.getHistoryOfRequests();
		for (RegistrationRequestWithHistory registrationRequestHistory : historyOfRequests.getRequest()) {
			this.historiqueDemandeWrapper.add(new HistoriqueDemandeWrapper(registrationRequestHistory));

		}

		this.etats = new ArrayList<EtatDestinataireWrapper>();

		PayerWithHistory.HistoryOfSituations historyOfSituations = payerWithHistory.getHistoryOfSituations();
		for(PayerSituationHistoryEntry payerSituationHistoryEntry: historyOfSituations.getSituation()){

			this.etats.add(new EtatDestinataireWrapper(payerSituationHistoryEntry));
		}


	}

	public long getCtbId() {
		return ctbId;
	}

	public void setCtbId(long ctbId) {
		this.ctbId = ctbId;
	}

	public EtatDestinataireWrapper getDernierEtat() {
		//Les états nous sont toujours renvoyés par ordre chronologique croissant par le ws efacture
		if(etats!=null && !etats.isEmpty()){
			return etats.get(etats.size()-1);
		}
		return null;
	}


 public boolean isActivable(){
	 final EtatDestinataireWrapper dernierEtat = getDernierEtat();
	 if(dernierEtat !=null){
		 return dernierEtat.getEtatDestinataire().isActivable();

	 }
	 return false;
 }

	public boolean isSuspendable(){
		final EtatDestinataireWrapper dernierEtat = getDernierEtat();
		if(dernierEtat !=null){
			return dernierEtat.getEtatDestinataire().isSuspendable();

		}
		return false;
	}


}
