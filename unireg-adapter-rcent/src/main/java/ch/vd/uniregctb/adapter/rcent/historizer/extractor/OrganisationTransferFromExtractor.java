package ch.vd.uniregctb.adapter.rcent.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v1.Identification;
import ch.vd.evd0022.v1.Organisation;

public class OrganisationTransferFromExtractor implements Function<Organisation, Stream<? extends BigInteger>> {

	@Override
	public Stream<BigInteger> apply(Organisation org) {
		return org.getTransferFrom().stream()
				.map(Identification::getCantonalId);
	}
}
