package ch.vd.uniregctb.mouvement.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.mouvement.view.MouvementListView;

/**
 * Classe mettant à disposition les methodes pour le controller
 *
 * @author xcifde
 *
 */
public interface MouvementEditManager extends AbstractMouvementManager {

	/**
	 * Alimente la vue MouvementListView en fonction d'un contribuable
	 * @param seulementTraites si levé, signifie que les mouvements de masse non "traités" ne sont pas pris en compte
	 * @return une vue MouvementListView
	 */
	@Transactional(readOnly = true)
	public abstract MouvementListView findByNumeroDossier(Long numero, boolean seulementTraites) throws ServiceInfrastructureException;

	/**
	 * Creer une vue pour le mvt de dossier
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	public MouvementDetailView creerMvt(Long numero) ;

	/**
	 * Creer une vue pour le mvt de dossier depuis la tache transmission de dossier
	 *
	 * @param numero
	 * @param idTache
	 * @return
	 */
	@Transactional(readOnly = true)
	public MouvementDetailView creerMvtForTacheTransmissionDossier(Long numero, Long idTache) throws ServiceInfrastructureException ;

	/**
	 * Persiste en base le nouveau mvt de dossier
	 *
	 * @param mvtDetailView
	 * @throws Exception
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(MouvementDetailView mvtDetailView) throws Exception ;

	/**
	 * Annule un mouvement
	 *
	 * @param idMvt
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerMvt(long idMvt);

	/**
	 * Retouve le numero du contribuable associé à un mouvement
	 *
	 * @param idMvt l'id du mouvement
	 * @return le numéro du contribuable associé au mouvement
	 */
	long getNumeroContribuable(Long idMvt);

}
