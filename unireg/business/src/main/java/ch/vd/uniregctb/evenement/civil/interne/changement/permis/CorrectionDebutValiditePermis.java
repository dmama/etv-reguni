package ch.vd.uniregctb.evenement.civil.interne.changement.permis;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypePermis;

public class CorrectionDebutValiditePermis extends EvenementCivilInterne {

	protected CorrectionDebutValiditePermis(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected CorrectionDebutValiditePermis(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PERMIS);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final Individu individu = getIndividu();
		final Permis permis = context.getServiceCivil().getPermis(getNoIndividu(), getDate());
		if (permis == null || permis.getTypePermis() != TypePermis.ETABLISSEMENT) {
			Audit.info(getNumeroEvenement(), "Permis autre que permis C à la date de l'événement : ignoré");
		}
		else {
			// nous avons donc une modification de date de début d'un permis C
			// la règle dit : si l'individu est mineur sans for aujourd'hui, on peut passer tout droit, sinon, on ne sait pas encore faire et on met en erreur
			final RegDate aujourdhui = RegDate.get();
			boolean ignorable = false;
			if (individu.isMineur(aujourdhui)) {
				final PersonnePhysique pp = getPrincipalPP();
				if (pp != null) {
					boolean mineurs = true;
					final Contribuable ctb;
					final EnsembleTiersCouple couple = context.getTiersService().getEnsembleTiersCouple(pp, aujourdhui);
					if (couple != null) {
						ctb = couple.getMenage();
						final PersonnePhysique conjoint = couple.getConjoint(pp);
						if (conjoint != null) {
							mineurs = context.getTiersService().isMineur(conjoint, aujourdhui);
						}
					}
					else {
						ctb = pp;
					}

					if (mineurs) {
						final List<ForFiscal> fors = ctb.getForsFiscauxValidAt(aujourdhui);
						if (fors == null || fors.isEmpty()) {
							ignorable = true;
							Audit.info(getNumeroEvenement(), "Permis C sur mineur sans for (à la date de traitement) : ignoré");
						}
					}
				}
			}

			if (!ignorable) {
				throw new EvenementCivilException("Permis C sur individu majeur ou ayant un for fiscal actif : veuillez traiter le cas manuellement.");
			}
		}

		return null;
	}
}
