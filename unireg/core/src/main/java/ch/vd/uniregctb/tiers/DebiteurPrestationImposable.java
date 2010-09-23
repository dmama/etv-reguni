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
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

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
	private PeriodiciteDecompte periodiciteDecompte;
	private PeriodeDecompte periodeDecompte;
	private ModeCommunication modeCommunication;
	private Boolean sansRappel;
	private Boolean sansListeRecapitulative;
	private Set<Periodicite> periodicites;


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
	public PeriodiciteDecompte getPeriodiciteDecompte() {
		return periodiciteDecompte;
	}

	public void setPeriodiciteDecompte(PeriodiciteDecompte thePeriodiciteDecompte) {
		periodiciteDecompte = thePeriodiciteDecompte;
	}

	@Column(name = "PERIODE_DECOMPTE", length = LengthConstants.DPI_PERIODE_DECOMPTE)
	@Type(type = "ch.vd.uniregctb.hibernate.PeriodeDecompteUserType")
	public PeriodeDecompte getPeriodeDecompte() {
		return periodeDecompte;
	}

	public void setPeriodeDecompte(PeriodeDecompte thePeriodeDecompte) {
		periodeDecompte = thePeriodeDecompte;
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
	public String getNatureTiers() {
		return DebiteurPrestationImposable.class.getSimpleName();
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
	 * {@inheritDoc}
	 */
	@Override
	public ValidationResults validateFors() {
		ValidationResults results = super.validateFors();

		final ForsParType fors = getForsParType(true /* triés par ordre chronologique */);

		// Les plages de validité des fors ne doivent pas se chevaucher
		ForDebiteurPrestationImposable lastFor = null;
		for (ForDebiteurPrestationImposable fdpis : fors.dpis) {
			if (lastFor != null && DateRangeHelper.intersect(lastFor, fdpis)) {
				results.addError("Le for DPI qui commence le " + fdpis.getDateDebut() + " et se termine le " + fdpis.getDateFin()
						+ " chevauche le for précédent");
			}
			lastFor = fdpis;
			if (fdpis.getTypeAutoriteFiscale().equals(TypeAutoriteFiscale.PAYS_HS)) {
				results.addError("Les for DPI hors suisse ne sont pas autorisés.");
			}
		}

		// Seuls les for DPI sont autorisés
		for (ForFiscal f : fors.principaux) {
			results.addError("Le for " + f + " n'est pas un type de for autorisé sur un débiteur de prestations imposables.");
		}
		for (ForFiscal f : fors.secondaires) {
			results.addError("Le for " + f + " n'est pas un type de for autorisé sur un débiteur de prestations imposables.");
		}

		return results;
	}


	@Override
	protected ValidationResults validateTypeAdresses() {

		ValidationResults results = new ValidationResults();

		final Set<AdresseTiers> adresses = getAdressesTiers();
		if (adresses != null) {
			for (AdresseTiers a : adresses) {
				if (a.isAnnule()) {
					continue;
				}
				if (a instanceof AdressePM) {
					results.addError("L'adresse de type 'personne morale' (numéro=" + a.getId() + ", début=" + a.getDateDebut() + ", fin="
							+ a.getDateFin() + ") n'est pas autorisée sur un débiteur de prestations imposables.");
				}
				else if (a instanceof AdresseCivile) {
					results.addError("L'adresse de type 'personne civile' (numéro=" + a.getId() + ", début=" + a.getDateDebut() + ", fin="
							+ a.getDateFin() + ") n'est pas autorisée sur un débiteur de prestations imposables.");
				}
			}
		}

		return results;
	}

	@Override
	public ValidationResults validate() {
		final ValidationResults results = super.validate();
		results.merge(validatePeriodicites());
		return results;
	}

	private ValidationResults validatePeriodicites() {
		final ValidationResults results = new ValidationResults();
		if (periodicites == null || periodicites.isEmpty()) {
			results.addWarning("ce débiteur n'a aucune périodicité");
		}
		else {
			// Les plages de validité des fors ne doivent pas se chevaucher
			Periodicite lastPeriodicite = null;
			for (Periodicite p : getPeriodicitesSorted()) {
				if (p.isAnnule()) {
					continue;
				}
				if (lastPeriodicite != null && DateRangeHelper.intersect(lastPeriodicite, p)) {
					results.addError("La périodicité qui commence le " + p.getDateDebut() + " et se termine le " + p.getDateFin() + " chevauche la périodicité précédente");
				}
				lastPeriodicite = p;
			}

			ForDebiteurPrestationImposable premierForFiscal = getPremierForDebiteur();
			if (premierForFiscal != null) {
				Periodicite premierePeriodicite = getPremierePeriodicite();
				if (premierForFiscal.getDateDebut().isBefore(premierePeriodicite.getDateDebut())) {
					results.addError(" aucune périodicité n'est définie entre le début d'activité le " +
							RegDateHelper.dateToDisplayString(premierForFiscal.getDateDebut()) +
							" et la date de début de la première périodicité " + RegDateHelper.dateToDisplayString(premierePeriodicite.getDateDebut()));
				}
			}

		}
		return results;
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
		else {
			return new Periodicite(periodiciteDecompte, periodeDecompte, RegDate.get(getLogCreationDate()), null);
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
			return new Periodicite(periodiciteDecompte, periodeDecompte, RegDate.get(getLogCreationDate()), null);
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
		else if (!categorieImpotSource.equals(other.categorieImpotSource))
			return false;
		if (modeCommunication == null) {
			if (other.modeCommunication != null)
				return false;
		}
		else if (!modeCommunication.equals(other.modeCommunication))
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
		if (periodeDecompte == null) {
			if (other.periodeDecompte != null)
				return false;
		}
		else if (!periodeDecompte.equals(other.periodeDecompte))
			return false;
		if (periodiciteDecompte == null) {
			if (other.periodiciteDecompte != null)
				return false;
		}
		else if (!periodiciteDecompte.equals(other.periodiciteDecompte))
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

	@Override
	@Transient
	protected boolean isDesactiveSelonFors(RegDate date) {

		// la désactivation d'un débiteur se fait par simple fermeture du for
		// -> s'il y a des fors mais qu'il n'y en a pas d'actif à la date demandée, on dira qu'il est désactivé

		final List<ForFiscal> fors = getForsFiscauxNonAnnules(false);
		return fors != null && fors.size() > 0 && getForDebiteurPrestationImposableAt(date) == null;
	}

	@Override
	@Transient
	public RegDate getDateDesactivation() {
		// c'est la date de fermeture du dernier for fiscal
		final List<ForFiscal> fors = getForsFiscauxNonAnnules(true);
		final ForFiscal dernierFor = fors != null && fors.size() > 0 ? fors.get(fors.size() - 1) : null;
		return dernierFor != null ? dernierFor.getDateFin() : null;
	}
}
