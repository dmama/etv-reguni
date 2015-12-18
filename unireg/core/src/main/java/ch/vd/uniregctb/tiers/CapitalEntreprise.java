package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;

@Entity
@Table(name = "CAPITAL_ENTREPRISE")
public class CapitalEntreprise extends HibernateDateRangeEntity implements LinkedEntity {

	private Long id;
	private Entreprise entreprise;
	private MontantMonetaire montant;

	/**
	 * Nécessaire pour Hibernate (et SuperGRA...)
	 */
	public CapitalEntreprise() {
	}

	public CapitalEntreprise(RegDate dateDebut, RegDate dateFin, MontantMonetaire montant) {
		super(dateDebut, dateFin);
		this.montant = montant;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@Column(name = "ID", nullable = false, updatable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "ENTREPRISE_ID", nullable = false)
	public Entreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(Entreprise entreprise) {
		this.entreprise = entreprise;
	}

	@Embedded
	public MontantMonetaire getMontant() {
		return montant;
	}

	public void setMontant(MontantMonetaire montant) {
		this.montant = montant;
	}

	@Override
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return entreprise == null ? null : Collections.singletonList(entreprise);
	}
}
