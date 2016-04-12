package ch.vd.uniregctb.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.ComparisonHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscal;
import ch.vd.uniregctb.type.GroupeTypesDocumentBatchLocal;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Entreprise connue du registre des personnes morales de l'ACI
 */
@Entity
@DiscriminatorValue("Entreprise")
public class Entreprise extends ContribuableImpositionPersonnesMorales {

	// Numéros migrés depuis SIMPA-PM puis générés pour les Entreprises
	public static final int FIRST_ID = 1;
	public static final int LAST_ID = 999999;

	/**
	 * Numéro cantonal (= dans RCEnt)
	 */
	private Long numeroEntreprise;

	private Set<RegimeFiscal> regimesFiscaux;
	private Set<DonneeCivileEntreprise> donneesCiviles;
	private Set<AllegementFiscal> allegementsFiscaux;
	private Set<Bouclement> bouclements;
	private Set<EtatEntreprise> etats;
	private Set<FlagEntreprise> flags;
	private RegDate dateDebutPremierExerciceCommercial;
	private Set<AutreDocumentFiscal> autresDocumentsFiscaux;

	@Column(name = "NUMERO_ENTREPRISE")
	@Index(name = "IDX_TIERS_NO_ENTREPRISE")
	public Long getNumeroEntreprise() {
		return numeroEntreprise;
	}

	public void setNumeroEntreprise(Long numeroEntreprise) {
		this.numeroEntreprise = numeroEntreprise;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "ENTREPRISE_ID")
	public Set<RegimeFiscal> getRegimesFiscaux() {
		return regimesFiscaux;
	}

	public void setRegimesFiscaux(Set<RegimeFiscal> regimesFiscaux) {
		this.regimesFiscaux = regimesFiscaux;
	}

	public void addRegimeFiscal(RegimeFiscal regimeFiscal) {
		if (regimeFiscal.getEntreprise() != null && regimeFiscal.getEntreprise() != this) {
			throw new IllegalArgumentException("Ce régime fiscal a déjà été associé à une autre entreprise");
		}

		if (this.regimesFiscaux == null) {
			this.regimesFiscaux = new HashSet<>();
		}
		this.regimesFiscaux.add(regimeFiscal);
		regimeFiscal.setEntreprise(this);
	}

	@Transient
	@NotNull
	public List<RegimeFiscal> getRegimesFiscauxNonAnnulesTries() {
		final List<RegimeFiscal> nonAnnules = AnnulableHelper.sansElementsAnnules(regimesFiscaux);
		Collections.sort(nonAnnules, new DateRangeComparator<RegimeFiscal>() {
			@Override
			public int compare(RegimeFiscal o1, RegimeFiscal o2) {
				int comparison = super.compare(o1, o2);
				if (comparison == 0) {
					comparison = o1.getPortee().compareTo(o2.getPortee());
				}
				return comparison;
			}
		});
		return nonAnnules;
	}

	@Transient
	@NotNull
	public List<RegimeFiscal> getRegimesFiscauxNonAnnulesTries(RegimeFiscal.Portee portee) {
		final List<RegimeFiscal> all = getRegimesFiscauxNonAnnulesTries();
		final List<RegimeFiscal> pourPortee = new ArrayList<>(all.size());
		for (RegimeFiscal rf : all) {
			if (rf.getPortee() == portee) {
				pourPortee.add(rf);
			}
		}
		return pourPortee;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "ENTREPRISE_ID")
	public Set<DonneeCivileEntreprise> getDonneesCiviles() {
		return donneesCiviles;
	}

	public void setDonneesCiviles(Set<DonneeCivileEntreprise> donneesCiviles) {
		this.donneesCiviles = donneesCiviles;
	}

	public void addDonneeCivile(DonneeCivileEntreprise donnee) {
		if (donnee.getEntreprise() != null && donnee.getEntreprise() != this) {
			throw new IllegalArgumentException("Ces données ont déjà été associées à une autre enteprise");
		}

		if (this.donneesCiviles == null) {
			this.donneesCiviles = new HashSet<>();
		}
		this.donneesCiviles.add(donnee);
		donnee.setEntreprise(this);
	}

	@NotNull
	private <T extends DonneeCivileEntreprise> List<T> extractDonneesCiviles(Class<T> clazz, boolean avecAnnulees, boolean triees) {
		final Set<DonneeCivileEntreprise> donneesCiviles = getDonneesCiviles();
		final List<T> list;
		if (donneesCiviles != null && !donneesCiviles.isEmpty()) {
			list = new ArrayList<>(donneesCiviles.size());
			for (DonneeCivileEntreprise dce : donneesCiviles) {
				if (clazz.isAssignableFrom(dce.getClass())) {
					//noinspection unchecked
					final T donnee = (T) dce;
					if (avecAnnulees || !donnee.isAnnule()) {
						list.add(donnee);
					}
				}
			}
			if (!list.isEmpty() && triees) {
				Collections.sort(list, new DateRangeComparator<>());
			}
		}
		else {
			list = Collections.emptyList();
		}
		return list;
	}

