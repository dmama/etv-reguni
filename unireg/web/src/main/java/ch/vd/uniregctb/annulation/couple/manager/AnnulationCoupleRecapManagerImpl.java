package ch.vd.uniregctb.annulation.couple.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.annulation.couple.view.AnnulationCoupleRecapView;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
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

		final AnnulationCoupleRecapView annulationCoupleRecapView =  new AnnulationCoupleRecapView();
		//FIXME (CGD) impléementer Ifosec

		final EnsembleTiersCouple couple = getEnsembleTiersCouple(numero, null);
		final MenageCommun menageCommun = couple.getMenage();
		final TiersGeneralView menageView = tiersGeneralManager.getTiers(menageCommun, true);
		annulationCoupleRecapView.setCouple(menageView);

		if (menageCommun != null) {
			final RapportEntreTiers dernierRapport = menageCommun.getDernierRapportObjet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
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
	public void save(AnnulationCoupleRecapView annulationCoupleRecapView) throws MetierServiceException {
		final EnsembleTiersCouple couple = getEnsembleTiersCouple(annulationCoupleRecapView.getCouple().getNumero(), annulationCoupleRecapView.getDateMenageCommun());
		metierService.annuleMariage(couple.getPrincipal(), couple.getConjoint(), annulationCoupleRecapView.getDateMenageCommun(), null);
	}

	private EnsembleTiersCouple getEnsembleTiersCouple(long noCtb, RegDate date) {
		final Tiers tiers = tiersService.getTiers(noCtb);
		if (tiers instanceof MenageCommun) {
			return tiersService.getEnsembleTiersCouple((MenageCommun) tiers, date);
		}
		else if (tiers instanceof PersonnePhysique) {
			return tiersService.getEnsembleTiersCouple((PersonnePhysique) tiers, date);
		}
		return null;
	}

	@Transactional(readOnly = true)
	public boolean isMenageCommunAvecPrincipal(long noCtb, RegDate date) {
		final EnsembleTiersCouple couple = getEnsembleTiersCouple(noCtb, date);
		return couple != null && (couple.getPrincipal() != null || couple.getConjoint() != null);
	}

}
