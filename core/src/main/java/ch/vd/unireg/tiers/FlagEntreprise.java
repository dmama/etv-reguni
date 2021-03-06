package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.type.GroupeFlagsEntreprise;
import ch.vd.unireg.type.TypeFlagEntreprise;

/**
 * Une durée de validité (depuis une année jusqu'à une autre, cette dernière étant optionnelle)
 * d'un flag/état/attribut associé à une entreprise
 */
@Entity
@Table(name = "FLAG_ENTREPRISE")
public class FlagEntreprise extends HibernateDateRangeEntity implements LinkedEntity {

	private Long id;
	private TypeFlagEntreprise type;
	private Entreprise entreprise;

	public FlagEntreprise() {
	}

	public FlagEntreprise(TypeFlagEntreprise type, RegDate dateDebut, RegDate dateFin) {
		super(dateDebut, dateFin);
		this.type = type;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	@Column(name = "ID", nullable = false, updatable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Transient
	public GroupeFlagsEntreprise getGroupe() {
		return type == null ? null : type.getGroupe();
	}

	@Column(name = "FLAG", length = LengthConstants.FLAG_ENTREPRISE_TYPE, nullable = false)
	@Enumerated(EnumType.STRING)
	public TypeFlagEntreprise getType() {
		return type;
	}

	public void setType(TypeFlagEntreprise type) {
		this.type = type;
	}

	@ManyToOne
	@JoinColumn(name = "ENTREPRISE_ID", foreignKey = @ForeignKey(name = "FK_FLAG_ENTRP_ID"))
	public Entreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(Entreprise entreprise) {
		this.entreprise = entreprise;
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return entreprise == null ? null : Collections.singletonList(entreprise);
	}
}
