package ch.vd.unireg.evenement.civil.interne.changement.permis;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.TypePermis;

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

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final Individu individu = getIndividu();
		final Permis permis = individu.getPermis().getPermisActif(getDate());
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
						if (fors.isEmpty()) {
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

		return HandleStatus.TRAITE;
	}
}
