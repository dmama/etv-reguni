package ch.vd.unireg.tiers;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessComparable;
import ch.vd.unireg.common.ComparisonHelper;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.TypeAutoriteFiscale;

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
@Table(name = "FOR_FISCAL", indexes = @Index(name = "IDX_FF_TIERS_ID", columnList = "TIERS_ID"))
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
	 * Indique l'impôt auquel un contribuable est soumis : - ICC/IFD (dit aussi impôt ordinaire ou IRF/IBC) - GI - DM - ...
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
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
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

	@Column(name = "GENRE_IMPOT", nullable = false, length = LengthConstants.FOR_GENRE)
	@Type(type = "ch.vd.unireg.hibernate.GenreImpotUserType")
	public GenreImpot getGenreImpot() {
		return genreImpot;
	}

	public void setGenreImpot(GenreImpot theGenreImpot) {
		genreImpot = theGenreImpot;
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
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return tiers == null ? null : Collections.singletonList(tiers);
	}
}
