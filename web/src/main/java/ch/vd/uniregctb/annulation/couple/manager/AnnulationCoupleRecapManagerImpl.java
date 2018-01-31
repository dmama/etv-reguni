package ch.vd.uniregctb.annulation.couple.manager;

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
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

	@Override
	@Transactional(readOnly = true)
	public RegDate getDateDebutDernierMenage(long idMenage) {
		final EnsembleTiersCouple couple = getEnsembleTiersCouple(idMenage, null);
		if (couple == null) {
			throw new TiersNotFoundException(idMenage);
		}
		return Optional.ofNullable(couple.getMenage())
				.map(menage -> menage.getDernierRapportObjet(TypeRapportEntreTiers.APPARTENANCE_MENAGE))
				.map(RapportEntreTiers::getDateDebut)
				.orElse(null);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void annuleCouple(long idMenage, RegDate dateReference) throws MetierServiceException {
		final EnsembleTiersCouple couple = getEnsembleTiersCouple(idMenage, dateReference);
		if (couple == null) {
			throw new TiersNotFoundException(idMenage);
		}
		metierService.annuleMariage(couple.getPrincipal(), couple.getConjoint(), dateReference, null);
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

	@Override
	@Transactional(readOnly = true)
	public boolean isMenageCommunAvecPrincipal(long noCtb, RegDate date) {
		final EnsembleTiersCouple couple = getEnsembleTiersCouple(noCtb, date);
		return couple != null && (couple.getPrincipal() != null || couple.getConjoint() != null);
	}

}
