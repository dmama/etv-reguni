package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.uniregctb.interfaces.model.EntiteOFS;

public abstract class EntiteOFSWrapper implements EntiteOFS {

	private final ch.vd.infrastructure.model.EntiteOFS target;

	public EntiteOFSWrapper(ch.vd.infrastructure.model.EntiteOFS target) {
		this.target = target;
	}

	public int getNoOFS() {
		return target.getNoOFS();
	}

	public String getNomMajuscule() {
		return target.getNomMajuscule();
	}

	public String getNomMinuscule() {
		return target.getNomMinuscule();
	}

	public String getSigleOFS() {
		return target.getSigleOFS();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + target.getNoOFS();
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
		EntiteOFSWrapper other = (EntiteOFSWrapper) obj;
		return target.getNoOFS() == other.target.getNoOFS();
	}
}
