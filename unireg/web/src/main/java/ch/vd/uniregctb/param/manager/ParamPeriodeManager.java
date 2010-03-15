package ch.vd.uniregctb.param.manager;

import java.util.List;

import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.param.view.ModeleDocumentView;
import ch.vd.uniregctb.param.view.ModeleFeuilleDocumentView;
import ch.vd.uniregctb.param.view.ParametrePeriodeFiscaleView;

public interface ParamPeriodeManager {

	/**
	 * @return La liste de toute les périodes fiscales
	 */
	List<PeriodeFiscale> getAllPeriodeFiscale();

	/**
	 *  Retrouve la liste des modèles de documents pour la periode donnée
	 *  
	 * @param periodeFiscale
	 * @return 
	 */
	List<ModeleDocument> getModeleDocuments(PeriodeFiscale periodeFiscale);
	
	/**
	 * @param modeleDocument
	 * @return
	 */
	List<ModeleFeuilleDocument> getModeleFeuilleDocuments(ModeleDocument modeleDocument);
	
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
	 * Initialise la nouvelle période fiscale
	 * 
	 * @return true si la nouvelle période est bien initialisée
	 */
	PeriodeFiscale initNouvellePeriodeFiscale();
	
	/**
	 * Créer un objet {@link ParametrePeriodeFiscaleView} pour l'année donnée.
	 * 
	 * @param annee L'id de la periode fiscale
	 * 
	 * @return true si la nouvelle période est bien initialisée
	 */	
	ParametrePeriodeFiscaleView createParametrePeriodeFiscaleViewEdit (Long idPeriode);

	/**
	 * Sauvegarde le formulaire d'édition des parametres de de période fiscale
	 *  
	 * @param command
	 */
	void saveParametrePeriodeFiscaleView(ParametrePeriodeFiscaleView command);
	
	/**
	 * Créer un objet {@link ModeleDocumentView} en vu d'un ajout.
	 * 
	 * @param idPeriode L'id de la {@link PeriodeFiscale} parente
	 * 
	 */	
	ModeleDocumentView createModeleDocumentViewAdd (Long idPeriode);
	
	/**
	 * Créer un objet {@link ModeleFeuilleDocumentView} en vue d'une edition.
	 * 
	 * @param idModele L'id du {@link ModeleFeuilleDocument} à éditer
	 * 
	 */	
	ModeleFeuilleDocumentView createModeleFeuilleDocumentViewEdit(Long periodeId, Long modeleId, Long feuilleId);
	
	
	/**
	 * Créer un objet {@link ModeleFeuilleDocumentView} en vu d'un ajout.
	 * 
	 * @param modeleId L'id du {@link ModeleDocument} parent
	 * 
	 */	
	ModeleFeuilleDocumentView createModeleFeuilleDocumentViewAdd(Long periodeId, Long modeleId);

	/**
	 * Sauvegarde le formulaire contenant un {@link ModeleDocument}
	 *  
	 * @param command
	 */
	void saveModeleDocumentView (ModeleDocumentView command);	
	
	/**
	 * Supprime le modele de document et les feuilles associées
	 * 
	 * @param idModeleDocument
	 */
	void deleteModeleDocument(Long idModeleDocument);
	
	/**
	 * Supprime une feuille de modèle de document
	 * 
	 * @param idModeleFeuilleDocument
	 */
	void deleteModeleFeuilleDocument(Long idModeleFeuilleDocument);

	/**
	 * Sauvegarde le formulaire contenant un {@link ModeleFeuilleDocument} en Ajout
	 *  
	 * @param command
	 */
	void saveModeleFeuilleDocumentViewAdd (ModeleFeuilleDocumentView mfdv);
	
	/**
	 * Sauvegarde le formulaire contenant un {@link ModeleFeuilleDocument} en Edition
	 *  
	 * @param command
	 */
	void saveModeleFeuilleDocumentViewEdit (ModeleFeuilleDocumentView mfdv);	
}
