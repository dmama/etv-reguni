package ch.vd.uniregctb.foncier.migration;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Clé d'identification d'un immeuble dans les données de dégrèvement exportées de SIMPA-PM.
 */
public class MigrationKey {

	public final long numeroEntreprise;
	public final long noAciCommune;
	public final String nomCommune;
	public final String noBaseParcelle;
	public final String noParcelle;
	public final String noLotPPE;

	public MigrationKey(@NotNull BaseMigrationData data) {
		this.numeroEntreprise = data.getNumeroEntreprise();
		this.noAciCommune = data.getNoAciCommune();
		this.nomCommune = data.getNomCommune();
		this.noBaseParcelle = data.getNoBaseParcelle();
		this.noParcelle = data.getNoParcelle();
		this.noLotPPE = data.getNoLotPPE();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final MigrationKey that = (MigrationKey) o;
		return numeroEntreprise == that.numeroEntreprise &&
				noAciCommune == that.noAciCommune &&
				Objects.equals(nomCommune, that.nomCommune) &&
				Objects.equals(noBaseParcelle, that.noBaseParcelle) &&
				Objects.equals(noParcelle, that.noParcelle) &&
				Objects.equals(noLotPPE, that.noLotPPE);
	}

	@Override
	public int hashCode() {
		return Objects.hash(numeroEntreprise, noAciCommune, nomCommune, noBaseParcelle, noParcelle, noLotPPE);
	}

	@Override
	public String toString() {
		return "{" +
				"numeroEntreprise=" + numeroEntreprise +
				", noAciCommune=" + noAciCommune +
				", nomCommune='" + nomCommune + '\'' +
				", noBaseParcelle='" + noBaseParcelle + '\'' +
				", noParcelle='" + noParcelle + '\'' +
				", noLotPPE='" + noLotPPE + '\'' +
				'}';
	}
}

