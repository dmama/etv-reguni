package ch.vd.unireg.interfaces.upi.data;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.type.Sexe;

public class UpiPersonInfo implements Serializable {

	private static final long serialVersionUID = 8304041485535145494L;

	private final String noAvs13;
	private final String prenoms;
	private final String nom;
	private final Sexe sexe;
	private final RegDate dateNaissance;
	private final RegDate dateDeces;
	private final Nationalite nationalite;
	private final NomPrenom nomPrenomMere;
	private final NomPrenom nomPrenomPere;

	public UpiPersonInfo(String noAvs13, String prenoms, String nom, Sexe sexe, RegDate dateNaissance, @Nullable RegDate dateDeces, Nationalite nationalite, @Nullable NomPrenom nomPrenomMere, @Nullable NomPrenom nomPrenomPere) {
		this.noAvs13 = noAvs13;
		this.prenoms = prenoms;
		this.nom = nom;
		this.sexe = sexe;
		this.dateNaissance = dateNaissance;
		this.dateDeces = dateDeces;
		this.nationalite = nationalite;
		this.nomPrenomMere = nomPrenomMere;
		this.nomPrenomPere = nomPrenomPere;
	}

	public String getNoAvs13() {
		return noAvs13;
	}

	public Sexe getSexe() {
		return sexe;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public String getPrenoms() {
		return prenoms;
	}

	public String getNom() {
		return nom;
	}

	@Nullable
	public RegDate getDateDeces() {
		return dateDeces;
	}

	public Nationalite getNationalite() {
		return nationalite;
	}

	@Nullable
	public NomPrenom getNomPrenomMere() {
		return nomPrenomMere;
	}

	@Nullable
	public NomPrenom getNomPrenomPere() {
		return nomPrenomPere;
	}
}
