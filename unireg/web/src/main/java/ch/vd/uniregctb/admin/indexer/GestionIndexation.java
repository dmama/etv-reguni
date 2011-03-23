package ch.vd.uniregctb.admin.indexer;

import ch.vd.uniregctb.tracing.TracingManager;


/**
 * Classe d'affichage pour la gestion de l'indexation
 * @author xcifde
 *
 */
public class GestionIndexation {

	private int nombreDocumentsIndexes;
	private String chemin;
	private String requete;
	private String id;


	/**
	 * @return the chemin
	 */
	public String getChemin() {
		return chemin;
	}

	/**
	 * @param chemin the chemin to set
	 */
	public void setChemin(String chemin) {
		this.chemin = chemin;
	}

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

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	/**
	 * @return the nombreDocumentsIndexes
	 */
	public int getNombreDocumentsIndexes() {
		return nombreDocumentsIndexes;
	}

	/**
	 * @param nombreDocumentsIndexes the nombreDocumentsIndexes to set
	 */
	public void setNombreDocumentsIndexes(int nombreDocumentsIndexes) {
		this.nombreDocumentsIndexes = nombreDocumentsIndexes;
	}

	/**
	 * @return the gestionPerfActif
	 */
	public boolean isGestionPerfActif() {
		return TracingManager.isActive();
	}

	/**
	 * @param gestionPerfActif the gestionPerfActif to set
	 */
	public void setGestionPerfActif(boolean gestionPerfActif) {
		TracingManager.setActive(gestionPerfActif);
	}

}
