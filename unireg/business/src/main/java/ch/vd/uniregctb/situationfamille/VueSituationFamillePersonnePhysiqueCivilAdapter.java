package ch.vd.uniregctb.situationfamille;

import java.util.Date;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.InterfaceDataException;
import ch.vd.uniregctb.type.EtatCivil;

/**
 * Permet d'adapter une situation de famille de personne physique en provenance du civil.
 */
public class VueSituationFamillePersonnePhysiqueCivilAdapter implements VueSituationFamillePersonnePhysique {

	private final EtatCivil etatCivil;
	private final RegDate dateFin;
	private final RegDate dateDebut;

	public VueSituationFamillePersonnePhysiqueCivilAdapter(ch.vd.uniregctb.interfaces.model.EtatCivil etatCourant,
			ch.vd.uniregctb.interfaces.model.EtatCivil etatSuivant, boolean nullAllowed) {
		this.etatCivil = EtatCivil.from(etatCourant.getTypeEtatCivil());
		this.dateDebut = etatCourant.getDateDebutValidite();
		if (etatSuivant == null) {
			this.dateFin =null;
		}
		else {
			final RegDate dateDebutSuivant = etatSuivant.getDateDebutValidite();
			if (dateDebutSuivant == null) {
				if (nullAllowed)
					this.dateFin = null;
				else {
				throw new InterfaceDataException("L'état civil [" + etatCivil.name() + "] commençant le " + dateDebut
						+ " est suivi par l'état civil [" + EtatCivil.from(etatSuivant.getTypeEtatCivil())
						+ "] qui possède une date de début nulle !");
				}
			}
			else {
				this.dateFin = dateDebutSuivant.getOneDayBefore();
			}
		}
	}

	public VueSituationFamillePersonnePhysiqueCivilAdapter(ch.vd.uniregctb.interfaces.model.EtatCivil etatCourant, RegDate dateDebut, RegDate dateFin) {
		this.etatCivil = EtatCivil.from(etatCourant.getTypeEtatCivil());
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	public VueSituationFamillePersonnePhysiqueCivilAdapter(EtatCivil etatCivil, RegDate dateDebut, RegDate dateFin) {
		this.etatCivil = etatCivil;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	public Long getId() {
		return null; // par définition
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public Integer getNombreEnfants() {
		return null;
	}

	public Source getSource() {
		return Source.CIVILE;
	}

	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	public EtatCivil getEtatCivil() {
		return etatCivil;
	}

	public boolean isAnnule() {
		return false; // par définition
	}

	public Date getAnnulationDate() {
		return null; // par définition
	}
}
