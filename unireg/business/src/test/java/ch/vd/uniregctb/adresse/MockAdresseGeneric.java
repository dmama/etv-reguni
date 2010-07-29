package ch.vd.uniregctb.adresse;

import java.sql.Timestamp;
import java.util.Date;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;

final class MockAdresseGeneric implements AdresseGenerique {

	private static final long serialVersionUID = 1873644937399229147L;

	private final Source source;
	private final RegDate debut;
	private final RegDate fin;

	MockAdresseGeneric(RegDate debut, RegDate fin, Source source) {
		this.fin = fin;
		this.debut = debut;
		this.source = source;
	}

	public Source getSource() {
		return source;
	}

	public boolean isDefault() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, getDateDebut(), getDateFin(), NullDateBehavior.LATEST);
	}

	public String getCasePostale() {
		return null;
	}

	public RegDate getDateDebut() {
		return debut;
	}

	public RegDate getDateFin() {
		return fin;
	}

	public String getLieu() {
		return null;
	}

	public String getLocalite() {
		return null;
	}

	public String getLocaliteComplete() {
		return null;
	}

	public String getNpa() {
		return null;
	}

	public String getNumero() {
		return null;
	}

	public String getNumeroAppartement() {
		return null;
	}

	public Integer getNumeroRue() {
		return null;
	}

	public int getNumeroOrdrePostal() {
		return 0;
	}

	public String getNumeroPostal() {
		return null;
	}

	public String getNumeroPostalComplementaire() {
		return null;
	}

	public Integer getNoOfsPays() {
		return null;
	}

	public String getRue() {
		return null;
	}

	public String getComplement() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((debut == null) ? 0 : debut.hashCode());
		result = prime * result + ((fin == null) ? 0 : fin.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
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
		final MockAdresseGeneric other = (MockAdresseGeneric) obj;
		if (debut == null) {
			if (other.debut != null)
				return false;
		}
		else if (!debut.equals(other.debut))
			return false;
		if (fin == null) {
			if (other.fin != null)
				return false;
		}
		else if (!fin.equals(other.fin))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		}
		else if (!source.equals(other.source))
			return false;
		return true;
	}

	public Date getAnnulationDate() {
		return null;
	}

	public String getAnnulationUser() {
		return null;
	}

	public Date getLogCreationDate() {
		return null;
	}

	public String getLogCreationUser() {
		return null;
	}

	public Timestamp getLogModifDate() {
		return null;
	}

	public String getLogModifUser() {
		return null;
	}

	public boolean isAnnule() {
		return false;
	}

	@Override
	public String toString() {
		return DateRangeHelper.toString(this);
	}

	public Long getId() {
		return null;
	}

	public CommuneSimple getCommuneAdresse() {
		return null;
	}
}
