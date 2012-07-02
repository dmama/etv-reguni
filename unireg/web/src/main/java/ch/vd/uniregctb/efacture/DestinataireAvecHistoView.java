package ch.vd.uniregctb.efacture;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Les états sont dans l'ordre inverse de leur validité (= l'état courant est en premier)
 */
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
		this.etats = etats;
	}

	public List<DemandeAvecHistoView> getDemandes() {
		return demandes;
	}

	public void setDemandes(List<DemandeAvecHistoView> demandes) {
		this.demandes = demandes;
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
		if (demandes == null) {
			return null;
		}
		else {
			DemandeAvecHistoView enCours = null;
			for (DemandeAvecHistoView candidate : demandes) {
				final EtatDemandeView etatCourant = candidate.getEtatCourant();
				if (etatCourant != null && etatCourant.getType().isEnCours()) {
					enCours = candidate;
					break;
				}
			}
			return enCours;
		}
	}
}
