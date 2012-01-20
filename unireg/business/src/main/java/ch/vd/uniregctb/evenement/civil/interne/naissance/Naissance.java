package ch.vd.uniregctb.evenement.civil.interne.naissance;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class Naissance extends EvenementCivilInterne {

	private static final Logger LOGGER = Logger.getLogger(Naissance.class);

	protected Naissance(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Naissance(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, List<Individu> parents, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.EvenementCivilInterne#isContribuablePresentBefore()
	 */
	@Override
	public boolean isContribuablePresentBefore() {
		/* Le contribuable n'existe pas à l'arrivée d'un événement naissance */
		return false;
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PARENTS);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		if ( FiscalDateHelper.isMajeurAt(getIndividu(), RegDate.get()) ) {
			erreurs.addErreur("L'individu ne devrait pas être majeur à la naissance");
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		LOGGER.debug("Traitement de la naissance de l'individu : " + getNoIndividu() );

		try {
			/*
			 * Transtypage de l'événement en naissance
			 */
			final Individu individu = getIndividu();
			final RegDate dateEvenement = getDate();

			/*
			 * Vérifie qu'aucun tiers n'existe encore rattaché à cet individu
			 */
			verifieNonExistenceTiers(individu.getNoTechnique());

			/*
			 *  Création d'un nouveau Tiers et sauvegarde de celui-ci
			 */
			PersonnePhysique bebe = new PersonnePhysique(true);
			bebe.setNumeroIndividu(individu.getNoTechnique());
			bebe = (PersonnePhysique) context.getTiersDAO().save(bebe);
			Audit.info(getNumeroEvenement(), "Création d'un nouveau tiers habitant (numéro: " + bebe.getNumero() + ')');

			context.getEvenementFiscalService().publierEvenementFiscalChangementSituation(bebe, dateEvenement, bebe.getId());

			// [UNIREG-3244] on envoie les fairs-parts de naissance
			final Contribuable parent = context.getTiersService().getAutoriteParentaleDe(bebe, dateEvenement);
			if (parent != null) {
				context.getEvenementFiscalService().publierEvenementFiscalNaissance(bebe, parent, dateEvenement);
			}

			return new Pair<PersonnePhysique, PersonnePhysique>(bebe, null);
		}
		catch (Exception e) {
			LOGGER.debug("Erreur lors de la sauvegarde du nouveau tiers", e);
			throw new EvenementCivilException(e.getMessage(), e);
		}
	}
}
