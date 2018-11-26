package ch.vd.unireg.param.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleFeuilleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.param.view.ModeleDocumentView;
import ch.vd.unireg.param.view.ParametrePeriodeFiscalePMEditView;
import ch.vd.unireg.param.view.ParametrePeriodeFiscalePPEditView;
import ch.vd.unireg.param.view.ParametrePeriodeFiscaleSNCEditView;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscaleEmolument;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscaleSNC;

public interface ParamPeriodeManager {

	/**
	 * @return La liste de toute les périodes fiscales
	 */
	@Transactional(readOnly = true)
	List<PeriodeFiscale> getAllPeriodeFiscale();

	/**
	 * @return la liste des modèles de documents pour la periode donnée
	 */
	@Transactional(readOnly = true)
	List<ModeleDocument> getModeleDocuments(PeriodeFiscale periodeFiscale);
	
	@Transactional(readOnly = true)
	List<ModeleFeuilleDocument> getModeleFeuilleDocuments(ModeleDocument modeleDocument);
	
	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PP vaudois ordinaire, pour une période donnée
	 */
	@Transactional(readOnly = true)
	ParametrePeriodeFiscalePP getPPVaudByPeriodeFiscale(PeriodeFiscale periodeFiscale);
	
	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PP vaudois à la dépense, pour une période donnée
	 */
	@Transactional(readOnly = true)
	ParametrePeriodeFiscalePP getPPDepenseByPeriodeFiscale(PeriodeFiscale periodeFiscale);
	
	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PP hors canton, pour une période donnée
	 */
	@Transactional(readOnly = true)
	ParametrePeriodeFiscalePP getPPHorsCantonByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PP hors Suisse, pour une période donnée
	 */
	@Transactional(readOnly = true)
	ParametrePeriodeFiscalePP getPPHorsSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un diplomate Suisse, pour une période donnée
	 */
	@Transactional(readOnly = true)
	ParametrePeriodeFiscalePP getPPDiplomateSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PM vaudois ordinaire, pour une période donnée
	 */
	@Transactional(readOnly = true)
	ParametrePeriodeFiscalePM getPMVaudByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PM hors canton, pour une période donnée
	 */
	@Transactional(readOnly = true)
	ParametrePeriodeFiscalePM getPMHorsCantonByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PM hors Suisse, pour une période donnée
	 */
	@Transactional(readOnly = true)
	ParametrePeriodeFiscalePM getPMHorsSuisseByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les parametres de période fiscale pour un contribuable PM reconnu d'utilité publique, pour une période donnée
	 */
	@Transactional(readOnly = true)
	ParametrePeriodeFiscalePM getPMUtilitePubliqueByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les paramètres de période fiscale pour les questionnaires SNC, pour une période donnée
	 */
	@Transactional(readOnly = true)
	ParametrePeriodeFiscaleSNC getSNCByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * @param periodeFiscale la période fiscale
	 * @return les paramètres de période fiscale pour les émoluments de sommation de DI PP, pour la période donnée
	 */
	@Transactional(readOnly = false)
	ParametrePeriodeFiscaleEmolument getEmolumentSommationDIPPByPeriodeFiscale(PeriodeFiscale periodeFiscale);

	/**
	 * Initialise la nouvelle période fiscale
	 * @return true si la nouvelle période est bien initialisée
	 */
	@Transactional(rollbackFor = Throwable.class)
	PeriodeFiscale initNouvellePeriodeFiscale();
	
	/**
	 * Créer un objet {@link ParametrePeriodeFiscalePPEditView} pour l'année donnée.
	 * 
	 * @param idPeriode L'id technique de la periode fiscale
	 */
	ParametrePeriodeFiscalePPEditView createParametrePeriodeFiscalePPEditView(Long idPeriode);

	/**
	 * Sauvegarde le formulaire d'édition des paramètres PP de de période fiscale
	 */
	@Transactional(rollbackFor = Throwable.class)
	void saveParametrePeriodeFiscaleView(ParametrePeriodeFiscalePPEditView command);
	
	/**
	 * Créer un objet {@link ParametrePeriodeFiscalePMEditView} pour l'année donnée.
	 *
	 * @param idPeriode L'id technique de la periode fiscale
	 */
	ParametrePeriodeFiscalePMEditView createParametrePeriodeFiscalePMEditView(Long idPeriode);

	/**
	 * Sauvegarde le formulaire d'édition des paramètres PM de de période fiscale
	 */
	@Transactional(rollbackFor = Throwable.class)
	void saveParametrePeriodeFiscaleView(ParametrePeriodeFiscalePMEditView command);

	/**
	 * @param idPeriode l'identifiant technique de la période fiscale
	 * @return une instance de {@link ParametrePeriodeFiscaleSNCEditView} pour la période fiscale indiquée
	 */
	ParametrePeriodeFiscaleSNCEditView createParametrePeriodeFiscaleSNCEditView(Long idPeriode);

	/**
	 * Sauvegarde les données en base
	 */
	@Transactional(rollbackFor = Throwable.class)
	void saveParametrePeriodeFiscaleView(ParametrePeriodeFiscaleSNCEditView command);

	/**
	 * Créer un objet {@link ModeleDocumentView} en vu d'un ajout.
	 * @param idPeriode L'id de la {@link PeriodeFiscale} parente
	 */
	@Transactional(readOnly = true)
	ModeleDocumentView createModeleDocumentViewAdd (Long idPeriode);

	/**
	 * Sauvegarde le formulaire contenant un {@link ModeleDocument}
	 */
	@Transactional(rollbackFor = Throwable.class)
	void saveModeleDocumentView (ModeleDocumentView command);
	
	/**
	 * Supprime le modele de document et les feuilles associées
	 */
	@Transactional(rollbackFor = Throwable.class)
	void deleteModeleDocument(Long idModeleDocument);
}
