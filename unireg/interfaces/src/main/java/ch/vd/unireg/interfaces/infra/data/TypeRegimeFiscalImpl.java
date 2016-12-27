package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.fidor.xml.regimefiscal.v1.RegimeFiscal;
import ch.vd.uniregctb.common.CollectionsUtils;

public class TypeRegimeFiscalImpl implements TypeRegimeFiscal, Serializable {

	private static final long serialVersionUID = -2020442250107554886L;

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
	private final List<PlagePeriodesFiscales> exonerations;

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

		if (regime.getExoneration() != null && !regime.getExoneration().isEmpty()) {
			exonerations = regime.getExoneration().stream()
					.filter(Objects::nonNull)
					.map(exo -> new PlagePeriodesFiscales(exo.getPeriodeFiscaleDebutValidite(), exo.getPeriodeFiscaleFinValidite()))
					.collect(Collectors.toList());
		}
		else {
			exonerations = Collections.emptyList();
		}
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
	public boolean isExoneration(int periodeFiscale) {
		return exonerations.stream()
				.anyMatch(exo -> exo.isDansPlage(periodeFiscale));
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
				", exonerations=[" + CollectionsUtils.toString(exonerations,
				                                               exo -> String.format("%d-%s", exo.getPeriodeDebut(), exo.getPeriodeFin() == null ? "?" : exo.getPeriodeFin()),
				                                               ", ",
				                                               StringUtils.EMPTY) + "]" +
				'}';
	}
}
