package ch.vd.uniregctb.couple.manager;

import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.couple.view.CoupleListView;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.manager.TiersManager;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Service offrant des methodes pour le controller PersonnePhysiqueListController
 *
 * @author xcifde
 *
 */
public class CoupleListManagerImpl extends TiersManager implements CoupleListManager {

	private final String PREMIERE_PAGE = "premier";
	private final String SECONDE_PAGE = "second";

	/**
	 * Alimente la vue CoupleListView (cas ou numeroPP selectionne)
	 *
	 * @param numeroPP
	 * @return une vue CoupleListView
	 */
	public CoupleListView get(Long numeroPP) {
		Tiers tiers = tiersService.getTiers(numeroPP) ;
		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.pp.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}
		PersonnePhysique pp = null;
		if (tiers instanceof PersonnePhysique) {
			pp = (PersonnePhysique) tiers;
		}
		else {
			return get();
		}
		//vérification des droits sur la première personne
		if (!checkDroitEdit(pp)) {
			CoupleListView coupleListView = get();
			coupleListView.setAllowed(false);
			return coupleListView;
		}
		TiersGeneralView premierPPView = tiersGeneralManager.get(pp, true);
		CoupleListView coupleListView = new CoupleListView();
		coupleListView.setAllowed(true);
		coupleListView.setPremierePersonne(premierPPView);
		coupleListView.setNumeroPremierePersonne(numeroPP);
		coupleListView.setPage(SECONDE_PAGE);
		//gestion des droits
		setDroit(coupleListView);

		return coupleListView;
	}

	/**
	 * Alimente la vue CoupleListView (cas ou numeroPP non selectionne)
	 *
	 * @return une vue CoupleListView
	 */
	public CoupleListView get() {
		CoupleListView coupleListView = new CoupleListView();
		coupleListView.setAllowed(true);
		coupleListView.setTypeRechercheDuNom(CoupleListView.TypeRecherche.EST_EXACTEMENT);
		coupleListView.setPage(PREMIERE_PAGE);
		//gestion des droits
		setDroit(coupleListView);

		return coupleListView;
	}

	/**
	 * value le type de recherche en fonction des droits de l'utilisateur
	 */
	private void setDroit(CoupleListView coupleListView) {
		boolean droitHab = SecurityProvider.isGranted(Role.MODIF_HAB_DEBPUR);
		boolean droitNHab = SecurityProvider.isGranted(Role.MODIF_NONHAB_DEBPUR);
		if (SecurityProvider.isGranted(Role.MODIF_VD_ORD) ||
			SecurityProvider.isGranted(Role.MODIF_VD_SOURC) ||
			SecurityProvider.isGranted(Role.MODIF_HC_HS)) {
			droitHab = true;
			droitNHab = true;
		}
		if (droitHab && droitNHab) {
			coupleListView.setTypeTiers(TiersCriteriaView.TypeTiers.PERSONNE_PHYSIQUE);
		}
		else if (droitHab) {
			coupleListView.setTypeTiers(TiersCriteriaView.TypeTiers.HABITANT);
		}
		else if (droitNHab) {
			coupleListView.setTypeTiers(TiersCriteriaView.TypeTiers.NON_HABITANT);
		}
		else {//pas les droits
			coupleListView.setAllowed(false);
		}
	}
}
