package ch.vd.unireg.interfaces.infra.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.fidor.xml.regimefiscal.v2.Exoneration;
import ch.vd.fidor.xml.regimefiscal.v2.RegimeFiscal;
import ch.vd.unireg.type.CategorieEntreprise;

public class TypeRegimeFiscalFidor extends AbstractTypeRegimeFiscal {

	private static final long serialVersionUID = 8293895518175253766L;

	public static TypeRegimeFiscal get(RegimeFiscal regime) {
		if (regime == null) {
			return null;
		}
		final List<PlageExonerationFiscale> exos;
		final List<Exoneration> exonerations = regime.getExoneration();
		if (exonerations != null && !exonerations.isEmpty()) {
			exos = new ArrayList<>(exonerations.size());
			for (final Exoneration exo : exonerations) {
				exos.add(new PlageExonerationFiscale(exo.getPeriodeFiscaleDebutValidite(), exo.getPeriodeFiscaleFinValidite(), genreImpotFromCode(exo.getGenreImpot().getCode()), modeExonerationFromCode(exo.getMode().value())));
			}
		}
		else {
			exos = Collections.emptyList();
		}
		return new TypeRegimeFiscalFidor(regime.getCode(), regime.getPeriodeFiscaleDebutValidite(), regime.getPeriodeFiscaleFinValidite(), regime.getLibelle(), regime.isCantonal(), regime.isFederal(),
		                                 categorieFromCode(regime.getCategorieEntreprise().getCode()), exos);
	}

	protected TypeRegimeFiscalFidor(String code, Integer premierePeriodeFiscaleValidite, Integer dernierePeriodeFiscaleValidite, String libelle, boolean cantonal, boolean federal,
	                                CategorieEntreprise categorie, List<PlageExonerationFiscale> exonerations) {
		super(code, premierePeriodeFiscaleValidite, dernierePeriodeFiscaleValidite, libelle, cantonal, federal, categorie, exonerations);
	}

	@NotNull
	private static CategorieEntreprise categorieFromCode(String code) {
		switch (code) {
		case "PM":
			return CategorieEntreprise.PM;
		case "APM":
			return CategorieEntreprise.APM;
		case "SP":
			return CategorieEntreprise.SP;
		case "EN_ATTENTE_DETERMINATION":
			return CategorieEntreprise.INDET;
		default:
			return CategorieEntreprise.AUTRE;
		}
	}

	@NotNull
	private static ModeExoneration modeExonerationFromCode(String code) {
		switch (code) {
		case "EXONERATION_TOTALE":
			return ModeExoneration.TOTALE;
		case "EXONERATION_DE_FAIT":
			return ModeExoneration.DE_FAIT;
		default:
			return ModeExoneration.AUTRE;
		}
	}

	@NotNull
	private static GenreImpotExoneration genreImpotFromCode(String code) {
		switch (code) {
		case "IBC":
			return GenreImpotExoneration.IBC;
		case "ICI":
			return GenreImpotExoneration.ICI;
		case "IFONC":
			return GenreImpotExoneration.IFONC;
		default:
			return GenreImpotExoneration.AUTRE;
		}
	}
}
