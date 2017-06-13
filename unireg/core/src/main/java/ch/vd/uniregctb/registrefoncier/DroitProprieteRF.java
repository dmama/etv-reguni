package ch.vd.uniregctb.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.registrefoncier.key.DroitRFKey;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.Contribuable;

@Entity
public abstract class DroitProprieteRF extends DroitRF {

	/**
	 * L'ayant-droit concerné par le droit.
	 */
	private AyantDroitRF ayantDroit;

	/**
	 * L'immeuble concerné par le droit.
	 */
	private ImmeubleRF immeuble;


	private Fraction part;

	private GenrePropriete regime;

	/**
	 * Les raison d'acquisition du droit.
	 */
	private Set<RaisonAcquisitionRF> raisonsAcquisition;

	// configuration hibernate : l'ayant-droit ne possède pas les droits (les droits pointent vers les ayants-droits, c'est tout)
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "AYANT_DROIT_ID")
	@Index(name = "IDX_DROIT_RF_AYANT_DROIT_ID", columnNames = "AYANT_DROIT_ID")
	@ForeignKey(name = "FK_DROIT_RF_AYANT_DROIT_ID")
	public AyantDroitRF getAyantDroit() {
		return ayantDroit;
	}

	public void setAyantDroit(AyantDroitRF ayantDroit) {
		this.ayantDroit = ayantDroit;
	}

	// configuration hibernate : l'immeuble ne possède pas les droits (les droits pointent vers les immeubles, c'est tout)
	@ManyToOne
	@JoinColumn(name = "IMMEUBLE_ID")
	@ForeignKey(name = "FK_DROIT_RF_IMMEUBLE_ID")
	@Index(name = "IDX_DROIT_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}


	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "numerateur", column = @Column(name = "PART_PROP_NUM")),
			@AttributeOverride(name = "denominateur", column = @Column(name = "PART_PROP_DENOM"))
	})
	public Fraction getPart() {
		return part;
	}

	public void setPart(Fraction part) {
		this.part = part;
	}

	@Column(name = "REGIME_PROPRIETE", length = LengthConstants.RF_GENRE_PROPRIETE)
	@Enumerated(EnumType.STRING)
	public GenrePropriete getRegime() {
		return regime;
	}

	public void setRegime(GenrePropriete regime) {
		this.regime = regime;
	}

	// configuration hibernate : le droit possède les raison d'acquisition
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "DROIT_ID", nullable = false)
	@ForeignKey(name = "FK_RAISON_ACQ_RF_DROIT_ID")
	public Set<RaisonAcquisitionRF> getRaisonsAcquisition() {
		return raisonsAcquisition;
	}

	public void setRaisonsAcquisition(Set<RaisonAcquisitionRF> raisonsAcquisition) {
		this.raisonsAcquisition = raisonsAcquisition;
	}

	public void addRaisonAcquisition(RaisonAcquisitionRF raison) {
		if (raisonsAcquisition == null) {
			raisonsAcquisition = new HashSet<>();
		}
		raison.setDroit(this);
		raisonsAcquisition.add(raison);
	}

	@NotNull
	@Transient
	@Override
	public TypeDroit getTypeDroit() {
		return TypeDroit.DROIT_PROPRIETE;
	}

	@Transient
	@Override
	public @NotNull List<AyantDroitRF> getAyantDroitList() {
		return Collections.singletonList(ayantDroit);
	}

	@Transient
	@Override
	public @NotNull List<ImmeubleRF> getImmeubleList() {
		return Collections.singletonList(immeuble);
	}

	/**
	 * Compare le droit courant avec un autre droit. Les propriétés utilisées pour la comparaison sont :
	 * <ul>
	 * <li>les dates de début et de fin</li>
	 * <li>l'id de l'ayant-droit</li>
	 * <li>l'id de l'immeuble</li>
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
		if (right instanceof DroitProprieteRF) {
			final DroitProprieteRF rightProp =(DroitProprieteRF) right;
			c = ObjectUtils.compare(ayantDroit.getId(), rightProp.ayantDroit.getId(), false);
			if (c != 0) {
				return c;
			}
			c = ObjectUtils.compare(immeuble.getId(), rightProp.immeuble.getId(), false);
		}
		else {
			c = -1;
		}
		return c;
	}

	@Override
	public List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled) {
		// on ne veut pas retourner les tiers Unireg dans le cas de la validation/indexation/parentés, car ils ne sont pas influencés par les données RF
		if (ayantDroit instanceof TiersRF && (context == Context.TACHES || context == Context.DATA_EVENT)) {
			final List<Object> list = new ArrayList<>();
			// on cherche tous les contribuables concernés ou ayant été concernés par ce droit
			list.addAll(findLinkedContribuables((TiersRF) ayantDroit));
			// on ajoute l'immeuble, évidemment
			list.add(immeuble);
			return list;
		}
		else {
			return Collections.singletonList(immeuble);
		}
	}

	/**
	 * @param tiersRF un tiers RF
	 * @return on cherche tous les contribuables concernés ou ayant été concernés par le tiers RF spécifié.
	 */
	@NotNull
	public static List<Contribuable> findLinkedContribuables(@NotNull TiersRF tiersRF) {
		return Optional.of(tiersRF)
				.map(TiersRF::getRapprochements) // la collection peut être nulle si l'entité vient juste d'être créée
				.map(r -> r.stream()
						.map(RapprochementRF::getContribuable)
						.collect(Collectors.toList()))
				.orElseGet(Collections::emptyList);
	}

	/**
	 * Calcule la date de début métier et le motif d'acquisition à partir de l'historique des raisons d'acquisition.
	 * @param droitPrecedentProvider un provider qui retourne le droit précédent chronologiquement) avec le même masterId qui spécifié.
	 */
	public void calculateDateEtMotifDebut(@NotNull Function<DroitRFKey, DroitProprieteRF> droitPrecedentProvider) {
		if (raisonsAcquisition == null || raisonsAcquisition.isEmpty()) {
			setDebutRaisonAcquisition(null);
		}
		else {
			final DroitProprieteRF precedent = droitPrecedentProvider.apply(new DroitRFKey(getMasterIdRF(), getVersionIdRF()));
			if (precedent == null || precedent.getRaisonsAcquisition() == null) {
				// il n'y a pas de droit précédent : on prend la raison d'acquisition la plus vieille comme référence
				final RaisonAcquisitionRF first = raisonsAcquisition.stream()
						.min(Comparator.naturalOrder())
						.orElse(null);
				setDebutRaisonAcquisition(first);
			}
			else {
				// il y a bien un droit précédent : on prend la nouvelle raison d'acquisition comme référence
				final RegDate derniereDate = precedent.getRaisonsAcquisition().stream()
						.map(RaisonAcquisitionRF::getDateAcquisition)
						.max(Comparator.naturalOrder())
						.orElse(null);
				final RaisonAcquisitionRF nouvelle = raisonsAcquisition.stream()
						.filter(r -> RegDateHelper.isAfter(r.getDateAcquisition(), derniereDate, NullDateBehavior.EARLIEST))
						.min(Comparator.naturalOrder())
						.orElse(null);
				setDebutRaisonAcquisition(nouvelle);
			}
		}
	}

	@Transient
	private void setDebutRaisonAcquisition(@Nullable RaisonAcquisitionRF raison) {
		if (raison == null) {
			setDateDebutMetier(null);
			setMotifDebut(null);
		}
		else {
			setDateDebutMetier(raison.getDateAcquisition());
			setMotifDebut(raison.getMotifAcquisition());
		}
	}
}
