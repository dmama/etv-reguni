package ch.vd.uniregctb.evenement.party.control;

import java.util.List;

public class TaxLiabilityControlResult<T> {

	private Origine origine;
	private List<T> sourceAssujettissements;


	private Long idTiersAssujetti;
	private TaxLiabilityControlEchec echec;

	public Long getIdTiersAssujetti() {
		return idTiersAssujetti;
	}

	public void setIdTiersAssujetti(Long idTiersAssujetti) {
		this.idTiersAssujetti = idTiersAssujetti;
	}

	public TaxLiabilityControlEchec getEchec() {
		return echec;
	}

	public void setEchec(TaxLiabilityControlEchec echec) {
		this.echec = echec;
	}

	public Origine getOrigine() {
		return origine;
	}

	public void setOrigine(Origine origine) {
		this.origine = origine;
	}

	public List<T> getSourceAssujettissements() {
		return sourceAssujettissements;
	}

	public void setSourceAssujettissements(List<T> sourceAssujettissements) {
		this.sourceAssujettissements = sourceAssujettissements;
	}


	public enum Origine{
		INITIAL,
		MENAGE_COMMUN,
		PARENT,
		MENAGE_COMMUN_PARENT;
	}
}
