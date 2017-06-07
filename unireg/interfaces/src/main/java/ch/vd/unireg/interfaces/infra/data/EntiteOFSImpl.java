package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

public abstract class EntiteOFSImpl implements EntiteOFS, Serializable {

	private static final long serialVersionUID = -7784763105975900783L;

	private final int noOFS;
	private final String nomCourt;
	private final String nomOfficiel;
	private final String sigleOFS;

	protected EntiteOFSImpl(int noOFS, String nomCourt, String nomOfficiel, @Nullable String sigleOFS) {
		this.noOFS = noOFS;
		this.nomCourt = nomCourt;
		this.nomOfficiel = nomOfficiel;
		this.sigleOFS = sigleOFS;
	}

	public EntiteOFSImpl(ch.vd.infrastructure.model.rest.EntiteOFS target) {
		this.noOFS = target.getNoOFS();
		this.nomCourt = target.getNomMinuscule();
		this.nomOfficiel = target.getNomMinuscule();
		this.sigleOFS = target.getSigleOFS();
	}

	@Override
	public int getNoOFS() {
		return noOFS;
	}

	@Override
	public String getNomCourt() {
		return nomCourt;
	}

	@Override
	public String getNomOfficiel() {
		return nomOfficiel;
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
		if (nomCourt != null ? !nomCourt.equals(that.nomCourt) : that.nomCourt != null) return false;
		if (nomOfficiel != null ? !nomOfficiel.equals(that.nomOfficiel) : that.nomOfficiel != null) return false;
		//noinspection RedundantIfStatement
		if (sigleOFS != null ? !sigleOFS.equals(that.sigleOFS) : that.sigleOFS != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = noOFS;
		result = 31 * result + (nomCourt != null ? nomCourt.hashCode() : 0);
		result = 31 * result + (nomOfficiel != null ? nomOfficiel.hashCode() : 0);
		result = 31 * result + (sigleOFS != null ? sigleOFS.hashCode() : 0);
		return result;
	}

	@Override
	public final String toString() {
		return String.format("%s{%s}", getClass().getSimpleName(), getMemberString());
	}

	protected String getMemberString() {
		return String.format("noOfs=%d, sigleOfs=%s, nomCourt=%s", noOFS, buildQuotedString(sigleOFS), buildQuotedString(nomCourt));
	}

	protected static String buildQuotedString(String str) {
		if (str == null) {
			return "null";
		}
		else {
			return String.format("'%s'", str);
		}
	}
}
