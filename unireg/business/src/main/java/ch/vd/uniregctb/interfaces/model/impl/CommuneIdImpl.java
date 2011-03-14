package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.uniregctb.interfaces.model.CommuneId;

public class CommuneIdImpl implements CommuneId {

	private long noOfs;
	private int numeroTechnique;

	public CommuneIdImpl() {
	}

	public CommuneIdImpl(long noOfs, int numeroTechnique) {
		this.noOfs = noOfs;
		this.numeroTechnique = numeroTechnique;
	}

	public long getNoOfs() {
		return noOfs;
	}

	public void setNoOfs(long noOfs) {
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
		int result = (int) (noOfs ^ (noOfs >>> 32));
		result = 31 * result + numeroTechnique;
		return result;
	}
}
