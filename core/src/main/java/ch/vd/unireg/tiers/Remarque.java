package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;

/**
 * [UNIREG-1059] Une remarque associée à un tiers. Un tiers peut posséder 0 ou n remarques.
 */
@Entity
@Table(name = "REMARQUE")
public class Remarque extends HibernateEntity implements LinkedEntity {

	private Long id;
	private String texte;
	private Tiers tiers;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "TEXTE", length = LengthConstants.TIERS_REMARQUE)
	public String getTexte() {
		return texte;
	}

	public void setTexte(String texte) {
		this.texte = texte;
	}

	@ManyToOne
	@JoinColumn(name = "TIERS_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_REMARQUE_TRS_ID"))
	@Index(name = "IDX_REMARQUE_TIERS_ID", columnNames = "TIERS_ID")
	public Tiers getTiers() {
		return tiers;
	}

	public void setTiers(Tiers tiers) {
		this.tiers = tiers;
	}

	@Override
	@Transient
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return tiers == null ? null : Collections.singletonList(tiers);
	}
}
