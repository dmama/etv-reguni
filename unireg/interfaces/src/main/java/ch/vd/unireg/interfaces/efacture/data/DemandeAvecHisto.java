package ch.vd.unireg.interfaces.efacture.data;

import java.util.ArrayList;
import java.util.List;

import ch.vd.evd0025.v1.RegistrationRequestHistoryEntry;
import ch.vd.evd0025.v1.RegistrationRequestWithHistory;
import ch.vd.unireg.interfaces.EvdHelper;

/**
 * Representation Interne UNIREG de la classe {@link RegistrationRequestWithHistory} de l' eVD-25
 */
public class DemandeAvecHisto extends Demande {

	private final List<EtatDemande> historiqueEtats;
	private final TypeDemande typeDemande;

	public List<EtatDemande> getHistoriqueEtats() {
		return historiqueEtats;
	}

	public TypeDemande getTypeDemande() {
		return typeDemande;
	}

	public DemandeAvecHisto(RegistrationRequestWithHistory request) {
		super(request);

		this.historiqueEtats = new ArrayList<>();
		if (request.getRegistrationRequestHistoryEntry() == null || request.getRegistrationRequestHistoryEntry().isEmpty()) {
			if (request.getRegistrationStatus() == null) {
				historiqueEtats.add(EtatDemande.newEtatDemandeFactice(TypeEtatDemande.IGNOREE));
			} else {
				historiqueEtats.add(EtatDemande.newEtatDemandeFactice(TypeEtatDemande.valueOf(request.getRegistrationStatus(), null)));
			}
		}
		else {
			for (RegistrationRequestHistoryEntry entry : request.getRegistrationRequestHistoryEntry()) {
				historiqueEtats.add(new EtatDemande(entry));
			}
		}
		this.typeDemande = EvdHelper.getTypeDemandeFromEvd25(request.getRegistrationMode());
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