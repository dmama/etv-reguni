package ch.vd.unireg.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.ComparisonHelper;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;

/**
 * Tiers ayant l'obligation de retenir et de verser périodiquement l'impôt dû sur les prestations imposables à la source
 */
@Entity
@DiscriminatorValue("DebiteurPrestationImposable")
public class DebiteurPrestationImposable extends Tiers {

	public static final int FIRST_MIGRATION_ID = 1000000;
	public static final int LAST_MIGRATION_ID = 1499999;
	public static final int FIRST_ID = 1500000;
	public static final int LAST_ID = 1999999;

	private String nom1;
	private String nom2;
	private CategorieImpotSource categorieImpotSource;
	private PeriodiciteDecompte periodiciteDecompteAvantMigration;
	private PeriodeDecompte periodeDecompteAvantMigration;
	private ModeCommunication modeCommunication;
	private Boolean sansRappel = Boolean.FALSE;
	private Boolean sansListeRecapitulative = Boolean.FALSE;
	private Set<Periodicite> periodicites;
	private Long logicielId;
	private Boolean aciAutreCanton = Boolean.FALSE;

	@Column(name = "DPI_NOM1", length = LengthConstants.DPI_NOM1)
	public String getNom1() {
		return nom1;
	}

	public void setNom1(String nom1) {
		this.nom1 = nom1;
	}

	@Column(name = "DPI_NOM2", length = LengthConstants.DPI_NOM2)
	public String getNom2() {
		return nom2;
	}

	public void setNom2(String nom2) {
		this.nom2 = nom2;
	}

	@Column(name = "CATEGORIE_IMPOT_SOURCE", length = LengthConstants.DPI_CATEGORIEIS)
	@Type(type = "ch.vd.unireg.hibernate.CategorieImpotSourceUserType")
	public CategorieImpotSource getCategorieImpotSource() {
		return categorieImpotSource;
	}

	public void setCategorieImpotSource(CategorieImpotSource theCategorieImpotSource) {
		categorieImpotSource = theCategorieImpotSource;
	}

	@Column(name = "PERIODICITE_DECOMPTE", length = LengthConstants.DPI_PERIODICITE)
	@Type(type = "ch.vd.unireg.hibernate.PeriodiciteDecompteUserType")
	public PeriodiciteDecompte getPeriodiciteDecompteAvantMigration() {
		return periodiciteDecompteAvantMigration;
	}

	public void setPeriodiciteDecompteAvantMigration(PeriodiciteDecompte thePeriodiciteDecompte) {
		periodiciteDecompteAvantMigration = thePeriodiciteDecompte;
	}

	@Column(name = "PERIODE_DECOMPTE", length = LengthConstants.DPI_PERIODE_DECOMPTE)
	@Type(type = "ch.vd.unireg.hibernate.PeriodeDecompteUserType")
	public PeriodeDecompte getPeriodeDecompteAvantMigration() {
		return periodeDecompteAvantMigration;
	}

	public void setPeriodeDecompteAvantMigration(PeriodeDecompte thePeriodeDecompte) {
		periodeDecompteAvantMigration = thePeriodeDecompte;
	}

	@Column(name = "MODE_COM", length = LengthConstants.DPI_MODECOM)
	@Type(type = "ch.vd.unireg.hibernate.ModeCommunicationUserType")
	public ModeCommunication getModeCommunication() {
		return modeCommunication;
	}

	public void setModeCommunication(ModeCommunication theModeCommunication) {
		modeCommunication = theModeCommunication;
	}

	@Column(name = "SANS_RAPPEL")
	public Boolean getSansRappel() {
		return sansRappel;
	}

	@Column(name = "DPI_ACI_HC")
	public Boolean getAciAutreCanton() {
		return aciAutreCanton;
	}

	public void setAciAutreCanton(Boolean aciAutreCanton) {
		this.aciAutreCanton = aciAutreCanton;
	}

	@Column(name = "LOGICIEL_ID")
	public Long getLogicielId() {
		return logicielId;
	}

	public void setLogicielId(Long logicielId) {
		this.logicielId = logicielId;
	}

	public void setSansRappel(Boolean theSansRappel) {
		sansRappel = theSansRappel;
	}

