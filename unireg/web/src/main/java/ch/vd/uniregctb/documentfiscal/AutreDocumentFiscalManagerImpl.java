package ch.vd.uniregctb.documentfiscal;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEtatAutreDocumentFiscal;

public class AutreDocumentFiscalManagerImpl implements AutreDocumentFiscalManager {

	private TiersService tiersService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@Transactional(rollbackFor = Throwable.class)
	@Override
	public ResultatQuittancement quittanceLettreBienvenue(long noCtb, RegDate dateRetour) {
		final Tiers tiers = tiersService.getTiers(noCtb);
		if (tiers == null || !(tiers instanceof Entreprise)) {
			return ResultatQuittancement.entrepriseInexistante();
		}

		final Entreprise entreprise = (Entreprise) tiers;
		final List<LettreBienvenue> lettresBienvenue = entreprise.getAutresDocumentsFiscaux(LettreBienvenue.class, true, false);
		if (lettresBienvenue.isEmpty()) {
			return ResultatQuittancement.rienAQuittancer(TypeAutreDocumentFiscal.LETTRE_BIENVENUE);
		}

		for (LettreBienvenue candidate : CollectionsUtils.revertedOrder(lettresBienvenue)) {
			if (candidate.getEtat() != TypeEtatAutreDocumentFiscal.RETOURNE) {
				candidate.setDateRetour(dateRetour);
				return ResultatQuittancement.ok();
			}
		}
		return ResultatQuittancement.rienAQuittancer(TypeAutreDocumentFiscal.LETTRE_BIENVENUE);
	}
}
