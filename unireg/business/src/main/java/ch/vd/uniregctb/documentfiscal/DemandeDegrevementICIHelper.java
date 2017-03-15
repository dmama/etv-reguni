package ch.vd.uniregctb.documentfiscal;

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
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.DescriptionBatimentRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;

/**
 * Rassemblement de méthodes utilitaires autour des demandes de dégrèvement ICI
 */
public abstract class DemandeDegrevementICIHelper {

	/**
	 * @param formulaire le formulaire
	 * @param rfService le service du registre foncier
	 * @return la commune de l'immeuble lié au formulaire de demande de dégrèvement
	 */
	@Nullable
	public static Commune getCommune(DemandeDegrevementICI formulaire, RegistreFoncierService rfService) {
		final RegDate dateReference = getDateReference(formulaire);
		return getCommune(formulaire.getImmeuble(), dateReference, rfService);
	}

	/**
	 * @param immeuble un immeuble
	 * @param dateReference une date de référence
	 * @param rfService le service du registre foncier
	 * @return la commune de l'immeuble, à la date de référence
	 */
	@Nullable
	static Commune getCommune(ImmeubleRF immeuble, RegDate dateReference, RegistreFoncierService rfService) {
		return rfService.getCommune(immeuble, dateReference);
	}

	/**
	 * @param formulaire le formulaire de demande de dégrèvement
	 * @param rfService le service du registre foncier
	 * @return l'estimation fiscale de l'immeuble lié au formulaire de demande de dégrèvement
	 */
	@Nullable
	public static Long getEstimationFiscale(DemandeDegrevementICI formulaire, RegistreFoncierService rfService) {
		final RegDate dateReference = getDateReference(formulaire);
		return getEstimationFiscale(formulaire.getImmeuble(), dateReference, rfService);
	}

	/**
	 * @param immeuble un immeuble
	 * @param dateReference une date de référence
	 * @param rfService le service du registre foncier
	 * @return l'estimation fiscale de l'immeuble, à la date de référence
	 */
	@Nullable
	static Long getEstimationFiscale(ImmeubleRF immeuble, RegDate dateReference, RegistreFoncierService rfService) {
		return rfService.getEstimationFiscale(immeuble, dateReference);
	}

	/**
	 * @param formulaire le formulaire de demande de dégrèvement
	 * @param rfService le service du registre foncier
	 * @return le numéro de parcelle complet (avec tirets si nécessaire) de l'immeuble lié au formulaire de demande de dégrèvement
	 */
	@Nullable
	public static String getNumeroParcelleComplet(DemandeDegrevementICI formulaire, RegistreFoncierService rfService) {
		final RegDate dateReference = getDateReference(formulaire);
		return getNumeroParcelleComplet(formulaire.getImmeuble(), dateReference, rfService);
	}

	/**
	 * @param immeuble un immeuble
	 * @param dateReference une date de référence
	 * @param rfService le service du registre foncier
	 * @return le numéro de parcelle complet de l'immeuble, à la date de référence
	 */
	@Nullable
	static String getNumeroParcelleComplet(ImmeubleRF immeuble, RegDate dateReference, RegistreFoncierService rfService) {
		return rfService.getNumeroParcelleComplet(immeuble, dateReference);
	}

	/**
	 * @param formulaire le formulaire de demande de dégrèvement
	 * @param maxLength la longueur maximale acceptable pour la description de la nature de l'immeuble lié au formulaire de demande de dégrèvement
	 * @return une chaîne de caractères qui décrit la nature de l'immeuble
	 */
	@Nullable
	public static String getNatureImmeuble(DemandeDegrevementICI formulaire, int maxLength) {
		final RegDate dateReference = getDateReference(formulaire);
		return getNatureImmeuble(formulaire.getImmeuble(), dateReference, maxLength);
	}

	/**
	 * @param immeuble un immeuble
	 * @param dateReference une date de référence
	 * @param maxLength la longueur maximale acceptable pour la description de la nature de l'immeuble
	 * @return la nature de l'immeuble, à la date de référence
	 */
	@Nullable
	static String getNatureImmeuble(ImmeubleRF immeuble, RegDate dateReference, int maxLength) {

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

	/**
	 * Méthode centralisée pour la détermination de la date de référence
	 * @param formulaire formulaire de demande de dégrèvement
	 * @return la date de référence pour les données à extraire
	 */
	private static RegDate getDateReference(DemandeDegrevementICI formulaire) {
		return RegDate.get(formulaire.getPeriodeFiscale(), 1, 1);
	}
}
