package ch.vd.uniregctb.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.SubValidateable;
import ch.vd.registre.base.validation.Validateable;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Rattachement d'un contribuable à une commune ou fraction de commune vaudoise.
 *
 * Ce rattachement peut être personnel (domicile ou séjour), auquel cas il est constitutif d'un for fiscal principal, ou économique, auquel
 * cas il est constitutif d'un for fiscal accessoire.
 *
 * Les régles de validité des différents enums en fonction des types concrets de fors fiscaux sont les suivants:
 *
 * <pre>
 *  Enum \ Type For Fiscal | ForFiscalPrincipal | ForFiscalSecondaire    | ForFiscalAutreElementImposable     |ForDPI                         |ForFiscalAutreImpot     |
 * ========================+====================+========================+====================================+===============================+========================|
 *  GenreImpot             | REVENU_FORTUNE     | REVENU_FORTUNE         | REVENU_FORTUNE                     | DEBITEUR_PRESTATION_IMPOSABLE | tous les autres        |
 * ------------------------+--------------------+------------------------+--------------------------------------------------------------------+------------------------|
 *  MotifRattachement      | DOMICILE           | ACTIVIE_INDEPENDANTE   | ACTIVITE_LUCRATIVE_CAS             | pas applicable                | pas applicable         |
 *                         | DIPLOMATE          | IMMEUBLE_PRIVE      	 | ADMINISTRATEUR                     |                               |                        |
 *                         |                    |                        | CREANCIER_HYPOTHECAIRE             |                               |                        |
 *                         |                    |                        | BENEFICIAIRE_PRESTATION_PREVOYANCE |                               |                        |
 *                         |                    |                        | LOI_TRAVAIL_AU_NOIR                |                               |                        |
 * ------------------------+--------------------+------------------------+--------------------------------------------------------------------+------------------------|
 *  TypeAutoriteFiscale    | toutes autorisées  | COMMUNE_OU_FRACTION_VD | COMMUNE_OU_FRACTION_VD             | COMMUNE_OU_FRACTION_VD        | COMMUNE_OU_FRACTION_VD |
 * ------------------------+--------------------+------------------------+--------------------------------------------------------------------+------------------------|
 *  ModeImposition         | toutes autorisées  | pas applicable         | pas applicable                     | pas applicable                | pas applicable         |
 * ------------------------+--------------------+------------------------+--------------------------------------------------------------------+------------------------|
 * </pre>
 */
@Entity
@Table(name = "FOR_FISCAL")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "FOR_TYPE", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("ForFiscal")
public abstract class ForFiscal extends HibernateEntity implements Comparable<ForFiscal>, DateRange, SubValidateable, Duplicable<ForFiscal> {

	private static final long serialVersionUID = -4147759696434131389L;

