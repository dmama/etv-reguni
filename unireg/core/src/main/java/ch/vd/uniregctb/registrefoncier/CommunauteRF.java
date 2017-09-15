package ch.vd.uniregctb.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.LengthConstants;

/**
 * Une communauté représente un groupement de tiers qui possèdent ensemble un droit sur un immeuble.
 */
@Entity
@DiscriminatorValue("Communaute")
public class CommunauteRF extends AyantDroitRF {

	private TypeCommunaute type;

	/**
	 * Les droits de propriété des membres de la communauté.
	 */
	private Set<DroitProprietePersonneRF> membres;

	/**
	 * Historique des regroupements de cette communauté vers des modèles de communautés.
	 */
	private Set<RegroupementCommunauteRF> regroupements;

	@Column(name = "TYPE_COMMUNAUTE", length = LengthConstants.RF_TYPE_COMMUNAUTE)
	@Enumerated(EnumType.STRING)
	public TypeCommunaute getType() {
		return type;
	}

	public void setType(TypeCommunaute type) {
		this.type = type;
	}

	@Override
	public void copyDataTo(AyantDroitRF right) {
		super.copyDataTo(right);
		final CommunauteRF commRight = (CommunauteRF) right;
		commRight.type = this.type;
	}

	// configuration hibernate : la comunauté ne possède pas les membres
	@OneToMany(mappedBy = "communaute")
	public Set<DroitProprietePersonneRF> getMembres() {
		return membres;
	}

	@Transient
	public void addMembre(DroitProprietePersonneRF droit) {
		if (membres == null) {
			membres = new HashSet<>();
		}
		membres.add(droit);
	}

	public void setMembres(Set<DroitProprietePersonneRF> membres) {
		this.membres = membres;
	}

	// configuration hibernate : la communauté possède les regroupements
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "COMMUNAUTE_ID", nullable = false)
	@ForeignKey(name = "FK_REGRCOMM_RF_COMMUNAUTE_ID")
	public Set<RegroupementCommunauteRF> getRegroupements() {
		return regroupements;
	}

	public void setRegroupements(Set<RegroupementCommunauteRF> regroupements) {
		this.regroupements = regroupements;
	}

	public void addRegroupement(@NotNull RegroupementCommunauteRF regroupement) {
		if (regroupements == null) {
			regroupements = new HashSet<>();
		}
		regroupement.setCommunaute(this);
		regroupements.add(regroupement);
	}
}
