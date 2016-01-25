package ch.vd.uniregctb.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * Transition vers l'état "En liquidation"
 *
 * @author Raphaël Marmier, 2016-01-21, <raphael.marmier@vd.ch>
 */
public class ToEnLiquidationTransitionEtatEntreprise extends BaseTransitionEtatEntreprise {
	public final TypeEtatEntreprise TARGET_TYPE = TypeEtatEntreprise.EN_LIQUIDATION;

	public ToEnLiquidationTransitionEtatEntreprise(TiersDAO tiersDAO, Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		super(tiersDAO, entreprise, date, generation);
	}

	@Override
	public EtatEntreprise apply() {
		final EtatEntreprise etat = new EtatEntreprise();
		etat.setType(TARGET_TYPE);
		etat.setDateObtention(getDate());
		etat.setGeneration(getGeneration());
		return getTiersDAO().addAndSave(getEntreprise(), etat);
	}

	@Override
	public TypeEtatEntreprise getType() {
		return TARGET_TYPE;
	}
}
