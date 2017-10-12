package ch.vd.uniregctb.registrefoncier.communaute;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRFPrincipalInfo;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

public class PrincipalCommunauteRFView implements Annulable {

	private final Long id;
	/**
	 * Vrai si le principal est par défaut, c'est-à-dire qu'il n'a pas été explicitement élu.
	 */
	private final boolean parDefaut;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final MembreCommunauteView principal;

	public PrincipalCommunauteRFView(@NotNull CommunauteRFPrincipalInfo principal, @NotNull TiersService tiersService, @NotNull RegistreFoncierService registreFoncierService) {
		this.id = principal.getId();
		this.parDefaut = principal.isParDefaut();
		this.dateDebut = principal.getDateDebut();
		this.dateFin = principal.getDateFin();
		final Contribuable ctb = (Contribuable) tiersService.getTiers(principal.getCtbId());
		final AyantDroitRF ayantDroit = Optional.ofNullable(principal.getAyantDroitId())
				.map(registreFoncierService::getAyantDroit)
				.orElse(null);
		this.principal = new MembreCommunauteView(ayantDroit, ctb, tiersService, registreFoncierService);
	}

	public Long getId() {
		return id;
	}

	public boolean isParDefaut() {
		return parDefaut;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public MembreCommunauteView getPrincipal() {
		return principal;
	}

	@Override
	public boolean isAnnule() {
		return false;
	}
}