package ch.vd.unireg.tiers.etats;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.etats.transition.ToAbsorbeeTransitionEtatEntrepriseFactory;
import ch.vd.unireg.tiers.etats.transition.ToDissouteTransitionEtatEntrepriseFactory;
import ch.vd.unireg.tiers.etats.transition.ToEnFailliteTransitionEtatEntrepriseFactory;
import ch.vd.unireg.tiers.etats.transition.ToEnLiquidationTransitionEtatEntrepriseFactory;
import ch.vd.unireg.tiers.etats.transition.ToFondeeTransitionEtatEntrepriseFactory;
import ch.vd.unireg.tiers.etats.transition.ToInscriteRCTransitionEtatEntrepriseFactory;
import ch.vd.unireg.tiers.etats.transition.ToRadieeRCTransitionEtatEntrepriseFactory;
import ch.vd.unireg.tiers.etats.transition.TransitionEtatEntreprise;
import ch.vd.unireg.tiers.etats.transition.TransitionEtatEntrepriseFactory;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

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
	private EvenementFiscalService evenementFiscalService;

	/*
	Liste des transitions disponibles vers l'état visé
	 */
	private final Map<TypeEtatEntreprise, TransitionEtatEntrepriseFactory> transitionFactoryMap = new EnumMap<>(TypeEtatEntreprise.class);

	@Override
	public void afterPropertiesSet() throws Exception {
		transitionFactoryMap.put(TypeEtatEntreprise.EN_LIQUIDATION, new ToEnLiquidationTransitionEtatEntrepriseFactory(tiersDAO, evenementFiscalService));
		transitionFactoryMap.put(TypeEtatEntreprise.EN_FAILLITE, new ToEnFailliteTransitionEtatEntrepriseFactory(tiersDAO, evenementFiscalService));
		transitionFactoryMap.put(TypeEtatEntreprise.ABSORBEE, new ToAbsorbeeTransitionEtatEntrepriseFactory(tiersDAO, evenementFiscalService));
		transitionFactoryMap.put(TypeEtatEntreprise.RADIEE_RC, new ToRadieeRCTransitionEtatEntrepriseFactory(tiersDAO, evenementFiscalService));
		transitionFactoryMap.put(TypeEtatEntreprise.INSCRITE_RC, new ToInscriteRCTransitionEtatEntrepriseFactory(tiersDAO, evenementFiscalService));
		transitionFactoryMap.put(TypeEtatEntreprise.FONDEE, new ToFondeeTransitionEtatEntrepriseFactory(tiersDAO, evenementFiscalService));
		transitionFactoryMap.put(TypeEtatEntreprise.DISSOUTE, new ToDissouteTransitionEtatEntrepriseFactory(tiersDAO, evenementFiscalService));
	}

	@Override
	public Map<TypeEtatEntreprise, TransitionEtatEntreprise> getTransitionsDisponibles(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		final Map<TypeEtatEntreprise, TransitionEtatEntreprise> disponibles = new EnumMap<>(TypeEtatEntreprise.class);
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

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		Objects.requireNonNull(evenementFiscalService);
		this.evenementFiscalService = evenementFiscalService;
	}
}
