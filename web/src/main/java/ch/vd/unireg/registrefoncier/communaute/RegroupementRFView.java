package ch.vd.unireg.registrefoncier.communaute;

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
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
		final CommunauteRF communaute = r.getCommunaute();
		this.communauteId = communaute.getId();
		final ImmeubleRF immeuble = getImmeuble(communaute);
		this.immeuble = new ImmeubleRFView(immeuble, registreFoncierService);
	}

	/**
	 * @param communaute une communauté
	 * @return l'immeuble possédé par la communauté
	 */
	@NotNull
	private static ImmeubleRF getImmeuble(@NotNull CommunauteRF communaute) {
		// [IMM-1271] une communauté peut posséder plusieurs droits, mais ils doivent tous pointer vers le même immeuble
		final List<ImmeubleRF> immeubles = communaute.getDroitsPropriete().stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(DroitProprieteRF::getImmeuble)
				.distinct()
				.collect(Collectors.toList());
		if (immeubles.size() > 1) {
			// par définition, une communauté ne possède qu'un seul immeuble
			throw new IllegalArgumentException("La communauté n°" + communaute.getId() + " possède plusieurs immeubles (incohérence des données)");
		}
		return immeubles.iterator().next();
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
