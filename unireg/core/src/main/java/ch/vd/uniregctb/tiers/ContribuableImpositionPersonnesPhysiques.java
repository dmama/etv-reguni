package ch.vd.uniregctb.tiers;

import javax.persistence.CascadeType;
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
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ComparisonHelper;

@Entity
public abstract class ContribuableImpositionPersonnesPhysiques extends Contribuable {

	public static final int CTB_GEN_FIRST_ID = 10000000;

	public static final int CTB_GEN_LAST_ID = 99999999;

	private Set<SituationFamille> situationsFamille;

	public ContribuableImpositionPersonnesPhysiques() {
	}

	public ContribuableImpositionPersonnesPhysiques(long numero) {
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

	@Transient
	@Override
	public String getRoleLigne1() {
		return "Contribuable PP";
	}

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
			situations = new ArrayList<>();
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
			this.situationsFamille = new HashSet<>();
		}
		nouvelleSituationFamille.setContribuable(this);
		this.situationsFamille.add(nouvelleSituationFamille);
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

		final ContribuableImpositionPersonnesPhysiques other = (ContribuableImpositionPersonnesPhysiques) obj;
		return ComparisonHelper.areEqual(situationsFamille, other.situationsFamille);
	}

	@Override
	public void addForFiscal(ForFiscal nouveauForFiscal) {

		// les seuls fors fiscaux principaux autorisés sont de la classe "ForFiscalPrincipalPP"
		if (nouveauForFiscal.isPrincipal() && !ForFiscalPrincipalPP.class.isAssignableFrom(nouveauForFiscal.getClass())) {
			throw new IllegalArgumentException("Le for fiscal principal " + nouveauForFiscal + " n'est pas autorisé pour les contribuables dits 'PP'");
		}

		super.addForFiscal(nouveauForFiscal);
	}

	@Transient
	@Override
	public ForFiscalPrincipalPP getPremierForFiscalPrincipal() {
		return (ForFiscalPrincipalPP) super.getPremierForFiscalPrincipal();
	}

	@Transient
	@Override
	public ForFiscalPrincipalPP getDernierForFiscalPrincipal() {
		return (ForFiscalPrincipalPP) super.getDernierForFiscalPrincipal();
	}

	@Transient
	@Override
	public ForFiscalPrincipalPP getDernierForFiscalPrincipalAvant(@Nullable RegDate date) {
		return (ForFiscalPrincipalPP) super.getDernierForFiscalPrincipalAvant(date);
	}

	@Transient
	@Override
	public ForFiscalPrincipalPP getDernierForFiscalPrincipalVaudois() {
		return (ForFiscalPrincipalPP) super.getDernierForFiscalPrincipalVaudois();
	}

	@Transient
	@Override
	public ForFiscalPrincipalPP getDernierForFiscalPrincipalVaudoisAvant(RegDate date) {
		return (ForFiscalPrincipalPP) super.getDernierForFiscalPrincipalVaudoisAvant(date);
	}

	@Transient
	@Override
	public ForFiscalPrincipalPP getForFiscalPrincipalAt(@Nullable RegDate date) {
		return (ForFiscalPrincipalPP) super.getForFiscalPrincipalAt(date);
	}

	@Transient
	@Override
	public List<ForFiscalPrincipalPP> getForsFiscauxPrincipauxActifsSorted() {
		return (List<ForFiscalPrincipalPP>) super.getForsFiscauxPrincipauxActifsSorted();
	}
}
