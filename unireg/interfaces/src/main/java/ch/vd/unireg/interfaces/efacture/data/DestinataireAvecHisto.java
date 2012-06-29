package ch.vd.unireg.interfaces.efacture.data;

import java.util.ArrayList;
import java.util.List;

import ch.vd.evd0025.v1.PayerSituationHistoryEntry;
import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.evd0025.v1.RegistrationRequestWithHistory;

/**
 * Representation interne UNIREG pour un Abonné e-Facture au {@link PayerWithHistory}
 * du evd e-facture
 */
public class DestinataireAvecHisto {
	private long ctbId;
	private List<DemandeAvecHisto> historiqueDemandes;
	private List<EtatDestinataire> etats;

	public List<DemandeAvecHisto> getHistoriqueDemandes() {
		return historiqueDemandes;
	}

	public List<EtatDestinataire> getEtats() {
		return etats;
	}

	public DestinataireAvecHisto(PayerWithHistory payerWithHistory, long ctbId) {
		this.ctbId = ctbId;
		this.historiqueDemandes = new ArrayList<DemandeAvecHisto>();
		PayerWithHistory.HistoryOfRequests historyOfRequests = payerWithHistory.getHistoryOfRequests();
		for (RegistrationRequestWithHistory registrationRequestHistory : historyOfRequests.getRequest()) {
			this.historiqueDemandes.add(new DemandeAvecHisto(registrationRequestHistory));

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
		//Les états nous sont toujours renvoyés dans l'ordre chronologique par le ws efacture
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
