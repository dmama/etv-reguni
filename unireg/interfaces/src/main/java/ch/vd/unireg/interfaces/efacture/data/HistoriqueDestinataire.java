package ch.vd.unireg.interfaces.efacture.data;

import java.util.ArrayList;
import java.util.List;

import ch.vd.evd0025.v1.PayerSituationHistoryEntry;
import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.evd0025.v1.RegistrationRequestWithHistory;

public class HistoriqueDestinataire {
	private long ctbId;
	private List<HistoriqueDemande> historiqueDemandeWrapper;
	private List<EtatDestinataire> etats;

	public List<HistoriqueDemande> getHistoriqueDemandeWrapper() {
		return historiqueDemandeWrapper;
	}

	public List<EtatDestinataire> getEtats() {
		return etats;
	}

	public HistoriqueDestinataire(PayerWithHistory payerWithHistory, long ctbId) {
		this.ctbId = ctbId;
		this.historiqueDemandeWrapper = new ArrayList<HistoriqueDemande>();
		PayerWithHistory.HistoryOfRequests historyOfRequests = payerWithHistory.getHistoryOfRequests();
		for (RegistrationRequestWithHistory registrationRequestHistory : historyOfRequests.getRequest()) {
			this.historiqueDemandeWrapper.add(new HistoriqueDemande(registrationRequestHistory));

		}

		this.etats = new ArrayList<EtatDestinataire>();

		PayerWithHistory.HistoryOfSituations historyOfSituations = payerWithHistory.getHistoryOfSituations();
		for(PayerSituationHistoryEntry payerSituationHistoryEntry: historyOfSituations.getSituation()){

			this.etats.add(new EtatDestinataire(payerSituationHistoryEntry));
		}


	}

	public long getCtbId() {
		return ctbId;
	}

	public void setCtbId(long ctbId) {
		this.ctbId = ctbId;
	}

	public EtatDestinataire getDernierEtat() {
		//Les états nous sont toujours renvoyés par ordre chronologique croissant par le ws efacture
		if(etats!=null && !etats.isEmpty()){
			return etats.get(etats.size()-1);
		}
		return null;
	}


 public boolean isActivable(){
	 final EtatDestinataire dernierEtat = getDernierEtat();
	 if(dernierEtat !=null){
		 return dernierEtat.getEtatDestinataire().isActivable();

	 }
	 return false;
 }

	public boolean isSuspendable(){
		final EtatDestinataire dernierEtat = getDernierEtat();
		if(dernierEtat !=null){
			return dernierEtat.getEtatDestinataire().isSuspendable();

		}
		return false;
	}


}
