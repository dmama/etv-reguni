package ch.vd.uniregctb.situationfamille;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.interfaces.InterfaceDataException;
import ch.vd.uniregctb.type.EtatCivil;

/**
 * Permet d'adapter une situation de famille de personne physique en provenance du civil.
 */
public class VueSituationFamillePersonnePhysiqueCivilAdapter implements VueSituationFamillePersonnePhysique {

	private final EtatCivil etatCivil;
	private final RegDate dateFin;
	private final RegDate dateDebut;

	public VueSituationFamillePersonnePhysiqueCivilAdapter(ch.vd.unireg.interfaces.civil.data.EtatCivil etatCourant,
			ch.vd.unireg.interfaces.civil.data.EtatCivil etatSuivant, boolean nullAllowed) {
		this.etatCivil = EtatCivilHelper.civil2core(etatCourant.getTypeEtatCivil());
		this.dateDebut = etatCourant.getDateDebut();
		if (etatSuivant == null) {
			this.dateFin =null;
		}
		else {
			final RegDate dateDebutSuivant = etatSuivant.getDateDebut();
			if (dateDebutSuivant == null) {
				if (nullAllowed)
					this.dateFin = null;
				else {
					throw new InterfaceDataException("L'état civil [" + etatCivil.name() + "] commençant le " + dateDebut
						+ " est suivi par l'état civil [" + EtatCivilHelper.civil2core(etatSuivant.getTypeEtatCivil())
						+ "] qui possède une date de début nulle !");
				}
			}
			else {
				this.dateFin = dateDebutSuivant.getOneDayBefore();
			}
		}
	}

	public VueSituationFamillePersonnePhysiqueCivilAdapter(ch.vd.unireg.interfaces.civil.data.EtatCivil etatCourant, RegDate dateDebut, RegDate dateFin) {
		this.etatCivil = EtatCivilHelper.civil2core(etatCourant.getTypeEtatCivil());
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	public VueSituationFamillePersonnePhysiqueCivilAdapter(EtatCivil etatCivil, RegDate dateDebut, RegDate dateFin) {
		this.etatCivil = etatCivil;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	@Override
	public Long getId() {
		return null; // par définition
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public Integer getNombreEnfants() {
		return null;
	}

	@Override
	public Source getSource() {
		return Source.CIVILE;
	}

	@Override
	public EtatCivil getEtatCivil() {
		return etatCivil;
	}

	@Override
	public boolean isAnnule() {
		return false; // par définition
	}

	@Override
	public Date getAnnulationDate() {
		return null; // par définition
	}
}
