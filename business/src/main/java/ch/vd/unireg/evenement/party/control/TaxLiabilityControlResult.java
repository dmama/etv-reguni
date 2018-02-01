package ch.vd.unireg.evenement.party.control;

import java.util.Set;

/**
 * @param <T> type de valeurs collectées ({@link ch.vd.unireg.type.ModeImposition} ou {@link ch.vd.unireg.metier.assujettissement.TypeAssujettissement})
 */
public class TaxLiabilityControlResult<T extends Enum<T>> {

	public enum Origine {
		INITIAL,
		MENAGE_COMMUN,
		PARENT,
		MENAGE_COMMUN_PARENT
	}

	private final Origine origine;
	private final Long idTiersAssujetti;
	private final Set<T> sourceAssujettissements;

	private final TaxLiabilityControlEchec echec;

	public TaxLiabilityControlResult(Origine origine, long idTiersAssujetti, Set<T> sourceAssujettissements) {
		this.origine = origine;
		this.sourceAssujettissements = sourceAssujettissements;
		this.idTiersAssujetti = idTiersAssujetti;
		this.echec = null;
	}

	public TaxLiabilityControlResult(TaxLiabilityControlEchec echec) {
		this.origine = null;
		this.sourceAssujettissements = null;
		this.idTiersAssujetti = null;
		this.echec = echec;
	}

	/**
	 * Surcharge de l'origine
	 * @param origine nouvelle origine
	 * @param result résultat dont les informations sont à conserver
	 */
	public TaxLiabilityControlResult(Origine origine, TaxLiabilityControlResult<T> result) {
		this.origine = origine;
		this.sourceAssujettissements = result.getSourceAssujettissements();
		this.idTiersAssujetti = result.getIdTiersAssujetti();
		this.echec = result.getEchec();
	}

	public Long getIdTiersAssujetti() {
		return idTiersAssujetti;
	}

	public TaxLiabilityControlEchec getEchec() {
		return echec;
	}

	public Origine getOrigine() {
		return origine;
	}

	public Set<T> getSourceAssujettissements() {
		return sourceAssujettissements;
	}
}
