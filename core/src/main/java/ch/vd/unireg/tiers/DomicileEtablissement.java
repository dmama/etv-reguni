package ch.vd.unireg.tiers;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
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

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.type.TypeAutoriteFiscale;

@Entity
@Table(name = "DOMICILE_ETABLISSEMENT", indexes = @Index(name = "IDX_DOM_ETB_ETB_ID", columnList = "ETABLISSEMENT_ID"))
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = false)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN")),
		@AttributeOverride(name = "numeroOfsAutoriteFiscale", column = @Column(name = "NUMERO_OFS_AUT_FISC", nullable = false)),
		@AttributeOverride(name = "typeAutoriteFiscale", column = @Column(name = "TYPE_AUT_FISC", nullable = false, length = LengthConstants.FOR_AUTORITEFISCALE))
})
public class DomicileEtablissement extends LocalisationDatee implements LinkedEntity, Duplicable<DomicileEtablissement> {

	private Long id;
	private Etablissement etablissement;

	public DomicileEtablissement() {
	}

	public DomicileEtablissement(RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noOfsAutoriteFiscale, Etablissement etablissement) {
		super(dateDebut, dateFin, typeAutoriteFiscale, noOfsAutoriteFiscale);
		this.etablissement = etablissement;
	}

	public DomicileEtablissement(DomicileEtablissement source) {
		super(source);
		this.etablissement = source.etablissement;
	}

	@Transient
	@Override
	public Object getKey() {
		return getId();
	}

	@Id
	@Column(name = "ID")
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "ETABLISSEMENT_ID", foreignKey = @ForeignKey(name = "FK_DOM_ETB_ETB_ID"))
	public Etablissement getEtablissement() {
		return etablissement;
	}

	public void setEtablissement(Etablissement etablissement) {
		this.etablissement = etablissement;
	}

	@Override
	public DomicileEtablissement duplicate() {
		return new DomicileEtablissement(this);
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return etablissement == null ? null : Collections.singletonList(etablissement);
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return super.isValidAt(date == null ? RegDate.get() : date);
	}
}
