package ch.vd.unireg.tiers.etats.transition;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

/**
 * Classe de base regroupant la partie commune des transitions.
 *
 * @author Raphaël Marmier, 2016-01-21, <raphael.marmier@vd.ch>
 */
public abstract class BaseTransitionEtatEntreprise implements TransitionEtatEntreprise {

	private final TiersDAO tiersDAO;
	private final EvenementFiscalService evenementFiscalService;

	private final Entreprise entreprise;
	private final RegDate date;
	private final TypeGenerationEtatEntreprise generation;

	public BaseTransitionEtatEntreprise(@NotNull TiersDAO tiersDAO, @NotNull Entreprise entreprise, @NotNull RegDate date, TypeGenerationEtatEntreprise generation, @NotNull EvenementFiscalService evenementFiscalService) {
		this.tiersDAO = tiersDAO;
		this.entreprise = entreprise;
		this.date = date;
		this.generation = generation;
		this.evenementFiscalService = evenementFiscalService;
	}

	@Override
	public abstract TypeEtatEntreprise getTypeDestination();

	@Override
	public EtatEntreprise apply() {
		final EtatEntreprise etat = new EtatEntreprise();
		etat.setType(getTypeDestination());
		etat.setDateObtention(getDate());
		etat.setGeneration(getGeneration());
		return getTiersDAO().addAndSave(getEntreprise(), etat);
	}

	protected TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	protected Entreprise getEntreprise() {
		return entreprise;
	}

	protected RegDate getDate() {
		return date;
	}

	protected TypeGenerationEtatEntreprise getGeneration() {
		return generation;
	}

	public EvenementFiscalService getEvenementFiscalService() {
		return evenementFiscalService;
	}
}