	/**
	 * @return l'id du contribuable associé au débiteur; ou <b>null</b> si le débiteur n'en possède pas.
	 */
	@Transient
	public Long getContribuableId() {
		return CollectionsUtils.unmodifiableNeverNull(getRapportsObjet()).stream()
				.filter(ContactImpotSource.class::isInstance)
				.filter(AnnulableHelper::nonAnnule)
				.filter(ret -> ret.getDateFin() == null)
				.findFirst()
				.map(RapportEntreTiers::getSujetId)
				.orElse(null);
	}

	@Column(name = "SANS_LISTE_RECAP")
	public Boolean getSansListeRecapitulative() {
		return sansListeRecapitulative;
	}

	public void setSansListeRecapitulative(Boolean theSansListeRecapitulative) {
		sansListeRecapitulative = theSansListeRecapitulative;
	}

	@Transient
	@Override
	public String getRoleLigne1() {
		return "Débiteur IS";
	}

	@Transient
	@Override
	public NatureTiers getNatureTiers() {
		return NatureTiers.DebiteurPrestationImposable;
	}

	@Transient
	@Override
	public TypeTiers getType() {
		return TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "DEBITEUR_ID", nullable = false)
	@ForeignKey(name = "FK_PERIODICITE_DB_ID")
	public Set<Periodicite> getPeriodicites() {
		return periodicites;
	}

	public void setPeriodicites(Set<Periodicite> periodicites) {
		this.periodicites = periodicites;
	}

	@Transient
	public boolean isSansLREmises(){
		final List<DeclarationImpotSource> declarations = getDeclarationsTriees(DeclarationImpotSource.class, false);
		return declarations.isEmpty();
	}

