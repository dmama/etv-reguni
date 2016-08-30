package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

import ch.vd.fidor.xml.regimefiscal.v1.RegimeFiscal;

public class TypeRegimeFiscalImpl implements TypeRegimeFiscal, Serializable {

	private static final long serialVersionUID = 8700466175917224034L;

	private final String code;
	private final String libelle;
	private final boolean cantonal;
	private final boolean federal;
	private final boolean pourPM;
	private final boolean pourAPM;
	private final boolean defaultPourPM;
	private final boolean defaultPourAPM;
	private final Integer premierePeriodeFiscaleValidite;
	private final Integer dernierePeriodeFiscaleValidite;

	public static TypeRegimeFiscal get(RegimeFiscal regime) {
		if (regime == null) {
			return null;
		}
		return new TypeRegimeFiscalImpl(regime);
	}

	private TypeRegimeFiscalImpl(RegimeFiscal regime) {
		this.code = regime.getCode();
		this.libelle = String.format("%s - %s", regime.getCode(), regime.getLibelle());
		this.cantonal = regime.isCantonal();
		this.federal = regime.isFederal();
		this.pourAPM = regime.isAPM();
		this.pourPM = regime.isPM();
		this.defaultPourAPM = regime.isDefaultAPM();
		this.defaultPourPM = regime.isDefaultPM();
		this.premierePeriodeFiscaleValidite = regime.getPeriodeFiscaleDebutValidite();
		this.dernierePeriodeFiscaleValidite = regime.getPeriodeFiscaleFinValidite();
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getLibelle() {
		return libelle;
	}

	@Override
	public boolean isCantonal() {
		return cantonal;
	}

	@Override
	public boolean isFederal() {
		return federal;
	}

	@Override
	public boolean isPourPM() {
		return pourPM;
	}

	@Override
	public boolean isPourAPM() {
		return pourAPM;
	}

	@Override
	public boolean isDefaultPourPM() {
		return defaultPourPM;
	}

	@Override
	public boolean isDefaultPourAPM() {
		return defaultPourAPM;
	}

	@Override
	public Integer getPremierePeriodeFiscaleValidite() {
		return premierePeriodeFiscaleValidite;
	}

	@Nullable
	@Override
	public Integer getDernierePeriodeFiscaleValidite() {
		return dernierePeriodeFiscaleValidite;
	}

	@Override
	public String toString() {
		return "TypeRegimeFiscalImpl{" +
				"code='" + code + '\'' +
				", libelle='" + libelle + '\'' +
				", cantonal=" + cantonal +
				", federal=" + federal +
				", pourPM=" + pourPM +
				", pourAPM=" + pourAPM +
				", defaultPourPM=" + defaultPourPM +
				", defaultPourAPM=" + defaultPourAPM +
				", premierePeriodeFiscaleValidite=" + premierePeriodeFiscaleValidite +
				", dernierePeriodeFiscaleValidite=" + dernierePeriodeFiscaleValidite +
				'}';
	}
}
