package ch.vd.unireg.registrefoncier.communaute;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.CommunauteRFPrincipalInfo;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TiersService;

public class PrincipalCommunauteRFView implements Annulable {

	private final Long id;
	/**
	 * Vrai si le principal est par défaut, c'est-à-dire qu'il n'a pas été explicitement élu.
	 */
	private final boolean parDefaut;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final MembreCommunauteView principal;
	private final boolean annule;

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
		this.annule = principal.isAnnule();
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
		return annule;
	}
}
