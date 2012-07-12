package ch.vd.uniregctb.efacture;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Les états sont dans l'ordre inverse de leur validité (= l'état courant est en premier)
 */
@SuppressWarnings("UnusedDeclaration")
public class DestinataireAvecHistoView {

	private long ctbId;
	private List<EtatDestinataireView> etats;
	private List<DemandeAvecHistoView> demandes;
	private boolean suspendable;
	private boolean activable;

	public long getCtbId() {
		return ctbId;
	}

	public void setCtbId(long ctbId) {
		this.ctbId = ctbId;
	}

	public List<EtatDestinataireView> getEtats() {
		return etats;
	}

	public void setEtats(List<EtatDestinataireView> etats) {
		if (etats == null || etats.isEmpty()) {
		   throw new IllegalArgumentException("etats ne peut être ni null ni vide");
		}
		this.etats = new ArrayList<EtatDestinataireView>(etats);
	}

	public List<DemandeAvecHistoView> getDemandes() {
		return demandes;
	}

	public void setDemandes(List<DemandeAvecHistoView> demandes) {
		this.demandes = new ArrayList<DemandeAvecHistoView>(demandes);
	}

	public boolean isSuspendable() {
		return suspendable;
	}

	public void setSuspendable(boolean suspendable) {
		this.suspendable = suspendable;
	}

	public boolean isActivable() {
		return activable;
	}

	public void setActivable(boolean activable) {
		this.activable = activable;
	}

	@Nullable
	public DemandeAvecHistoView getDemandeEnCours() {
		DemandeAvecHistoView enCours = null;
		if (demandes != null) {
			for (DemandeAvecHistoView candidate : demandes) {
				if (candidate.getEtatCourant().getType().isEnCours()) {
					enCours = candidate;
					break;
				}
			}
		}
		return enCours;
	}
}
