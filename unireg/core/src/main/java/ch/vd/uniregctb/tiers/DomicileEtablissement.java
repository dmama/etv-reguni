package ch.vd.uniregctb.tiers;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@Entity
@Table(name = "DOMICILE_ETABLISSEMENT")
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
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "ETABLISSEMENT_ID")
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
	public List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled) {
		return etablissement == null ? null : Collections.singletonList(etablissement);
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return super.isValidAt(date == null ? RegDate.get() : date);
	}
}
