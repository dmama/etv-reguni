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

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
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

	private static final long serialVersionUID = -9115124361562905652L;

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
	private Boolean sansRappel;
	private Boolean sansListeRecapitulative;
	private Set<Periodicite> periodicites;
	private Long logicielId;


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

	/**
	 * @return Retourne les Periodicités triées par - La date d'ouverture
	 */
	@Transient
	public List<Periodicite> getPeriodicitesSorted() {
		if (periodicites == null) {
			return null;
		}
		final List<Periodicite> list = new ArrayList<Periodicite>(periodicites);
		Collections.sort(list, new DateRangeComparator<Periodicite>());
		return list;
	}

	@Transient
	public Periodicite getDernierePeriodicite() {

		List<Periodicite> list = getPeriodicitesSorted();
		if (list != null) {
			for (int i = list.size() - 1; i >= 0; i--) {
				Periodicite periodicite = list.get(i);
				if (!periodicite.isAnnule()) {
					return periodicite;
				}
			}
		}
		return null;
	}

	@Transient
	public Periodicite getPremierePeriodicite() {
		List<Periodicite> list = getPeriodicitesSorted();
		if (list != null) {
			for (Periodicite periodicite : list) {
				if (!periodicite.isAnnule()) {
					return periodicite;
				}
			}
		}
		return null;
	}

	/**
	 * @param sort <code>true</code> si les periodicites doivent être triées; <code>false</code> autrement.
	 * @return les périodicités non annulés
	 */
	@Transient
	public List<Periodicite> getPeriodicitesNonAnnules(boolean sort) {
		List<Periodicite> periodicitesNonAnnulees = new ArrayList<Periodicite>();
		if (periodicites != null) {
			for (Periodicite p : periodicites) {
				if (!p.isAnnule()) {
					periodicitesNonAnnulees.add(p);
				}
			}
		}
		if (sort) {
			Collections.sort(periodicitesNonAnnulees, new DateRangeComparator<Periodicite>());
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

	/**
	 * Ajoute une periodicite
	 *
	 * @param nouvellePeriodicite la periodicité à ajouter
	 */
	public void addPeriodicite(Periodicite nouvellePeriodicite) {
		if (this.periodicites == null) {
			this.periodicites = new HashSet<Periodicite>();
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
			return new Periodicite(periodiciteDecompteAvantMigration, periodeDecompteAvantMigration, RegDate.get(getLogCreationDate()), null);
		}
		else {

			for (Periodicite p : periodicites) {
				if (p.isValidAt(date)) {
					return p;
				}
			}
			//Si aucune périodicité n'est trouvé et que la date spécifé se trouve avant la date de début de validité
			//de la première periodicité et que celle ci est unique, on la renvoie
			List<Periodicite> periodicitesTriees = new ArrayList<Periodicite>(periodicites.size());
			periodicitesTriees.addAll(periodicites);
			Collections.sort(periodicitesTriees,new DateRangeComparator<Periodicite>());
			Periodicite premiere = periodicitesTriees.get(0);
			if(premiere.getPeriodiciteDecompte() == PeriodiciteDecompte.UNIQUE && date!=null && date.isBefore(premiere.getDateDebut())){
				return premiere;
			}
			

			return null;
		}

	}

	public Periodicite findPeriodicite(RegDate dateDebutPeriode, RegDate dateFinPeriode){
			Periodicite periodiciteAt = this.getPeriodiciteAt(dateDebutPeriode);
		//[UNIREG-3115] Periodicite non trouvé en debut de periode de lR on cherche à la fin.
		if(periodiciteAt==null){
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
		DebiteurPrestationImposable other = (DebiteurPrestationImposable) obj;
		if (categorieImpotSource == null) {
			if (other.categorieImpotSource != null)
				return false;
		}
		else if (categorieImpotSource != other.categorieImpotSource)
			return false;
		if (modeCommunication == null) {
			if (other.modeCommunication != null)
				return false;
		}
		else if (modeCommunication != other.modeCommunication)
			return false;
		if (nom1 == null) {
			if (other.nom1 != null)
				return false;
		}
		else if (!nom1.equals(other.nom1))
			return false;
		if (nom2 == null) {
			if (other.nom2 != null)
				return false;
		}
		else if (!nom2.equals(other.nom2))
			return false;
		if (periodeDecompteAvantMigration == null) {
			if (other.periodeDecompteAvantMigration != null)
				return false;
		}
		else if (periodeDecompteAvantMigration != other.periodeDecompteAvantMigration)
			return false;
		if (periodiciteDecompteAvantMigration == null) {
			if (other.periodiciteDecompteAvantMigration != null)
				return false;
		}
		else if (periodiciteDecompteAvantMigration != other.periodiciteDecompteAvantMigration)
			return false;
		if (sansListeRecapitulative == null) {
			if (other.sansListeRecapitulative != null)
				return false;
		}
		else if (!sansListeRecapitulative.equals(other.sansListeRecapitulative))
			return false;
		if (sansRappel == null) {
			if (other.sansRappel != null)
				return false;
		}
		else if (!sansRappel.equals(other.sansRappel))
			return false;
		return true;
	}
}
