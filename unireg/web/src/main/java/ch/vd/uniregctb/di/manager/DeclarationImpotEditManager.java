package ch.vd.uniregctb.di.manager;


import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.delai.DelaiDeclarationView;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;
import ch.vd.uniregctb.di.view.DeclarationImpotImpressionView;
import ch.vd.uniregctb.di.view.DeclarationImpotListView;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;

/**
 * Service offrant des methodes pour gérer le controller DeclarationImpotEditController
 *
 * @author xcifde
 *
 */
public interface DeclarationImpotEditManager {

	public static final String CANNOT_ADD_NEW_DI = "Le contribuable n'est pas assujetti, ou toutes ses déclarations sont déjà créées.";

	/**
	 * Contrôle que la DI existe
	 *
	 * @param id
	 */
	public void controleDI(Long id) ;

	/**
	 * Alimente la vue en fonction de l'ID de la DI
	 */
	public void get(Long id, DeclarationImpotDetailView view);

	/**
	 * Reactualise la vue
	 *
	 * @param diEditView
	 * @return
	 */
	public DeclarationImpotDetailView refresh(DeclarationImpotDetailView diEditView);


	/**
	 * Alimente la vue en fonction d'un contribuable
	 */
	public abstract void findByNumero(Long numero, DeclarationImpotListView view);

	/**
	 * Retourne la vue de création d'une nouvelle DI sur le contribuable spécifié.
	 * <p>
	 * La nouvelle DI crée est la suivante dans l'ordre chronologique, ou - si le paramètre annee est renseigné - la DI pour l'année
	 * spécifiée. Cette méthode vérifie si le contribuable est bien assujetti avant de créer une déclaration, et si ce n'est pas le cas lève
	 * une exception de validation.
	 *
	 * @param numeroCtb
	 *            le numéro de contribuable
	 * @param range
	 *            le plage de validité de la déclaration
	 * @param view
	 *            le form backing object a compléter
	 */
	public abstract void creerDI(Long numeroCtb, DateRange range, DeclarationImpotDetailView view);

	/**
	 * [UNIREG-832] Calcule les dates de début et de fin pour la création de la prochaine d'impôt sur un contribuable. Si plusieurs
	 * déclarations n'ont pas été envoyées durant les années précédentes, cette méthode retourne les dates de la déclaration non-envoyée la
	 * plus ancienne.
	 * <p>
	 * <b>Note:</b> Si le contribuable n'est pas du tout assujetti, ou s'il n'est plus assujetti et que toutes ses déclarations ont été
	 * envoyées, la méthode retourne <code>null</code>.
	 *
	 * @param numero
	 *            le numéro de contribuable
	 * @return une liste de périodes d'imposition à utiliser pour une nouvelle déclaration, <code>null</code> s'il n'est pas possible
	 *         d'ajouter une déclaration d'impôt.
	 * @throws ValidationException
	 *             si le contribuable ne valide pas, ou s'il n'est pas possible de déterminer son assujettissement.
	 */
	public List<PeriodeImposition> calculateRangesProchainesDIs(Long numero) throws ValidationException;

	/**
	 * calcul l'année d'une nouvelle DI et vérifie que la période fiscale correspondante existe
	 * utilisé par creerDI
	 *
	 * @param numero
	 * @return une date dont l'année correspond à la période fiscale d'une nouvelle DI à créer ou null si la période fiscale n'existe pas
	 */
	public abstract RegDate getDateNewDi(Long numero);

	/**
	 * Persiste en base et indexe le tiers modifie
	 *
	 * @param idDi
	 * @param delai
	 */
	@Transactional(rollbackFor = Throwable.class)
	public DeclarationImpotOrdinaire save(DeclarationImpotDetailView diEditView) throws Exception ;

	/**
	 * Imprime une DI vierge
	 * Partie envoie
	 * @param diEditView
	 * @throws EditiqueException
	 */
	@Transactional(rollbackFor = Throwable.class)
	public String envoieImpressionLocalDI(DeclarationImpotDetailView diEditView) throws Exception  ;

	/**
	 * Partie reception d'un document de l'editique
	 * Partie reception
	 * @param lrEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract byte[] recoitImpressionLocal(String docID) throws DeclarationException;

	/**
	 * Annule une DI
	 *
	 * @param diEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void annulerDI(DeclarationImpotDetailView diEditView);

	/**
	 * Mintient une DI et passe la tâche à traitée
	 *
	 * @param idTache
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void maintenirDI(Long idTache);

	/**
	 * Annule un delai
	 *
	 * @param diEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void annulerDelai(DeclarationImpotDetailView diEditView, Long idDelai);

	/**
	 * Persiste en base le delai
	 *
	 * @param delaiView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void saveDelai(DelaiDeclarationView delaiView);

	/**
	 * Alimente la vue contribuable pour la DI
	 *
	 * @param numero
	 * @return
	 * @throws AdressesResolutionException
	 */
	public abstract TiersGeneralView creerCtbDI(Long numero) throws AdressesResolutionException;

	/**
	 * Alimente la vue du controller DeclarationImpotImpressionController
	 *
	 * @param id
	 * @param type Document
	 * @return
	 */
	public DeclarationImpotImpressionView getView(Long id, String typeDocument);


	/**
	 * Sommer une Declaration Impot
	 *
	 * @param bean
	 */
	@Transactional(rollbackFor = Throwable.class)
	public String envoieImpressionLocalSommationDI(DeclarationImpotDetailView bean)  throws EditiqueException;

	/**
	 * Imprimer la lettre de confirmation de délai
	 *
	 * @param bean
	 * @param idDelai
	 */
	public String envoieImpressionLocalConfirmationDelai(DeclarationImpotDetailView bean, Long idDelai)  throws EditiqueException;

	/**
	 * Imprimer un duplicata de sommation de declaration Impot
	 *
	 * @param bean
	 */
	@Transactional(rollbackFor = Throwable.class)
	public String envoieImpressionLocalDuplicataSommationDI(DeclarationImpotDetailView bean)  throws EditiqueException;

	/**
	 * Imprimer la chemise de taxation d'office
	 *
	 * @param bean
	 * @param idDelai
	 */
	public String envoieImpressionLocalTaxationOffice(DeclarationImpotDetailView bean)  throws EditiqueException;

	/**
	 * Cree une vue pour le delai d'une declaration
	 *
	 * @param idDeclaration
	 * @return
	 */
	public DelaiDeclarationView creerDelai(Long idDeclaration);

	/**
	 * @param diImpressionView
	 * @param declaration
	 * @throws DeclarationException
	 * @throws DeclarationException
	 */
	@Transactional(rollbackFor = Throwable.class)
	public String envoieImpressionLocalDuplicataDI(DeclarationImpotImpressionView diImpressionView) throws DeclarationException;


	/**
	 * Récupère la copie conforme de la sommation
	 * @param diEditView
	 * @return
	 * @throws EditiqueException
	 */
	public byte[] getCopieConformeSommation(DeclarationImpotDetailView diEditView) throws EditiqueException ;

}

