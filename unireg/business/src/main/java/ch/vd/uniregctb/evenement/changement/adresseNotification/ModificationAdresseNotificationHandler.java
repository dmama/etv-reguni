package ch.vd.uniregctb.evenement.changement.adresseNotification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.changement.AbstractChangementHandler;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
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

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Throwable.class)
	public void handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		final long noIndividu = evenement.getIndividu().getNoTechnique();
		Audit.info(evenement.getNumeroEvenement(), String.format("%s de l'individu : %d", evenement.getType().getDescription(), noIndividu));

		// Fermetures des adresses temporaires dans le fiscal
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
				final String msg = String.format("Impossible de trouver la commune du for actif au %s de l'indidivu %d (ctb %s)",
												RegDateHelper.dateToDisplayString(dateTraitement), noIndividu, FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()));
				throw new EvenementCivilHandlerException(msg);
			}
			else {
				// la commune d'annonce de l'événement n'est pas forcément la commune de domicile
				// de l'individu, donc il faut aller chercher la commune de domicile...
				final AdressesCiviles adressesCiviles = adresseService.getAdressesCiviles(pp, dateTraitement);
				try {
					final Commune commune = infraService.getCommuneByAdresse(adressesCiviles.principale);
					if (commune == null || commune.getNoOFSEtendu() != ofsCommune) {
						throw new EvenementCivilHandlerException("Evénement de correction d'adresse avec changement de commune");
					}
				}
				catch (InfrastructureException e) {
					final String msg = String.format("Impossible de trouver la commune de l'adresse principale au %s de l'individu %d", RegDateHelper.dateToDisplayString(dateTraitement), noIndividu);
					throw new EvenementCivilHandlerException(msg, e);
				}
			}
		}
		else if (TypeEvenementCivil.MODIF_ADRESSE_NOTIFICATION == evenement.getType()) {
			fermeAdresseTiersTemporaire(pp, evenement.getDate().getOneDayBefore());
		}

		super.handle(evenement, warnings);
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new ModificationAdresseNotificationAdapter();
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.MODIF_ADRESSE_NOTIFICATION);
		types.add(TypeEvenementCivil.CORREC_ADRESSE);
		return types;
	}
}
