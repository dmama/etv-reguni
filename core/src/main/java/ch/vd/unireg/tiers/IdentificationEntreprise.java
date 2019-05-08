package ch.vd.unireg.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;

@Entity
@Table(name = "IDENTIFICATION_ENTREPRISE", indexes = @Index(name = "IDX_ID_ENTREPRISE_TIERS_ID", columnList = "TIERS_ID"))
public class IdentificationEntreprise extends HibernateEntity implements LinkedEntity {

	private Long id;
	private Contribuable ctb;
	private String numeroIde;

	public IdentificationEntreprise() {
	}

	public IdentificationEntreprise(String numeroIde) {
		this.numeroIde = numeroIde;
	}

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

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "TIERS_ID", insertable = false, updatable = false, nullable = false)
	public Contribuable getCtb() {
		return ctb;
	}

	public void setCtb(Contribuable ctb) {
		this.ctb = ctb;
	}

	@Column(name = "NUMERO_IDE", length = LengthConstants.IDENT_ENTREPRISE_IDE, nullable = false)
	public String getNumeroIde() {
		return numeroIde;
	}

	public void setNumeroIde(String numeroIde) {
		this.numeroIde = numeroIde;
	}

	@Override
	@Transient
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return ctb == null ? null : Collections.singletonList(ctb);
	}
}
