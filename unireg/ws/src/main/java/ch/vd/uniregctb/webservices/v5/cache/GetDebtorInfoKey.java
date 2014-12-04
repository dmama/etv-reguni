package ch.vd.uniregctb.webservices.v5.cache;

final class GetDebtorInfoKey extends PartyCacheKey {

	public final int pf;

	GetDebtorInfoKey(long partyNo, int pf) {
		super(partyNo);
		this.pf = pf;
	}

	@Override
	protected String toStringPart() {
		return String.format(", pf=%d", pf);
	}
}
