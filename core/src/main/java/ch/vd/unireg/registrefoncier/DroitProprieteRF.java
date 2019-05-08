package ch.vd.unireg.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.EntityKey;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.common.linkedentity.LinkedEntityPhase;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.FusionEntreprises;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tiers;

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
	@JoinColumn(name = "AYANT_DROIT_ID", foreignKey = @ForeignKey(name = "FK_DROIT_RF_AYANT_DROIT_ID"))
	public AyantDroitRF getAyantDroit() {
		return ayantDroit;
	}

	public void setAyantDroit(AyantDroitRF ayantDroit) {
		this.ayantDroit = ayantDroit;
	}

	// configuration hibernate : l'immeuble ne possède pas les droits (les droits pointent vers les immeubles, c'est tout)
	@ManyToOne
	@JoinColumn(name = "IMMEUBLE_ID", foreignKey = @ForeignKey(name = "FK_DROIT_RF_IMMEUBLE_ID"))
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
	@JoinColumn(name = "DROIT_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_RAISON_ACQ_RF_DROIT_ID"))
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
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		// on ne veut pas retourner les tiers Unireg dans le cas de la validation/indexation/parentés, car ils ne sont pas influencés par les données RF
		if (ayantDroit instanceof TiersRF && (context.getPhase() == LinkedEntityPhase.TACHES || context.getPhase() == LinkedEntityPhase.DATA_EVENT)) {

			final List<Object> list = new ArrayList<>();

			// on cherche tous les contribuables concernés ou ayant été concernés par ce droit
			final List<Contribuable> contribuables = findLinkedContribuables((TiersRF) ayantDroit);
			list.addAll(contribuables);

			// [SIFISC-24999] on ajoute les héritiers des contribuables trouvés (car les droits des décédés sont exposés sur les héritiers)
			final List<EntityKey> keysHeritage = findHeirsKeys(contribuables);
			list.addAll(keysHeritage);

			// [SIFISC-24999] on ajoute les entreprises absorbantes en cas de fusion (car les droits des entreprises absorbées sont exposés sur les entreprises absorbantes)
			final List<EntityKey> keysAcquiringCompany = findAcquiringOrganisationKeys(contribuables);
			list.addAll(keysAcquiringCompany);

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
	 * Détermine et retourne les clés d'entité des héritiers des contribuables spécifiés. Cette méthode s'arrête au premier niveau d'héritage comme convenu avec Raphaël Carbo en séance.
	 *
	 * @param contribuables une liste de contribuables
	 * @return la liste des clés des héritiers.
	 */
	@NotNull
	public static List<EntityKey> findHeirsKeys(@NotNull Collection<Contribuable> contribuables) {

		if (contribuables.isEmpty()) {
			// short path
			return Collections.emptyList();
		}

		// on construit la liste des clés des héritiers
		return contribuables.stream()
				.map(Tiers::getRapportsObjet)       // on part du 'décédé'
				.flatMap(Collection::stream)
				.filter(AnnulableHelper::nonAnnule)
				.filter(Heritage.class::isInstance) // on prend les rapports d'héritage
				.map(RapportEntreTiers::getSujetId) // on prend les ids des héritiers
				.distinct()
				.map(id -> new EntityKey(Tiers.class, id))
				.collect(Collectors.toList());
	}

	/**
	 * Détermine et retourne les clés d'entité des entreprises absorbantes des contribuables spécifiés. Cette méthode s'arrête au premier niveau des fusions d'entreprises comme convenu avec Raphaël Carbo en séance.
	 *
	 * @param contribuables une liste de contribuables
	 * @return la liste des clés des entreprises absorbantes.
	 */
	@NotNull
	public static List<EntityKey> findAcquiringOrganisationKeys(@NotNull Collection<Contribuable> contribuables) {

		if (contribuables.isEmpty()) {
			// short path
			return Collections.emptyList();
		}

		// on construit la liste des clés des entreprises absorbantes
		return contribuables.stream()
				.map(Tiers::getRapportsSujet)                   // on part de l'entreprise absorbée
				.flatMap(Collection::stream)
				.filter(AnnulableHelper::nonAnnule)
				.filter(FusionEntreprises.class::isInstance)    // on prend les rapports de fusion d'entreprise
				.map(RapportEntreTiers::getObjetId)             // on prend les ids des entreprises absorbantes
				.distinct()
				.map(id -> new EntityKey(Tiers.class, id))
				.collect(Collectors.toList());
	}

	@Transient
	@Override
	protected String getBusinessName() {
		return super.getBusinessName() + " " + getMasterIdRF() + "/" + getVersionIdRF();
	}
}
