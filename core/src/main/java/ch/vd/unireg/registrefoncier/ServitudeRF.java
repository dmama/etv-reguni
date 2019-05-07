package ch.vd.unireg.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.common.linkedentity.LinkedEntityPhase;

/**
 * Servitude sur un immeuble. L'ayant-droit d'un droit habitation est soit une personne morale, soit une personne physique.
 */
@Entity
public abstract class ServitudeRF extends DroitRF implements Duplicable<ServitudeRF> {

	/**
	 * Les ayant-droits bénéficiaires de la servitude.
	 */
	private Set<BeneficeServitudeRF> benefices;

	/**
	 * Les immeubles chargé par la servitude.
	 */
	private Set<ChargeServitudeRF> charges;

	/**
	 * L'identifiant métier public du droit.
	 */
	private IdentifiantDroitRF identifiantDroit;

	/**
	 * Le numéro d'affaire.
	 */
	@Nullable
	private IdentifiantAffaireRF numeroAffaire;

	public ServitudeRF() {
	}

	public ServitudeRF(ServitudeRF right) {
		super(right);

		// fait une copie profonde des liens vers les bénéficiaires
		this.benefices = (right.benefices == null ? new HashSet<>() : right.benefices.stream()
				.map(BeneficeServitudeRF::duplicate)
				.collect(Collectors.toSet()));
		this.benefices.forEach(lien -> lien.setServitude(this));

		// fait une copie profonde des liens vers les immeubles
		this.charges = (right.charges == null ? new HashSet<>() : right.charges.stream()
				.map(ChargeServitudeRF::duplicate)
				.collect(Collectors.toSet()));
		this.charges.forEach(lien -> lien.setServitude(this));

		this.identifiantDroit = (right.identifiantDroit == null ? null : new IdentifiantDroitRF(right.identifiantDroit));
		this.numeroAffaire = (right.numeroAffaire == null ? null : new IdentifiantAffaireRF(right.numeroAffaire));
	}

	// configuration hibernate : la servitude possède les liens vers les bénéficiaires
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "DROIT_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_SERV_AD_RF_DROIT_ID"))
	public Set<BeneficeServitudeRF> getBenefices() {
		return benefices;
	}

	public void setBenefices(Set<BeneficeServitudeRF> implantations) {
		this.benefices = implantations;
	}

	public void addBenefice(BeneficeServitudeRF benefice) {
		if (this.benefices == null) {
			this.benefices = new HashSet<>();
		}
		benefice.setServitude(this);
		this.benefices.add(benefice);
	}

	// configuration hibernate : la servitude possède les liens vers les bénéficiaires
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "DROIT_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_SERV_IMM_RF_DROIT_ID"))
	public Set<ChargeServitudeRF> getCharges() {
		return charges;
	}

	public void setCharges(Set<ChargeServitudeRF> charges) {
		this.charges = charges;
	}

	public void addCharge(ChargeServitudeRF charge) {
		if (this.charges == null) {
			this.charges = new HashSet<>();
		}
		charge.setServitude(this);
		this.charges.add(charge);
	}

	@Column(name = "IDENTIFIANT_DROIT", length = LengthConstants.RF_IDENTIFIANT_DROIT)
	@Type(type = "ch.vd.unireg.hibernate.IdentifiantDroitRFUserType")
	public IdentifiantDroitRF getIdentifiantDroit() {
		return identifiantDroit;
	}

	public void setIdentifiantDroit(IdentifiantDroitRF identifiantDroit) {
		this.identifiantDroit = identifiantDroit;
	}

	@Nullable
	@Column(name = "NO_AFFAIRE", length = LengthConstants.RF_NO_AFFAIRE)
	@Type(type = "ch.vd.unireg.hibernate.IdentifiantAffaireRFUserType")
	public IdentifiantAffaireRF getNumeroAffaire() {
		return numeroAffaire;
	}

	public void setNumeroAffaire(@Nullable IdentifiantAffaireRF numeroAffaire) {
		this.numeroAffaire = numeroAffaire;
	}

	@NotNull
	@Override
	@Transient
	public TypeDroit getTypeDroit() {
		return TypeDroit.SERVITUDE;
	}

	@Transient
	@Override
	public @NotNull List<AyantDroitRF> getAyantDroitList() {
		return benefices.stream()
				.map(BeneficeServitudeRF::getAyantDroit)
				.collect(Collectors.toList());
	}

	@Transient
	@Override
	public @NotNull List<ImmeubleRF> getImmeubleList() {
		return charges.stream()
				.map(ChargeServitudeRF::getImmeuble)
				.collect(Collectors.toList());
	}

	/**
	 * Compare le droit courant avec un autre droit. Les propriétés utilisées pour la comparaison sont :
	 * <ul>
	 * <li>les dates de début et de fin</li>
	 * <li>le nombre d'ayant-droits</li>
	 * <li>le nombre d'immeubles</li>
	 * </ul>
	 *
	 * @param right un autre droit.
	 * @return le résultat de la comparaison selon {@link Comparable#compareTo(Object)}.
	 */
	@Override
	public int compareTo(@NotNull DroitRF right) {
		int c = super.compareTo(right);
		if (c != 0) {
			return c;
		}
		if (right instanceof ServitudeRF) {
			final ServitudeRF rightProp =(ServitudeRF) right;
			// TODO (msi) peut-être faire un comparaison plus fine...
			c = ObjectUtils.compare(benefices.size(), rightProp.benefices.size(), false);
			if (c != 0) {
				return c;
			}
			c = ObjectUtils.compare(charges.size(), rightProp.charges.size(), false);
		}
		else {
			c = 1;
		}
		return c;
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		// on ne veut pas retourner les tiers Unireg dans le cas de la validation/indexation/parentés, car ils ne sont pas influencés par les données RF
		if ((context.getPhase() == LinkedEntityPhase.TACHES || context.getPhase() == LinkedEntityPhase.DATA_EVENT) && benefices != null) {
			final List<Object> list = new ArrayList<>();
			benefices.stream()
					.map(BeneficeServitudeRF::getAyantDroit)
					.filter(TiersRF.class::isInstance)
					.map(TiersRF.class::cast)
					.forEach(tiers -> {
						// on cherche tous les contribuables concernés ou ayant été concernés par ce tiers
						list.addAll(DroitProprieteRF.findLinkedContribuables(tiers));
					});
			// on ajoute les immeubles, évidemment
			if (charges != null) {
				list.addAll(charges.stream()
						            .map(ChargeServitudeRF::getImmeuble)
						            .collect(Collectors.toList()));
			}
			return list;
		}
		else {
			return charges == null ? Collections.emptyList() : charges.stream()
					.map(ChargeServitudeRF::getImmeuble)
					.collect(Collectors.toList());
		}
	}
}
