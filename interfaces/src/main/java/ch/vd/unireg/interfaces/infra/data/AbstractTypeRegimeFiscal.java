package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.type.CategorieEntreprise;

/**
 * Données simples d'un type de régime fiscal.
 */
public abstract class AbstractTypeRegimeFiscal implements TypeRegimeFiscal, Serializable {

	private static final long serialVersionUID = -3971126478803725637L;

	private final String code;
	private final String libelle;
	private final boolean cantonal;
	private final boolean federal;
	private final CategorieEntreprise categorie;
	private final Integer premierePeriodeFiscaleValidite;
	private final Integer dernierePeriodeFiscaleValidite;
	private final Map<GenreImpotExoneration, List<PlageExonerationFiscale>> exonerations;

	protected AbstractTypeRegimeFiscal(String code, Integer premierePeriodeFiscaleValidite, Integer dernierePeriodeFiscaleValidite, String libelle, boolean cantonal, boolean federal,
	                                   CategorieEntreprise categorie, List<PlageExonerationFiscale> exonerations) {
		this.code = code;
		this.libelle = libelle;
		this.cantonal = cantonal;
		this.federal = federal;
		this.categorie = categorie;
		this.premierePeriodeFiscaleValidite = premierePeriodeFiscaleValidite;
		this.dernierePeriodeFiscaleValidite = dernierePeriodeFiscaleValidite;

		if (exonerations != null && !exonerations.isEmpty()) {
			this.exonerations = exonerations.stream()
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(PlageExonerationFiscale::getGenreImpot,
					                          Collections::singletonList,
					                          ListUtils::union,
					                          () -> new EnumMap<>(GenreImpotExoneration.class)));
		}
		else {
			this.exonerations = Collections.emptyMap();
		}
	}


	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getLibelle() {
		return libelle;
	}

	@Override
	public String getLibelleAvecCode() {
		return String.format("%s - %s", code, libelle);
	}

	@Override
	public boolean isCantonal() {
		return cantonal;
	}

	@Override
	public boolean isFederal() {
		return federal;
	}

	@Override
	public CategorieEntreprise getCategorie() {
		return categorie;
	}

	@Override
	public boolean isIndetermine() {
		return categorie == CategorieEntreprise.INDET;
	}

	@Override
	public boolean isSocieteDePersonnes() {
		return categorie == CategorieEntreprise.SP;
	}

	@Override
	@Nullable
	public PlageExonerationFiscale getExonerationIBC(int periode) {
		return getExonerationFiscalePourPeriodeEtGenreImpot(GenreImpotExoneration.IBC, periode);
	}

	@Override
	@Nullable
	public PlageExonerationFiscale getExonerationICI(int periode) {
		return getExonerationFiscalePourPeriodeEtGenreImpot(GenreImpotExoneration.ICI, periode);
	}

	@Override
	@Nullable
	public PlageExonerationFiscale getExonerationIFONC(int periode) {
		return getExonerationFiscalePourPeriodeEtGenreImpot(GenreImpotExoneration.IFONC, periode);
	}

	@Override
	@NotNull
	public List<PlageExonerationFiscale> getExonerations(GenreImpotExoneration genreImpot) {
		return Optional.ofNullable(genreImpot)
				.map(exonerations::get)
				.map(Collections::unmodifiableList)
				.orElseGet(Collections::emptyList);
	}

	@Nullable
	private PlageExonerationFiscale getExonerationFiscalePourPeriodeEtGenreImpot(@NotNull GenreImpotExoneration genreImpot, int periode) {
		final List<PlageExonerationFiscale> plages = getExonerations(genreImpot);
		return plages.stream()
				.filter(plage -> plage.isDansPlage(periode))
				.findFirst()
				.orElse(null);
	}

	@Override
	public Integer getPremierePeriodeFiscaleValidite() {
		return premierePeriodeFiscaleValidite;
	}

	@Nullable
	@Override
	public Integer getDernierePeriodeFiscaleValidite() {
		return dernierePeriodeFiscaleValidite;
	}

	@Override
	public String toString() {
		final StringRenderer<PlageExonerationFiscale> plageRenderer =
				plage -> String.format("%d-%s (%s)",
				                       plage.getPeriodeDebut(),
				                       plage.getPeriodeFin() != null ? plage.getPeriodeFin() : "?",
				                       plage.getMode());

		return getClass().getSimpleName() + "{" +
				"code='" + code + '\'' +
				", libelle='" + libelle + '\'' +
				", cantonal=" + cantonal +
				", federal=" + federal +
				", catégorie=" + categorie +
				", premierePeriodeFiscaleValidite=" + premierePeriodeFiscaleValidite +
				", dernierePeriodeFiscaleValidite=" + dernierePeriodeFiscaleValidite +
				", exonerationsIBC=[" + CollectionsUtils.toString(exonerations.get(GenreImpotExoneration.IBC), plageRenderer, ", ", StringUtils.EMPTY) + "]" +
				", exonerationsICI=[" + CollectionsUtils.toString(exonerations.get(GenreImpotExoneration.ICI), plageRenderer, ", ", StringUtils.EMPTY) + "]" +
				", exonerationsIFONC=[" + CollectionsUtils.toString(exonerations.get(GenreImpotExoneration.IFONC), plageRenderer, ", ", StringUtils.EMPTY) + "]" +
				'}';
	}
}
