package ch.vd.uniregctb.web.xt.handler;

import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Pays;
import org.apache.commons.lang.StringEscapeUtils;


public class WrapperCommuneOuPays {

	private final String nomComplet;
	private final String numero;

	public WrapperCommuneOuPays(Commune commune) {
		nomComplet = StringEscapeUtils.escapeXml(commune.getNomMinuscule());
		numero = String.valueOf(commune.getNoOFS());
	}

	public WrapperCommuneOuPays(Pays pays) {
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
