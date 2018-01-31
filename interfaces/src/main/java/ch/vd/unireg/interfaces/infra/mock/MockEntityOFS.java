package ch.vd.unireg.interfaces.infra.mock;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.infra.data.EntiteOFS;

public abstract class MockEntityOFS implements EntiteOFS {

	private int noOFS;
	private String nomCourt;
	private String nomOfficiel;
	private String sigleOFS;

	public MockEntityOFS() {
	}

	public MockEntityOFS(EntiteOFS entite) {
		this.noOFS = entite.getNoOFS();
		this.nomCourt = entite.getNomCourt();
		this.nomOfficiel = entite.getNomOfficiel();
		this.sigleOFS = entite.getSigleOFS();
	}

	public MockEntityOFS(int noOFS, @Nullable String sigleOFS, String nomCourt, String nomOfficiel) {
		this.noOFS = noOFS;
		this.sigleOFS = sigleOFS;
		this.nomCourt = nomCourt;
		this.nomOfficiel = nomOfficiel;
	}

	@Override
	public int getNoOFS() {
		return noOFS;
	}

	public void setNoOFS(int noOFS) {
		this.noOFS = noOFS;
	}

	@Override
	public String getNomCourt() {
		return nomCourt;
	}

	public void setNomCourt(String nomCourt) {
		this.nomCourt = nomCourt;
	}

	@Override
	public String getNomOfficiel() {
		return nomOfficiel;
	}

	public void setNomOfficiel(String nomOfficiel) {
		this.nomOfficiel = nomOfficiel;
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
