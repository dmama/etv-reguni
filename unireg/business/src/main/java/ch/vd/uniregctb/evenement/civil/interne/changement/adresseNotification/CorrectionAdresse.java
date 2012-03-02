package ch.vd.uniregctb.evenement.civil.interne.changement.adresseNotification;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class CorrectionAdresse extends ModificationAdresseBase {

	public CorrectionAdresse(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	public CorrectionAdresse(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
	}

	@Override
	protected void doHandle(PersonnePhysique pp, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		// [UNIREG-1892] on ignore la date de l'événement et on ne prend en compte que
		// la date de traitement par rapport au for actif sur la personne
		final RegDate dateTraitement = RegDate.get();
		final ForFiscalPrincipal ffpActif = pp.getForFiscalPrincipalAt(dateTraitement);
		final Integer ofsCommune;
		if (ffpActif == null) {
			// on regarde un couple, éventuellement
			final EnsembleTiersCouple ensemble = context.getTiersService().getEnsembleTiersCouple(pp, dateTraitement);
			if (ensemble == null) {
				ofsCommune = null;
			}
			else {
				final MenageCommun mc = ensemble.getMenage();
				final ForFiscalPrincipal ffpActifMenage = mc.getForFiscalPrincipalAt(dateTraitement);
				if (ffpActifMenage != null && ffpActifMenage.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					ofsCommune = ffpActifMenage.getNumeroOfsAutoriteFiscale();
				}
				else {
					ofsCommune = null;
				}
			}
		}
		else if (ffpActif.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			ofsCommune = ffpActif.getNumeroOfsAutoriteFiscale();
		}
		else {
			ofsCommune = null;
		}

		if (ofsCommune == null) {

			// si l'individu est mineur... on laisse passer...
			final Individu individu = getIndividu();
			if (!individu.isMineur(dateTraitement)) {

				final String msg = String.format("Impossible de trouver la commune du for actif au %s de l'individu %d (ctb %s)",
												RegDateHelper.dateToDisplayString(dateTraitement), pp.getNumeroIndividu(), FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()));
				throw new EvenementCivilException(msg);
			}
		}
		else {
			// la commune d'annonce de l'événement n'est pas forcément la commune de domicile
			// de l'individu, donc il faut aller chercher la commune de domicile...
			final Commune commune = getCommuneDomicileAtDate(pp, dateTraitement);
			if (commune == null || commune.getNoOFSEtendu() != ofsCommune) {
				throw new EvenementCivilException("Evénement de correction d'adresse avec changement de commune");
			}
		}
	}

	private Commune getCommuneDomicileAtDate(PersonnePhysique pp, RegDate date) throws EvenementCivilException {
		try {
			final AdressesCiviles adresses = context.getAdresseService().getAdressesCiviles(pp, date, false);
			return context.getServiceInfra().getCommuneByAdresse(adresses.principale, date);
		}
		catch (ServiceInfrastructureException e) {
			final String msg = String.format("Impossible de trouver la commune de l'adresse principale au %s de l'individu %d", RegDateHelper.dateToDisplayString(date), pp.getNumeroIndividu());
			throw new EvenementCivilException(msg, e);
		}
		catch (AdresseException e) {
			final String msg = String.format("Impossible de résoudre les adresses civiles au %s de l'individu %d", RegDateHelper.dateToDisplayString(date), pp.getNumeroIndividu());
			throw new EvenementCivilException(msg, e);
		}
	}
}
