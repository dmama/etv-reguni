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


	/**
	 * Veuf marie seul ou pas
	 *
	 * @param noTiers numéro du tiers dont on veut connaître un peu plus
	 * @return <code>true</code> si veuf marié seul, <code>false</code> sinon
	 */
	@Transactional(readOnly = true)
	boolean isVeuvageMarieSeul(long noTiers);

	/**
	 * Indique si le tiers est décédé
	 *
	 * @param noTiers numéro du tiers dont on veut savoir s'il est décédé
	 * @return <code>true</code> si le tiers est une personne physique décédée, <code>false</code> sinon
	 */
	@Transactional(readOnly = true)
	boolean isDecede(long noTiers);
}
