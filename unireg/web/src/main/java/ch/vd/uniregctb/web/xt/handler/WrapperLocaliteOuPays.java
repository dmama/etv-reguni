package ch.vd.uniregctb.web.xt.handler;


import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Pays;
import org.apache.commons.lang.StringEscapeUtils;


public class WrapperLocaliteOuPays {

	private String nomComplet;
	private String numero;

	public WrapperLocaliteOuPays(Localite localite) {
		nomComplet = StringEscapeUtils.escapeXml(localite.getNomAbregeMinuscule());
		numero = String.valueOf(localite.getNoOrdre());
	}

	public WrapperLocaliteOuPays(Pays pays) {
		nomComplet = StringEscapeUtils.escapeXml(pays.getNomMinuscule());
		numero = String.valueOf(pays.getNoOFS());
	}

	/**
	 * @return the nom2
	 */
	public String getNomComplet() {
		return nomComplet;
	}

	/**
	 * @return the numero
	 */
	public String getNumero() {
		return numero;
	}
}
