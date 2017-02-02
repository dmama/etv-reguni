package ch.vd.uniregctb.foncier.migration;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identifie le numéro de parcelle (+ les indexes en cas de PPE) d'un immeuble issu de SIMPA-PM.
 */
public class MigrationParcelle {

	private int noParcelle;
	@Nullable
	private Integer index1;
	@Nullable
	private Integer index2;
	@Nullable
	private Integer index3;

	public MigrationParcelle(@NotNull String baseParcelle, @Nullable String parcelle, @Nullable String lotPPE) {
		// [SIFISC-23111] nouvelle règle de transcription
		// - si le numéro de parcelle est vide, on prend le numéro de parcelle de base et on découpe pour extraire les indexes
		// - si le numéro de parcelle n'est pas vide, il est pris tel quel (sans indexes)
		// - le numéro de lot PPE n'est donc jamais utilisé !

		if (StringUtils.isBlank(parcelle)) {
			// si le numéro de parcelle est renseigné, c'est toujours lui qui prime sur le numéro de base
			parcelle = baseParcelle;
		}
		if (parcelle.contains("-")) {
			// le numéro de parcelle contient les indexes des lots PPE, on les parse
			final String[] tokens = parcelle.split("-");
			noParcelle = Integer.parseInt(tokens[0]);
			index1 = Integer.parseInt(tokens[1]);
			index2 = tokens.length > 2 ? Integer.parseInt(tokens[2]) : null;
			index3 = tokens.length > 3 ? Integer.parseInt(tokens[3]) : null;
		}
		else {
			noParcelle = Integer.parseInt(parcelle);
			index1 = null;
			index2 = null;
			index3 = null;
		}
	}

	public int getNoParcelle() {
		return noParcelle;
	}

	@Nullable
	public Integer getIndex1() {
		return index1;
	}

	@Nullable
	public Integer getIndex2() {
		return index2;
	}

	@Nullable
	public Integer getIndex3() {
		return index3;
	}

	@Override
	public String toString() {
		return noParcelle + "/" + index1 + "/" + index2 + "/" + index3;
	}
}
