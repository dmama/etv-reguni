package ch.vd.unireg.tiers.vignette;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.MessageSource;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.TiersService;

public class EnsembleTiersCoupleView {

	private VignetteView principal;
	private VignetteView conjoint;
	private VignetteView menage;

	public EnsembleTiersCoupleView(@NotNull EnsembleTiersCouple ensemble, TiersService tiersService, AdresseService adresseService, AvatarService avatarService, ServiceInfrastructureService infraService, MessageSource messageSource) {
		if (ensemble.getPrincipal() != null) {
			this.principal = new VignetteView(ensemble.getPrincipal(), false, false, false, false, false, tiersService, adresseService, avatarService, infraService, messageSource);
		}
		if (ensemble.getConjoint() != null) {
			this.conjoint = new VignetteView(ensemble.getConjoint(), false, false, false, false, false, tiersService, adresseService, avatarService, infraService, messageSource);
		}
		this.menage = new VignetteView(ensemble.getMenage(), false, false, false, false, false, tiersService, adresseService, avatarService, infraService, messageSource);
	}

	public VignetteView getPrincipal() {
		return principal;
	}

	public VignetteView getConjoint() {
		return conjoint;
	}

	public VignetteView getMenage() {
		return menage;
	}
}
