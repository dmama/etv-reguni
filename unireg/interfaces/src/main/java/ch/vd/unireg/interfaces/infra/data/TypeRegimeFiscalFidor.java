package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.fidor.xml.regimefiscal.v2.Exoneration;
import ch.vd.fidor.xml.regimefiscal.v2.RegimeFiscal;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.type.CategorieEntreprise;

public class TypeRegimeFiscalFidor implements TypeRegimeFiscal, Serializable {

	private static final long serialVersionUID = -7161144696098610744L;

	private final String code;
	private final String libelle;
	private final boolean cantonal;
	private final boolean federal;
	private final CategorieEntreprise categorie;
	private final Integer premierePeriodeFiscaleValidite;
	private final Integer dernierePeriodeFiscaleValidite;
	private final List<PlageExonerationFiscales> exonerationsIBC;
	private final List<PlageExonerationFiscales> exonerationsICI;
	private final List<PlageExonerationFiscales> exonerationsIFONC;

	public static TypeRegimeFiscal get(RegimeFiscal regime) {
		if (regime == null) {
			return null;
		}
		final List<PlageExonerationFiscales> exos;
		final List<Exoneration> exonerations = regime.getExoneration();
		if (exonerations != null && !exonerations.isEmpty()) {
			exos = new ArrayList<>(exonerations.size());
			for (final Exoneration exo : exonerations) {
				exos.add(new PlageExonerationFiscales(exo.getPeriodeFiscaleDebutValidite(), exo.getPeriodeFiscaleFinValidite(), GenreImpot.fromCode(exo.getGenreImpot().getCode()),
				                                      ModeExoneration.fromCode(exo.getMode().value())));
			}
		}
		else {
			exos = Collections.emptyList();
		}
		return new TypeRegimeFiscalFidor(regime.getCode(), regime.getPeriodeFiscaleDebutValidite(), regime.getPeriodeFiscaleFinValidite(), regime.getLibelle(), regime.isCantonal(), regime.isFederal(),
		                                 categorieFromCode(regime.getCategorieEntreprise().getCode()), exos);
	}

	protected TypeRegimeFiscalFidor(String code, Integer premierePeriodeFiscaleValidite, Integer dernierePeriodeFiscaleValidite, String libelle, boolean cantonal, boolean federal,
	                                CategorieEntreprise categorie, List<PlageExonerationFiscales> exonerations) {
		this.code = code;
		this.libelle = libelle;
		this.cantonal = cantonal;
		this.federal = federal;
		this.categorie = categorie;
		this.premierePeriodeFiscaleValidite = premierePeriodeFiscaleValidite;
		this.dernierePeriodeFiscaleValidite = dernierePeriodeFiscaleValidite;

		if (exonerations == null || exonerations.size() == 0) {
			exonerationsIBC = Collections.emptyList();
			exonerationsICI = Collections.emptyList();
			exonerationsIFONC = Collections.emptyList();
		}
		else {
			exonerationsIBC = new ArrayList<>();
			exonerationsICI = new ArrayList<>();
			exonerationsIFONC = new ArrayList<>();
			exonerations.stream()
					.filter(Objects::nonNull)
					.forEach(plage -> {
						switch (plage.getGenreImpot()) {
						case IBC:
							exonerationsIBC.add(plage);
							break;
						case ICI:
							exonerationsICI.add(plage);
							break;
						case IFONC:
							exonerationsIFONC.add(plage);
							break;
						case AUTRE:
							break;
						}});
		}
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
	public boolean isExoneration(int periodeFiscale) {
		return exonerationsIBC.stream()
				.anyMatch(exo -> exo.isDansPlage(periodeFiscale));
	}

	@Override
	public boolean isExonerationIBC(int periodeFiscale) {
		return exonerationsIBC.stream()
				.anyMatch(exo -> exo.isDansPlage(periodeFiscale));
	}

	@Override
	public boolean isExonerationICI(int periodeFiscale) {
		return exonerationsICI.stream()
				.anyMatch(exo -> exo.isDansPlage(periodeFiscale));
	}

	@Override
	public boolean isExonerationIFONC(int periodeFiscale) {
		return exonerationsIFONC.stream()
				.anyMatch(exo -> exo.isDansPlage(periodeFiscale));
	}

	@Override
	public List<PlageExonerationFiscales> getExonerationsIBC() {
		return Collections.unmodifiableList(exonerationsIBC);
	}

	@Override
	public List<PlageExonerationFiscales> getExonerationsICI() {
		return Collections.unmodifiableList(exonerationsICI);
	}

	@Override
	public List<PlageExonerationFiscales> getExonerationsIFONC() {
		return Collections.unmodifiableList(exonerationsIFONC);
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
		return "TypeRegimeFiscalFidor{" +
				"code='" + code + '\'' +
				", libelle='" + libelle + '\'' +
				", cantonal=" + cantonal +
				", federal=" + federal +
				", catégorie=" + categorie +
				", premierePeriodeFiscaleValidite=" + premierePeriodeFiscaleValidite +
				", dernierePeriodeFiscaleValidite=" + dernierePeriodeFiscaleValidite +
				", exonerationsIBC=[" + CollectionsUtils.toString(exonerationsIBC,
				                                               exo -> String.format("%d-%s", exo.getPeriodeDebut(), exo.getPeriodeFin() == null ? "?" : exo.getPeriodeFin()),
				                                               ", ",
				                                               StringUtils.EMPTY) + "]" +
				", exonerationsICI=[" + CollectionsUtils.toString(exonerationsICI,
				                                               exo -> String.format("%d-%s", exo.getPeriodeDebut(), exo.getPeriodeFin() == null ? "?" : exo.getPeriodeFin()),
				                                               ", ",
				                                               StringUtils.EMPTY) + "]" +
				", exonerationsIFONC=[" + CollectionsUtils.toString(exonerationsIFONC,
				                                               exo -> String.format("%d-%s", exo.getPeriodeDebut(), exo.getPeriodeFin() == null ? "?" : exo.getPeriodeFin()),
				                                               ", ",
				                                               StringUtils.EMPTY) + "]" +
				'}';
	}
}
