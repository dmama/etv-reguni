package ch.vd.unireg.registrefoncier.communaute;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.registrefoncier.RegroupementCommunauteRF;

public class RegroupementRFView {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final long communauteId;
	private final ImmeubleRFView immeuble;

	public RegroupementRFView(@NotNull RegroupementCommunauteRF r, @NotNull RegistreFoncierService registreFoncierService) {
		this.dateDebut = r.getDateDebut();
		this.dateFin = r.getDateFin();
		this.communauteId = r.getCommunaute().getId();
		final Set<DroitProprieteRF> droits = r.getCommunaute().getDroitsPropriete();
		if (droits.size() > 1) {
			// par définition, une communauté ne possède qu'un seul droit
			throw new IllegalArgumentException("La communauté n°" + r.getCommunaute().getId() + " possède plusieurs droits");
		}
		final DroitProprieteRF droit = droits.iterator().next();
		this.immeuble = new ImmeubleRFView(droit.getImmeuble(), registreFoncierService);
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public long getCommunauteId() {
		return communauteId;
	}

	public ImmeubleRFView getImmeuble() {
		return immeuble;
	}
}
