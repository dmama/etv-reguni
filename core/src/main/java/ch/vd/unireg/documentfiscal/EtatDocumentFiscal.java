package ch.vd.unireg.documentfiscal;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

/**
 * @author jec
 */
@Entity
@Table(name = "ETAT_DOCUMENT_FISCAL")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "ETAT_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class EtatDocumentFiscal<E extends EtatDocumentFiscal> extends HibernateEntity implements DateRange, Comparable<EtatDocumentFiscal>, LinkedEntity {

	private Long id;
	private RegDate dateObtention;
	private DocumentFiscal documentFiscal;
	private TypeEtatDocumentFiscal etat;

	public EtatDocumentFiscal() {
		etat = getType();
	}

	public EtatDocumentFiscal(RegDate dateObtention) {
		etat = getType();
		this.dateObtention = dateObtention;

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

	@Column(name = "TYPE", length = LengthConstants.TYPE_ETAT_DOC, nullable = false)
	@Enumerated(value = EnumType.STRING)
	public TypeEtatDocumentFiscal getEtat() {
		return etat;
	}

	public void setEtat(TypeEtatDocumentFiscal etat) {
		this.etat = etat;
	}

	@Transient
	public abstract TypeEtatDocumentFiscal getType();

	@Transient
	public Comparator<EtatDeclaration> getComparator() {
		return new Comparator<>();
	}

	/**
	 * @return the dateObtention
	 */
	@Column(name = "DATE_OBTENTION", nullable = false)
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateObtention() {
		return dateObtention;
	}

	/**
	 * @param theDateObtention the dateObtention to set
	 */
	public void setDateObtention(RegDate theDateObtention) {
		dateObtention = theDateObtention;
	}

	/**
	 * @return the document
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	@ForeignKey(name = "FK_ET_DOCFISC_DOCFISC_ID")
	@Index(name = "IDX_ET_DOCFISC_DOCFISC_ID", columnNames = "DOCUMENT_FISCAL_ID")
	public DocumentFiscal getDocumentFiscal() {
		return documentFiscal;
	}

	/**
	 * @param theDocumentFiscal the declaration to set
	 */
	public void setDocumentFiscal(DocumentFiscal theDocumentFiscal) {
		documentFiscal = theDocumentFiscal;
	}

	/**
	 * Compare d'apres la date de EtatDeclaration
	 *
	 * @see Comparable#compareTo(Object)
	 */
	@Override
	public int compareTo(@NotNull EtatDocumentFiscal other) {
		// Descending =>   * -1
		return - DateRangeComparator.compareRanges(this, other);
	}

	/**
	 * return date obtention
	 */
	@Override
	@Transient
	public RegDate getDateDebut() {
		return dateObtention;
	}

	/**
	 * return null
	 */
	@Override
	@Transient
	public RegDate getDateFin() {
		// On n'a pas de date de fin, on renvoie null
		return null;
	}

	@Override
	@Transient
	public boolean isValidAt(RegDate date) {
		return true;
	}

	@Override
	public String toString() {
		final String dateObtentionStr = dateObtention != null ? RegDateHelper.dateToDisplayString(dateObtention) : "?";
		return String.format("%s le %s", getClass().getSimpleName(), dateObtentionStr);
	}

	@Override
	@Transient
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return documentFiscal == null ? null : Collections.singletonList(documentFiscal);
	}

	/**
	 * Permet de trier les états d'un document fiscal du plus ancien au plus récent. En cas de plusieurs états tombant le même jour, des règles
	 * de métier permettent de les départager.
	 *
	 * @author Manuel Siggen <manuel.siggen@vd.ch>
	 */
	public static class Comparator<T extends EtatDocumentFiscal> implements java.util.Comparator<T> {

		@Override
		public int compare(T o1, T o2) {

			final RegDate dateObtention1 = o1.getDateObtention();
			final RegDate dateObtention2 = o2.getDateObtention();

			// [SIFISC-17758] dans l'écran SuperGRA, quand on ajoute à la main un état, la date d'obtention n'est pas encore assignée...
			if (dateObtention1 == null || dateObtention2 == null) {
				if (dateObtention1 != dateObtention2) {
					return dateObtention1 == null ? -1 : 1;
				}
			}

			if (dateObtention1 != dateObtention2) {
				// cas normal
				return dateObtention1.compareTo(dateObtention2);
			}

			// cas exceptionnel : deux états obtenu le même jour.
			final TypeEtatDocumentFiscal etat1 = o1.getEtat();
			final TypeEtatDocumentFiscal etat2 = o2.getEtat();

			// [SIFISC-17758] dans l'écran SuperGRA, les états ne sont pas toujours renseignés quand on lance la validation
			if (etat1 == null || etat2 == null) {
				if (etat1 != etat2) {
					return etat1 == null ? -1 : 1;
				}
			}

			// l'ordre est simplement l'ordre logique de l'enum
			if (etat1 != etat2) {
				return etat1.compareTo(etat2);
			}

			// s'il y a des états identiques annulés aux mêmes dates, on mets l'état annulé avant
			final boolean e1annule = o1.isAnnule();
			final boolean e2annule = o2.isAnnule();
			return e1annule == e2annule ? 0 : (e1annule ? -1 : 1);
		}
	}

}
