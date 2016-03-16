package ch.vd.uniregctb.migration.pm.engine.data;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.engine.AbstractEntityMigrator;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCanton;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForPrincipal;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Classe qui contient la donnée d'une commune Suisse ou d'un pays, de manière exclusive
 */
public final class CommuneOuPays {

	private final TypeAutoriteFiscale typeAutoriteFiscale;
	private final Integer numeroOfsAutoriteFiscale;

	public CommuneOuPays(Integer noOfs, TypeAutoriteFiscale typeAutoriteFiscale) {
		this.numeroOfsAutoriteFiscale = noOfs;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	public CommuneOuPays(@NotNull Supplier<RegpmCommune> communeSupplier, @NotNull Supplier<Integer> noOfsPaysSupplier) {
		final RegpmCommune commune = communeSupplier.get();
		if (commune != null) {
			this.numeroOfsAutoriteFiscale = AbstractEntityMigrator.NO_OFS_COMMUNE_EXTRACTOR.apply(commune);
			this.typeAutoriteFiscale = commune.getCanton() == RegpmCanton.VD ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
		}
		else {
			this.numeroOfsAutoriteFiscale = noOfsPaysSupplier.get();
			this.typeAutoriteFiscale = TypeAutoriteFiscale.PAYS_HS;
		}
	}

	public CommuneOuPays(@NotNull RegpmForPrincipal ffp) {
		this(ffp::getCommune, ffp::getOfsPays);
	}

	/**
	 * @return le type d'autorité fiscale représentée par l'entité
	 */
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	/**
	 * @return le numéro OFS de la commune ou du pays (voir {@link #getTypeAutoriteFiscale()})
	 */
	public Integer getNumeroOfsAutoriteFiscale() {
		return numeroOfsAutoriteFiscale;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final CommuneOuPays that = (CommuneOuPays) o;

		if (typeAutoriteFiscale != that.typeAutoriteFiscale) return false;
		return numeroOfsAutoriteFiscale != null ? numeroOfsAutoriteFiscale.equals(that.numeroOfsAutoriteFiscale) : that.numeroOfsAutoriteFiscale == null;

	}

	@Override
	public int hashCode() {
		int result = typeAutoriteFiscale != null ? typeAutoriteFiscale.hashCode() : 0;
		result = 31 * result + (numeroOfsAutoriteFiscale != null ? numeroOfsAutoriteFiscale.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return String.format("%s/%d", getTypeAutoriteFiscale(), getNumeroOfsAutoriteFiscale());
	}
}
