package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.EntiteOFS;

public abstract class EntiteOFSImpl implements EntiteOFS, Serializable {

	private static final long serialVersionUID = 2664100976020066771L;

	private final int noOFS;
	private final String nomMajuscule;
	private final String nomMinuscule;
	private final String sigleOFS;

	protected EntiteOFSImpl(int noOFS, String nomMajuscule, String nomMinuscule, String sigleOFS) {
		this.noOFS = noOFS;
		this.nomMajuscule = nomMajuscule;
		this.nomMinuscule = nomMinuscule;
		this.sigleOFS = sigleOFS;
	}

	public EntiteOFSImpl(ch.vd.infrastructure.model.EntiteOFS target) {
		this.noOFS = target.getNoOFS();
		this.nomMajuscule = target.getNomMajuscule();
		this.nomMinuscule = target.getNomMinuscule();
		this.sigleOFS = target.getSigleOFS();
	}

	@Override
	public int getNoOFS() {
		return noOFS;
	}

	@Override
	public String getNomMajuscule() {
		return nomMajuscule;
	}

	@Override
	public String getNomMinuscule() {
		return nomMinuscule;
	}

	@Override
	public String getSigleOFS() {
		return sigleOFS;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final EntiteOFSImpl that = (EntiteOFSImpl) o;

		if (noOFS != that.noOFS) return false;
		if (nomMajuscule != null ? !nomMajuscule.equals(that.nomMajuscule) : that.nomMajuscule != null) return false;
		if (nomMinuscule != null ? !nomMinuscule.equals(that.nomMinuscule) : that.nomMinuscule != null) return false;
		if (sigleOFS != null ? !sigleOFS.equals(that.sigleOFS) : that.sigleOFS != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = noOFS;
		result = 31 * result + (nomMajuscule != null ? nomMajuscule.hashCode() : 0);
		result = 31 * result + (nomMinuscule != null ? nomMinuscule.hashCode() : 0);
		result = 31 * result + (sigleOFS != null ? sigleOFS.hashCode() : 0);
		return result;
	}
}
