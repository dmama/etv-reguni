package ch.vd.uniregctb.evenement.civil.interne.changement.permis;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

public class CorrectionDebutValiditePermis extends EvenementCivilInterne {

	protected CorrectionDebutValiditePermis(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilInterneException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected CorrectionDebutValiditePermis(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce,
	                                        EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.CORREC_DEBUT_VALIDITE_PERMIS, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PERMIS);
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		final Individu individu = getIndividu();
		final Permis permis = individu.getPermisActif(getDate());
		if (permis == null || permis.getTypePermis() != TypePermis.ETABLISSEMENT) {
			Audit.info(getNumeroEvenement(), String.format("Permis autre que permis C à la date de l'événement : ignoré"));
		}
		else {
			// nous avons donc une modification de date de début d'un permis C
			// la règle dit : si l'individu est mineur sans for aujourd'hui, on peut passer tout droit, sinon, on ne sait pas encore faire et on met en erreur
			final RegDate aujourdhui = RegDate.get();
			boolean ignorable = false;
			if (individu.isMineur(aujourdhui)) {
				final PersonnePhysique pp = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(individu.getNoTechnique());
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
						if (fors == null || fors.size() == 0) {
							ignorable = true;
							Audit.info(getNumeroEvenement(), String.format("Permis C sur mineur sans for (à la date de traitement) : ignoré"));
						}
					}
				}
			}

			if (!ignorable) {
				throw new EvenementCivilHandlerException("Permis C sur individu majeur ou ayant un for fiscal actif : veuillez traiter le cas manuellement.");
			}
		}

		return null;
	}
}
