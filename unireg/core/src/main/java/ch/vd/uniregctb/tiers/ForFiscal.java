package ch.vd.uniregctb.tiers;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
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
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessComparable;
import ch.vd.uniregctb.common.ComparisonHelper;
import ch.vd.uniregctb.common.Duplicable;
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
 *                         | DIPLOMATE_SUISSE   | IMMEUBLE_PRIVE      	 | ADMINISTRATEUR                     |                               |                        |
 *                         | DIPLOMATE_ETRANGER | SEJOUR_SAISONNIER      | CREANCIER_HYPOTHECAIRE             |                               |                        |
 *                         |                    | DIRIGEANT_SOCIETE      | PRESTATION_PREVOYANCE              |                               |                        |
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
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_OUVERTURE", nullable = false)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FERMETURE")),
		@AttributeOverride(name = "numeroOfsAutoriteFiscale", column = @Column(name = "NUMERO_OFS", nullable = false)),
		@AttributeOverride(name = "typeAutoriteFiscale", column = @Column(name = "TYPE_AUT_FISC", nullable = false, length = LengthConstants.FOR_AUTORITEFISCALE))
})
public abstract class ForFiscal extends LocalisationDatee implements Comparable<ForFiscal>, Duplicable<ForFiscal>, BusinessComparable<ForFiscal>, LinkedEntity {

	/**
	 * The ID
	 */
	private Long id;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc --> Indique l'impôt auquel un contribuable est soumis : - ICC/IFD (dit aussi impôt
	 * ordinaire ou IRF/IBC) - GI - DM - ...
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8ZVx9Edygsbnw9h5bVw"
	 */
	private GenreImpot genreImpot;

	private Tiers tiers;

	public ForFiscal() {
	}

	public ForFiscal(RegDate ouverture, RegDate fermeture, GenreImpot genreImpot, Integer numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale) {
		super(ouverture, fermeture, typeAutoriteFiscale, numeroOfsAutoriteFiscale);
		this.genreImpot = genreImpot;
	}

	public ForFiscal(ForFiscal ff) {
		super(ff);
		this.genreImpot = ff.genreImpot;
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
	 * @param theId
	 *            the id to set
	 */
	public void setId(Long theId) {
		this.id = theId;
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
	 * Compare d'apres la date dur for
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(@NotNull ForFiscal o) {
		return getDateDebut().compareTo(o.getDateDebut());
	}

	@Transient
	public boolean isPrincipal() {
		return false;
	}

	@Transient
	public boolean isDebiteur() {
		return false;
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
	 * En cas de date nulle, vérifie la validité du for <b>à la date du jour</b> (et pas le fait que le for n'a pas de date de fin)
	 * @see DateRange#isValidAt(ch.vd.registre.base.date.RegDate)
	 */
	@Override
	public boolean isValidAt(@Nullable RegDate date) {
		return super.isValidAt(date == null ? RegDate.get() : date);
	}

	protected void dumpForDebug(int nbTabs) {
		ddump(nbTabs, "Genre: "+genreImpot);
		ddump(nbTabs, "Début: "+getDateDebut());
		ddump(nbTabs, "Fin: "+getDateFin());
		ddump(nbTabs, "Principal: "+isPrincipal());
		ddump(nbTabs, "Type: "+getTypeAutoriteFiscale()+" / OFS: "+getNumeroOfsAutoriteFiscale());
	}

	/**
	 * Retourne true si le for contient les mêmes informations que celui passé en paramètre.
	 *
	 * Cette méthode ne doit pas être renommée en equals, cela provoquerait des conflits avec Hibernate.
	 */
	public boolean equalsTo(ForFiscal obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		return ComparisonHelper.areEqual(getDateDebut(), obj.getDateDebut())
				&& ComparisonHelper.areEqual(getDateFin(), obj.getDateFin())
				&& ComparisonHelper.areEqual(genreImpot, obj.genreImpot)
				&& ComparisonHelper.areEqual(getNumeroOfsAutoriteFiscale(), obj.getNumeroOfsAutoriteFiscale())
				&& ComparisonHelper.areEqual(getTypeAutoriteFiscale(), obj.getTypeAutoriteFiscale())
				&& ComparisonHelper.areEqual(isAnnule(), obj.isAnnule());
	}

	@Override
	@Transient
	public List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled) {
		return tiers == null ? null : Collections.singletonList(tiers);
	}
}