	/**
	 * @return Retourne les Periodicités triées par - La date d'ouverture (sans les annulées)
	 */
	@NotNull
	@Transient
	public List<Periodicite> getPeriodicitesSorted() {
		if (periodicites == null || periodicites.isEmpty()) {
			return Collections.emptyList();
		}
		return periodicites.stream()
				.filter(AnnulableHelper::nonAnnule)
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toList());
	}

	@Transient
	public Periodicite getDernierePeriodicite() {
		final List<Periodicite> list = getPeriodicitesSorted();
		return list.isEmpty() ? null : CollectionsUtils.getLastElement(list);
	}

	@Transient
	public Periodicite getPremierePeriodicite() {
		final List<Periodicite> list = getPeriodicitesSorted();
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * @param sort <code>true</code> si les periodicites doivent être triées; <code>false</code> autrement.
	 * @return les périodicités non annulés
	 */
	@Transient
	@NotNull
	public List<Periodicite> getPeriodicitesNonAnnulees(boolean sort) {
		final List<Periodicite> periodicitesNonAnnulees = AnnulableHelper.sansElementsAnnules(periodicites);
		if (sort) {
			periodicitesNonAnnulees.sort(new DateRangeComparator<>());
		}
		return periodicitesNonAnnulees;
	}

	@Transient
	public ForDebiteurPrestationImposable getPremierForDebiteur() {
		return getStreamForsFiscaux(ForDebiteurPrestationImposable.class, false)
				.min(FOR_FISCAL_COMPARATOR)
				.orElse(null);
	}

	@Transient
	public ForDebiteurPrestationImposable getDernierForDebiteur() {
		return getStreamForsFiscaux(ForDebiteurPrestationImposable.class, false)
				.max(FOR_FISCAL_COMPARATOR)
				.orElse(null);
	}

	@Transient
	public ForDebiteurPrestationImposable getDernierForDebiteurAvant(RegDate date) {
		return getStreamForsFiscaux(ForDebiteurPrestationImposable.class, false)
				.filter(ff -> RegDateHelper.isBeforeOrEqual(ff.getDateDebut(), date, NullDateBehavior.LATEST))
				.max(FOR_FISCAL_COMPARATOR)
				.orElse(null);
	}

	/**
	 * Renvoie le ForDebiteurPrestationImposable actif à la date donnée en entrée
	 *
	 * @param date
	 * @return
	 */
	@Transient
	public ForDebiteurPrestationImposable getForDebiteurPrestationImposableAt(@Nullable RegDate date) {
		return getStreamForsFiscaux(ForDebiteurPrestationImposable.class, false)
				.filter(ff -> ff.isValidAt(date))
				.findFirst()
				.orElse(null);
	}

	/**
	 * Renvoie le premier ForDebiteurPrestationImposable actif après la date donnée en entrée
	 *
	 * @param date
	 * @return
	 */
	@Transient
	public ForDebiteurPrestationImposable getForDebiteurPrestationImposableAfter(RegDate date) {
		return getStreamForsFiscaux(ForDebiteurPrestationImposable.class, false)
				.filter(ff -> ff.getDateDebut().isAfter(date))
				.min(Comparator.comparing(ForFiscal::getDateDebut))
				.orElse(null);
	}

	@Override
	@Transient
	protected boolean isDesactiveSelonFors(RegDate date) {
		// pour un débiteur, on dira qu'il est désactivé à une date donnée s'il n'y a pas de for
		// débiteur actif à la date donnée et que le dernier for fiscal débiteur a été fermé
		// pour un motif "ANNULATION"

		final boolean desactive;
		final ForDebiteurPrestationImposable courant = getForDebiteurPrestationImposableAt(date);
		if (courant == null) {
			final ForDebiteurPrestationImposable dernier = getDernierForDebiteurAvant(date);
			desactive = dernier != null && dernier.getMotifFermeture() == MotifFor.ANNULATION;
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
		final ForDebiteurPrestationImposable courant = getForDebiteurPrestationImposableAt(null);
		if (courant == null) {
			final ForDebiteurPrestationImposable dernier = getDernierForDebiteur();
			date = dernier != null && dernier.getMotifFermeture() == MotifFor.ANNULATION ? dernier.getDateFin() : null;
		}
		else {
			date = null;
		}
		return date;
	}

	/**
	 * Ajoute une periodicite
	 *
	 * @param nouvellePeriodicite la periodicité à ajouter
	 */
	public void addPeriodicite(Periodicite nouvellePeriodicite) {
		if (this.periodicites == null) {
			this.periodicites = new HashSet<>();
		}

		this.periodicites.add(nouvellePeriodicite);
		Assert.isTrue(nouvellePeriodicite.getDebiteur() == null || nouvellePeriodicite.getDebiteur() == this);
		nouvellePeriodicite.setDebiteur(this);

	}

	/**
	 * Retourne la periodicite active à une date donnée.
	 *
	 * @param date la date à laquelle la periodicite est active, ou <b>null</b> pour obtenir la periodicite courante
	 * @return la periodicite correspondante, ou nulle si aucune periodicite ne correspond aux critères.
	 */
	@Transient
	public Periodicite getPeriodiciteAt(RegDate date) {

		if (periodicites == null || periodicites.isEmpty()) {
			//On retourne la periodicite presente sur le debiteur
			//ce mecanisme est temporaire, il permet d'eviter les nullPointeurExceptiosn
			//pour les tiers n'ayant pas encore d'historique de périodicités.
			return new Periodicite(periodiciteDecompteAvantMigration, periodeDecompteAvantMigration, RegDateHelper.get(getLogCreationDate()), null);
		}
		else {
			for (Periodicite p : periodicites) {
				if (p.isValidAt(date)) {
					return p;
				}
			}
			//Si aucune périodicité n'est trouvé et que la date spécifé se trouve avant la date de début de validité
			//de la première periodicité et que celle ci est unique, on la renvoie
			final List<Periodicite> periodicitesTriees = getPeriodicitesNonAnnulees(true);
			if (!periodicitesTriees.isEmpty()) {
				final Periodicite premiere = periodicitesTriees.get(0);
				if (premiere.getPeriodiciteDecompte() == PeriodiciteDecompte.UNIQUE && date != null && date.isBefore(premiere.getDateDebut())) {
					return premiere;
				}
			}
			
			return null;
		}
	}

	public Periodicite findPeriodicite(RegDate dateDebutPeriode, RegDate dateFinPeriode) {
		Periodicite periodiciteAt = this.getPeriodiciteAt(dateDebutPeriode);
		//[UNIREG-3115] Periodicite non trouvé en debut de periode de lR on cherche à la fin.
		if (periodiciteAt == null) {
			periodiciteAt = this.getPeriodiciteAt(dateFinPeriode);
		}
		return periodiciteAt;
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

		final DebiteurPrestationImposable other = (DebiteurPrestationImposable) obj;
		return ComparisonHelper.areEqual(categorieImpotSource, other.categorieImpotSource)
				&& ComparisonHelper.areEqual(modeCommunication, other.modeCommunication)
				&& ComparisonHelper.areEqual(nom1, other.nom1)
				&& ComparisonHelper.areEqual(nom2, other.nom2)
				&& ComparisonHelper.areEqual(periodeDecompteAvantMigration, other.periodeDecompteAvantMigration)
				&& ComparisonHelper.areEqual(periodiciteDecompteAvantMigration, other.periodiciteDecompteAvantMigration);
	}
}
