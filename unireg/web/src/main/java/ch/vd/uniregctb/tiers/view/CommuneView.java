package ch.vd.uniregctb.tiers.view;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Commune;

/**
 * Vue d'une commune qui a un numéro OFS et un nom
 */
public class CommuneView {

	private final int noOfs;
	private final String nom;
	@Nullable
	private final RegDate dateDebut;
	@Nullable
	private final RegDate dateFin;

	public CommuneView(int noOfs, String nom) {
		this.noOfs = noOfs;
		this.nom = nom;
		this.dateDebut = null;
		this.dateFin = null;
	}

	public CommuneView(int noOfs, String nom, @Nullable RegDate dateDebut, @Nullable RegDate dateFin) {
		this.noOfs = noOfs;
		this.nom = nom;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	public CommuneView(@NotNull Commune commune) {
		this.noOfs = commune.getNoOFS();
		this.nom = commune.getNomOfficiel();
		this.dateDebut = commune.getDateDebutValidite();
		this.dateFin = commune.getDateFinValidite();
	}

	public int getNoOfs() {
		return noOfs;
	}

	public String getNom() {
		return nom;
	}

	public String getNomEtValidite() {
		if (dateDebut == null && dateFin == null) {
			// commune éternellement valide : on retourne juste son nom
			return nom;
		}
		else {
			String validite = "";
			if (dateDebut != null) {
				validite = "depuis le " + RegDateHelper.dateToDisplayString(dateDebut);
			}
			if (dateDebut != null && dateFin != null) {
				validite += " ";
			}
			if (dateFin != null) {
				validite += "jusqu'au " + RegDateHelper.dateToDisplayString(dateFin);
			}
			return nom + " (valide " + validite + ")";
		}
	}

	/**
	 * @return la date de début de validité de la commune
	 */
	@Nullable
	public RegDate getDateDebut() {
		return dateDebut;
	}

	/**
	 * @return la date de fin de validité de la commune
	 */
	@Nullable
	public RegDate getDateFin() {
		return dateFin;
	}
}
