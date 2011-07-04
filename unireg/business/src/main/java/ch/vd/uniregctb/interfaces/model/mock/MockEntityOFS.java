package ch.vd.uniregctb.interfaces.model.mock;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.interfaces.model.EntiteOFS;

public abstract class MockEntityOFS implements EntiteOFS {

	private int noOFS;
	private String nomMajuscule;
	private String nomMinuscule;
	private String sigleOFS;

	public MockEntityOFS() {
	}

	public MockEntityOFS(EntiteOFS entite) {
		this.noOFS = entite.getNoOFS();
		this.nomMajuscule = entite.getNomMajuscule();
		this.nomMinuscule = entite.getNomMinuscule();
		this.sigleOFS = entite.getSigleOFS();
	}

	public MockEntityOFS(int noOFS, @Nullable String sigleOFS, String nomMinuscule) {
		this.noOFS = noOFS;
		this.sigleOFS = sigleOFS;
		this.nomMinuscule = nomMinuscule;
	}

	@Override
	public int getNoOFS() {
		return noOFS;
	}

	public void setNoOFS(int noOFS) {
		this.noOFS = noOFS;
	}

	@Override
	public String getNomMajuscule() {
		return nomMajuscule;
	}

	public void setNomMajuscule(String nomMajuscule) {
		this.nomMajuscule = nomMajuscule;
	}

	@Override
	public String getNomMinuscule() {
		return nomMinuscule;
	}

	public void setNomMinuscule(String nomMinuscule) {
		this.nomMinuscule = nomMinuscule;
	}

	@Override
	public String getSigleOFS() {
		return sigleOFS;
	}

	public void setSigleOFS(String sigleOFS) {
		this.sigleOFS = sigleOFS;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + noOFS;
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
		MockEntityOFS other = (MockEntityOFS) obj;
		return noOFS == other.noOFS;
	}
}
