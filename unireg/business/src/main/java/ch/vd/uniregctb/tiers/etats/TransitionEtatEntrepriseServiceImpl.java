package ch.vd.uniregctb.tiers.etats;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.etats.transition.ToAbsorbeeTransitionEtatEntrepriseFactory;
import ch.vd.uniregctb.tiers.etats.transition.ToDissouteTransitionEtatEntrepriseFactory;
import ch.vd.uniregctb.tiers.etats.transition.ToEnFailliteTransitionEtatEntrepriseFactory;
import ch.vd.uniregctb.tiers.etats.transition.ToEnLiquidationTransitionEtatEntrepriseFactory;
import ch.vd.uniregctb.tiers.etats.transition.ToFondeeTransitionEtatEntrepriseFactory;
import ch.vd.uniregctb.tiers.etats.transition.ToInscriteRCTransitionEtatEntrepriseFactory;
import ch.vd.uniregctb.tiers.etats.transition.ToRadieeRCTransitionEtatEntrepriseFactory;
import ch.vd.uniregctb.tiers.etats.transition.TransitionEtatEntreprise;
import ch.vd.uniregctb.tiers.etats.transition.TransitionEtatEntrepriseFactory;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * Factory pour la création des transitions d'état de l'entreprise. Les transitions disponibles sont conservées dans une
 * map indexée par type d'état cible.
 *
 * <p>
 *     Lors de la recherche des transition disponible, cette map est utilisée pour déterminer les transitions candidates.
 *     Celles-ci déterminent encore si elles sont vraiment disponibles en l'espèce, et ne font partie du résultat renvoyé
 *     à l'appelant que si c'est bien le cas.
 * </p>
 *
 * @author Raphaël Marmier, 2016-01-21, <raphael.marmier@vd.ch>
 */
public class TransitionEtatEntrepriseServiceImpl implements TransitionEtatEntrepriseService, InitializingBean {

	protected static final Logger LOGGER = LoggerFactory.getLogger(TransitionEtatEntrepriseServiceImpl.class);

	private TiersDAO tiersDAO;
	private ServiceOrganisationService serviceOrganisation;

	/*
	Liste des transitions disponibles vers l'état visé
	 */
	private final Map<TypeEtatEntreprise, TransitionEtatEntrepriseFactory> transitionFactoryMap = new HashMap<>();

	@Override
	public void afterPropertiesSet() throws Exception {
		transitionFactoryMap.put(TypeEtatEntreprise.EN_LIQUIDATION, new ToEnLiquidationTransitionEtatEntrepriseFactory(tiersDAO));
		transitionFactoryMap.put(TypeEtatEntreprise.EN_FAILLITE, new ToEnFailliteTransitionEtatEntrepriseFactory(tiersDAO));
		transitionFactoryMap.put(TypeEtatEntreprise.ABSORBEE, new ToAbsorbeeTransitionEtatEntrepriseFactory(tiersDAO));
		transitionFactoryMap.put(TypeEtatEntreprise.RADIEE_RC, new ToRadieeRCTransitionEtatEntrepriseFactory(tiersDAO, serviceOrganisation));
		transitionFactoryMap.put(TypeEtatEntreprise.INSCRITE_RC, new ToInscriteRCTransitionEtatEntrepriseFactory(tiersDAO));
		transitionFactoryMap.put(TypeEtatEntreprise.FONDEE, new ToFondeeTransitionEtatEntrepriseFactory(tiersDAO));
		transitionFactoryMap.put(TypeEtatEntreprise.DISSOUTE, new ToDissouteTransitionEtatEntrepriseFactory(tiersDAO));
	}

	@Override
	public Map<TypeEtatEntreprise, TransitionEtatEntreprise> getTransitionsDisponibles(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		if (entreprise.getEtatActuel() == null) {
			LOGGER.info("{} n'a pas d'état! Pas de transitions d'état disponibles.", entreprise.toString());
			return Collections.emptyMap();
		}
		Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = new HashMap<>();
		for (Map.Entry<TypeEtatEntreprise, TransitionEtatEntrepriseFactory> entry : transitionFactoryMap.entrySet()) {
			final TransitionEtatEntreprise transition = entry.getValue().create(entreprise, date, generation);
			if (transition != null) {
				disponibles.put(entry.getKey(), transition);
			}
		}
		return disponibles;
	}

	@Override
	public TransitionEtatEntreprise getTransitionVersEtat(TypeEtatEntreprise type, Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		final TransitionEtatEntrepriseFactory factory = transitionFactoryMap.get(type);
		return factory.create(entreprise, date, generation);
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		Objects.requireNonNull(tiersDAO);
		this.tiersDAO = tiersDAO;
	}

	public void setServiceOrganisation(ServiceOrganisationService serviceOrganisation) {
		Objects.requireNonNull(serviceOrganisation);
		this.serviceOrganisation = serviceOrganisation;
	}
}
