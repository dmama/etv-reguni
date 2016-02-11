package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;

@Entity
public abstract class AllegementFiscalCantonCommune extends AllegementFiscal {

	public enum Type {
		TEMPORAIRE_91LI,
		EXONERATION_90LI,
		SOCIETE_SERVICE,
		IMMEUBLE_SI_SUBVENTIONNEE,
		EXONERATION_SPECIALE,
		TRANSPORTS_CONCESSIONNES,
		HOLDING_IMMEUBLE
	}

	private Type type;

	public AllegementFiscalCantonCommune() {
	}

	public AllegementFiscalCantonCommune(RegDate dateDebut, @Nullable RegDate dateFin, @Nullable BigDecimal pourcentageAllegement, TypeImpot typeImpot, Type type) {
		super(dateDebut, dateFin, pourcentageAllegement, typeImpot);
		this.type = type;
	}

	public AllegementFiscalCantonCommune(AllegementFiscalCantonCommune src) {
		super(src);
		this.type = src.type;
	}

	@Column(name = "TYPE_ALLEGEMENT_ICC", length = LengthConstants.ALLEGEMENT_FISCAL_TYPE_ICC)
	@Enumerated(value = EnumType.STRING)
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
}
