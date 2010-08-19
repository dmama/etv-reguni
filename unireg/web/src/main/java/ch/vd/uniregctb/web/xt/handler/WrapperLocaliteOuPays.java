package ch.vd.uniregctb.web.xt.handler;


import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Pays;
import org.apache.commons.lang.StringEscapeUtils;


public class WrapperLocaliteOuPays {

	private final String nomComplet;
	private final String numero;

	public WrapperLocaliteOuPays(Localite localite) {
		this(localite.getNomAbregeMinuscule(), localite.getNoOrdre());
	}

	public WrapperLocaliteOuPays(Pays pays) {
		this(pays.getNomMinuscule(), pays.getNoOFS());
	}

	private WrapperLocaliteOuPays(String nomBrut, int numero) {
		this.nomComplet = StringEscapeUtils.escapeXml(nomBrut);
		this.numero = String.valueOf(numero);
	}

	/**
	 * @return the nom à renvoyer
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
