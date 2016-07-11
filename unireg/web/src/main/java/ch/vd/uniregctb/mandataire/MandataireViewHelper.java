package ch.vd.uniregctb.mandataire;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public abstract class MandataireViewHelper {

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
