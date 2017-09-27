package ch.vd.uniregctb.registrefoncier.communaute;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.RegroupementCommunauteRF;

public class RegroupementRFView {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final ImmeubleRFView immeuble;

	public RegroupementRFView(@NotNull RegroupementCommunauteRF r, @NotNull RegistreFoncierService registreFoncierService) {
		this.dateDebut = r.getDateDebut();
		this.dateFin = r.getDateFin();
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

	public ImmeubleRFView getImmeuble() {
		return immeuble;
	}
}
