package ch.vd.unireg.interfaces.civil.mock;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.uniregctb.type.TypePermis;

public class MockPermis implements Permis {

	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;
	private RegDate dateAnnulation;
	private RegDate dateValeur;
	private TypePermis typePermis;

	public MockPermis() {
	}

	public MockPermis(RegDate dateDebutValidite, RegDate dateFinValidite, RegDate dateAnnulation, @NotNull RegDate dateValeur, TypePermis typePermis) {
		this.dateDebutValidite = dateDebutValidite;
		this.dateFinValidite = dateFinValidite;
		this.dateAnnulation = dateAnnulation;
		this.dateValeur = dateValeur;
		this.typePermis = typePermis;
	}

	public MockPermis(@NotNull RegDate dateDebutValidite, RegDate dateFinValidite, RegDate dateAnnulation, TypePermis typePermis) {
		this(dateDebutValidite, dateFinValidite, dateAnnulation, dateDebutValidite, typePermis);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebutValidite;
	}

	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
		if (dateValeur == null) {
			this.dateValeur = dateDebutValidite;
		}
	}

	@Override
	public RegDate getDateFin() {
		return dateFinValidite;
	}

	public void setDateFinValidite(RegDate dateFinValidite) {
		this.dateFinValidite = dateFinValidite;
	}

	@Override
	public RegDate getDateValeur() {
		return dateValeur;
	}

	public void setDateValeur(@NotNull RegDate dateValeur) {
		this.dateValeur = dateValeur;
	}

	@Override
	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	public void setDateAnnulation(RegDate dateAnnulation) {
		this.dateAnnulation = dateAnnulation;
	}

	@Override
	public TypePermis getTypePermis() {
		return typePermis;
	}

	public void setTypePermis(TypePermis typePermis) {
		this.typePermis = typePermis;
	}

	@Override
	public String toString() {
		return "MockPermis{" +
				"dateValeur=" + dateValeur +
				", dateDebutValidite=" + dateDebutValidite +
				", dateFinValidite=" + dateFinValidite +
				", dateAnnulation=" + dateAnnulation +
				", typePermis=" + typePermis +
				'}';
	}
}
