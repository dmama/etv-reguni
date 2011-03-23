package ch.vd.uniregctb.identification.contribuable.tooltip.individu;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public class IdentificationIndividuTooltipManagerImpl implements IdentificationIndividuTooltipManager {

	private TiersDAO tiersDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Transactional(readOnly = true)
	public Long getNumeroIndividuFromCtb(Long noCtb) {
		if (noCtb == null) {
			return null;
		}

		Long noInd = null;
		final Tiers tiers = tiersDAO.get(noCtb);
		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isHabitantVD()) {
				noInd = pp.getNumeroIndividu();
			}
		}

		return noInd;
	}
}