	@Transient
	@NotNull
	public List<RaisonSocialeFiscaleEntreprise> getRaisonsSocialesNonAnnuleesTriees() {
		return extractDonneesCiviles(RaisonSocialeFiscaleEntreprise.class, false, true);
	}

	@Transient
	@NotNull
	public List<RaisonSocialeFiscaleEntreprise> getRaisonsSocialesTriees() {
		return extractDonneesCiviles(RaisonSocialeFiscaleEntreprise.class, true, true);
	}

	@Transient
	@NotNull
	public List<FormeJuridiqueFiscaleEntreprise> getFormesJuridiquesNonAnnuleesTriees() {
		return extractDonneesCiviles(FormeJuridiqueFiscaleEntreprise.class, false, true);
	}

	@Transient
	@NotNull
	public List<FormeJuridiqueFiscaleEntreprise> getFormesJuridiquesTriees() {
		return extractDonneesCiviles(FormeJuridiqueFiscaleEntreprise.class, true, true);
	}

	@Transient
	@NotNull
	public List<CapitalFiscalEntreprise> getCapitauxNonAnnulesTries() {
		return extractDonneesCiviles(CapitalFiscalEntreprise.class, false, true);
	}

	@Transient
	@NotNull
	public List<CapitalFiscalEntreprise> getCapitauxTries() {
		return extractDonneesCiviles(CapitalFiscalEntreprise.class, true, true);
	}

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "ENTREPRISE_ID")
	public Set<AllegementFiscal> getAllegementsFiscaux() {
		return allegementsFiscaux;
	}

	public void setAllegementsFiscaux(Set<AllegementFiscal> allegementsFiscaux) {
		this.allegementsFiscaux = allegementsFiscaux;
	}

	public void addAllegementFiscal(AllegementFiscal af) {
		if (af.getEntreprise() != null && af.getEntreprise() != this) {
			throw new IllegalArgumentException("Cet allègement fiscal est déjà associé à une autre entreprise");
		}

		if (this.allegementsFiscaux == null) {
			this.allegementsFiscaux = new HashSet<>();
		}
		this.allegementsFiscaux.add(af);
		af.setEntreprise(this);
	}

	@Transient
	@NotNull
	public List<AllegementFiscal> getAllegementsFiscauxNonAnnulesTries() {
		final List<AllegementFiscal> nonAnnules = AnnulableHelper.sansElementsAnnules(allegementsFiscaux);
		Collections.sort(nonAnnules, new DateRangeComparator<AllegementFiscal>() {
			@Override
			public int compare(AllegementFiscal o1, AllegementFiscal o2) {
				int comparison = super.compare(o1, o2);
				if (comparison == 0) {
					comparison = o1.getTypeImpot().compareTo(o2.getTypeImpot());
					if (comparison == 0) {
						comparison = o1.getTypeCollectivite().compareTo(o2.getTypeCollectivite());
						if (comparison == 0 && o1.getTypeCollectivite() == AllegementFiscal.TypeCollectivite.COMMUNE) {
							final Integer ofsCommune1 = ((AllegementFiscalCommune) o1).getNoOfsCommune();
							final Integer ofsCommune2 = ((AllegementFiscalCommune) o2).getNoOfsCommune();
							if (ofsCommune1 == null && ofsCommune2 != null) {
								comparison = -1;
							}
							else if (ofsCommune1 != null) {
								comparison = (ofsCommune2 == null ? 1 : Integer.compare(ofsCommune1, ofsCommune2));
							}
						}
					}
				}
				return comparison;
			}
		});
		return nonAnnules;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "ENTREPRISE_ID")
	public Set<Bouclement> getBouclements() {
		return bouclements;
	}

	public void setBouclements(Set<Bouclement> bouclements) {
		this.bouclements = bouclements;
	}

	public void addBouclement(Bouclement bouclement) {
		if (bouclement.getEntreprise() != null && bouclement.getEntreprise() != this) {
			throw new IllegalArgumentException("Ce bouclement est déjà associé à une autre entreprise");
		}

		if (this.bouclements == null) {
			this.bouclements = new HashSet<>();
		}
		this.bouclements.add(bouclement);
		bouclement.setEntreprise(this);
	}

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "ENTREPRISE_ID")
	public Set<EtatEntreprise> getEtats() {
		return etats;
	}

	public void addEtat(EtatEntreprise etat) {
		if (etat.getEntreprise() != null && etat.getEntreprise() != this) {
			throw new IllegalArgumentException("Cet état est déjà associé à une autre entreprise.");
		}

		if (this.etats == null) {
			this.etats = new LinkedHashSet<>();         // pour garder l'ordre d'insertion dans la base
		}
		this.etats.add(etat);
		etat.setEntreprise(this);
	}

	@Transient
	public List<EtatEntreprise> getEtatsNonAnnulesTries() {
		final List<EtatEntreprise> nonAnnules = AnnulableHelper.sansElementsAnnules(etats);
		Collections.sort(nonAnnules);
		return nonAnnules;
	}

	@Transient
	public EtatEntreprise getEtatActuel() {
		final List<EtatEntreprise> nonAnnules = getEtatsNonAnnulesTries();
		if (!nonAnnules.isEmpty()) {
			return CollectionsUtils.getLastElement(nonAnnules);
		}
		return null;
	}

	public void setEtats(Set<EtatEntreprise> etats) {
		this.etats = etats;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "ENTREPRISE_ID")
	public Set<FlagEntreprise> getFlags() {
		return flags;
	}

	@Transient
	public List<FlagEntreprise> getFlagsNonAnnulesTries() {
		final List<FlagEntreprise> nonAnnules = AnnulableHelper.sansElementsAnnules(flags);
		Collections.sort(nonAnnules, new DateRangeComparator<>());
		return nonAnnules;
	}

	public void setFlags(Set<FlagEntreprise> flags) {
		this.flags = flags;
	}

	public void addFlag(FlagEntreprise flag) {
		if (flag.getEntreprise() != null && flag.getEntreprise() != this) {
			throw new IllegalArgumentException("Ce flag est déjà associé à une autre entreprise.");
		}

		if (this.flags == null) {
			this.flags = new HashSet<>();
		}
		this.flags.add(flag);
		flag.setEntreprise(this);
	}

	@Column(name = "DATE_DEBUT_PREMIER_EXERCICE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebutPremierExerciceCommercial() {
		return dateDebutPremierExerciceCommercial;
	}

	public void setDateDebutPremierExerciceCommercial(RegDate dateDebutPremierExerciceCommercial) {
		this.dateDebutPremierExerciceCommercial = dateDebutPremierExerciceCommercial;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "ENTREPRISE_ID")
	public Set<AutreDocumentFiscal> getAutresDocumentsFiscaux() {
		return autresDocumentsFiscaux;
	}

	public void setAutresDocumentsFiscaux(Set<AutreDocumentFiscal> autresDocumentsFiscaux) {
		this.autresDocumentsFiscaux = autresDocumentsFiscaux;
	}

	public void addAutreDocumentFiscal(AutreDocumentFiscal document) {
		if (document.getEntreprise() != null && document.getEntreprise() != this) {
			throw new IllegalArgumentException("Ce document est déjà associé à une autre entreprise.");
		}

		if (this.autresDocumentsFiscaux == null) {
			this.autresDocumentsFiscaux = new HashSet<>();
		}
		this.autresDocumentsFiscaux.add(document);
		document.setEntreprise(this);
	}

	@NotNull
	@Transient
	public <T extends AutreDocumentFiscal> List<T> getAutresDocumentsFiscaux(Class<T> clazz, boolean sorted, boolean avecAnnules) {
		if (this.autresDocumentsFiscaux == null || this.autresDocumentsFiscaux.isEmpty()) {
			return Collections.emptyList();
		}

		final List<T> liste = new ArrayList<>(this.autresDocumentsFiscaux.size());
		for (AutreDocumentFiscal adf : this.autresDocumentsFiscaux) {
			if (adf != null && clazz.isAssignableFrom(adf.getClass())) {
				if (avecAnnules || !adf.isAnnule()) {
					//noinspection unchecked
					liste.add((T) adf);
				}
			}
		}
		if (sorted && liste.size() > 1) {
			Collections.sort(liste, new Comparator<T>() {
				@Override
				public int compare(T o1, T o2) {
					return NullDateBehavior.EARLIEST.compare(o1.getDateEnvoi(), o2.getDateEnvoi());
				}
			});
		}
		return liste;
	}

	public Entreprise() {
	}

	public Entreprise(long numero) {
		super(numero);
	}

	@Transient
	@Override
	public NatureTiers getNatureTiers() {
		return NatureTiers.Entreprise;
	}

	@Transient
	@Override
	public TypeTiers getType() {
		return TypeTiers.ENTREPRISE;
	}

	@Override
	public boolean equalsTo(Tiers obj) {
		if (this == obj)
			return true;
		if (!super.equalsTo(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;

		final Entreprise other = (Entreprise) obj;
		return ComparisonHelper.areEqual(numeroEntreprise, other.numeroEntreprise);
	}

	@Transient
	public boolean isConnueAuCivil() {
		return numeroEntreprise != null;
	}

	@Override
	public boolean shouldAssignCodeControle(DeclarationImpotOrdinairePM di) {
		if (super.shouldAssignCodeControle(di)) {
			// [SIFISC-17952] seulement pour les PM non-HC à la fin de la période d'imposition
			if (di.getModeleDocument() != null && GroupeTypesDocumentBatchLocal.DI_PM.hasType(di.getModeleDocument().getTypeDocument())) {
				// ok, nous avons une PM (par opposition à APM), voyons maintenant le for principal
				final ForFiscalPrincipal ffp = getDernierForFiscalPrincipalAvant(di.getDateFinExerciceCommercial());
				if (ffp != null && ffp.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_HC) {
					// si le for fiscal est VD ou HS, on génère un code de contrôle
					return true;
				}
			}
		}
		return false;
	}
}
