package ch.vd.uniregctb.tiers.etats.transition;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * Classe de base regroupant la partie commune des transitions.
 *
 * @author Raphaël Marmier, 2016-01-21, <raphael.marmier@vd.ch>
 */
public abstract class BaseTransitionEtatEntreprise implements TransitionEtatEntreprise {

	private final TiersDAO tiersDAO;

	private final Entreprise entreprise;
	private final RegDate date;
	private final TypeGenerationEtatEntreprise generation;

	public BaseTransitionEtatEntreprise(@NotNull TiersDAO tiersDAO, @NotNull Entreprise entreprise, @NotNull RegDate date, TypeGenerationEtatEntreprise generation) {
		this.tiersDAO = tiersDAO;
		this.entreprise = entreprise;
		this.date = date;
		this.generation = generation;
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
}