	/**
	 * The ID
	 */
	private Long id;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc --> Date de début du rattachement personnel ou économique
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8x1x9Edygsbnw9h5bVw"
	 */
	private RegDate dateDebut;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc --> Date de la fermeture du for pour un impôt donné.
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8yVx9Edygsbnw9h5bVw"
	 */
	private RegDate dateFin;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc --> Indique l'impôt auquel un contribuable est soumis : - ICC/IFD (dit aussi impôt
	 * ordinaire ou IRF/IBC) - GI - DM - ...
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8ZVx9Edygsbnw9h5bVw"
	 */
	private GenreImpot genreImpot;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_iGQgMJRQEdyIw97l1zxC4Q"
	 */
	private TypeAutoriteFiscale typeAutoriteFiscale;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_pP7yIJNfEdygKK6Oe0tVlw"
	 */
	private Integer numeroOfsAutoriteFiscale;

	private Tiers tiers;

	public ForFiscal() {
	}

	public ForFiscal(RegDate ouverture, RegDate fermeture, GenreImpot genreImpot, Integer numeroOfsAutoriteFiscale,
			TypeAutoriteFiscale typeAutoriteFiscale) {
		this.dateDebut = ouverture;
		this.dateFin = fermeture;
		setGenreImpot(genreImpot); // virtual
		this.numeroOfsAutoriteFiscale = numeroOfsAutoriteFiscale;
		setTypeAutoriteFiscale(typeAutoriteFiscale); // virtual
	}

	public ForFiscal(ForFiscal ff) {
		this(ff.getDateDebut(), ff.getDateFin(), ff.getGenreImpot(), ff.getNumeroOfsAutoriteFiscale(), ff.getTypeAutoriteFiscale());
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Long theId) {
		this.id = theId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * La date d'ouverture du for fiscal.
	 * <!-- end-user-doc -->
	 * @return the dateOuverture
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8x1x9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "DATE_OUVERTURE", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		// begin-user-code
		return dateDebut;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * La date d'ouverture du for fiscal.
	 * <!-- end-user-doc -->
	 * @param theDateOuverture the dateOuverture to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8x1x9Edygsbnw9h5bVw?SETTER"
	 */
	public void setDateDebut(RegDate date) {
		// begin-user-code
		dateDebut = date;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * La date de fermeture du for fiscal.
	 * <!-- end-user-doc -->
	 * @return the dateFermeture
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8yVx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "DATE_FERMETURE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		// begin-user-code
		return dateFin;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * La date de fermeture du for fiscal.
	 * <!-- end-user-doc -->
	 * @param theDateFermeture the dateFermeture to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8yVx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setDateFin(RegDate date) {
		// begin-user-code
		dateFin = date;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the genreImpot
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8ZVx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "GENRE_IMPOT", nullable = false, length = LengthConstants.FOR_GENRE)
	@Type(type = "ch.vd.uniregctb.hibernate.GenreImpotUserType")
	public GenreImpot getGenreImpot() {
		// begin-user-code
		return genreImpot;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theGenreImpot the genreImpot to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8ZVx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setGenreImpot(GenreImpot theGenreImpot) {
		// begin-user-code
		genreImpot = theGenreImpot;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the numeroOfsAutoriteFiscale
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_pP7yIJNfEdygKK6Oe0tVlw?GETTER"
	 */
	@Column(name = "NUMERO_OFS", nullable = false)
	public Integer getNumeroOfsAutoriteFiscale() {
		// begin-user-code
		return numeroOfsAutoriteFiscale;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theNumeroOfsAutoriteFiscale the numeroOfsAutoriteFiscale to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_pP7yIJNfEdygKK6Oe0tVlw?SETTER"
	 */
	public void setNumeroOfsAutoriteFiscale(Integer theNumeroOfsAutoriteFiscale) {
		// begin-user-code
		numeroOfsAutoriteFiscale = theNumeroOfsAutoriteFiscale;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the typeAutoriteFiscaleFiscale
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_iGQgMJRQEdyIw97l1zxC4Q?GETTER"
	 */
	@Column(name = "TYPE_AUT_FISC", nullable = false, length = LengthConstants.FOR_AUTORITEFISCALE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeAutoriteFiscaleUserType")
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		// begin-user-code
		return typeAutoriteFiscale;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theTypeAutoriteFiscaleFiscale the typeAutoriteFiscaleFiscale to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_iGQgMJRQEdyIw97l1zxC4Q?SETTER"
	 */
	public void setTypeAutoriteFiscale(TypeAutoriteFiscale theTypeAutoriteFiscaleFiscale) {
		// begin-user-code
		typeAutoriteFiscale = theTypeAutoriteFiscaleFiscale;
		// end-user-code
	}

	/**
	 * Compare d'apres la date dur for
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(ForFiscal o) {
		int value = getDateDebut().compareTo(o.getDateDebut());
		return value;
	}

	@Transient
	public boolean isPrincipal() {
		return false;
	}

	@Transient
	public boolean isDebiteur() {
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (" + (dateDebut != null ? dateDebut : "?" )  + " - " + (dateFin != null ? dateFin : "?") + ")";
	}

	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "TIERS_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_FF_TIERS_ID", columnNames = "TIERS_ID")
	public Tiers getTiers() {
		return tiers;
	}

	public void setTiers(Tiers tiers) {
		this.tiers = tiers;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	protected void dumpForDebug(int nbTabs) {
		ddump(nbTabs, "Genre: "+genreImpot);
		ddump(nbTabs, "Début: "+getDateDebut());
		ddump(nbTabs, "Fin: "+getDateFin());
		ddump(nbTabs, "Principal: "+isPrincipal());
		ddump(nbTabs, "Type: "+typeAutoriteFiscale+" / OFS: "+numeroOfsAutoriteFiscale);
	}

	/**
	 * {@inheritDoc}
	 */
	public ValidationResults validate() {

		ValidationResults results = new ValidationResults();

		// Date de début doit être avant la date de fin
		// Si "date de début" = "date de fin", c'est un cas OK (for qui dure 1 jour)
		// Cette validation n'est pas faite si le for est annulé.
		if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin) && !isAnnule()) {
			results.addError("Le for " + toString() + " possède une date de début qui est après la date de fin: début = " + dateDebut
					+ " fin = " + dateFin + "");
		}

		return results;
	}

	/**
	 * {@inheritDoc}
	 */
	@Transient
	public Validateable getMaster() {
		return tiers;
	}

	/**
	 * Retourne true si le for contient les mêmes informations que celui passé en paramètre.
	 *
	 * Cette méthode ne doit pas être renommée en equals, cela provoquerait des conflits avec Hibernate.
	 */
	public boolean equalsTo(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ForFiscal other = (ForFiscal) obj;
		if (dateDebut == null) {
			if (other.dateDebut != null)
				return false;
		} else if (!dateDebut.equals(other.dateDebut))
			return false;
		if (dateFin == null) {
			if (other.dateFin != null)
				return false;
		} else if (!dateFin.equals(other.dateFin))
			return false;
		if (genreImpot == null) {
			if (other.genreImpot != null)
				return false;
		} else if (!genreImpot.equals(other.genreImpot))
			return false;
		if (numeroOfsAutoriteFiscale == null) {
			if (other.numeroOfsAutoriteFiscale != null)
				return false;
		} else if (!numeroOfsAutoriteFiscale
				.equals(other.numeroOfsAutoriteFiscale))
			return false;
		if (typeAutoriteFiscale == null) {
			if (other.typeAutoriteFiscale != null)
				return false;
		} else if (!typeAutoriteFiscale.equals(other.typeAutoriteFiscale))
			return false;
		return isAnnule() == other.isAnnule();
	}
}
