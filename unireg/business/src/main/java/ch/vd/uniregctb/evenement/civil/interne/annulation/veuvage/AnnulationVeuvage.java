package ch.vd.uniregctb.evenement.civil.interne.annulation.veuvage;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;

public class AnnulationVeuvage extends EvenementCivilInterne {

    private final boolean traiteRedondance; // Basiquement, on ne traite la redondance que pour les evts issus de Rcpers
    private boolean isRedondant;

	protected AnnulationVeuvage(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
        this.traiteRedondance = false;
	}

	protected AnnulationVeuvage(EvenementCivilEch evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
        this.traiteRedondance = true;
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AnnulationVeuvage(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
        traiteRedondance = false;
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		PersonnePhysique veuf = getPrincipalPP();

		/*
				 * Récupération du ménage du veuf
				 */
		EnsembleTiersCouple menageComplet = context.getTiersService().getEnsembleTiersCouple(veuf, getDate().getOneDayAfter());

		if (menageComplet != null || (menageComplet != null && (menageComplet.getMenage() != null || menageComplet.getConjoint(veuf) != null))) {
            if (traiteRedondance) {
                // L'évenement est peut-être Redondant,pour detecter ça on verifie que :
                //   - l'individu a un for annulé le jour suivant le veuvage
                //   - le for annulé a pour motif: veuvage
                isRedondant = getPrincipalPP().hasForFiscalPrincipalAnnule(getDate().getOneDayAfter(), MotifFor.VEUVAGE_DECES);
            }

            if (!traiteRedondance || !isRedondant) {
                // Normalement l'événement de veuvage ne doit s'appliquer aux personnes encores mariées (seules).
                erreurs.addErreur("L'événement d'annulation veuvage ne peut pas s'appliquer à une personne mariée.");
            }
		}
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

        if (traiteRedondance && isRedondant) {
            return HandleStatus.REDONDANT;
        }

		/*
		 * Obtention du tiers
		 */
		PersonnePhysique veuf = getPrincipalPP();

		/*
		 * Traitement de l'événement
		 */
		try {
			context.getMetierService().annuleVeuvage(veuf, getDate(), getNumeroEvenement());
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
		return HandleStatus.TRAITE;
	}
}
