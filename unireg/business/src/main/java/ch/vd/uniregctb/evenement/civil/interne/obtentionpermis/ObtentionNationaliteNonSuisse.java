package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class ObtentionNationaliteNonSuisse extends ObtentionNationalite {

	protected ObtentionNationaliteNonSuisse(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	protected ObtentionNationaliteNonSuisse(EvenementCivilEch evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour les tests uniquement
	 */
	@SuppressWarnings({"JavaDoc"})
	protected ObtentionNationaliteNonSuisse(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Integer numeroOfsEtenduCommunePrincipale, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, numeroOfsEtenduCommunePrincipale, false, context);
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
			for (Nationalite nationalite : getIndividu().getNationalites()) {
				if (getDate().equals(nationalite.getDateDebutValidite())) {
					pp.setNumeroOfsNationalite(nationalite.getPays().getNoOFS());
					Audit.info(getNumeroEvenement(), String.format("L'individu %d (tiers non-habitant %s) a maintenant la nationalité du pays '%s'",
							getNoIndividu(), FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()), nationalite.getPays().getNomMinuscule()));
					break;
				}
			}
		}

		Audit.info(getNumeroEvenement(), "Nationalité non suisse : ignorée fiscalement");
		return false;
	}
}
