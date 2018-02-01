package ch.vd.unireg.evenement.reqdes.reception;

import ch.vd.unireg.common.NomPrenom;

/**
 * Lien depuis une partie prenante (voir {@link ReqDesPartiePrenante}) vers son conjoint,
 * soit lui-aussi partie prenante à l'acte (champ {@link #link}), soit pas (champ {@link #nomPrenom})
 */
public final class LienPartenaire {

	private final Integer link;
	private final NomPrenom nomPrenom;

	public LienPartenaire(int link) {
		this.link = link;
		this.nomPrenom = null;
	}

	public LienPartenaire(NomPrenom nomPrenom) {
		this.nomPrenom = nomPrenom;
		this.link = null;
	}

	public Integer getLink() {
		return link;
	}

	public NomPrenom getNomPrenom() {
		return nomPrenom;
	}
}
