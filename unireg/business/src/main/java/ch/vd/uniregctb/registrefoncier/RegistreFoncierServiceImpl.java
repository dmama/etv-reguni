package ch.vd.uniregctb.registrefoncier;

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.tiers.Contribuable;

public class RegistreFoncierServiceImpl implements RegistreFoncierService {

	@NotNull
	@Override
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb) {
		return ctb.getRapprochementsRF().stream()
				.filter(r -> r.isValidAt(null))         // on ne prend que les rapprochements valides
				.map(RapprochementRF::getTiersRF)       // on général, il n'y a qu'un tiers RF, mais le modèle permet d'en avoir plusieurs
				.flatMap(r -> r.getDroits().stream())   // si on a plusieurs tiers rapprochés, on prend l'ensemble des droits
				.sorted(new DateRangeComparator<>(DateRangeComparator.CompareOrder.ASCENDING))
				.collect(Collectors.toList());
	}
}
