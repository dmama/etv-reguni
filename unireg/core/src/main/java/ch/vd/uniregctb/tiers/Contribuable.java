package ch.vd.uniregctb.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.type.MotifFor;

@Entity
public abstract class Contribuable extends Tiers {

	public static final int CTB_GEN_FIRST_ID = 10000000;

	public static final int CTB_GEN_LAST_ID = 99999999;

	private Set<SituationFamille> situationsFamille;
	private Set<MouvementDossier> mouvementsDossier;
	private Set<Immeuble> immeubles;

	private RegDate dateLimiteExclusionEnvoiDeclarationImpot;

	public Contribuable() {
	}

	public Contribuable(long numero) {
		super(numero);
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "CTB_ID", nullable = false)
	@ForeignKey(name = "FK_SF_CTB_ID")
	public Set<SituationFamille> getSituationsFamille() {
		return situationsFamille;
	}

	public void setSituationsFamille(Set<SituationFamille> theSituationsFamille) {
		situationsFamille = theSituationsFamille;
	}

	@OneToMany(mappedBy = "contribuable", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_MOV_DOS_CTB_ID")
	public Set<MouvementDossier> getMouvementsDossier() {
		return mouvementsDossier;
	}

	public void setMouvementsDossier(Set<MouvementDossier> theMouvementDossier) {
		mouvementsDossier = theMouvementDossier;
	}

	@OneToMany(mappedBy = "contribuable", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_IMM_CTB_ID")
	public Set<Immeuble> getImmeubles() {
		return immeubles;
	}

	public void setImmeubles(Set<Immeuble> immeubles) {
		this.immeubles = immeubles;
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
	public SituationFamille getSituationFamilleAt(@Nullable RegDate date) {

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
	 * @return les situations de famille non-annulées triées par - La date d'ouverture
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
	 * @param nouveauMouvementDossier
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
	 * Ajoute un immeuble au contribuable.
	 *
	 * @param immeuble l'immeuble à ajouter
	 */
	public void addImmeuble(Immeuble immeuble) {
		if (immeubles == null) {
			this.immeubles = new HashSet<Immeuble>();
		}
		immeuble.setContribuable(this);
		this.immeubles.add(immeuble);
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
		@SuppressWarnings("SimplifiableIfStatement")
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
	protected boolean isDesactiveSelonFors(@Nullable RegDate date) {
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
