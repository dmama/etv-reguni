package ch.vd.unireg.foncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.Optional;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.Duplicable;

@Entity
@DiscriminatorValue(value = "DegrevementICI")
public class DegrevementICI extends AllegementFoncier implements Duplicable<DegrevementICI> {

	// big décimals avec précision de 2 décimales
	private static final BigDecimal ZERO = BigDecimal.valueOf(0L, 2);
	private static final BigDecimal CENT = BigDecimal.valueOf(10000L, 2);

	private DonneesUtilisation location;
	private DonneesUtilisation propreUsage;
	private DonneesLoiLogement loiLogement;

	/**
	 * Flag setté par l'intégration automatique des données en provenance de e-Dégrèvement si l'un des champs
	 * n'est pas intégrable (= mauvais format, valeur hors plage...)
	 */
	private Boolean nonIntegrable;

	public DegrevementICI() {
	}

	private DegrevementICI(DegrevementICI src) {
		super(src);
		this.location = Optional.ofNullable(src.location).map(DonneesUtilisation::new).orElse(null);
		this.propreUsage = Optional.ofNullable(src.propreUsage).map(DonneesUtilisation::new).orElse(null);
		this.loiLogement = Optional.ofNullable(src.loiLogement).map(DonneesLoiLogement::new).orElse(null);
		this.nonIntegrable = src.nonIntegrable;
	}

	@Override
	public DegrevementICI duplicate() {
		return new DegrevementICI(this);
	}

	@Transient
	@Override
	public TypeImpot getTypeImpot() {
		return TypeImpot.ICI;
	}

	@Nullable
	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "revenu", column = @Column(name = "DEG_LOC_REVENU")),
			@AttributeOverride(name = "volume", column = @Column(name = "DEG_LOC_VOLUME")),
			@AttributeOverride(name = "surface", column = @Column(name = "DEG_LOC_SURFACE")),
			@AttributeOverride(name = "pourcentage", column = @Column(name = "DEG_LOC_POURCENT", precision = 5, scale = 2)),
			@AttributeOverride(name = "pourcentageArrete", column = @Column(name = "DEG_LOC_POURCENT_ARRETE", precision = 5, scale = 2))
	})
	public DonneesUtilisation getLocation() {
		return location;
	}

	public void setLocation(@Nullable DonneesUtilisation location) {
		this.location = location;
	}

	@Nullable
	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "revenu", column = @Column(name = "DEG_PRUS_REVENU")),
			@AttributeOverride(name = "volume", column = @Column(name = "DEG_PRUS_VOLUME")),
			@AttributeOverride(name = "surface", column = @Column(name = "DEG_PRUS_SURFACE")),
			@AttributeOverride(name = "pourcentage", column = @Column(name = "DEG_PRUS_POURCENT", precision = 5, scale = 2)),
			@AttributeOverride(name = "pourcentageArrete", column = @Column(name = "DEG_PRUS_POURCENT_ARRETE", precision = 5, scale = 2))
	})
	public DonneesUtilisation getPropreUsage() {
		return propreUsage;
	}

	public void setPropreUsage(@Nullable DonneesUtilisation propreUsage) {
		this.propreUsage = propreUsage;
	}

	@Nullable
	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "dateOctroi", column = @Column(name = "DEG_LL_OCTROI")),
			@AttributeOverride(name = "dateEcheance", column = @Column(name = "DEG_LL_ECHEANCE")),
			@AttributeOverride(name = "pourcentageCaractereSocial", column = @Column(name = "DEG_LL_CARAC_SOCIAL_POURCENT", precision = 5, scale = 2)),
			@AttributeOverride(name = "controleOfficeLogement", column = @Column(name = "DEG_LL_CTRL_OFFICE_LOGEMENT"))
	})
	public DonneesLoiLogement getLoiLogement() {
		return loiLogement;
	}

	public void setLoiLogement(@Nullable DonneesLoiLogement loiLogement) {
		this.loiLogement = loiLogement;
	}

	@Column(name = "DEG_NON_INTEGRABLE")
	public Boolean getNonIntegrable() {
		return nonIntegrable;
	}

	@Transient
	public boolean isNonIntegrable() {
		return nonIntegrable != null && nonIntegrable;
	}

	public void setNonIntegrable(Boolean nonIntegrable) {
		this.nonIntegrable = nonIntegrable;
	}

	/**
	 * @return la valeur (entre 0 et 100) calculée à partir des valeurs arrêtées du pourcentage de dégrèvement global (en prenant
	 * en compte la part de propre usage complète, et la part de location déterminée par le contrôle sur la loi sur le logement)
	 */
	@Transient
	@NotNull
	public BigDecimal getPourcentageDegrevement() {
		final Optional<BigDecimal> loc = Optional.ofNullable(this.location).map(DonneesUtilisation::getPourcentageArrete);
		final Optional<BigDecimal> pu = Optional.ofNullable(this.propreUsage).map(DonneesUtilisation::getPourcentageArrete);

		if (!loc.isPresent() && !pu.isPresent()) {
			// [SIFISC-26123] si aucune valeur n'est arrêtée, le dégrèvement final doit être 0% (et non pas une valeur nulle)
			return ZERO;
		}

		// [SIFISC-27250] note: si l'un des deux valeurs manque, il ne faut pas la déduire à partir de l'autre
		final BigDecimal location = loc.orElse(ZERO);
		final BigDecimal propreUsage = pu.orElse(ZERO);
		if (propreUsage.compareTo(CENT) >= 0) {
			// de toute façon, on ne pourra pas faire plus...
			return CENT;
		}

		// prise en compte de la loi sur le logement
		final BigDecimal caractereSocial = Optional.ofNullable(this.loiLogement)
				.filter(l -> BooleanUtils.isTrue(l.getControleOfficeLogement()))
				.map(DonneesLoiLogement::getPourcentageCaractereSocial)
				.orElse(ZERO);

		// dégrèvement = PU + (LL * LOC)
		final BigDecimal social = caractereSocial.multiply(location).movePointLeft(2);
		final BigDecimal degrevement = propreUsage.add(social);

		// limitation à la plage autorisée 0-100
		if (degrevement.compareTo(ZERO) <= 0) {
			return ZERO;
		}
		else if (degrevement.compareTo(CENT) >= 0) {
			return CENT;
		}
		else {
			return degrevement.setScale(2, BigDecimal.ROUND_HALF_EVEN);
		}

	}
}
