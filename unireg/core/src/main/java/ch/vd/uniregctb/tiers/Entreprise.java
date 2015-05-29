package ch.vd.uniregctb.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.ComparisonHelper;

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

	private Set<RegimeFiscal> regimesFiscaux;
	private Set<DonneesRegistreCommerce> donneesRC;
	private Set<AllegementFiscal> allegementsFiscaux;

	@Column(name = "NUMERO_ENTREPRISE")
	@Index(name = "IDX_TIERS_NO_ENTREPRISE")
	public Long getNumeroEntreprise() {
		return numeroEntreprise;
	}

	public void setNumeroEntreprise(Long numeroEntreprise) {
		this.numeroEntreprise = numeroEntreprise;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "ENTREPRISE_ID")
	public Set<RegimeFiscal> getRegimesFiscaux() {
		return regimesFiscaux;
	}

	public void setRegimesFiscaux(Set<RegimeFiscal> regimesFiscaux) {
		this.regimesFiscaux = regimesFiscaux;
	}

	public void addRegimeFiscal(RegimeFiscal regimeFiscal) {
		if (regimeFiscal.getEntreprise() != null && regimeFiscal.getEntreprise() != this) {
			throw new IllegalArgumentException("Ce régime fiscal a déjà été associé à une autre entreprise");
		}

		if (this.regimesFiscaux == null) {
			this.regimesFiscaux = new HashSet<>();
		}
		this.regimesFiscaux.add(regimeFiscal);
		regimeFiscal.setEntreprise(this);
	}

	@Transient
	@NotNull
	public List<RegimeFiscal> getRegimesFiscauxNonAnnulesTries() {
		final List<RegimeFiscal> nonAnnules = AnnulableHelper.sansElementsAnnules(regimesFiscaux);
		Collections.sort(nonAnnules, new DateRangeComparator<RegimeFiscal>() {
			@Override
			public int compare(RegimeFiscal o1, RegimeFiscal o2) {
				int comparison = super.compare(o1, o2);
				if (comparison == 0) {
					comparison = o1.getPortee().compareTo(o2.getPortee());
				}
				return comparison;
			}
		});
		return nonAnnules;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "ENTREPRISE_ID")
	public Set<DonneesRegistreCommerce> getDonneesRC() {
		return donneesRC;
	}

	public void setDonneesRC(Set<DonneesRegistreCommerce> donneesRC) {
		this.donneesRC = donneesRC;
	}

	public void addDonneesRC(DonneesRegistreCommerce donneesRC) {
		if (donneesRC.getEntreprise() != null && donneesRC.getEntreprise() != this) {
			throw new IllegalArgumentException("Ces données ont déjà été associées à une autre enteprise");
		}

		if (this.donneesRC == null) {
			this.donneesRC = new HashSet<>();
		}
		this.donneesRC.add(donneesRC);
		donneesRC.setEntreprise(this);
	}

	@Transient
	@NotNull
	public List<DonneesRegistreCommerce> getDonneesRegistreCommerceNonAnnuleesTriees() {
		final List<DonneesRegistreCommerce> nonAnnulees = AnnulableHelper.sansElementsAnnules(donneesRC);
		Collections.sort(nonAnnulees, new DateRangeComparator<DonneesRegistreCommerce>());
		return nonAnnulees;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "ENTREPRISE_ID")
	public Set<AllegementFiscal> getAllegementsFiscaux() {
		return allegementsFiscaux;
	}

	public void setAllegementsFiscaux(Set<AllegementFiscal> allegementsFiscaux) {
		this.allegementsFiscaux = allegementsFiscaux;
	}

	public void addAllegementFiscal(AllegementFiscal af) {
		if (af.getEntreprise() != null && af.getEntreprise() != this) {
			throw new IllegalArgumentException("Cet allègement fiscal est déjà associé à une autre entreprise");
		}

		if (this.allegementsFiscaux == null) {
			this.allegementsFiscaux = new HashSet<>();
		}
		this.allegementsFiscaux.add(af);
		af.setEntreprise(this);
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

	@Override
	public boolean equalsTo(Tiers obj) {
		if (this == obj)
			return true;
		if (!super.equalsTo(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;

		final Entreprise other = (Entreprise) obj;
		return ComparisonHelper.areEqual(numeroEntreprise, other.numeroEntreprise);
	}
}
