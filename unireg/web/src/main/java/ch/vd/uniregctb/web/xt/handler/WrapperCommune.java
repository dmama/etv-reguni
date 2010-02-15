/**
 *
 */
package ch.vd.uniregctb.web.xt.handler;

import ch.vd.uniregctb.interfaces.model.Commune;


/**
 * @author xcicfh
 *
 */
public class WrapperCommune {
	private String nomMinuscule = "???";
	private String noTechnique ="???";
	private String noOFS = "???";

	public WrapperCommune( Commune commune) {
			this.nomMinuscule = commune.getNomMinuscule();
			this.noTechnique = String.valueOf(commune.getNoOFSEtendu());
			this.noOFS = String.valueOf(commune.getNoOFS());
	}

	/**
	 * @return the nomMinuscule
	 */
	public String getNomMinuscule() {
		return nomMinuscule;
	}

	/**
	 * @return the noTechnique
	 */
	public String getNoTechnique() {
		return noTechnique;
	}

	/**
	 * @return the noOFS
	 */
	public String getNoOFS() {
		return noOFS;
	}
}
