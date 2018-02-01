package ch.vd.unireg.registrefoncier;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AnnulableHelper;

/**
 * Méthodes utilitaires autour des immeubles
 */
public abstract class ImmeubleHelper {

	/**
	 * @param immeuble un immeuble
	 * @param dateReference une date de référence
	 * @param maxLength la longueur maximale acceptable pour la description de la nature de l'immeuble
	 * @return la nature de l'immeuble, à la date de référence
	 */
	@Nullable
	public static String getNatureImmeuble(ImmeubleRF immeuble, RegDate dateReference, int maxLength) {

		// comment trouver la description d'une implantation ?
		final Function<ImplantationRF, DescriptionBatimentRF> descriptionFromImplantation =
				implantation -> Stream.of(implantation.getBatiment())
						.filter(AnnulableHelper::nonAnnule)                 // implantation -> bâtiment non-annulé
						.map(BatimentRF::getDescriptions)
						.flatMap(Set::stream)
						.filter(AnnulableHelper::nonAnnule)
						.filter(desc -> desc.isValidAt(dateReference))      // bâtiment -> descriptions valides non-annulées
						.findFirst()
						.orElse(null);

		// composantes qui viennent des bâtiments
		final Stream<String> streamNaturesBatiments = immeuble.getImplantations().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(impl -> impl.isValidAt(dateReference))      // immeuble -> implantations valides non-annulées
				.map(implantation -> {
					final DescriptionBatimentRF description = descriptionFromImplantation.apply(implantation);
					if (description != null) {
						final Integer surface = Optional.ofNullable(implantation.getSurface()).orElse(description.getSurface());
						return Pair.of(description, surface);
					}
					return null;
				})
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(Pair::getRight, Comparator.nullsLast(Comparator.reverseOrder())))      // tris par surface décroissante
				.map(Pair::getLeft)
				.map(DescriptionBatimentRF::getType);

		// composantes qui viennent des surfaces au sol
		final Stream<String> streamNaturesSurfaces = immeuble.getSurfacesAuSol().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(ss -> ss.isValidAt(dateReference))        // immeuble -> surfaces au sol valides non-annulés
				.sorted(Comparator.comparingInt(SurfaceAuSolRF::getSurface).reversed())     // tri par surface décroissante
				.map(SurfaceAuSolRF::getType);

		// d'abord les bâtiments, puis les surfaces au sol
		final List<String> composantes = Stream.concat(streamNaturesBatiments, streamNaturesSurfaces)
				.map(StringUtils::trimToNull)
				.filter(Objects::nonNull)
				.distinct()                     // il ne sert à rien de présenter plusieurs fois la même nature
				.collect(Collectors.toList());

		if (composantes.isEmpty()) {
			return null;
		}

		// [SIFISC-23178] on prend des composantes entières (sauf si la première est déjà trop grande)
		while (true) {
			final String nature = composantes.stream().collect(Collectors.joining(" / "));
			if (nature.length() <= maxLength) {
				return nature;
			}

			if (composantes.size() == 1) {
				// un seul élément trop grand... abréviation
				return StringUtils.abbreviate(nature, maxLength);
			}

			// on enlève un élément et on ré-essaie
			composantes.remove(composantes.size() - 1);
		}
	}
}
