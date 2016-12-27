package ch.vd.uniregctb.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
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
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.ComparisonHelper;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc --> Tiers ayant l'obligation de retenir et de verser périodiquement l'impôt dû sur les prestations imposables à la source
 *
 * @uml.annotations derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8kFx9Edygsbnw9h5bVw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8kFx9Edygsbnw9h5bVw"
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
	@Type(type = "ch.vd.uniregctb.hibernate.CategorieImpotSourceUserType")
	public CategorieImpotSource getCategorieImpotSource() {
		return categorieImpotSource;
	}

	public void setCategorieImpotSource(CategorieImpotSource theCategorieImpotSource) {
		categorieImpotSource = theCategorieImpotSource;
	}

	@Column(name = "PERIODICITE_DECOMPTE", length = LengthConstants.DPI_PERIODICITE)
	@Type(type = "ch.vd.uniregctb.hibernate.PeriodiciteDecompteUserType")
	public PeriodiciteDecompte getPeriodiciteDecompteAvantMigration() {
		return periodiciteDecompteAvantMigration;
	}

	public void setPeriodiciteDecompteAvantMigration(PeriodiciteDecompte thePeriodiciteDecompte) {
		periodiciteDecompteAvantMigration = thePeriodiciteDecompte;
	}

	@Column(name = "PERIODE_DECOMPTE", length = LengthConstants.DPI_PERIODE_DECOMPTE)
	@Type(type = "ch.vd.uniregctb.hibernate.PeriodeDecompteUserType")
	public PeriodeDecompte getPeriodeDecompteAvantMigration() {
		return periodeDecompteAvantMigration;
	}

	public void setPeriodeDecompteAvantMigration(PeriodeDecompte thePeriodeDecompte) {
		periodeDecompteAvantMigration = thePeriodeDecompte;
	}

	@Column(name = "MODE_COM", length = LengthConstants.DPI_MODECOM)
	@Type(type = "ch.vd.uniregctb.hibernate.ModeCommunicationUserType")
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

		Long ctbId = null;

		final Set<RapportEntreTiers> rapports = getRapportsObjet();
		if (rapports != null) {
			for (RapportEntreTiers r : rapports) {
				if (r.isValidAt(null) && r instanceof ContactImpotSource) {
					ctbId = r.getSujetId();
					break;
				}
			}
		}

		return ctbId;
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
	@Transient
	public List<Periodicite> getPeriodicitesSorted() {
		if (periodicites == null) {
			return null;
		}
		final List<Periodicite> list = new ArrayList<>(periodicites.size());
		for (Periodicite p : periodicites) {
			if (!p.isAnnule()) {
				list.add(p);
			}
		}
		Collections.sort(list, new DateRangeComparator<>());
		return list;
	}

	@Transient
	public Periodicite getDernierePeriodicite() {
		final List<Periodicite> list = getPeriodicitesSorted();
		if (list != null && !list.isEmpty()) {
			return list.get(list.size() - 1);
		}
		else {
			return null;
		}
	}

	@Transient
	public Periodicite getPremierePeriodicite() {
		final List<Periodicite> list = getPeriodicitesSorted();
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		}
		else {
			return null;
		}
	}

	/**
	 * @param sort <code>true</code> si les periodicites doivent être triées; <code>false</code> autrement.
	 * @return les périodicités non annulés
	 */
	@Transient
	@NotNull
	public List<Periodicite> getPeriodicitesNonAnnulees(boolean sort) {
		final List<Periodicite> periodicitesNonAnnulees = new ArrayList<>();
		if (periodicites != null) {
			for (Periodicite p : periodicites) {
				if (!p.isAnnule()) {
					periodicitesNonAnnulees.add(p);
				}
			}
		}
		if (sort) {
			Collections.sort(periodicitesNonAnnulees, new DateRangeComparator<>());
		}
		return periodicitesNonAnnulees;
	}




	@Transient
	public ForDebiteurPrestationImposable getPremierForDebiteur() {
		List<ForFiscal> list = getForsFiscauxSorted();
		if (list != null) {
			for (ForFiscal forFiscal : list) {
				if (!forFiscal.isAnnule()) {
					return (ForDebiteurPrestationImposable) forFiscal;
				}
			}
		}
		return null;
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
