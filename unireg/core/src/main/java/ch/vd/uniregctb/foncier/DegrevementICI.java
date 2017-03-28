package ch.vd.uniregctb.foncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.Duplicable;

@Entity
@DiscriminatorValue(value = "DegrevementICI")
public class DegrevementICI extends AllegementFoncier implements Duplicable<DegrevementICI> {

	private DonneesUtilisation location;
	private DonneesUtilisation propreUsage;
	private DonneesLoiLogement loiLogement;

	public DegrevementICI() {
	}

	private DegrevementICI(DegrevementICI src) {
		super(src);
		this.location = Optional.ofNullable(src.location).map(DonneesUtilisation::new).orElse(null);
		this.propreUsage = Optional.ofNullable(src.propreUsage).map(DonneesUtilisation::new).orElse(null);
		this.loiLogement = Optional.ofNullable(src.loiLogement).map(DonneesLoiLogement::new).orElse(null);
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
}
