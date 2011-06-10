package ch.vd.uniregctb.declaration.ordinaire;

public class ModeleFeuilleDocumentEditique implements Comparable<ModeleFeuilleDocumentEditique>{

	private String intituleFeuille;
	private Integer nbreIntituleFeuille;
	private String numeroFormulaire;

	public String getIntituleFeuille() {
		return intituleFeuille;
	}
	public void setIntituleFeuille(String intituleFeuille) {
		this.intituleFeuille = intituleFeuille;
	}

	public Integer getNbreIntituleFeuille() {
		return nbreIntituleFeuille;
	}
	public void setNbreIntituleFeuille(Integer nbreIntituleFeuille) {
		this.nbreIntituleFeuille = nbreIntituleFeuille;
	}
	public String getNumeroFormulaire() {
		return numeroFormulaire;
	}
	public void setNumeroFormulaire(String numeroFormulaire) {
		this.numeroFormulaire = numeroFormulaire;
	}

	/**
	 * Compare d'apres le numeroFormulaire de ModeleFeuilleDocumentView
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ModeleFeuilleDocumentEditique modeleFeuilleDocumentView) {
		String numeroFormulaire = getNumeroFormulaire();
		String autreNumeroFormulaire = modeleFeuilleDocumentView.getNumeroFormulaire();

		int value = numeroFormulaire.compareTo(autreNumeroFormulaire);
		return value;
	}

}
