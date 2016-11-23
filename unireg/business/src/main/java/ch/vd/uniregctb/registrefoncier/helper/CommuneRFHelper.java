package ch.vd.uniregctb.registrefoncier.helper;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.GrundstueckNummer;
import ch.vd.uniregctb.registrefoncier.CommuneRF;

public abstract class CommuneRFHelper {
	private CommuneRFHelper() {
	}

	@NotNull
	public static CommuneRF newCommuneRF(@NotNull GrundstueckNummer communeImport, @NotNull Function<String, Integer> nOfsProvider) {

		final String nom = communeImport.getGemeindenamen();
		final Integer noOfs = nOfsProvider.apply(nom);
		if (noOfs == null) {
			throw new IllegalArgumentException("Pas de numéro Ofs pour la commune [" + nom + "]");
		}

		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(communeImport.getBfsNr());
		commune.setNomRf(nom);
		commune.setNoOfs(noOfs);
		return commune;
	}
}
