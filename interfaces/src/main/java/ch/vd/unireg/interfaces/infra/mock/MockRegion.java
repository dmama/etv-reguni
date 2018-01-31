package ch.vd.unireg.interfaces.infra.mock;

import ch.vd.unireg.interfaces.infra.data.Region;

public class MockRegion extends MockEntiteFiscale implements Region {

	public static final MockRegion Lausanne = new MockRegion(1, "Lausanne");
	public static final MockRegion Nyon = new MockRegion(2, "Nyon");
	public static final MockRegion Vevey = new MockRegion(3, "Vevey");
	public static final MockRegion Yverdon = new MockRegion(4, "Yverdon");

	public MockRegion() {
		super();
	}

	public MockRegion(Integer code, String designation) {
		super(code, designation);
	}
}
