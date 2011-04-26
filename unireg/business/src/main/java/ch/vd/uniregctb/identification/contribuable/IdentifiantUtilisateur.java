package ch.vd.uniregctb.identification.contribuable;

/**Classe utilitaire permettande de stocker le visa et le nom d'un operateur ayant effectuer une identification manuelle
 *
 */
public class IdentifiantUtilisateur {
	private String visa;
	private String nomComplet;

	public IdentifiantUtilisateur(String visa, String nomComplet) {
		this.visa = visa;
		this.nomComplet = nomComplet;
	}

	public String getVisa() {
		return visa;
	}

	public void setVisa(String visa) {
		this.visa = visa;
	}

	public String getNomComplet() {
		return nomComplet;
	}

	public void setNomComplet(String nomComplet) {
		this.nomComplet = nomComplet;
	}
}
