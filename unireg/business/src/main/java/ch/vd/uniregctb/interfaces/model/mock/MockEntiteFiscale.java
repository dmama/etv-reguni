package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.EntiteFiscale;

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
	public int hashCode() {
		final int prime = 36;
		int result = 1;
		result = prime * result + code;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MockEntiteFiscale other = (MockEntiteFiscale) obj;
		return code == other.code;
	}
}
