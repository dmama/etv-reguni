package ch.vd.uniregctb.annulation.couple.manager;

import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.annulation.couple.view.AnnulationCoupleRecapView;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class AnnulationCoupleRecapManagerImpl implements AnnulationCoupleRecapManager {

	private TiersService tiersService;

	private MetierService metierService;

	private TiersGeneralManager tiersGeneralManager;


	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public MetierService getMetierService() {
		return metierService;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public TiersGeneralManager getTiersGeneralManager() {
		return tiersGeneralManager;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	/**
	 * Alimente la vue AnnulationCoupleRecapView
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	public AnnulationCoupleRecapView get(Long numero) {
		AnnulationCoupleRecapView annulationCoupleRecapView =  new AnnulationCoupleRecapView();
		//FIXME (CGD) impléementer Ifosec
		MenageCommun menage = (MenageCommun) tiersService.getTiers(numero);
		TiersGeneralView menageView = tiersGeneralManager.getTiers(menage, true);
		annulationCoupleRecapView.setCouple(menageView);

		EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(menage, null);

		final MenageCommun menageCommun = ensembleTiersCouple.getMenage();
		if (menageCommun != null) {
			RapportEntreTiers dernierRapport = menageCommun.getDernierRapportObjet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
			annulationCoupleRecapView.setDateMenageCommun(dernierRapport.getDateDebut());
		}

		return annulationCoupleRecapView;
	}

	/**
	 * Persiste le rapport
	 *
	 * @param annulationCoupleRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(AnnulationCoupleRecapView annulationCoupleRecapView) {
		MenageCommun menage = (MenageCommun) tiersService.getTiers(annulationCoupleRecapView.getCouple().getNumero());
		EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(menage, annulationCoupleRecapView.getDateMenageCommun());
		metierService.annuleMariage(ensembleTiersCouple.getPrincipal(), ensembleTiersCouple.getConjoint(), annulationCoupleRecapView.getDateMenageCommun(), null);
	}

}
