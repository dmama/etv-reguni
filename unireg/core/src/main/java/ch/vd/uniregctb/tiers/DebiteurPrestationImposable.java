package ch.vd.uniregctb.tiers;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc --> Tiers ayant l'obligation de retenir et de verser périodiquement l'impôt dû sur les
 * prestations imposables à la source
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

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8k1x9Edygsbnw9h5bVw"
	 */
	private CategorieImpotSource categorieImpotSource;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8lFx9Edygsbnw9h5bVw"
	 */
	private PeriodiciteDecompte periodiciteDecompte;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_b-4YMPZ6EdyDE6gdiHo60A"
	 */
	private PeriodeDecompte periodeDecompte;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8lVx9Edygsbnw9h5bVw"
	 */
	private ModeCommunication modeCommunication;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_diY2cOOvEdyN8MKJ3LhMnw"
	 */
	private Boolean sansRappel;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_kUiSsOuREdyoJZZczGWapw"
	 */
	private Boolean sansListeRecapitulative;


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

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the categorieImpotSource
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8k1x9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "CATEGORIE_IMPOT_SOURCE", length = LengthConstants.DPI_CATEGORIEIS)
	@Type(type = "ch.vd.uniregctb.hibernate.CategorieImpotSourceUserType")
	public CategorieImpotSource getCategorieImpotSource() {
		// begin-user-code
		return categorieImpotSource;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theCategorieImpotSource
	 *            the categorieImpotSource to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8k1x9Edygsbnw9h5bVw?SETTER"
	 */
	public void setCategorieImpotSource(CategorieImpotSource theCategorieImpotSource) {
		// begin-user-code
		categorieImpotSource = theCategorieImpotSource;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the periodiciteDecompte
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8lFx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "PERIODICITE_DECOMPTE", length = LengthConstants.DPI_PERIODICITE)
	@Type(type = "ch.vd.uniregctb.hibernate.PeriodiciteDecompteUserType")
	public PeriodiciteDecompte getPeriodiciteDecompte() {
		// begin-user-code
		return periodiciteDecompte;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param thePeriodiciteDecompte
	 *            the periodiciteDecompte to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8lFx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setPeriodiciteDecompte(PeriodiciteDecompte thePeriodiciteDecompte) {
		// begin-user-code
		periodiciteDecompte = thePeriodiciteDecompte;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the periodeDecompte
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_b-4YMPZ6EdyDE6gdiHo60A?GETTER"
	 */
	@Column(name = "PERIODE_DECOMPTE", length = LengthConstants.DPI_PERIODE_DECOMPTE)
	@Type(type = "ch.vd.uniregctb.hibernate.PeriodeDecompteUserType")
	public PeriodeDecompte getPeriodeDecompte() {
		// begin-user-code
		return periodeDecompte;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param thePeriodeDecompte the periodeDecompte to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_b-4YMPZ6EdyDE6gdiHo60A?SETTER"
	 */
	public void setPeriodeDecompte(PeriodeDecompte thePeriodeDecompte) {
		// begin-user-code
		periodeDecompte = thePeriodeDecompte;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the modeCommunication
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8lVx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "MODE_COM", length = LengthConstants.DPI_MODECOM)
	@Type(type = "ch.vd.uniregctb.hibernate.ModeCommunicationUserType")
	public ModeCommunication getModeCommunication() {
		// begin-user-code
		return modeCommunication;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theModeCommunication
	 *            the modeCommunication to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8lVx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setModeCommunication(ModeCommunication theModeCommunication) {
		// begin-user-code
		modeCommunication = theModeCommunication;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the sansRappel
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_diY2cOOvEdyN8MKJ3LhMnw?GETTER"
	 */
	@Column(name = "SANS_RAPPEL")
	public Boolean getSansRappel() {
		// begin-user-code
		return sansRappel;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theSansRappel
	 *            the sansRappel to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_diY2cOOvEdyN8MKJ3LhMnw?SETTER"
	 */
	public void setSansRappel(Boolean theSansRappel) {
		// begin-user-code
		sansRappel = theSansRappel;
		// end-user-code
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

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @return the sansListeRecapitulative
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_kUiSsOuREdyoJZZczGWapw?GETTER"
	 */
	@Column(name = "SANS_LISTE_RECAP")
	public Boolean getSansListeRecapitulative() {
		// begin-user-code
		return sansListeRecapitulative;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theSansListeRecapitulative
	 *            the sansListeRecapitulative to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_kUiSsOuREdyoJZZczGWapw?SETTER"
	 */
	public void setSansListeRecapitulative(Boolean theSansListeRecapitulative) {
		// begin-user-code
		sansListeRecapitulative = theSansListeRecapitulative;
		// end-user-code
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
}
