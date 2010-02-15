package ch.vd.uniregctb.mouvement.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
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
	public abstract MouvementListView findByNumeroDossier(Long numero, boolean seulementTraites) throws InfrastructureException;

	/**
	 * Creer une vue pour le mvt de dossier
	 *
	 * @param numero
	 * @return
	 */
	public MouvementDetailView creerMvt(Long numero) ;

	/**
	 * Creer une vue pour le mvt de dossier depuis la tache transmission de dossier
	 *
	 * @param numero
	 * @param idTache
	 * @return
	 */
	public MouvementDetailView creerMvtForTacheTransmissionDossier(Long numero, Long idTache) throws InfrastructureException ;

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

}
