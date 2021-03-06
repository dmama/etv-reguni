package ch.vd.unireg.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.ComparisonHelper;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationAvecCodeControle;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;

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
	@JoinColumn(name = "CTB_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_SF_CTB_ID"))
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
		if (situationsFamille == null || situationsFamille.isEmpty()) {
			return null;
		}
		return situationsFamille.stream()
				.filter(situ -> situ.isValidAt(date))
				.findFirst()
				.orElse(null);
	}

	/**
	 * @return les situations de famille non-annulées triées par - La date d'ouverture
	 */
	@NotNull
	@Transient
	public List<SituationFamille> getSituationsFamilleSorted() {
		return CollectionsUtils.unmodifiableNeverNull(situationsFamille).stream()
				.filter(AnnulableHelper::nonAnnule)
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toList());
	}

	// ***********************************************
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
	public synchronized void addDeclaration(Declaration declaration) {
		if (declaration instanceof DeclarationImpotOrdinairePP) {
			final int pf = declaration.getPeriode().getAnnee();
			if (pf >= DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE) {
				// [SIFISC-1368] Les déclaration d'impôt ordinaires possèdent un code contrôle (un pour chaque pair contribuable/période fiscale) qui doit
				// être générée/assigné au moment de l'insertion
				final DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) declaration;

				if (di.getCodeControle() == null) {
					// pour les PP, on reprend le même code de contrôle pour toutes les DI de l'année
					final Set<Declaration> declarationsExistantes = getDeclarations();
					String codeControleUtilise = null;
					if (declarationsExistantes != null) {
						for (Declaration existante : declarationsExistantes) {
							if (existante.getPeriode().getAnnee() == pf && existante instanceof DeclarationImpotOrdinairePP) {
								codeControleUtilise = ((DeclarationImpotOrdinairePP) existante).getCodeControle();
								if (codeControleUtilise != null) {
									break;
								}
							}
						}
					}

					// aucun code déjà utilisé sur cette PF pour le moment, il faut en générer un nouveau
					if (codeControleUtilise == null) {
						codeControleUtilise = DeclarationAvecCodeControle.generateCodeControle();
						if (declarationsExistantes != null) {
							for (Declaration existante : declarationsExistantes) {
								// on profite pour assigner le code de contrôle généré à toutes les déclarations préexistantes de la période (= rattrapage de données)
								if (existante.getPeriode().getAnnee() == pf && existante instanceof DeclarationImpotOrdinairePP) {
									((DeclarationImpotOrdinairePP) existante).setCodeControle(codeControleUtilise);
								}
							}
						}
					}

					// assignation du code de contrôle
					di.setCodeControle(codeControleUtilise);
				}
			}
		}
		super.addDeclaration(declaration);
	}

	public DeclarationImpotOrdinairePP getDeclarationActiveAt(RegDate date) {
		final List<DeclarationImpotOrdinairePP> declarations = getDeclarationsTriees(DeclarationImpotOrdinairePP.class, false);
		if (declarations.isEmpty()) {
			return null;
		}
		return DateRangeHelper.rangeAt(declarations, date);
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

	@NotNull
	@Transient
	@Override
	public List<ForFiscalPrincipalPP> getForsFiscauxPrincipauxActifsSorted() {
		return (List<ForFiscalPrincipalPP>) super.getForsFiscauxPrincipauxActifsSorted();
	}
}
