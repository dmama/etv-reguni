package ch.vd.unireg.evenement.civil.interne.changement.identificateur;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.interne.changement.ChangementBase;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.tiers.PersonnePhysique;

public class ChangementIdentificateur extends ChangementBase {
	
	protected ChangementIdentificateur(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	public ChangementIdentificateur(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
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
		context.audit.info(getNumeroEvenement(), String.format("Traitement du changement d'identificateur de l'individu : %d", noIndividu));

		final PersonnePhysique pp = getPrincipalPP();
		if (pp != null && !pp.isHabitantVD()) {
			// pour les non-habitants, il faut recharger les données, non?
			// quelles sont les données à recharger ? NAVS13 pour sûr !
			final Individu individu = context.getTiersService().getIndividu(pp);
			final String navs13 = individu.getNouveauNoAVS();
			if (shouldOverwriteAvs(navs13)) {
				pp.setNumeroAssureSocial(StringUtils.trimToNull(navs13));
			}

			// .. et aussi les noms officiels des parents (qui viennent aussi des données UPI)

			// noms et prénoms officiels de la mère
			final NomPrenom nomOfficielMere = individu.getNomOfficielMere();
			if (nomOfficielMere != null) {
				pp.setNomMere(nomOfficielMere.getNom());
				pp.setPrenomsMere(nomOfficielMere.getPrenom());
			}

			// nom et prénoms officiels du père
			final NomPrenom nomOfficielPere = individu.getNomOfficielPere();
			if (nomOfficielPere != null) {
				pp.setNomPere(nomOfficielPere.getNom());
				pp.setPrenomsPere(nomOfficielPere.getPrenom());
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
