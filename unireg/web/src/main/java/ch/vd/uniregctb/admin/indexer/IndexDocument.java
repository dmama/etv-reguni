package ch.vd.uniregctb.admin.indexer;

public class IndexDocument {

	private String entityId;
	private String nomCourrier1;
	private String nomCourrier2;
	private String nomFor;
	private String npa;
	private String localite;
	private String numeroAvs;
	private String dateNaissance;

	/**
	 * @return the entityId
	 */
	public String getEntityId() {
		return entityId;
	}

	/**
	 * @param entityId the entityId to set
	 */
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	/**
	 * @return the nomCourrier1
	 */
	public String getNomCourrier1() {
		return nomCourrier1;
	}

	/**
	 * @param nomCourrier1 the nomCourrier1 to set
	 */
	public void setNomCourrier1(String nomCourrier1) {
		this.nomCourrier1 = nomCourrier1;
	}

	/**
	 * @return the nomCourrier2
	 */
	public String getNomCourrier2() {
		return nomCourrier2;
	}

	/**
	 * @param nomCourrier2 the nomCourrier2 to set
	 */
	public void setNomCourrier2(String nomCourrier2) {
		this.nomCourrier2 = nomCourrier2;
	}

	/**
	 * @return the nomFor
	 */
	public String getNomFor() {
		return nomFor;
	}

	/**
	 * @param nomFor the nomFor to set
	 */
	public void setNomFor(String nomFor) {
		this.nomFor = nomFor;
	}

	public String getNpa() {
		return npa;
	}

	public void setNpa(String npa) {
		this.npa = npa;
	}

	/**
	 * @return the localite
	 */
	public String getLocalite() {
		return localite;
	}

	/**
	 * @param localite the localite to set
	 */
	public void setLocalite(String localite) {
		this.localite = localite;
	}

	/**
	 * @return the numeroAvs
	 */
	public String getNumeroAvs() {
		return numeroAvs;
	}

	/**
	 * @param numeroAvs the numeroAvs to set
	 */
	public void setNumeroAvs(String numeroAvs) {
		this.numeroAvs = numeroAvs;
	}

	/**
	 * @return the dateNaissance
	 */
	public String getDateNaissance() {
		return dateNaissance;
	}

	/**
	 * @param dateNaissance the dateNaissance to set
	 */
	public void setDateNaissance(String dateNaissance) {
		this.dateNaissance = dateNaissance;
	}
}
