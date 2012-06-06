package ch.vd.uniregctb.efacture;

import java.util.List;

import ch.vd.registre.base.date.RegDate;

public class HistoriqueDemande {

	private long idDemande;
	private RegDate dateDemande;
	private List<EtatDemande> etats;

	public long getIdDemande() {
		return idDemande;
	}

	public void setIdDemande(long idDemande) {
		this.idDemande = idDemande;
	}

	public List<EtatDemande> getEtats() {
		return etats;
	}

	public void setEtats(List<EtatDemande> etats) {
		this.etats = etats;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}
}
