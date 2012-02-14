package ch.vd.uniregctb.evenement.civil.interne.changement.identificateur;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.interne.changement.ChangementBase;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class ChangementIdentificateur extends ChangementBase {
	
	protected ChangementIdentificateur(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	public ChangementIdentificateur(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected ChangementIdentificateur(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final long noIndividu = getNoIndividu();
		Audit.info(getNumeroEvenement(), String.format("Traitement du changement d'identificateur de l'individu : %d", noIndividu));

		final PersonnePhysique pp = getPrincipalPP();
		if (pp != null && !pp.isHabitantVD()) {
			// pour les non-habitants, il faut recharger les données, non?
			// quelles sont les données à recharger ? NAVS13 pour sûr !
			final Individu individu = context.getTiersService().getIndividu(pp);
			final String navs13 = individu.getNouveauNoAVS();
			if (shouldOverwriteAvs(navs13)) {
				pp.setNumeroAssureSocial(StringUtils.trimToNull(navs13));
			}
		}

		return super.handle(warnings);
	}
	
	protected boolean shouldOverwriteAvs(String navs13) {
		return StringUtils.isNotBlank(navs13);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		/* l'existance de l'individu est vérifié dans validateCommon */
	}
}
