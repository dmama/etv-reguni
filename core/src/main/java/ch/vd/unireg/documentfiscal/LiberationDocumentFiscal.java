package ch.vd.unireg.documentfiscal;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;


@Entity
@Table(name = "LIBERATION_DOCUMENT_FISCAL")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "LIBERATION_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class LiberationDocumentFiscal extends HibernateEntity implements Comparable<LiberationDocumentFiscal>, LinkedEntity {

	private Long id;
	private DocumentFiscal documentFiscal;
	private RegDate dateLiberation;
	private String businessId;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	public void setId(Long theId) {
		this.id = theId;
	}

	@Column(name = "DATE_LIBERATION")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateLiberation() {
		return dateLiberation;
	}

	public void setDateLiberation(RegDate dateLiberation) {
		this.dateLiberation = dateLiberation;
	}

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	@ForeignKey(name = "FK_LIB_DOCFISC_DOCFISC_ID")
	@Index(name = "IDX_LIB_DOCFISC_DOCFISC_ID", columnNames = "DOCUMENT_FISCAL_ID")
	public DocumentFiscal getDocumentFiscal() {
		return documentFiscal;
	}

	public void setDocumentFiscal(DocumentFiscal theDocumentFiscal) {
		documentFiscal = theDocumentFiscal;
	}

	@Override
	public int compareTo(@NotNull LiberationDocumentFiscal liberationDocumentFiscal) {
		final RegDate dateLiberation = liberationDocumentFiscal.getDateLiberation();
		return dateLiberation.compareTo(dateLiberation);
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return getDocumentFiscal() == null ? null : Collections.singletonList(getDocumentFiscal());
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}
	@Column(name = "MSG_LIB_BUSINESSID", length = LengthConstants.DI_LIBERATION)
	public String getBusinessId() {
		return businessId;
	}


	/**
	 * Comparateur qui trie les liberations de déclaration du plus ancien au plus récent.
	 */
	public static class Comparator implements java.util.Comparator<LiberationDocumentFiscal> {
		@Override
		public int compare(LiberationDocumentFiscal o1, LiberationDocumentFiscal o2) {
			// de la plus petite à la plus grande
			return NullDateBehavior.EARLIEST.compare(o1.getDateLiberation(), o2.getDateLiberation());
		}
	}
}
