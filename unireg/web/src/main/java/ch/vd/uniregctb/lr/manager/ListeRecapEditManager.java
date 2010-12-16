package ch.vd.uniregctb.lr.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.delai.DelaiDeclarationView;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.lr.view.ListeRecapListView;

/**
 * Service offrant des methodes pour gérer le controller ListeRecapEditController
 *
 * @author xcifde
 *
 */
public interface ListeRecapEditManager {

	/**
	 * Alimente la vue ListeRecapEditView en fonction de l'ID de la LR
	 * @param id
	 * @return une vue ListeRecapEditView
	 */
	@Transactional(readOnly = true)
	ListeRecapDetailView get(Long id) ;

	/**
	 * Rafraichissement de la vue
	 *
	 * @param view
	 * @return
	 */
	@Transactional(readOnly = true)
	ListeRecapDetailView refresh(ListeRecapDetailView view) ;

	/**
	 * Alimente la vue ListeRecapListView en fonction d'un debiteur
	 * @return une vue ListeRecapListView
	 */
	@Transactional(readOnly = true)
	ListeRecapListView findByNumero(Long numero) ;

	@Transactional(readOnly = true)
	DelaiDeclarationView creerDelai(Long idLr);

	/**
	 * Cree une nouvelle LR
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	ListeRecapDetailView creerLr(Long numero) ;

	/**
	 * Persiste en base et indexe le tiers modifie
	 *
	 * @param lrEditView
	 * @throws Exception
	 */
	@Transactional(rollbackFor = Throwable.class)
	DeclarationImpotSource save(ListeRecapDetailView lrEditView);

	/**
	 * Annule un delai
	 *
	 * @param lrEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	void annulerDelai(ListeRecapDetailView lrEditView, Long idDelai);

	/**
	 * Persiste en base et indexe le tiers modifie
	 */
	@Transactional(rollbackFor = Throwable.class)
	void saveDelai(DelaiDeclarationView delai);

	/**
	 * Contrôle la présence de la LR
	 *
	 * @param id
	 */
	@Transactional(readOnly = true)
	void controleLR(Long id);

	/**
	 * Imprime une LR vierge
	 * Partie envoie
	 * @param lrEditView
	 * @throws EditiqueException
	 */
	@Transactional(rollbackFor = Throwable.class)
	EditiqueResultat envoieImpressionLocalLR(ListeRecapDetailView lrEditView) throws EditiqueException;

	/**
	 * Imprime une sommation LR
	 * Partie envoie
	 * @param lrEditView
	 * @throws EditiqueException
	 */
	@Transactional(rollbackFor = Throwable.class)
	EditiqueResultat envoieImpressionLocalSommationLR(ListeRecapDetailView lrEditView) throws EditiqueException;

	/**
	 * Imprime un duplicata de LR
	 * @param lrEditView
	 * @return
	 * @throws Exception
	 */
	@Transactional(readOnly = true)
	EditiqueResultat envoieImpressionLocalDuplicataLR(ListeRecapDetailView lrEditView) throws EditiqueException;

	/**
	 * Annule une LR
	 *
	 * @param lrEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	void annulerLR(ListeRecapDetailView lrEditView);
}
