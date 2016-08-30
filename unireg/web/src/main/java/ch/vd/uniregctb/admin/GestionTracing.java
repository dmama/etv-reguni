package ch.vd.uniregctb.admin;

import ch.vd.uniregctb.tracing.TracingManager;


/**
 * Classe d'affichage pour la gestion de Tracing
 * @author xsikce
 *
 */
public class GestionTracing {
	
	
	private int nombreDocumentsIndexes;
	
	private String chemin;
	
	private String requete;

	
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
