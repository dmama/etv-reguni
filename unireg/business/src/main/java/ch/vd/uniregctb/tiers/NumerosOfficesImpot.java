package ch.vd.uniregctb.tiers;

public class NumerosOfficesImpot {

	/**
	 * Le numéro de <b>tiers</b> de l'office d'impôt de district
	 */
	private long oid;

	/**
	 * Le numéro de <b>tiers</b> de l'office d'impôt de région
	 */
	private long oir;

	public NumerosOfficesImpot() {
	}

	public NumerosOfficesImpot(long oid, long oir) {
		this.oid = oid;
		this.oir = oir;
	}

	/**
	 * @return le numéro de <b>tiers</b> de l'office d'impôt de district
	 */
	public long getOid() {
		return oid;
	}

	public void setOid(long oid) {
		this.oid = oid;
	}

	/**
	 * @return le numéro de <b>tiers</b> de l'office d'impôt de région
	 */
	public long getOir() {
		return oir;
	}

	public void setOir(long oir) {
		this.oir = oir;
	}
}
