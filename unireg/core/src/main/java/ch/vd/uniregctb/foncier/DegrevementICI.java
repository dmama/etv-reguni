package ch.vd.uniregctb.foncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
@DiscriminatorValue(value = "DegrevementICI")
public class DegrevementICI extends AllegementFoncier {

	private DonneesUtilisation location;
	private DonneesUtilisation propreUsage;
	private DonneesLoiLogement loiLogement;

	public DegrevementICI() {
	}

	@Transient
	@Override
	public TypeImpot getTypeImpot() {
		return TypeImpot.ICI;
	}

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

	public void setLocation(DonneesUtilisation location) {
		this.location = location;
	}

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

	public void setPropreUsage(DonneesUtilisation propreUsage) {
		this.propreUsage = propreUsage;
	}

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "dateOctroi", column = @Column(name = "DEG_LL_OCTROI")),
			@AttributeOverride(name = "dateEcheance", column = @Column(name = "DEG_LL_ECHEANCE")),
			@AttributeOverride(name = "pourcentageCaractereSocial", column = @Column(name = "DEG_LL_CARAC_SOCIAL_POURCENT", precision = 5, scale = 2))
	})
	public DonneesLoiLogement getLoiLogement() {
		return loiLogement;
	}

	public void setLoiLogement(DonneesLoiLogement loiLogement) {
		this.loiLogement = loiLogement;
	}
}
