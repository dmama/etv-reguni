package ch.vd.unireg.mandataire;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

public abstract class MandataireViewHelper {

	/**
	 * Comparateur canonique sur les vues mandataires
	 * <br/>
	 * <ul>
	 *     <li>les annulés à la fin</li>
	 *     <li>selon le type de mandat : GENERAL, puis SPECIAL, puis TIERS</li>
	 *     <li>du plus récent au plus vieux</li>
	 * </ul>
	 */
	public static final Comparator<MandataireView> BASIC_COMPARATOR = new AnnulableHelper.AnnulesApresWrappingComparator<>(new Comparator<MandataireView>() {
		@Override
		public int compare(MandataireView o1, MandataireView o2) {
			int comparison = o1.getTypeMandat().compareTo(o2.getTypeMandat());
			if (comparison == 0) {
				comparison = -DateRangeComparator.compareRanges(o1, o2);
			}
			return comparison;
		}
	});

	/**
	 * Comparateur canonique sur les vues d'adresses "courrier" mandataire
	 * <br/>
	 * <ul>
	 *     <li>les annulés à la fin</li>
	 *     <li>selon le type de mandat : GENERAL, puis SPECIAL, puis TIERS (ce dernier n'a normalement pas de sens ici)</li>
	 *     <li>du plus récent au plus vieux</li>
	 *     <li>selon l'ordre alphabétique du libellé du genre d'impôt</li>
	 * </ul>
	 */
	public static final Comparator<MandataireCourrierView> COURRIER_COMPARATOR = new AnnulableHelper.AnnulesApresWrappingComparator<>(new Comparator<MandataireCourrierView>() {
		@Override
		public int compare(MandataireCourrierView o1, MandataireCourrierView o2) {
			int comparison = BASIC_COMPARATOR.compare(o1, o2);
			if (comparison == 0) {
				if (o1.getLibelleGenreImpot() != null) {
					if (o2.getLibelleGenreImpot() != null) {
						comparison = o1.getLibelleGenreImpot().compareTo(o2.getLibelleGenreImpot());
					}
					else {
						comparison = -1;
					}
				}
				else if (o2.getLibelleGenreImpot() != null) {
					comparison = 1;
				}
			}
			return comparison;
		}
	});

	@Nullable
	public static String extractLibelleGenreImpot(String codeGenreImpot, ServiceInfrastructureService infraService) {
		if (StringUtils.isBlank(codeGenreImpot)) {
			return null;
		}

		final List<GenreImpotMandataire> tousGenres = infraService.getGenresImpotMandataires();
		for (GenreImpotMandataire genreImpot : tousGenres) {
			if (codeGenreImpot.equals(genreImpot.getCode())) {
				return genreImpot.getLibelle();
			}
		}

		return String.format("## Genre d'impôt inconnu : %s", codeGenreImpot);
	}

}
