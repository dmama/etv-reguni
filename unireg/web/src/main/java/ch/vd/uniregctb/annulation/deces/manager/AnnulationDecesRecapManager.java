package ch.vd.uniregctb.annulation.deces.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.annulation.deces.view.AnnulationDecesRecapView;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public interface AnnulationDecesRecapManager {

	/**
	 * Alimente la vue AnnulationDecesRecapView
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	public AnnulationDecesRecapView get(Long numero);





	/**
	 * Persiste le rapport
	 *
	 * @param annulationDecesRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(AnnulationDecesRecapView annulationDecesRecapView);


	/**Veuf marie seul ou pas
	 *
	 * @param tiers
	 * @return true si veuf marie seul false sinon
	 */
	@Transactional(readOnly = true)
	boolean isVeuvageMarieSeul(PersonnePhysique tiers);

	/**Indique si le tiers est décédé
	 *
	 * @param tiers
	 * @return
	 */
	@Transactional(readOnly = true)
	boolean isDecede(PersonnePhysique tiers);
}
