package ch.vd.uniregctb.tiers.manager;

import java.util.List;

import ch.vd.uniregctb.tiers.*;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.view.SituationFamilleView;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Service à disposition du controller TiersSituationFamilleController
 *
 * @author xcifde
 *
 */
public class SituationFamilleManagerImpl extends TiersManager implements SituationFamilleManager {

	private static final String SITUATION_FAMILLE = "SituationFamille";

	private static final String SITUATION_FAMILLE_MENAGE_COMMUN = "SituationFamilleMenageCommun";

	private SituationFamilleDAO situationFamilleDAO;

	private EvenementFiscalService evenementFiscalService;

	/**
	 * Annule une situation de famille
	 *
	 * @param idSituationFamille
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerSituationFamille(Long idSituationFamille) {
		situationFamilleService.annulerSituationFamille(idSituationFamille);
	}

	/**
	 * Cree une nouvelle vue SituationFamilleView
	 *
	 * @param numeroCtb
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public SituationFamilleView create(Long numeroCtb) throws AdresseException {
		final Contribuable contribuable = (Contribuable) tiersService.getTiers(numeroCtb);

		if (contribuable == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant", null,
					WebContextUtils.getDefaultLocale()));
		}

		final SituationFamilleView situationFamilleView = new SituationFamilleView();
		if ((contribuable instanceof PersonnePhysique || contribuable instanceof MenageCommun) && SecurityProvider.isGranted(Role.SIT_FAM)
				&& SecurityProvider.getDroitAcces(contribuable) != null && isSituationFamilleActive(contribuable)) {

			situationFamilleView.setNumeroCtb(numeroCtb);
			situationFamilleView.setAllowed(true);
			if (contribuable instanceof MenageCommun) {
				situationFamilleView.setNatureSituationFamille(SITUATION_FAMILLE_MENAGE_COMMUN);
				final EnsembleTiersCouple ensembleCouple = tiersService.getEnsembleTiersCouple((MenageCommun) contribuable, null);
				final PersonnePhysique ppPrincipal = ensembleCouple.getPrincipal();
				situationFamilleView.setNumeroTiers1(ppPrincipal.getNumero());

				final List<String> nomCourrierPrincipal = adresseService.getNomCourrier(ppPrincipal, null, false);
				situationFamilleView.setNomCourrier1Tiers1(nomCourrierPrincipal.get(0));

				final PersonnePhysique ppConjoint = ensembleCouple.getConjoint();
				// Suite au cas UNIREG-1278 on teste la presence du conjoint afin d'éviter un NPE
				// dans le cas d'un marié seul
				if (ppConjoint != null) {
					situationFamilleView.setNumeroTiers2(ppConjoint.getNumero());

					final List<String> nomCourrierConjoint = adresseService.getNomCourrier(ppConjoint, null, false);
					situationFamilleView.setNomCourrier1Tiers2(nomCourrierConjoint.get(0));
				}
				situationFamilleView.setNumeroTiersRevenuPlusEleve(situationFamilleView.getNumeroTiers1());
			}
			else {
				situationFamilleView.setNatureSituationFamille(SITUATION_FAMILLE);
			}
		}
		else {
			situationFamilleView.setAllowed(false);
		}
		return situationFamilleView;
	}

	/**
	 * Sauvegarde de la situation de famille
	 *
	 * @param situationFamilleView
	 * @return
	 */
	@Transactional(rollbackFor = Throwable.class)
	public SituationFamille save(SituationFamilleView situationFamilleView) {

		Contribuable contribuable = (Contribuable) tiersService.getTiers(situationFamilleView.getNumeroCtb());

		if (situationFamilleView.getNatureSituationFamille().equals(SITUATION_FAMILLE_MENAGE_COMMUN)) {
			SituationFamilleMenageCommun situationFamilleMenageCommun = new SituationFamilleMenageCommun();
			situationFamilleMenageCommun.setAnnule(situationFamilleView.isAnnule());
			RegDate dateDebut = RegDate.get(situationFamilleView.getDateDebut());
			situationFamilleMenageCommun.setDateDebut(dateDebut);
			situationFamilleMenageCommun.setDateFin(null);
			situationFamilleMenageCommun.setEtatCivil(situationFamilleView.getEtatCivil());
			situationFamilleMenageCommun.setNombreEnfants(situationFamilleView.getNombreEnfants());
			situationFamilleMenageCommun.setTarifApplicable(situationFamilleView.getTarifImpotSource());
			situationFamilleMenageCommun.setContribuablePrincipalId(situationFamilleView.getNumeroTiersRevenuPlusEleve());
			contribuable.closeSituationFamilleActive(dateDebut.addDays(-1));
			contribuable.addSituationFamille(situationFamilleMenageCommun);
			evenementFiscalService.publierEvenementFiscalChangementSituation(contribuable, dateDebut, new Long(1));
			return situationFamilleMenageCommun;

		}
		else {
			SituationFamille situationFamille = new SituationFamillePersonnePhysique();
			situationFamille.setAnnule(situationFamilleView.isAnnule());
			RegDate dateDebut = RegDate.get(situationFamilleView.getDateDebut());
			situationFamille.setDateDebut(dateDebut);
			situationFamille.setDateFin(null);
			situationFamille.setEtatCivil(situationFamilleView.getEtatCivil());
			situationFamille.setNombreEnfants(situationFamilleView.getNombreEnfants());
			contribuable.closeSituationFamilleActive(dateDebut.addDays(-1));
			contribuable.addSituationFamille(situationFamille);
			evenementFiscalService.publierEvenementFiscalChangementSituation(contribuable, dateDebut, new Long(1));
			return situationFamille;
		}
	}

	public SituationFamilleDAO getSituationFamilleDAO() {
		return situationFamilleDAO;
	}

	public void setSituationFamilleDAO(SituationFamilleDAO situationFamilleDAO) {
		this.situationFamilleDAO = situationFamilleDAO;
	}

	public EvenementFiscalService getEvenementFiscalService() {
		return evenementFiscalService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}
}
