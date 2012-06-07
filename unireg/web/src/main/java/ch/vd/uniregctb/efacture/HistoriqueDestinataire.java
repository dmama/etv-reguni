package ch.vd.uniregctb.efacture;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Les états sont dans l'ordre inverse de leur validité (= l'état courant est en premier)
 */
public class HistoriqueDestinataire {

	private long ctbId;
	private List<EtatDestinataire> etats;
	private List<HistoriqueDemande> demandes;
	private boolean suspendable;
	private boolean activable;

	public long getCtbId() {
		return ctbId;
	}

	public void setCtbId(long ctbId) {
		this.ctbId = ctbId;
	}

	public List<EtatDestinataire> getEtats() {
		return etats;
	}

	public void setEtats(List<EtatDestinataire> etats) {
		this.etats = etats;
	}

	public List<HistoriqueDemande> getDemandes() {
		return demandes;
	}

	public void setDemandes(List<HistoriqueDemande> demandes) {
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
	public HistoriqueDemande getDemandeEnCours() {
		if (demandes == null) {
			return null;
		}
		else {
			HistoriqueDemande enCours = null;
			for (HistoriqueDemande candidate : demandes) {
				final EtatDemande etatCourant = candidate.getEtatCourant();
				if (etatCourant != null && etatCourant.getType().isEnCours()) {
					enCours = candidate;
					break;
				}
			}
			return enCours;
		}
	}
}
