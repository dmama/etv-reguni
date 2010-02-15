/**
 *
 */
package ch.vd.uniregctb.web.xt.handler;

import ch.vd.uniregctb.interfaces.model.Localite;

/**
 * @author xcicfh
 *
 */
public class WrapperLocalite {

	private final String nomCompletMinuscule;
	private final String nomCommune;
	private final String noOrdre;
	private final String npa;
	private final String numCommune;

	public WrapperLocalite(Localite localite) {
		this.nomCompletMinuscule = localite.getNomAbregeMinuscule();
		this.noOrdre = String.valueOf(localite.getNoOrdre());
		this.npa = localite.getNPA().toString();
		this.nomCommune = localite.getNomAbregeMinuscule();
		this.numCommune = localite.getNoCommune().toString();
	}

	/**
	 * @return the nomCompletMinuscule
	 */
	public String getNomCompletMinuscule() {
		return nomCompletMinuscule;
	}

	/**
	 * @return the noOrdre
	 */
	public String getNoOrdre() {
		return noOrdre;
	}

	/**
	 * @return the npa
	 */
	public String getNpa() {
		return npa;
	}

	/**
	 * @return the nomCommune
	 */
	public String getNomCommune() {
		return nomCommune;
	}

	/**
	 * @return the numCommune
	 */
	public String getNumCommune() {
		return numCommune;
	}


}
