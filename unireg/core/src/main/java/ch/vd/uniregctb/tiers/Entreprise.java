package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

/**
 * Entreprise ou l'etablissement connue du registre des personnes morales de
 * l'ACI
 */
@Entity
@DiscriminatorValue("Entreprise")
public class Entreprise extends ContribuableImpositionPersonnesMorales {

	// Numéros migrés depuis SIMPA-PM
	public static final int FIRST_ID = 1;
	public static final int LAST_ID = 999999;

	// Numéros générés pour AutreCommunauté et CollectiviteAdministrative
	public static final int PM_GEN_FIRST_ID = 2000000;
	public static final int PM_GEN_LAST_ID = 2999999;

	/**
	 * Numéro cantonal (= dans RCEnt)
	 */
	private Long numeroEntreprise;

	@Column(name = "NUMERO_ENTREPRISE")
	@Index(name = "IDX_TIERS_NO_ENTREPRISE")
	public Long getNumeroEntreprise() {
		return numeroEntreprise;
	}

	public void setNumeroEntreprise(Long numeroEntreprise) {
		this.numeroEntreprise = numeroEntreprise;
	}

	public Entreprise() {
	}

	public Entreprise(long numero) {
		super(numero);
	}

	@Transient
	@Override
	public NatureTiers getNatureTiers() {
		return NatureTiers.Entreprise;
	}

	@Transient
	@Override
	public TypeTiers getType() {
		return TypeTiers.ENTREPRISE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equalsTo(Tiers obj) {
		return this == obj || super.equalsTo(obj) && getClass() == obj.getClass();
	}
}
