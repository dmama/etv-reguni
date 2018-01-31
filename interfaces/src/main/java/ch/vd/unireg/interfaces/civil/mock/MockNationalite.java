package ch.vd.unireg.interfaces.civil.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.infra.data.Pays;

public class MockNationalite implements Nationalite {

	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;
	private Pays pays;

	public MockNationalite() {
	}

	public MockNationalite(RegDate dateDebutValidite, RegDate dateFinValidite, Pays pays) {
		this.dateDebutValidite = dateDebutValidite;
		this.dateFinValidite = dateFinValidite;
		this.pays = pays;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebutValidite;
	}

	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}

	@Override
	public RegDate getDateFin() {
		return dateFinValidite;
	}

	public void setDateFinValidite(RegDate dateFinValidite) {
		this.dateFinValidite = dateFinValidite;
	}

	@Override
	public Pays getPays() {
		return pays;
	}

	public void setPays(Pays pays) {
		this.pays = pays;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final MockNationalite that = (MockNationalite) o;

		if (dateDebutValidite != null ? !dateDebutValidite.equals(that.dateDebutValidite) : that.dateDebutValidite != null) return false;
		if (dateFinValidite != null ? !dateFinValidite.equals(that.dateFinValidite) : that.dateFinValidite != null) return false;
		if (pays != null ? !pays.equals(that.pays) : that.pays != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = dateDebutValidite != null ? dateDebutValidite.hashCode() : 0;
		result = 31 * result + (dateFinValidite != null ? dateFinValidite.hashCode() : 0);
		result = 31 * result + (pays != null ? pays.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "MockNationalite{" +
				"dateDebutValidite=" + dateDebutValidite +
				", dateFinValidite=" + dateFinValidite +
				", pays=" + pays +
				'}';
	}

	public static MockNationalite of(RegDate dateDebut, RegDate dateFin, Pays pays) {
		return new MockNationalite(dateDebut, dateFin, pays);
	}
}
