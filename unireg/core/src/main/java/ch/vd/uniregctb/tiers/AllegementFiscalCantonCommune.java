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
		ARTICLE_91_LI,
		ARTICLE_90IG_LI,
		ARTICLE_90H_LI,
		ARTICLE_90C_LI,
		ARTICLE_90E_LI,
		SOCIETE_DE_BASE,
		IMMEUBLE_SOC_IMMOB_SUBV,
		EXONERATION_SPECIALE,
		ARTICLE_90F_LI,
		SOCIETE_MEXICAINE
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
