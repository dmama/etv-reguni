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
	private List<EtatDestinataire> historiquesEtats;

	public List<DemandeAvecHisto> getHistoriqueDemandes() {
		return historiqueDemandes;
	}

	public List<EtatDestinataire> getHistoriquesEtats() {
		return historiquesEtats;
	}

	public DestinataireAvecHisto(PayerWithHistory payerWithHistory, long ctbId) {
		this.ctbId = ctbId;
		this.historiqueDemandes = new ArrayList<DemandeAvecHisto>();
		List<RegistrationRequestWithHistory> historyOfRequests = payerWithHistory.getHistoryOfRequests().getRequest();
		for (RegistrationRequestWithHistory registrationRequestHistory : historyOfRequests) {
			this.historiqueDemandes.add(new DemandeAvecHisto(registrationRequestHistory));
		}
		this.historiquesEtats = new ArrayList<EtatDestinataire>();
		List<PayerSituationHistoryEntry> historyOfSituations = payerWithHistory.getHistoryOfSituations().getSituation();
		if (historyOfSituations == null || historyOfSituations.isEmpty()) {
			if (payerWithHistory.getPayerStatus() == null) {
				throw new NullPointerException("Le statut du destinataire ne doit pas être null");
			}
			this.historiquesEtats.add(EtatDestinataire.newEtatDestinataireFactice(TypeEtatDestinataire.valueOf(payerWithHistory.getPayerStatus())));
		} else {
			for(PayerSituationHistoryEntry payerSituationHistoryEntry: historyOfSituations){
				this.historiquesEtats.add(new EtatDestinataire(payerSituationHistoryEntry));
			}
		}
	}

	public long getCtbId() {
		return ctbId;
	}

	public EtatDestinataire getDernierEtat() {
		return historiquesEtats.get(historiquesEtats.size() - 1);
	}


    public boolean isActivable(){
		 return getDernierEtat().getType().isActivable();
	}

	public boolean isSuspendable(){
		return getDernierEtat().getType().isSuspendable();
	}


}
