package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.common.linkedentity.LinkedEntityPhase;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonneRF;
import ch.vd.unireg.registrefoncier.RapprochementRF;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +------------------+                   +------------------+
 *   |     Héritier     |                   |      Défunt      |
 *   +------------------+                   +------------------+
 *           ^                                        ^
 *           ¦  sujet  +--------------------+  objet  ¦
 *           +---------|     Héritage       |---------+
 *                     +--------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("Heritage")
public class Heritage extends RapportEntreTiers {

	private static final String HERITIER = "héritier";
	private static final String DEFUNT = "défunt(e)";

	/**
	 * Vrai si l'héritier est le principal de la communauté d'héritiers (voir SIFISC-24999).
	 */
	private Boolean principalCommunaute = false;

	public Heritage() {
	}

	public Heritage(RegDate dateDebut, RegDate dateFin, PersonnePhysique heritier, PersonnePhysique defunt, Boolean principal) {
		super(dateDebut, dateFin, heritier, defunt);
		this.principalCommunaute  = principal;
	}

	public Heritage(RegDate dateDebut, RegDate dateFin, Long heritierId, Long defuntId, Boolean principalCommunaute) {
		super(dateDebut, dateFin, heritierId, defuntId);
		this.principalCommunaute = principalCommunaute;
	}

	protected Heritage(Heritage heritage) {
		super(heritage);
	}

	@NotNull
	public static Heritage adapt(@NotNull Heritage range, @Nullable RegDate debut, @Nullable RegDate fin) {
		final RegDate dateDebut = (debut == null ? range.getDateDebut() : debut);
		final RegDate dateFin = (fin == null ? range.getDateFin() : fin);
		return new Heritage(dateDebut, dateFin, range.getSujetId(), range.getObjetId(), range.getPrincipalCommunaute());
	}

	@Override
	public Heritage duplicate() {
		return new Heritage(this);
	}

	@Column(name = "PRINCIPAL_COMM_HERITIERS")
	public Boolean getPrincipalCommunaute() {
		return principalCommunaute;
	}

	public void setPrincipalCommunaute(Boolean principalCommunaute) {
		this.principalCommunaute = principalCommunaute;
	}

	@Transient
	@Override
	public String getDescriptionTypeObjet() {
		return DEFUNT;
	}

	@Transient
	@Override
	public String getDescriptionTypeSujet() {
		return HERITIER;
	}

	@Transient
	@Override
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.HERITAGE;
	}

	@Transient
	@Override
	protected String getBusinessName() {
		// redéfini ici à cause de l'accent qui n'apparaît pas dans le nom de la classe...
		return "Héritage";
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {

		if (!includeAnnuled && isAnnule()) {
			return null;
		}

		//noinspection unchecked
		final List<Object> entities = (List<Object>) super.getLinkedEntities(context, includeAnnuled);

		// on ne retourne les communautés RF que dans le cas de l'envoi de data events, car les autres phases (validation, indexation et tâches) ne sont pas influencées par les données du RF
		if (context.getPhase() == LinkedEntityPhase.DATA_EVENT) {

			// on ajoute les éventuelles communautés RF dont le défunt fait partie
			final PersonnePhysique defunt = context.getHibernateTemplate().get(PersonnePhysique.class, getObjetId());
			entities.addAll(findCommunautesRF(defunt));
		}

		return entities;
	}

	/**
	 * Détermine les communautés RF dans lesquelles la personne physique spécifiée est membre.
	 *
	 * @param pp une personne physique
	 * @return l'ensemble des communautés RF demandées
	 */
	@NotNull
	public static Set<CommunauteRF> findCommunautesRF(@Nullable PersonnePhysique pp) {
		return Optional.ofNullable(pp)
				.map(Contribuable::getRapprochementsRF)
				.map(r -> r.stream()
						.filter(AnnulableHelper::nonAnnule)
						.map(RapprochementRF::getTiersRF)
						.map(AyantDroitRF::getDroitsPropriete)
						.flatMap(Collection::stream)
						.filter(AnnulableHelper::nonAnnule)
						.filter(DroitProprietePersonneRF.class::isInstance)
						.map(DroitProprietePersonneRF.class::cast)
						.filter(d -> d.getCommunaute() != null)
						.map(DroitProprietePersonneRF::getCommunaute)
						.collect(Collectors.toSet()))
				.orElse(Collections.emptySet());
	}
}
