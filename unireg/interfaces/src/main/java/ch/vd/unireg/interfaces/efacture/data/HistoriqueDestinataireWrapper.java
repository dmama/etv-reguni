package ch.vd.unireg.interfaces.efacture.data;

import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
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

		// tri par ordre croissant
		final List<EtatDestinataireWrapper> etatsSorted = getEtatsSorted();
		if (etatsSorted == null || etatsSorted.isEmpty()) {
			return null;
		}

		// récupère le dernier état non-annulé (qui n'est pas un retour, donc)
		for (int i = etatsSorted.size() - 1; i >= 0; --i) {
			final EtatDestinataireWrapper e = etatsSorted.get(i);
				return e;
		}

		return null;
		//return etats.get(etats.size()-1);
	}

	@Transient
	public List<EtatDestinataireWrapper> getEtatsSorted() {

		if (etats == null) {
			return null;
		}

		// tri par ordre croissant
		final List<EtatDestinataireWrapper> list = new ArrayList<EtatDestinataireWrapper>(etats);
		Collections.sort(list, new EtatDestinataireWrapper.Comparator());

		return list;
	}


 public boolean isActivable(){
	 final EtatDestinataireWrapper dernierEtat = getDernierEtat();
	 if(dernierEtat !=null){
		 return dernierEtat.getStatusDestinataire().isActivable();

	 }
	 return false;
 }

	public boolean isSuspendable(){
		final EtatDestinataireWrapper dernierEtat = getDernierEtat();
		if(dernierEtat !=null){
			return dernierEtat.getStatusDestinataire().isSuspendable();

		}
		return false;
	}


}
