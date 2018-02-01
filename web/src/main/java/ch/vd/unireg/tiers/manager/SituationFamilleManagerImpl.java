package ch.vd.unireg.tiers.manager;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.tiers.SituationFamilleDAO;
import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.tiers.SituationFamillePersonnePhysique;
import ch.vd.unireg.tiers.view.SituationFamilleView;

/**
 * Service à disposition du controller SituationFamilleController
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
	@Override
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
	@Override
	public SituationFamilleView create(Long numeroCtb) throws AdresseException {
		final Contribuable contribuable = (Contribuable) tiersService.getTiers(numeroCtb);

		if (contribuable == null) {
			throw new TiersNotFoundException(numeroCtb);
		}

		final SituationFamilleView situationFamilleView = new SituationFamilleView();
		situationFamilleView.setNumeroCtb(numeroCtb);

		if ((contribuable instanceof PersonnePhysique || contribuable instanceof MenageCommun) && SecurityHelper.isGranted(securityProvider, Role.SIT_FAM)
				&& SecurityHelper.getDroitAcces(securityProvider, contribuable) != null && isSituationFamilleActive(contribuable)) {

			situationFamilleView.setEditable(true);
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
			situationFamilleView.setEditable(false);
		}
		return situationFamilleView;
	}

	/**
	 * Sauvegarde de la situation de famille
	 *
	 * @param situationFamilleView
	 */
	@Override
	public void save(SituationFamilleView situationFamilleView) {

		final ContribuableImpositionPersonnesPhysiques contribuable = (ContribuableImpositionPersonnesPhysiques) tiersService.getTiers(situationFamilleView.getNumeroCtb());

		final SituationFamille situationFamille;
		if (situationFamilleView.getNatureSituationFamille().equals(SITUATION_FAMILLE_MENAGE_COMMUN)) {
			final SituationFamilleMenageCommun situationFamilleMenageCommun = new SituationFamilleMenageCommun();
			situationFamilleMenageCommun.setTarifApplicable(situationFamilleView.getTarifImpotSource());
			situationFamilleMenageCommun.setContribuablePrincipalId(situationFamilleView.getNumeroTiersRevenuPlusEleve());
			situationFamille = situationFamilleMenageCommun;
		}
		else {
			situationFamille = new SituationFamillePersonnePhysique();
		}

		situationFamille.setAnnule(situationFamilleView.isAnnule());
		final RegDate dateDebut = RegDateHelper.get(situationFamilleView.getDateDebut());
		situationFamille.setDateDebut(dateDebut);
		situationFamille.setDateFin(null);
		situationFamille.setEtatCivil(situationFamilleView.getEtatCivil());
		situationFamille.setNombreEnfants(situationFamilleView.getNombreEnfants());
		contribuable.closeSituationFamilleActive(dateDebut.addDays(-1));

		tiersDAO.addAndSave(contribuable, situationFamille);
		evenementFiscalService.publierEvenementFiscalChangementSituationFamille(dateDebut, contribuable);
	}

	@Override
	public @Nullable Contribuable getContribuableForSituation(long situationId) {
		final SituationFamille situation= situationFamilleDAO.get(situationId);
		return situation == null ? null : situation.getContribuable();
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setSituationFamilleDAO(SituationFamilleDAO situationFamilleDAO) {
		this.situationFamilleDAO = situationFamilleDAO;
	}
}
