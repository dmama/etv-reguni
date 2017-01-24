package ch.vd.uniregctb.foncier.migration;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Clé d'identification d'un immeuble dans les données de dégrèvement exportées de SIMPA-PM.
 */
public class MigrationDDKey {

	public final long numeroEntreprise;
	public final long noAciCommune;
	public final String noBaseParcelle;
	public final String noParcelle;
	public final String noLotPPE;

	public MigrationDDKey(@NotNull MigrationDD dd) {
		this.numeroEntreprise = dd.getNumeroEntreprise();
		this.noAciCommune = dd.getNoAciCommune();
		this.noBaseParcelle = dd.getNoBaseParcelle();
		this.noParcelle = dd.getNoParcelle();
		this.noLotPPE = dd.getNoLotPPE();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final MigrationDDKey that = (MigrationDDKey) o;
		return numeroEntreprise == that.numeroEntreprise &&
				noAciCommune == that.noAciCommune &&
				Objects.equals(noBaseParcelle, that.noBaseParcelle) &&
				Objects.equals(noParcelle, that.noParcelle) &&
				Objects.equals(noLotPPE, that.noLotPPE);
	}

	@Override
	public int hashCode() {
		return Objects.hash(numeroEntreprise, noAciCommune, noBaseParcelle, noParcelle, noLotPPE);
	}
}

