package ch.vd.uniregctb.admin.evenementExterne;

import java.util.Date;

public class QuittancementView {

	private String numeroCtb;
	private Date dateDebut;
	private Date dateFin;
	private Date dateQuittance;
	private String typeQuittance;

	/**
	 * @return the numeroCtb
	 */
	public String getNumeroCtb() {
		return numeroCtb;
	}

	/**
	 * @param numeroCtb
	 *            the numeroCtb to set
	 */
	public void setNumeroCtb(String numeroCtb) {
		this.numeroCtb = numeroCtb;
	}

	/**
	 * @return the dateDebut
	 */
	public Date getDateDebut() {
		return dateDebut;
	}

	/**
	 * @param dateDebut
	 *            the dateDebut to set
	 */
	public void setDateDebut(Date dateDebut) {
		this.dateDebut = dateDebut;
	}

	/**
	 * @return the dateFin
	 */
	public Date getDateFin() {
		return dateFin;
	}

	/**
	 * @param dateFin
	 *            the dateFin to set
	 */
	public void setDateFin(Date dateFin) {
		this.dateFin = dateFin;
	}

	/**
	 * @return the dateQuittance
	 */
	public Date getDateQuittance() {
		return dateQuittance;
	}

	/**
	 * @param dateQuittance
	 *            the dateQuittance to set
	 */
	public void setDateQuittance(Date dateQuittance) {
		this.dateQuittance = dateQuittance;
	}

	/**
	 * @return the typeQuittance
	 */
	public String getTypeQuittance() {
		return typeQuittance;
	}

	/**
	 * @param typeQuittance
	 *            the typeQuittance to set
	 */
	public void setTypeQuittance(String typeQuittance) {
		this.typeQuittance = typeQuittance;
	}

}
