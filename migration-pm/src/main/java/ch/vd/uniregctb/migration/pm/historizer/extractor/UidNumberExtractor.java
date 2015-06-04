package ch.vd.uniregctb.migration.pm.historizer.extractor;

import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v1.Identifier;

public class UidNumberExtractor implements Function<List<Identifier>, String> {

	/**
	 * catégorie (ou clé) de l'identifiant
	 */
	private static final String CH_UID_KEY = "CH.IDE";

	@Nullable
	@Override
	public String apply(List<Identifier> identifiers) {
		return identifiers.stream()
				.filter(i -> CH_UID_KEY.equals(i.getIdentifierCategory()))
				.findAny()
				.map(Identifier::getIdentifierValue)
				.orElse(null);
	}
}
