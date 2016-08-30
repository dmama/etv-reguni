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
	 * @param periodeFiscale la période fiscale
	 * @return la liste des parametres de periode pour une periode donnée
	 */
	List<ParametrePeriodeFiscale> getByPeriodeFiscale(PeriodeFiscale periodeFiscale);
	
	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PP vaudois ordinaire, pour une période donnée
	 */
	ParametrePeriodeFiscalePP getPPVaudByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PP vaudois à la dépense, pour une période donnée
	 */
	ParametrePeriodeFiscalePP getPPDepenseByPeriodeFiscale(PeriodeFiscale periodeFiscale);
	
	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PP hors canton, pour une période donnée
	 */
	ParametrePeriodeFiscalePP getPPHorsCantonByPeriodeFiscale(PeriodeFiscale periodeFiscale);
	
	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PP hors Suisse, pour une période donnée
	 */
	ParametrePeriodeFiscalePP getPPHorsSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale);
	
	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un diplomate Suisse, pour une période donnée
	 */
	ParametrePeriodeFiscalePP getPPDiplomateSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PM vaudois, pour une période donnée
	 */
	ParametrePeriodeFiscalePM getPMVaudByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PM hors canton, pour une période donnée
	 */
	ParametrePeriodeFiscalePM getPMHorsCantonByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PM hors Suisse, pour une période donnée
	 */
	ParametrePeriodeFiscalePM getPMHorsSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PM reconnu d'utilité publique, pour une période donnée
	 */
	ParametrePeriodeFiscalePM getPMUtilitePubliqueByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les paramètres de période fiscale pour les questionnaires SNC, pour une période donnée
	 */
	ParametrePeriodeFiscaleSNC getSNCByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les paramètres de période fiscale pour l'émolument des sommations de DI PP, pour la période donnée
	 */
	ParametrePeriodeFiscaleEmolument getEmolumentSommationDIPPByPeriodeFiscale(PeriodeFiscale periodeFiscale);
}
