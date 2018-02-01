package ch.vd.unireg.evenement.civil.interne.obtentionpermis;

import java.util.Collection;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.NationaliteHelper;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.tiers.PersonnePhysique;

public class ObtentionNationaliteNonSuisse extends ObtentionNationalite {

	protected ObtentionNationaliteNonSuisse(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	protected ObtentionNationaliteNonSuisse(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour les tests uniquement
	 */
	@SuppressWarnings({"JavaDoc"})
	protected ObtentionNationaliteNonSuisse(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Integer numeroOfsCommunePrincipale, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, numeroOfsCommunePrincipale, false, context);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// on  ne valide rien...
	}

	@Override
	protected boolean doHandle(PersonnePhysique pp, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		// quelle que soit la nationalité, si l'individu correspond à un non-habitant (= ancien habitant)
		// il faut mettre à jour la nationalité chez nous
		if (pp != null && !pp.isHabitantVD()) {
			final Collection<Nationalite> nationalites = context.getServiceCivil().getNationalites(getNoIndividu(), getDate());
			final List<Nationalite> startAt = NationaliteHelper.startingAt(nationalites, getDate());
			if (startAt == null || startAt.size() == 0) {
				throw new EvenementCivilException("L'individu n°" + getNoIndividu() + " ne possède pas de nationalité qui commence à la date = " + RegDateHelper.dateToDisplayString(getDate()));
			}

			// je prends la première nationalité non-Suisse que je vois
			Nationalite ref = null;
			for (Nationalite candidate : startAt) {
				if (candidate.getPays() == null || !candidate.getPays().isSuisse()) {
					ref = candidate;
					break;
				}
			}
			if (ref == null) {
				throw new EvenementCivilException("L'individu n°" + getNoIndividu() + " ne possède pas de nationalité non-suisse qui commence à la date = " + RegDateHelper.dateToDisplayString(getDate()));
			}
			final Pays pays = ref.getPays();
			final int noOfs = pays != null ? pays.getNoOFS() : ServiceInfrastructureRaw.noPaysInconnu;
			pp.setNumeroOfsNationalite(noOfs);
			Audit.info(getNumeroEvenement(), String.format("L'individu %d (tiers non-habitant %s) a maintenant la nationalité du pays '%s'",
			                                               getNoIndividu(),
			                                               FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()),
			                                               pays != null ? pays.getNomCourt() : "inconnu"));
		}

		Audit.info(getNumeroEvenement(), "Nationalité non suisse : ignorée fiscalement");
		return false;
	}
}
