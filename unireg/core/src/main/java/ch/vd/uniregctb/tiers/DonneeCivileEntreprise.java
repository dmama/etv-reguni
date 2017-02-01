package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;

@Entity
@Table(name = "DONNEE_CIVILE_ENTREPRISE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DONNEE_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class DonneeCivileEntreprise extends HibernateDateRangeEntity implements LinkedEntity {

	private Long id;
	private Entreprise entreprise;

	public DonneeCivileEntreprise() {
	}

	public DonneeCivileEntreprise(RegDate dateDebut, RegDate dateFin) {
		super(dateDebut, dateFin);
	}

	public DonneeCivileEntreprise(DonneeCivileEntreprise source) {
		super(source.getDateDebut(), source.getDateFin());
		this.setEntreprise(source.getEntreprise());
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

	@Override
	public List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled) {
		return entreprise == null ? null : Collections.singletonList(entreprise);
	}
}
