package ch.vd.unireg.common;

import java.util.Objects;

import ch.vd.registre.base.date.RegDate;

/**
 * Container pour les informations dissociées de nom/prénom/date naissance/date décès sur une personne physique
 */
public class NomPrenomDates extends NomPrenom {

	private static final long serialVersionUID = -7362455867913595648L;

	private final RegDate dateNaissance;
	private final RegDate dateDeces;

	public NomPrenomDates(String nom, String prenom, RegDate dateNaissance, RegDate dateDeces) {
		super(nom, prenom);
		this.dateNaissance = dateNaissance;
		this.dateDeces = dateDeces;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public RegDate getDateDeces() {
		return dateDeces;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		final NomPrenomDates that = (NomPrenomDates) o;
		return Objects.equals(dateNaissance, that.dateNaissance) &&
				Objects.equals(dateDeces, that.dateDeces);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), dateNaissance, dateDeces);
	}

	@Override
	public String toString() {
		return "NomPrenomDateNaissance{" +
				"nom='" + getNom() + '\'' +
				", prenom='" + getPrenom() + '\'' +
				", dateNaissance=" + dateNaissance +
				", dateDeces=" + dateDeces +
				"}";
	}
}
