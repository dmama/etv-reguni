package ch.vd.unireg.registrefoncier.dataimport.helper;

import java.util.Objects;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.GrundstueckNummer;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.registrefoncier.key.CommuneNoType;

public abstract class SituationRFHelper {

	private SituationRFHelper() {
	}

	public static boolean dataEquals(@NotNull SituationRF situation, @NotNull GrundstueckNummer grundstueckNummer) {
		return dataEquals(situation, newSituationRF(grundstueckNummer, SituationRFHelper::simplisticCommuneProvider));
	}

	/**
	 * Provider de commune simplifié au maximum pour retourner une commune avec juste le noRf de renseigné de manière à permettre le test d'égalité.
	 */
	@Nullable
	private static CommuneRF simplisticCommuneProvider(@Nullable Integer numero) {
		if (numero == null) {
			return null;
		}

		final CommuneNoType typeNumero = CommuneNoType.detect(numero);

		final CommuneRF c = new CommuneRF();
		switch (typeNumero) {
		case RF:
			c.setNoRf(numero);
			break;
		case OFS:
			c.setNoOfs(numero);
			break;
		default:
			throw new IllegalArgumentException("Type de numéro inconnu = [" + typeNumero + "]");
		}

		return c;
	}

	public static boolean dataEquals(@NotNull SituationRF left, @NotNull SituationRF right) {
		return CommuneRFHelper.dataEquals(left.getCommune(), right.getCommune()) &&
				left.getNoParcelle() == right.getNoParcelle() &&
				Objects.equals(left.getIndex1(), right.getIndex1()) &&
				Objects.equals(left.getIndex2(), right.getIndex2()) &&
				Objects.equals(left.getIndex3(), right.getIndex3());
	}

	@NotNull
	public static SituationRF newSituationRF(@NotNull GrundstueckNummer grundstueckNummer,
	                                         @NotNull Function<Integer, CommuneRF> communeProvider) {
		final SituationRF situation = new SituationRF();
		situation.setCommune(communeProvider.apply(grundstueckNummer.getBfsNr()));
		situation.setNoParcelle(grundstueckNummer.getStammNr());
		situation.setIndex1(grundstueckNummer.getIndexNr1());
		situation.setIndex2(grundstueckNummer.getIndexNr2());
		situation.setIndex3(grundstueckNummer.getIndexNr3());
		return situation;
	}
}
