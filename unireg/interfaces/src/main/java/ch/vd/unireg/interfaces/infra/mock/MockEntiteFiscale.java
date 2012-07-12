package ch.vd.unireg.interfaces.infra.mock;

import ch.vd.unireg.interfaces.infra.data.EntiteFiscale;

public abstract class MockEntiteFiscale implements EntiteFiscale {
	private Integer code;
	private String designation;

	@Override
	public Integer getCode() {
		return code;
	}

	@Override
	public String getDesignation() {
		return designation;
	}

	public MockEntiteFiscale() {
	}

	public MockEntiteFiscale(Integer code, String designation) {
		this.code = code;
		this.designation = designation;
	}


	public MockEntiteFiscale(EntiteFiscale entite) {
		this.code = entite.getCode();
		this.designation = entite.getDesignation();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final MockEntiteFiscale that = (MockEntiteFiscale) o;
		return code == null ? that.code == null : code.equals(that.code);
	}

	@Override
	public int hashCode() {
		return code == null ? 0 : code.hashCode();
	}
}
