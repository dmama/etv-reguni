package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.type.MotifFor;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 *
 * @author jec
 *
 * @uml.annotations derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8i1x9Edygsbnw9h5bVw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8i1x9Edygsbnw9h5bVw"
 */
@Entity
public abstract class Contribuable extends Tiers {

	public static final int CTB_GEN_FIRST_ID = 10000000;

	public static final int CTB_GEN_LAST_ID = 99999999;

	/**
	 *
	 */
	private static final long serialVersionUID = -3641798749343787983L;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_9KImIuxIEdycMumkNMs2uQ"
	 */
	private Set<SituationFamille> situationsFamille;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3qxWkVjJEd2uSoZKEkgcsw"
	 */
	private Set<MouvementDossier> mouvementsDossier;

	private RegDate dateLimiteExclusionEnvoiDeclarationImpot;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the situationsFamille
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_9KImIuxIEdycMumkNMs2uQ?GETTER"
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "CTB_ID", nullable = false)
	@ForeignKey(name = "FK_SF_CTB_ID")
	public Set<SituationFamille> getSituationsFamille() {
		// begin-user-code
		return situationsFamille;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theSituationsFamille
	 *            the situationsFamille to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_9KImIuxIEdycMumkNMs2uQ?SETTER"
	 */
	public void setSituationsFamille(Set<SituationFamille> theSituationsFamille) {
		// begin-user-code
		situationsFamille = theSituationsFamille;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the mouvementDossier
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3qxWkVjJEd2uSoZKEkgcsw?GETTER"
	 */
	@OneToMany(mappedBy = "contribuable", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_MOV_DOS_CTB_ID")
	public Set<MouvementDossier> getMouvementsDossier() {
		// begin-user-code
		return mouvementsDossier;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theMouvementDossier
	 *            the mouvementDossier to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3qxWkVjJEd2uSoZKEkgcsw?SETTER"
	 */
	public void setMouvementsDossier(Set<MouvementDossier> theMouvementDossier) {
		// begin-user-code
		mouvementsDossier = theMouvementDossier;
		// end-user-code
	}

	/**
	 * @return la date jusqu'à laquelle les tâches d'envoi de déclarations d'impôts ne doivent pas être traitées par les <b>batches</b>; ou
	 *         <i>null</i> si les tâches peuvent être traitées sans restriction.
	 */
	@Column(name = "DATE_LIMITE_EXCLUSION", nullable = true)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateLimiteExclusionEnvoiDeclarationImpot() {
		return dateLimiteExclusionEnvoiDeclarationImpot;
	}

	public void setDateLimiteExclusionEnvoiDeclarationImpot(RegDate dateLimiteExclusionEnvoiDeclarationImpot) {
		this.dateLimiteExclusionEnvoiDeclarationImpot = dateLimiteExclusionEnvoiDeclarationImpot;
	}

	// ***********************************************
	@Transient
	public SituationFamille getSituationFamilleActive() {
		return getSituationFamilleAt(null);
	}

	@Transient
	public SituationFamille getSituationFamilleAt(RegDate date) {

		if (situationsFamille == null) {
			return null;
		}

		for (SituationFamille situation : situationsFamille) {
			if (situation.isValidAt(date)) {
				return situation;
			}
		}

		return null;
	}

	/**
	 * Retourne les situations de famille non-annulées triées par - La date d'ouverture
	 *
	 * @return
	 */
	@Transient
	public List<SituationFamille> getSituationsFamilleSorted() {
		List<SituationFamille> situations = null;
		if (situationsFamille != null) {
			situations = new ArrayList<SituationFamille>();
			for (SituationFamille situation : situationsFamille) {
				if (!situation.isAnnule())
					situations.add(situation);
			}
			Collections.sort(situations, new DateRangeComparator<SituationFamille>());
		}
		return situations;
	}

	@Transient
	public ForFiscalPrincipal getPremierForFiscalPrincipal() {
		List<ForFiscal> list = getForsFiscauxSorted();
		if (list != null) {
			for (ForFiscal forFiscal : list) {
				if (!forFiscal.isAnnule() && forFiscal.isPrincipal()) {
					return (ForFiscalPrincipal) forFiscal;
				}
			}
		}
		return null;
	}

	// ***********************************************
	@Transient
	public void closeSituationFamilleActive(RegDate dateFin) {
		final SituationFamille situation = getSituationFamilleActive();
		if (situation != null) {
			if (situation.getDateDebut() != null && situation.getDateDebut().isAfter(dateFin)) {
				situation.setAnnule(true);
			}
			else {
				situation.setDateFin(dateFin);
			}
		}
	}

	/**
	 * Ajoute une situation de famille
	 *
	 * @param nouvelleSituationFamille
	 *            la situation de famille à ajouter
	 */
	public void addSituationFamille(SituationFamille nouvelleSituationFamille) {
		if (this.situationsFamille == null) {
			this.situationsFamille = new HashSet<SituationFamille>();
		}
		nouvelleSituationFamille.setContribuable(this);
		this.situationsFamille.add(nouvelleSituationFamille);
	}

	/**
	 * Ajoute un mouvement de dossier
	 *
	 * @param mouvementDossier
	 *            le mouvement de dossier à ajouter
	 */
	public void addMouvementDossier(MouvementDossier nouveauMouvementDossier) {
		if (this.mouvementsDossier == null) {
			this.mouvementsDossier = new HashSet<MouvementDossier>();
		}
		nouveauMouvementDossier.setContribuable(this);
		this.mouvementsDossier.add(nouveauMouvementDossier);
	}

	/**
	 * Liste de fors fiscaux spécialisée de manière à ne stocker que les fors débutant le plus tôt (= date de début).
	 * <p>
	 * Dans la majorité des cas, cette liste ne contiendra qu'un seul for. Ce n'est que dans le cas où plusieurs fors débutent en même temps
	 * qu'elle en contiendra plusieurs.
	 */
	protected static class FirstForsList extends ArrayList<ForFiscal> {

		private static final long serialVersionUID = 5767109372815011124L;

		/**
		 * Teste un for comme candidat à l'insertion dans la liste des fors. Un candidat est accepté dans la liste si sa date de début est
		 * plus petite ou égale à celle des fors existants dans la liste. Dans le cas où sa date de début est plus petite, les fors
		 * existants sont enlevés de la liste.
		 *
		 * @return <b>true</b> si le for a été inséré dans la liste, <b>false</b> autrement.
		 */
		public boolean checkFor(ForFiscal candidat) {
			final int s = size();
			if (s == 0) {
				// pas de fors dans la liste -> on ajoute le nouveau
				return add(candidat);
			}
			else {
				final RegDate dateDebut = get(0).getDateDebut(); // tous les fors dans la liste sont sensés posséder la même date de début
				final RegDate candidatDateDebut = candidat.getDateDebut();

				if (dateDebut == null) {
					if (candidatDateDebut == null) {
						// le nouveau for débute en même temps que les fors stockés dans la liste -> on l'ajoute
						return add(candidat);
					}
					else {
						// le nouveau for est après les fors stockés dans la liste -> on l'ignore
						return false;
					}
				}
				else {
					if (candidatDateDebut == null || candidatDateDebut.isBefore(dateDebut)) {
						// le nouveau for est avant les fors stockés dans la liste -> on vide la liste et ajoute le for
						clear();
						return add(candidat);
					}
					else if (candidatDateDebut.isAfter(dateDebut)) {
						// le nouveau for est après les fors stockés dans la liste -> on l'ignore
						return false;
					}
					else {
						// le nouveau for débute en même temps que les fors stockés dans la liste -> on l'ajoute
						return add(candidat);
					}
				}
			}
		}

		/**
		 * @return le premier for fiscal dont le numéro Ofs est celui spécifié en paramètre.
		 */
		public ForFiscal findForWithNumeroOfs(Integer noOfs) {
			Assert.notNull(noOfs);
			for (ForFiscal f : this) {
				if (noOfs.equals(f.getNumeroOfsAutoriteFiscale())) {
					return f;
				}
			}
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ValidationResults validate() {
		final ValidationResults results = super.validate();
		if (!isAnnule()) {
			results.merge(validateSituationsFamille());
		}
		return results;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ValidationResults validateFors() {

		ValidationResults results = super.validateFors();

		final ForsParType fors = getForsParType(true /* triés par ordre chronologique */);

		// Les plages de validité des fors principaux ne doivent pas se chevaucher
		ForFiscalPrincipal lastFor = null;
		for (ForFiscalPrincipal ffp : fors.principaux) {
			if (lastFor != null && DateRangeHelper.intersect(lastFor, ffp)) {
				results.addError(String.format("Le for principal qui commence le %s chevauche le for précédent", RegDateHelper.dateToDisplayString(ffp.getDateDebut())));
			}
			lastFor = ffp;
		}

		// Pour chaque for secondaire il doit exister un for principal valide
		for (ForFiscalSecondaire fs : fors.secondaires) {
			if (!existForPrincipal(fors.principaux, fs.getDateDebut(), fs.getDateFin())) {
				String msg = String.format("Il n'y a pas de for principal pour accompagner le for secondaire qui commence le %s", RegDateHelper.dateToDisplayString(fs.getDateDebut()));
				if (fs.getDateFin() != null) {
					msg += String.format(" et se termine le %s", RegDateHelper.dateToDisplayString(fs.getDateFin()));
				}
				results.addError(msg);
			}
		}

		// Pour chaque for autre élément imposable il doit exister un for principal valide
		for (ForFiscalAutreElementImposable fs : fors.autreElementImpot) {
			if (!existForPrincipal(fors.principaux, fs.getDateDebut(), fs.getDateFin())) {
				String msg = String.format("Il n'y a pas de for principal pour accompagner le for autre élément imposable qui commence le %s", RegDateHelper.dateToDisplayString(fs.getDateDebut()));
				if (fs.getDateFin() != null) {
					msg += String.format(" et se termine le %s", RegDateHelper.dateToDisplayString(fs.getDateFin()));
				}
				results.addError(msg);
			}
		}

		// Les for DPI ne sont pas autorisés
		for (ForDebiteurPrestationImposable fpdi : fors.dpis) {
			results.addError("Le for " + fpdi + " n'est pas un type de for autorisé sur un contribuable.");
		}

		return results;
	}

	private ValidationResults validateSituationsFamille() {

		final ValidationResults results = new ValidationResults();

		final List<SituationFamille> situationsSorted = getSituationsFamilleSorted();
		if (situationsSorted != null) {

			// On valide toutes les situations de familles pour elles-mêmes
			for (SituationFamille s : situationsSorted) {
				results.merge(s.validate());
			}

			// Les plages de validité des situations de familles ne doivent pas se chevaucher
			SituationFamille lastSituation = null;
			for (SituationFamille situation : situationsSorted) {
				if (lastSituation != null && DateRangeHelper.intersect(lastSituation, situation)) {
					results.addError(String.format("La situation de famille qui commence le %s chevauche la situation précédente", RegDateHelper.dateToDisplayString(situation.getDateDebut())));
				}
				lastSituation = situation;
			}
		}

		return results;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equalsTo(Tiers obj) {
		if (this == obj)
			return true;
		if (!super.equalsTo(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Contribuable other = (Contribuable) obj;
		if (situationsFamille == null) {
			if (other.situationsFamille != null)
				return false;
		}
		else if (!situationsFamille.equals(other.situationsFamille))
			return false;
		return true;
	}

	@Override
	@Transient
	protected boolean isDesactiveSelonFors(RegDate date) {
		// pour un contribuable, on dira qu'il est désactivé à une date donnée s'il n'y a pas de for
		// principal actif à la date donnée et que le dernier for fiscal principal a été fermé
		// pour un motif "ANNULATION"

		final boolean desactive;
		final ForFiscalPrincipal ffpCourant = getForFiscalPrincipalAt(date);
		if (ffpCourant == null) {
			final ForFiscalPrincipal ffpPrecedent = getDernierForFiscalPrincipalAvant(date);
			desactive = ffpPrecedent != null && ffpPrecedent.getMotifFermeture() == MotifFor.ANNULATION;
		}
		else {
			desactive = false;
		}
		return desactive;
	}

	@Override
	@Transient
	public RegDate getDateDesactivation() {
		final RegDate date;
		final ForFiscalPrincipal ffpCourant = getForFiscalPrincipalAt(null);
		if (ffpCourant == null) {
			final ForFiscalPrincipal dernier = getDernierForFiscalPrincipal();
			date = dernier != null && dernier.getMotifFermeture() == MotifFor.ANNULATION ? dernier.getDateFin() : null;
		}
		else {
			date = null;
		}
		return date;
	}
}
