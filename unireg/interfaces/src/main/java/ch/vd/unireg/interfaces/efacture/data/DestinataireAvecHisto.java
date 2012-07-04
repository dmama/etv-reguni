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
		List<RegistrationRequestWithHistory> historyOfRequests = payerWithHistory.getHistoryOfRequests().getRequest();
		if (historyOfRequests.isEmpty()) {
			throw new IllegalArgumentException("Un PayerWithHistory doit avoir au moins une RegistrationRequestWithHistory");
		}
		for (RegistrationRequestWithHistory registrationRequestHistory : historyOfRequests) {
			this.historiqueDemandes.add(new DemandeAvecHisto(registrationRequestHistory));
		}
		this.etats = new ArrayList<EtatDestinataire>();
		List<PayerSituationHistoryEntry> historyOfSituations = payerWithHistory.getHistoryOfSituations().getSituation();
		if (historyOfSituations.isEmpty()) {
			throw new IllegalArgumentException("Un PayerWithHistory doit au mins avoir une PayerSituationHistoryEntry");
		}
		for(PayerSituationHistoryEntry payerSituationHistoryEntry: historyOfSituations){
			this.etats.add(new EtatDestinataire(payerSituationHistoryEntry));
		}
	}

	public long getCtbId() {
		return ctbId;
	}

	public EtatDestinataire getDernierEtat() {
		return etats.get(etats.size()-1);
	}


    public boolean isActivable(){
		 return getDernierEtat().getType().isActivable();
	}

	public boolean isSuspendable(){
		return getDernierEtat().getType().isSuspendable();
	}


}
