package ch.vd.uniregctb.migration.pm.historizer.extractor.organization;

import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v1.Identifier;

public class NumeroIdeExtractor implements Function<List<Identifier>, String> {

	@Nullable
	@Override
	public String apply(List<Identifier> identifiers) {
		return identifiers.stream()
				.filter(i -> "CH.IDE".equals(i.getIdentifierCategory()))
				.findAny()
				.map(Identifier::getIdentifierValue)
				.orElse(null);
	}
}
