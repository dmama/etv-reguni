package ch.vd.uniregctb.evenement.civil.interne.changement.adresseNotification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.changement.AbstractChangementHandler;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class ModificationAdresseNotificationHandler extends AbstractChangementHandler {

	private AdresseService adresseService;

	private ServiceInfrastructureService infraService;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	private CommuneSimple getCommuneAtDate(PersonnePhysique pp, RegDate date) throws EvenementCivilHandlerException {
		try {
			final AdressesCiviles adresses = adresseService.getAdressesCiviles(pp, date, false);
			return infraService.getCommuneByAdresse(adresses.principale);
		}
		catch (InfrastructureException e) {
			final String msg = String.format("Impossible de trouver la commune de l'adresse principale au %s de l'individu %d", RegDateHelper.dateToDisplayString(date), pp.getNumeroIndividu());
			throw new EvenementCivilHandlerException(msg, e);
		}
		catch (AdresseException e) {
			final String msg = String.format("Impossible de résoudre les adresses civiles au %s de l'individu %d", RegDateHelper.dateToDisplayString(date), pp.getNumeroIndividu());
			throw new EvenementCivilHandlerException(msg, e);
		}
	}

	@Override
	protected boolean autoriseIndividuInconnuFiscalement() {
		return false;
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Throwable.class)
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		final long noIndividu = evenement.getNoIndividu();
		Audit.info(evenement.getNumeroEvenement(), String.format("%s de l'individu : %d", evenement.getType().getDescription(), noIndividu));

		final PersonnePhysique pp = getService().getPersonnePhysiqueByNumeroIndividu(noIndividu);
		if (pp == null) {
			throw new EvenementCivilHandlerException("Impossible de retrouver le tiers correspondant à l'individu " + noIndividu);
		}

		if (TypeEvenementCivil.CORREC_ADRESSE == evenement.getType()) {
			// [UNIREG-1892] on ignore la date de l'événement et on ne prend en compte que
			// la date de traitement par rapport au for actif sur la personne
			final RegDate dateTraitement = RegDate.get();
			final ForFiscalPrincipal ffpActif = pp.getForFiscalPrincipalAt(dateTraitement);
			final Integer ofsCommune;
			if (ffpActif == null) {
				// on regarde un couple, éventuellement
				final EnsembleTiersCouple ensemble = getService().getEnsembleTiersCouple(pp, dateTraitement);
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
				final Individu individu = evenement.getIndividu();
				if (!individu.isMineur(dateTraitement)) {

					final String msg = String.format("Impossible de trouver la commune du for actif au %s de l'indidivu %d (ctb %s)",
													RegDateHelper.dateToDisplayString(dateTraitement), noIndividu, FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()));
					throw new EvenementCivilHandlerException(msg);
				}
			}
			else {
				// la commune d'annonce de l'événement n'est pas forcément la commune de domicile
				// de l'individu, donc il faut aller chercher la commune de domicile...
				final CommuneSimple commune = getCommuneAtDate(pp, dateTraitement);
				if (commune == null || commune.getNoOFSEtendu() != ofsCommune) {
					throw new EvenementCivilHandlerException("Evénement de correction d'adresse avec changement de commune");
				}
			}
		}
		else if (TypeEvenementCivil.MODIF_ADRESSE_NOTIFICATION == evenement.getType()) {
			// Fermetures des adresses temporaires dans le fiscal
			fermeAdresseTiersTemporaire(pp, evenement.getDate().getOneDayBefore());
		}

		return super.handle(evenement, warnings);
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new ModificationAdresseNotificationAdapter(event, context, this);
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		final Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.MODIF_ADRESSE_NOTIFICATION);
		types.add(TypeEvenementCivil.CORREC_ADRESSE);
		return types;
	}
}
