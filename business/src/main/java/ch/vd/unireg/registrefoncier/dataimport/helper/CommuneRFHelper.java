package ch.vd.unireg.registrefoncier.dataimport.helper;

import java.util.Objects;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.GrundstueckNummer;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.key.CommuneNoType;

public abstract class CommuneRFHelper {
	private CommuneRFHelper() {
	}

	public static boolean dataEquals(@NotNull CommuneRF left, @NotNull CommuneRF right) {
		// [SIFISC-30558] deux communes sont considérées égales si elles possèdent le même numéro RF *ou* le même numéro Ofs (on ne tient pas compte des numéros à zéro)
		return (left.getNoRf() > 0 && Objects.equals(left.getNoRf(), right.getNoRf()) ||
				left.getNoOfs() > 0 && Objects.equals(left.getNoOfs(), right.getNoOfs()));
	}

	@NotNull
	public static CommuneRF newCommuneRF(@NotNull GrundstueckNummer communeImport, @NotNull Function<String, Integer> nOfsProvider) {

		final String nom = communeImport.getGemeindenamen();
		final int numero = communeImport.getBfsNr();
		final CommuneNoType typeNumero = CommuneNoType.detect(numero);

		final int noRf;
		final Integer noOfs;

		switch (typeNumero) {
		case RF:
			noRf = numero;
			noOfs = nOfsProvider.apply(nom);
			if (noOfs == null) {
				throw new IllegalArgumentException("Pas de numéro Ofs pour la commune [" + nom + "]");
			}
			break;
		case OFS:
			noRf = 0;   // à partir du moment où le RF bascule sur le numéro Ofs, l'ancien numéro RF n'est plus attributé sur les nouvelles communes
			noOfs = numero;
			break;
		default:
			throw new IllegalArgumentException("Type de numéro inconnu = [" + typeNumero + "]");
		}

		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(noRf);
		commune.setNomRf(nom);
		commune.setNoOfs(noOfs);
		return commune;
	}
}
