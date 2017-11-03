package ch.vd.uniregctb.tiers;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessComparable;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.EntityKey;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

@Entity
@Table(name = "RAPPORT_ENTRE_TIERS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "RAPPORT_ENTRE_TIERS_TYPE", discriminatorType = DiscriminatorType.STRING)
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = true)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN", nullable = true))
})
public abstract class RapportEntreTiers extends HibernateDateRangeEntity implements Duplicable<RapportEntreTiers>, LinkedEntity, BusinessComparable<RapportEntreTiers> {

	/**
	 * The ID
	 */
	private Long id;
	private Long sujetId;
	private Long objetId;


	public RapportEntreTiers() {
		// nothing to do here
	}

	public RapportEntreTiers(RegDate dateDebut, RegDate dateFin, Tiers sujet, Tiers objet) {
		super(dateDebut, dateFin);
		this.sujetId = (sujet == null ? null : sujet.getId());
		this.objetId = (objet == null ? null : objet.getId());
	}

	protected RapportEntreTiers(RegDate dateDebut, RegDate dateFin, Long sujetId, Long objetId) {
		super(dateDebut, dateFin);
		this.sujetId = sujetId;
		this.objetId = objetId;
	}

	public RapportEntreTiers(RapportEntreTiers rapport) {
		super(rapport);
		this.sujetId = rapport.sujetId;
		this.objetId = rapport.objetId;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	/**
	 * @param theId
	 *            the id to set
	 */
	public void setId(Long theId) {
		this.id = theId;
	}

	@Column(name = "TIERS_SUJET_ID", nullable = false)
	@Index(name = "IDX_RET_TRS_SUJ_ID", columnNames = "TIERS_SUJET_ID")
	@ForeignKey(name = "FK_RET_TRS_SUJ_ID")
	public Long getSujetId() {
		return sujetId;
	}

	public void setSujetId(Long sujetId) {
		this.sujetId = sujetId;
	}

	public void setSujet(Tiers sujet) {
		this.sujetId = (sujet == null ? null : sujet.getId());
	}

	@Column(name = "TIERS_OBJET_ID", nullable = false)
	@Index(name = "IDX_RET_TRS_OBJ_ID", columnNames = "TIERS_OBJET_ID")
	@ForeignKey(name = "FK_RET_TRS_OBJ_ID")
	public Long getObjetId() {
		return objetId;
	}

	public void setObjetId(Long objetId) {
		this.objetId = objetId;
	}

	public void setObjet(Tiers objet) {
		this.objetId = (objet == null ? null : objet.getId());
	}

	/**
	 * Retourne une chaîne de caractères qui décrit le rôle <i>objet</i> de ce rapport entre tiers
	 * @return "ménage commun", "personne physique", "contribuable", "employeur"...
	 */
	@Transient
	public abstract String getDescriptionTypeObjet();

	/**
	 * Retourne une chaîne de caractères qui décrit le rôle <i>sujet</i> de ce rapport entre tiers
	 * @return "ménage commun", "personne physique", "contribuable", "employeur"...
	 */
	@Transient
	public abstract String getDescriptionTypeSujet();

	@Override
	@Transient
	public List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled) {

		if (!includeAnnuled && isAnnule()) {
			return null;
		}

		if (sujetId == null && objetId == null) {
			return null;
		}

		final List<Object> list = new ArrayList<>(2);
		if (sujetId != null) {
			list.add(new EntityKey(Tiers.class, sujetId));
		}
		if (objetId != null) {
			list.add(new EntityKey(Tiers.class, objetId));
		}

		return list;
	}

	public boolean equalsTo(RapportEntreTiers rapportEntreTiers) {

		// [UNIREG-3168] dès qu'au moins un des deux rapports est annulé, on ne peut plus avoir d'égalité
		if (isAnnule() || rapportEntreTiers.isAnnule()) {
			return false;
		}

		final EqualsBuilder equalsBuilder = new EqualsBuilder();
		equalsBuilder.append(this.sujetId, rapportEntreTiers.sujetId);
		equalsBuilder.append(this.getDateDebut(), rapportEntreTiers.getDateDebut());
		equalsBuilder.append(this.getDateFin(), rapportEntreTiers.getDateFin());
		equalsBuilder.append(this.objetId, rapportEntreTiers.objetId);
		equalsBuilder.append(this.getType(), rapportEntreTiers.getType());
		return equalsBuilder.isEquals();
	}

	@Transient
	public abstract TypeRapportEntreTiers getType();
}
