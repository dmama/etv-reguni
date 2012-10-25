package ch.vd.unireg.interfaces.efacture.data;

import java.util.ArrayList;
import java.util.List;

import ch.vd.evd0025.v1.RegistrationRequestHistoryEntry;
import ch.vd.evd0025.v1.RegistrationRequestWithHistory;

/**
 * Representation Interne UNIREG de la classe {@link RegistrationRequestWithHistory} de l' eVD-25
 */
public class DemandeAvecHisto extends Demande {

	private List<EtatDemande> historiqueEtats;

	public List<EtatDemande> getHistoriqueEtats() {
		return historiqueEtats;
	}

	public DemandeAvecHisto(RegistrationRequestWithHistory request) {
		super(request);

		this.historiqueEtats = new ArrayList<EtatDemande>();
		if (request.getRegistrationRequestHistoryEntry() == null || request.getRegistrationRequestHistoryEntry().isEmpty()) {
			if (request.getRegistrationStatus() == null) {
				historiqueEtats.add(EtatDemande.newEtatDemandeFactice(TypeEtatDemande.IGNOREE));
			} else {
				historiqueEtats.add(EtatDemande.newEtatDemandeFactice(TypeEtatDemande.valueOf(request.getRegistrationStatus(), null)));
			}
		} else {
			for (RegistrationRequestHistoryEntry entry : request.getRegistrationRequestHistoryEntry()) {
				historiqueEtats.add(new EtatDemande(entry));
			}
		}
	}

	public EtatDemande getDernierEtat() {
		return historiqueEtats.get(historiqueEtats.size() - 1);
	}

	/**
	 * @return si la demande est en attente (contact ou signature)
	 */
	public boolean isEnAttente() {
		return getDernierEtat().getType().isEnAttente();
	}
}