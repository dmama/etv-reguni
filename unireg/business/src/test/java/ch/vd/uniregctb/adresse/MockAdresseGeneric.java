package ch.vd.uniregctb.adresse;

import java.sql.Timestamp;
import java.util.Date;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CasePostale;

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

	@Override
	public Source getSource() {
		return source;
	}

	@Override
	public boolean isDefault() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, getDateDebut(), getDateFin(), NullDateBehavior.LATEST);
	}

	@Override
	public CasePostale getCasePostale() {
		return null;
	}

	@Override
	public RegDate getDateDebut() {
		return debut;
	}

	@Override
	public RegDate getDateFin() {
		return fin;
	}

	public String getLieu() {
		return null;
	}

	@Override
	public String getLocalite() {
		return null;
	}

	@Override
	public String getLocaliteComplete() {
		return null;
	}

	public String getNpa() {
		return null;
	}

	@Override
	public String getNumero() {
		return null;
	}

	@Override
	public String getNumeroAppartement() {
		return null;
	}

	@Override
	public Integer getNumeroRue() {
		return null;
	}

	@Override
	public int getNumeroOrdrePostal() {
		return 0;
	}

	@Override
	public String getNumeroPostal() {
		return null;
	}

	@Override
	public String getNumeroPostalComplementaire() {
		return null;
	}

	@Override
	public Integer getNoOfsPays() {
		return null;
	}

	@Override
	public String getRue() {
		return null;
	}

	@Override
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
		else if (source != other.source)
			return false;
		return true;
	}

	@Override
	public Date getAnnulationDate() {
		return null;
	}

	@Override
	public String getAnnulationUser() {
		return null;
	}

	@Override
	public Date getLogCreationDate() {
		return null;
	}

	@Override
	public String getLogCreationUser() {
		return null;
	}

	@Override
	public Timestamp getLogModifDate() {
		return null;
	}

	@Override
	public String getLogModifUser() {
		return null;
	}

	@Override
	public boolean isAnnule() {
		return false;
	}

	@Override
	public String toString() {
		return DateRangeHelper.toString(this);
	}

	@Override
	public Long getId() {
		return null;
	}

	@Override
	public boolean isPermanente() {
		return false;
	}

	@Nullable
	@Override
	public Integer getNoOfsCommuneAdresse() {
		return null;
	}

	@Override
	public Integer getEgid() {
		return null;
	}

	@Override
	public Integer getEwid() {
		return null;
	}
}
