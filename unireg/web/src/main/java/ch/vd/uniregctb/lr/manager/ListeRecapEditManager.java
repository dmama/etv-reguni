package ch.vd.uniregctb.lr.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.lr.view.ListeRecapListView;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

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
	public ListeRecapDetailView get(Long id) ;

	/**
	 * Rafraichissement de la vue
	 *
	 * @param view
	 * @return
	 */
	@Transactional(readOnly = true)
	public ListeRecapDetailView refresh(ListeRecapDetailView view) ;

	/**
	 * Alimente la vue ListeRecapListView en fonction d'un debiteur
	 * @return une vue ListeRecapListView
	 */
	@Transactional(readOnly = true)
	public abstract ListeRecapListView findByNumero(Long numero) ;

	/**
	 * Cree une nouvelle LR
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	public abstract ListeRecapDetailView creerLr(Long numero) ;

	/**
	 * Persiste en base et indexe le tiers modifie
	 *
	 * @param lrEditView
	 * @throws Exception
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract DeclarationImpotSource save(ListeRecapDetailView lrEditView) throws Exception;

	/**
	 * Annule un delai
	 *
	 * @param lrEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void annulerDelai(ListeRecapDetailView lrEditView, Long idDelai);

	/**
	 * Persiste en base et indexe le tiers modifie
	 *
	 * @param lrEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void saveDelai(Long idLr, DelaiDeclaration delai);

	/**
	 * Contrôle la présence de la LR
	 *
	 * @param id
	 */
	@Transactional(readOnly = true)
	public void controleLR(Long id);

	/**
	 * Imprime une LR vierge
	 * Partie envoie
	 * @param lrEditView
	 * @throws EditiqueException
	 */
	@Transactional(rollbackFor = Throwable.class)
	public String envoieImpressionLocalLR(ListeRecapDetailView lrEditView) throws Exception  ;

	/**
	 * Imprime une sommation LR
	 * Partie envoie
	 * @param lrEditView
	 * @throws EditiqueException
	 */
	@Transactional(rollbackFor = Throwable.class)
	public String envoieImpressionLocalSommationLR(ListeRecapDetailView lrEditView) throws Exception  ;

	/**
	 * Imprime un duplicata de LR
	 * @param lrEditView
	 * @return
	 * @throws Exception
	 */
	@Transactional(readOnly = true)
	public String envoieImpressionLocalDuplicataLR(ListeRecapDetailView lrEditView) throws Exception  ;

	/**
	 * Imprime une LR vierge
	 * Partie reception
	 * @param docID
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract byte[] recoitImpressionLocal(String docID) throws DeclarationException;

	/**
	 * Annule une LR
	 *
	 * @param lrEditView
	 */
	@Transactional
	public abstract void annulerLR(ListeRecapDetailView lrEditView);

}
