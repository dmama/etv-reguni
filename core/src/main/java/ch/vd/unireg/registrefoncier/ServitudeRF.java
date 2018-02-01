package ch.vd.unireg.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	 * Les ayant-droits concernés par la servitude.
	 */
	private Set<AyantDroitRF> ayantDroits;

	/**
	 * Les immeubles concernés par la servitude.
	 */
	private Set<ImmeubleRF> immeubles;

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
		this.ayantDroits = (right.ayantDroits == null ? null : new HashSet<>(right.ayantDroits));
		this.immeubles = (right.immeubles == null ? null : new HashSet<>(right.immeubles));
		this.identifiantDroit = (right.identifiantDroit == null ? null : new IdentifiantDroitRF(right.identifiantDroit));
		this.numeroAffaire = (right.numeroAffaire == null ? null : new IdentifiantAffaireRF(right.numeroAffaire));
	}

	// configuration hibernate : la servitude possède les ayants-droit
	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "RF_SERVITUDE_AYANT_DROIT",
			joinColumns = @JoinColumn(name = "DROIT_ID"),
			inverseJoinColumns = @JoinColumn(name = "AYANT_DROIT_ID"),
			uniqueConstraints = @UniqueConstraint(name = "IDX_SERVITUDE_AYANT_DROIT_ID", columnNames = {"DROIT_ID", "AYANT_DROIT_ID"}))
	public Set<AyantDroitRF> getAyantDroits() {
		return ayantDroits;
	}

	public void setAyantDroits(Set<AyantDroitRF> ayantDroits) {
		this.ayantDroits = ayantDroits;
	}

	public void addAyantDroit(AyantDroitRF ayantDroit) {
		if (this.ayantDroits == null) {
			this.ayantDroits = new HashSet<>();
		}
		this.ayantDroits.add(ayantDroit);
	}

	// configuration hibernate : la servitude possède les immeubles
	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "RF_SERVITUDE_IMMEUBLE",
			joinColumns = @JoinColumn(name = "DROIT_ID"),
			inverseJoinColumns = @JoinColumn(name = "IMMEUBLE_ID"),
			uniqueConstraints = @UniqueConstraint(name = "IDX_SERVITUDE_IMMEUBLE_ID", columnNames = {"DROIT_ID", "IMMEUBLE_ID"}))
	public Set<ImmeubleRF> getImmeubles() {
		return immeubles;
	}

	public void setImmeubles(Set<ImmeubleRF> immeubles) {
		this.immeubles = immeubles;
	}

	public void addImmeuble(ImmeubleRF immeuble) {
		if (this.immeubles == null) {
			this.immeubles = new HashSet<>();
		}
		this.immeubles.add(immeuble);
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
		return new ArrayList<>(ayantDroits);
	}

	@Transient
	@Override
	public @NotNull List<ImmeubleRF> getImmeubleList() {
		return new ArrayList<>(immeubles);
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
			c = ObjectUtils.compare(ayantDroits.size(), rightProp.ayantDroits.size(), false);
			if (c != 0) {
				return c;
			}
			c = ObjectUtils.compare(immeubles.size(), rightProp.immeubles.size(), false);
		}
		else {
			c = 1;
		}
		return c;
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		// on ne veut pas retourner les tiers Unireg dans le cas de la validation/indexation/parentés, car ils ne sont pas influencés par les données RF
		if ((context.getPhase() == LinkedEntityPhase.TACHES || context.getPhase() == LinkedEntityPhase.DATA_EVENT) && ayantDroits != null) {
			final List<Object> list = new ArrayList<>();
			for (AyantDroitRF ayantDroit : ayantDroits) {
				if (ayantDroit instanceof TiersRF) {
					// on cherche tous les contribuables concernés ou ayant été concernés par ce tiers
					list.addAll(DroitProprieteRF.findLinkedContribuables((TiersRF) ayantDroit));
				}
			}
			// on ajoute les immeubles, évidemment
			if (immeubles != null) {
				list.addAll(immeubles);
			}
			return list;
		}
		else {
			return immeubles == null ? Collections.emptyList() : new ArrayList<>(immeubles);
		}
	}
}
