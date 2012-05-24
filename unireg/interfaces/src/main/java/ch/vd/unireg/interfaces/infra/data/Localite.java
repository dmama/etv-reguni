package ch.vd.unireg.interfaces.infra.data;

import ch.vd.registre.base.date.RegDate;

public interface Localite {

	public Integer getChiffreComplementaire();

	public RegDate getDateFinValidite();

	public String getNomAbregeMajuscule();

	public String getNomAbregeMinuscule();

	public String getNomCompletMajuscule();

	public String getNomCompletMinuscule();

	public Integer getNoOrdre();

	public Integer getNPA();

	public Integer getComplementNPA();

	public Integer getNoCommune();

	/**
	 * Indique si la localite est valide à la date du jour.
	 *
	 * @return <code>true</code> si la date de fin de validité de la localite n'est pas renseignée ou si la date de fin
	 *         de validité de la localite est égale ou postérieure à la date du jour.
	 */
	public boolean isValide();

	/**
	 * @return une commune associée à la localité.
	 */
	public Commune getCommuneLocalite();
}
