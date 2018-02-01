package ch.vd.unireg.admin.indexer;


/**
 * Classe d'affichage pour la gestion de l'indexation
 */
public class GestionIndexation {

	private String requete;
	private Long id;
	private Long indNo;
	private boolean logIndividu = true;


	/**
	 * @return the requete
	 */
	public String getRequete() {
		return requete;
	}

	/**
	 * @param requete the requete to set
	 */
	public void setRequete(String requete) {
		this.requete = requete;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getIndNo() {
		return indNo;
	}

	public void setIndNo(Long indNo) {
		this.indNo = indNo;
	}

	public boolean isLogIndividu() {
		return logIndividu;
	}

	public void setLogIndividu(boolean logIndividu) {
		this.logIndividu = logIndividu;
	}
}
