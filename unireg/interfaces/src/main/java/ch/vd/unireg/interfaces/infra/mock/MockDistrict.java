package ch.vd.unireg.interfaces.infra.mock;

import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.Region;

public class MockDistrict extends MockEntiteFiscale implements District {

	private Region region;
	public static final MockDistrict Aigle = new MockDistrict(1, "Aigle", MockRegion.Vevey);
	public static final MockDistrict Echallens = new MockDistrict(2, "Echallens", MockRegion.Yverdon);
	public static final MockDistrict Grandson = new MockDistrict(3, "Grandson", MockRegion.Yverdon);
	public static final MockDistrict Lausanne = new MockDistrict(4, "Lausanne", MockRegion.Lausanne);
	public static final MockDistrict LaVallee = new MockDistrict(5, "La Vallée", MockRegion.Yverdon);
	public static final MockDistrict Lavaux = new MockDistrict(6, "Lavaux", MockRegion.Vevey);
	public static final MockDistrict Morges = new MockDistrict(7, "Morges", MockRegion.Nyon);
	public static final MockDistrict Moudon = new MockDistrict(8, "Moudon", MockRegion.Yverdon);
	public static final MockDistrict Nyon = new MockDistrict(9, "Nyon", MockRegion.Nyon);
	public static final MockDistrict Orbe = new MockDistrict(10, "Orbe", MockRegion.Yverdon);
	public static final MockDistrict Payerne = new MockDistrict(11, "Payerne", MockRegion.Yverdon);
	public static final MockDistrict PaysDenHaut = new MockDistrict(12, "Pays d'Enhaut", MockRegion.Vevey);
	public static final MockDistrict RolleAubonne = new MockDistrict(13, "Rolle-Aubonne", MockRegion.Nyon);
	public static final MockDistrict Vevey = new MockDistrict(14, "Vevey", MockRegion.Vevey);
	public static final MockDistrict Yverdon = new MockDistrict(15, "Yverdon", MockRegion.Yverdon);

	@Override
	public Region getRegion() {
		return region;
	}

	public MockDistrict(Region region) {
		this.region = region;
	}

	public MockDistrict(Integer code, String designation, Region region) {
		super(code, designation);
		this.region = region;
	}
}
