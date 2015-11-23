package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

@Entity
@Table(name = "ETAT_ENTREPRISE")
public class EtatEntreprise extends HibernateEntity implements LinkedEntity, Comparable<EtatEntreprise> {

	private Long id;
	private RegDate dateObtention;
	private TypeEtatEntreprise type;
	private Entreprise entreprise;

	@Transient
	@Override
	public Long getKey() {
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

	@Column(name = "DATE_OBTENTION", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateObtention() {
		return dateObtention;
	}

	public void setDateObtention(RegDate dateObtention) {
		this.dateObtention = dateObtention;
	}

	@Column(name = "TYPE_ETAT", length = LengthConstants.ETATENT_ETAT, nullable = false)
	@Enumerated(value = EnumType.STRING)
	public TypeEtatEntreprise getType() {
		return type;
	}

	public void setType(TypeEtatEntreprise type) {
		this.type = type;
	}

	@ManyToOne
	@JoinColumn(name = "ENTREPRISE_ID")
	public Entreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(Entreprise entreprise) {
		this.entreprise = entreprise;
	}

	@Override
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return entreprise == null ? null : Collections.singletonList(entreprise);
	}

	@Override
	public int compareTo(EtatEntreprise o) {
		int comparison = NullDateBehavior.EARLIEST.compare(dateObtention, o.dateObtention);
		if (comparison == 0) {
			comparison = Long.compare(id, o.id);
		}
		return comparison;
	}

	@Override
	public String toString() {
		return String.format("Etat %s obtenu le %s", type, RegDateHelper.dateToDisplayString(dateObtention));
	}
}
