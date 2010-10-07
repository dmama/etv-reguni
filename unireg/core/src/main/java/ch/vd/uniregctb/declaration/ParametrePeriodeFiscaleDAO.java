package ch.vd.uniregctb.declaration;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;

/**
 * Gestion de l'acces aux données pour les objects {@link ParametrePeriodeFiscale} 
 * 
 * @author xsifnr
 *
 */
public interface ParametrePeriodeFiscaleDAO extends GenericDAO<ParametrePeriodeFiscale, Long> {

	/**
	 * Retrouve la liste des parametres de periode pour une periode donnée
	 * 
	 * @param periodeFiscale la période fiscale
	 * @return
	 */
	List<ParametrePeriodeFiscale> getByPeriodeFiscale(PeriodeFiscale periodeFiscale);
	
	/**
	 * Retrouve les parametres de période fiscale pour un contribuable vaudois ordinaire, pour une période donnée
	 * 
	 * @param periodeFiscale la période fiscale
	 * @return
	 */
	ParametrePeriodeFiscale getVaudByPeriodeFiscale(PeriodeFiscale periodeFiscale);
	
	
	/**
	 * Retrouve les parametres de période fiscale pour un contribuable vaudois à la dépense, pour une période donnée
	 * 
	 * @param periodeFiscale la période fiscale
	 * @return
	 */
	ParametrePeriodeFiscale getDepenseByPeriodeFiscale(PeriodeFiscale periodeFiscale);
	
	/**
	 * Retrouve les parametres de période fiscale pour un contribuable hors canton, pour une période donnée
	 * 
	 * @param periodeFiscale la période fiscale
	 * @return
	 */
	ParametrePeriodeFiscale getHorsCantonByPeriodeFiscale(PeriodeFiscale periodeFiscale);
	
	
	/**
	 * 
	 * Retrouve les parametres de période fiscale pour un contribuable hors Suisse, pour une période donnée
	 * 
	 * @param periodeFiscale la période fiscale
	 * @return
	 */
	ParametrePeriodeFiscale getHorsSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale);
	
	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un diplomate Suisse, pour une période donnée
	 */
	ParametrePeriodeFiscale getDiplomateSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale);
}
