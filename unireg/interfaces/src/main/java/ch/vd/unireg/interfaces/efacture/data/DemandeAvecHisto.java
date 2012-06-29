package ch.vd.unireg.interfaces.efacture.data;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

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
		for (RegistrationRequestHistoryEntry entry : request.getRegistrationRequestHistoryEntry()) {
			historiqueEtats.add(new EtatDemande(entry));
		}

	}

	@Nullable
	public EtatDemande getDernierEtat() {
		//Les états nous sont toujours renvoyés dans l'ordre chronologique par le ws efacture
		if(historiqueEtats !=null && !historiqueEtats.isEmpty()){
			return historiqueEtats.get(historiqueEtats.size()-1);
		}
		return null;
	}

	/**
	 *
	 * La demande est réputée en cours de traitement si :
	 *
	 * <ul>
	 *     <li>l'état est a VALIDATION_EN_COURS</li>
	 *     <li>la demande est en attente (le code raison n'est pas null)</li>
	 * </ul>
	 *
	 * @return si la demande est en cours de traitement.
	 *
	 */
	public boolean isEnCoursDeTraitement () {
		final EtatDemande dernierEtat = getDernierEtat();
		return dernierEtat != null
				&& dernierEtat.getTypeEtatDemande() == TypeEtatDemande.VALIDATION_EN_COURS
				&& TypeAttenteDemande.valueOf(dernierEtat.getCodeRaison()) != TypeAttenteDemande.PAS_EN_ATTENTE;
	}
}