package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.uniregctb.interfaces.model.CommuneId;

public class CommuneIdImpl implements CommuneId {

	private int noOfs;
	private int numeroTechnique;

	public CommuneIdImpl() {
	}

	public CommuneIdImpl(int noOfs, int numeroTechnique) {
		this.noOfs = noOfs;
		this.numeroTechnique = numeroTechnique;
	}

	public int getNoOfs() {
		return noOfs;
	}

	public void setNoOfs(int noOfs) {
		this.noOfs = noOfs;
	}

	public int getNumeroTechnique() {
		return numeroTechnique;
	}

	public void setNumeroTechnique(int numeroTechnique) {
		this.numeroTechnique = numeroTechnique;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final CommuneIdImpl communeId = (CommuneIdImpl) o;

		return noOfs == communeId.noOfs && numeroTechnique == communeId.numeroTechnique;
	}

	@Override
	public int hashCode() {
		int result = noOfs;
		result = 31 * result + numeroTechnique;
		return result;
	}
}
