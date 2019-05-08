package ch.vd.unireg.tiers;

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
import java.util.stream.Collectors;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.ComparisonHelper;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscal;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.GroupeFlagsEntreprise;

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

	/**
	 * Texte libre permettant d'indiquer le secteur d'activité dans lequel opère l'entreprise (Demandé par l'IDE pour l'annonce).
	 */
	private String secteurActivite;

	/**
	 * Flag signalant qu'il faut réévaluer l'envoi éventuel d'une annonce de l'entreprise au registre IDE.
	 */
	private boolean ideDirty;

	/**
	 * Flag permettant de désactiver l'envoi d'annonce au registre IDE pour cette entreprise
	 */
	private boolean ideDesactive;

	@Column(name = "NUMERO_ENTREPRISE")
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

	private static final Comparator<RegimeFiscal.Portee> PORTEE_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());

	private static final Comparator<RegimeFiscal> REGIME_FISCAL_COMPARATOR = new DateRangeComparator<RegimeFiscal>() {
		@Override
		public int compare(RegimeFiscal o1, RegimeFiscal o2) {
			int comparison = super.compare(o1, o2);
			if (comparison == 0) {
				// [SIFISC-20655] Dans SuperGRA, il est tout-à-fait possible d'avoir des données invalides à trier, y compris donc des régimes fiscaux sans portée...
				comparison = PORTEE_COMPARATOR.compare(o1.getPortee(), o2.getPortee());
			}
			return comparison;
		}
	};

	@Transient
	@NotNull
	public List<RegimeFiscal> getRegimesFiscauxNonAnnulesTries() {
		final List<RegimeFiscal> nonAnnules = AnnulableHelper.sansElementsAnnules(regimesFiscaux);
		nonAnnules.sort(REGIME_FISCAL_COMPARATOR);
		return nonAnnules;
	}

	@Transient
	@NotNull
	public List<RegimeFiscal> getRegimesFiscauxNonAnnulesTries(RegimeFiscal.Portee portee) {
		final List<RegimeFiscal> all = getRegimesFiscauxNonAnnulesTries();
		return all.stream()
				.filter(rf -> rf.getPortee() == portee)
				.collect(Collectors.toList());
	}

	@Transient
	@Nullable
	public RegimeFiscal getRegimeFiscalActif(@NotNull RegimeFiscal.Portee portee) {
		return regimesFiscaux.stream()
				.filter(rf -> rf.getPortee() == portee)
				.filter(r -> r.isValidAt(null))
				.findAny()
				.orElse(null);
	}

	// configuration hibernate : l'entreprise possède les données civiles
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "ENTREPRISE_ID", nullable = false)
	public Set<DonneeCivileEntreprise> getDonneesCiviles() {
		return donneesCiviles;
	}

	public void setDonneesCiviles(Set<DonneeCivileEntreprise> donneesCiviles) {
		this.donneesCiviles = donneesCiviles;
	}

	public void addDonneeCivile(DonneeCivileEntreprise donnee) {
		if (donnee.getEntreprise() != null && donnee.getEntreprise() != this) {
			throw new IllegalArgumentException("Ces données ont déjà été associées à une autre entreprise");
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
				list.sort(new DateRangeComparator<>());
			}
		}
		else {
			list = Collections.emptyList();
		}
		return list;
	}

	@Transient
	public ActiviteEconomique getActiviteEconomiquePrincipaleValidAt(RegDate date) {
		return CollectionsUtils.unmodifiableNeverNull(getRapportsSujet()).stream()
				.filter(ActiviteEconomique.class::isInstance)
				.map(ActiviteEconomique.class::cast)
				.filter(ActiviteEconomique::isPrincipal)
				.filter(ret -> ret.isValidAt(date))
				.findFirst()
				.orElse(null);
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

	private static final Comparator<Integer> INTEGER_COMPARATOR_NULLS_FIRST = Comparator.nullsFirst(Comparator.naturalOrder());

	private static final Comparator<AllegementFiscal.TypeImpot> TYPE_IMPOT_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());

	private static final Comparator<AllegementFiscal.TypeCollectivite> TYPE_COLLECTIVITE_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());

	private static final Comparator<AllegementFiscal> ALLEGEMENT_FISCAL_COMPARATOR = new DateRangeComparator<AllegementFiscal>() {
		@Override
		public int compare(AllegementFiscal o1, AllegementFiscal o2) {
			int comparison = super.compare(o1, o2);
			if (comparison == 0) {
				comparison = TYPE_IMPOT_COMPARATOR.compare(o1.getTypeImpot(), o2.getTypeImpot());
				if (comparison == 0) {
					comparison = TYPE_COLLECTIVITE_COMPARATOR.compare(o1.getTypeCollectivite(), o2.getTypeCollectivite());
					if (comparison == 0 && o1.getTypeCollectivite() == AllegementFiscal.TypeCollectivite.COMMUNE) {
						final Integer ofsCommune1 = ((AllegementFiscalCommune) o1).getNoOfsCommune();
						final Integer ofsCommune2 = ((AllegementFiscalCommune) o2).getNoOfsCommune();
						comparison = INTEGER_COMPARATOR_NULLS_FIRST.compare(ofsCommune1, ofsCommune2);
					}
				}
			}
			return comparison;
		}
	};

	@Transient
	@NotNull
	public List<AllegementFiscal> getAllegementsFiscauxNonAnnulesTries() {
		final List<AllegementFiscal> nonAnnules = AnnulableHelper.sansElementsAnnules(allegementsFiscaux);
		nonAnnules.sort(ALLEGEMENT_FISCAL_COMPARATOR);
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
	@NotNull
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

	@Transient
	public EtatEntreprise getEtatAt(RegDate date) {
		final List<EtatEntreprise> nonAnnules = getEtatsNonAnnulesTries();
		return nonAnnules.stream().filter(e -> e.getDateObtention().isBeforeOrEqual(date)).
				max(Comparator.comparing(EtatEntreprise::getDateObtention, NullDateBehavior.EARLIEST::compare)).orElse(null);

	}

	public void setEtats(Set<EtatEntreprise> etats) {
		this.etats = etats;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "ENTREPRISE_ID")
	public Set<FlagEntreprise> getFlags() {
		return flags;
	}

	@NotNull
	@Transient
	public List<FlagEntreprise> getFlagsNonAnnulesTries() {
		final List<FlagEntreprise> nonAnnules = AnnulableHelper.sansElementsAnnules(flags);
		nonAnnules.sort(new DateRangeComparator<>());
		return nonAnnules;
	}

	@NotNull
	@Transient
	public List<FlagEntreprise> getFlagsNonAnnulesTries(GroupeFlagsEntreprise groupe) {
		final List<FlagEntreprise> tous = getFlagsNonAnnulesTries();
		return tous.stream()
				.filter(flag -> flag.getGroupe() == groupe)
				.collect(Collectors.toList());
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
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateDebutPremierExerciceCommercial() {
		return dateDebutPremierExerciceCommercial;
	}

	public void setDateDebutPremierExerciceCommercial(RegDate dateDebutPremierExerciceCommercial) {
		this.dateDebutPremierExerciceCommercial = dateDebutPremierExerciceCommercial;
	}

	@Transient
	public Set<AutreDocumentFiscal> getAutresDocumentsFiscaux() {
		return getDocumentsFiscaux() == null ? null : getDocumentsFiscaux().stream().filter(d -> AutreDocumentFiscal.class.isAssignableFrom(d.getClass())).map(d -> (AutreDocumentFiscal) d).collect(Collectors.toSet());
	}

	public void addAutreDocumentFiscal(AutreDocumentFiscal document) {
		if (document.getEntreprise() != null && document.getEntreprise() != this) {
			throw new IllegalArgumentException("Ce document est déjà associé à une autre entreprise.");
		}
		addDocumentFiscal(document);
	}


	@Column(name = "SECTEUR_ACTIVITE", length = LengthConstants.TIERS_SECTEUR_ACTIVITE)
	public String getSecteurActivite() {
		return secteurActivite;
	}

	public void setSecteurActivite(String secteurActivite) {
		this.secteurActivite = secteurActivite;
	}

	/**
	 * <p>
	 * Méthode métier à appeler lors de l'enregistrement d'un nouveau secteur d'activité, pour lequel le flag IDEDirty doit être mis à <code>true</code>.
	 * </p>
	 * <p>
	 * Pourquoi cette méthode dédiée? C'est beaucoup plus facile que de devoir détecter ce changement dans l'intercepteur tout en ignorant tous les autres.
	 * </p>
	 *
	 * @param nouveauSecteurActivite
	 */
	public void changeSecteurActivite(String nouveauSecteurActivite) {
		String precedantSecteurActivite = this.secteurActivite;
		this.setIdeDirty(precedantSecteurActivite != null && nouveauSecteurActivite != null && !nouveauSecteurActivite.equals(precedantSecteurActivite) ||
				                 nouveauSecteurActivite != null && precedantSecteurActivite == null || nouveauSecteurActivite == null && precedantSecteurActivite != null);

		this.secteurActivite = nouveauSecteurActivite;
	}

	@Column(name = "IDE_DIRTY")
	public boolean isIdeDirty() {
		return ideDirty;
	}

	public void setIdeDirty(boolean ideDirty) {
		this.ideDirty = ideDirty;
	}

	@Column(name = "IDE_DESACTIVE")
	public boolean isIdeDesactive() {
		return ideDesactive;
	}

	public void setIdeDesactive(boolean ideDesactive) {
		this.ideDesactive = ideDesactive;
	}

	@NotNull
	@Transient
	public <T extends AutreDocumentFiscal> List<T> getAutresDocumentsFiscaux(Class<T> clazz, boolean sorted, boolean avecAnnules) {
		final Set<AutreDocumentFiscal> autresDocumentsFiscaux = getAutresDocumentsFiscaux();
		if (autresDocumentsFiscaux == null || autresDocumentsFiscaux.isEmpty()) {
			return Collections.emptyList();
		}

		final List<T> liste = new ArrayList<>(autresDocumentsFiscaux.size());
		for (AutreDocumentFiscal adf : autresDocumentsFiscaux) {
			if (adf != null && clazz.isAssignableFrom(adf.getClass())) {
				if (avecAnnules || !adf.isAnnule()) {
					//noinspection unchecked
					liste.add((T) adf);
				}
			}
		}
		if (sorted && liste.size() > 1) {
			liste.sort(Comparator.comparing(AutreDocumentFiscal::getDateEnvoi, NullDateBehavior.EARLIEST::compare)
					           .thenComparing(AutreDocumentFiscal::getId, Comparator.nullsLast(Comparator.naturalOrder())));
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


	@Transient
	public boolean isSNC() {
		final ForFiscalPrincipal dernierForSnc = this.getDernierForFiscalPrincipal();
		return dernierForSnc != null && dernierForSnc.getGenreImpot() == GenreImpot.REVENU_FORTUNE;
	}

	@Transient
	public boolean hasBouclements(){
		return bouclements !=null && !AnnulableHelper.sansElementsAnnules(bouclements).isEmpty();
	}

	@Transient
	public boolean hasDateDebutPremierExercice(){
		return dateDebutPremierExerciceCommercial!=null;
	};

}
