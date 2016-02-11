package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;
import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;

@Entity
@DiscriminatorValue("CONFEDERATION")
public class AllegementFiscalConfederation extends AllegementFiscal {

	public enum Type {
		TEMPORAIRE_91LI,
		EXONERATION_90LI,
		IMMEUBLE_SI_SUBVENTIONNEE,
		EXONERATION_SPECIALE,
		TRANSPORTS_CONCESSIONNES
	}

	private Type type;

	public AllegementFiscalConfederation() {
	}

	public AllegementFiscalConfederation(RegDate dateDebut, @Nullable RegDate dateFin, @Nullable BigDecimal pourcentageAllegement, TypeImpot typeImpot, Type type) {
		super(dateDebut, dateFin, pourcentageAllegement, typeImpot);
		this.type = type;
	}

	public AllegementFiscalConfederation(AllegementFiscalConfederation src) {
		super(src);
		this.type = src.type;
	}

	@Transient
	@Override
	public TypeCollectivite getTypeCollectivite() {
		return TypeCollectivite.CONFEDERATION;
	}

	@Transient
	@Override
	public AllegementFiscalConfederation duplicate() {
		return new AllegementFiscalConfederation(this);
	}

	@Column(name = "TYPE_ALLEGEMENT_IFD", length = LengthConstants.ALLEGEMENT_FISCAL_TYPE_IFD)
	@Enumerated(value = EnumType.STRING)
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
}
