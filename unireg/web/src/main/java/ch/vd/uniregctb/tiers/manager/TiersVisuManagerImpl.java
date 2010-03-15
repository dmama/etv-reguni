package ch.vd.uniregctb.tiers.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.*;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailComparator;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.tiers.view.AdresseViewComparator;
import ch.vd.uniregctb.tiers.view.TiersVisuView;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Service qui fournit les methodes pour visualiser un tiers
 *
 * @author xcifde
 *
 */
public class TiersVisuManagerImpl extends TiersManager implements TiersVisuManager {

	private MouvementVisuManager mouvementVisuManager;

	public void setMouvementVisuManager(MouvementVisuManager mouvementVisuManager) {
		this.mouvementVisuManager = mouvementVisuManager;
	}

	/**
	 * Charge les informations dans TiersVisuView
	 *
	 * @param numero
	 * @return un objet TiersVisuView
	 * @throws AdressesResolutionException
	 */
	public TiersVisuView getView(Long numero, boolean adressesHisto, WebParamPagination webParamPagination) throws AdresseException, InfrastructureException {

		TiersVisuView tiersVisuView = new TiersVisuView();
		tiersVisuView.setAdressesHisto(adressesHisto);

		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		setTiersGeneralView(tiersVisuView, tiers);

		if (tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isHabitant()) {
				setHabitant(tiersVisuView, pp);
			} else {
				tiersVisuView.setTiers(pp);
			}
		}
		else if (tiers instanceof MenageCommun) {
			MenageCommun menageCommun = (MenageCommun) tiers;
			setMenageCommun(tiersVisuView, menageCommun);
		}
		else if (tiers instanceof Entreprise) {
			Entreprise entreprise = (Entreprise) tiers;
			setEntreprise(tiersVisuView, entreprise);
		}
		else if (tiers instanceof AutreCommunaute) {
			AutreCommunaute autreCommunaute = (AutreCommunaute) tiers;
			tiersVisuView.setTiers(autreCommunaute);
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
			setDebiteurPrestationImposable(tiersVisuView, dpi, webParamPagination);
			setContribuablesAssocies(tiersVisuView, dpi);
			setForsFiscauxDebiteur(tiersVisuView, dpi);
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			tiersVisuView.setTiers(tiers);
		}

		if(tiersVisuView.getTiers() != null){
			if (tiers instanceof Contribuable) {
				Contribuable contribuable = (Contribuable) tiers;
				tiersVisuView.setDebiteurs(getDebiteurs(contribuable));
				tiersVisuView.setDis(getDeclarationsImpotOrdinaire(contribuable));
				tiersVisuView.setMouvements(getMouvements(contribuable));
				setForsFiscaux(tiersVisuView, contribuable);
				setSituationsFamille(tiersVisuView, contribuable);
			}

			tiersVisuView.setHistoriqueAdresses(getAdressesHistoriques(tiers, adressesHisto));

			List<RapportView> rapportsView = getRapports(tiers);

			// filtrer les rapports entre tiers si l'utiliateur a des droits en visu limitée
			if (SecurityProvider.isGranted(Role.VISU_LIMITE)) {
				for (RapportView rv : rapportsView) {
					if (!TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(rv.getTypeRapportEntreTiers())){

					}
				}
			}
			if (tiers instanceof PersonnePhysique) {
				PersonnePhysique pp = (PersonnePhysique) tiers;
				if (pp.getNumeroIndividu() != null && pp.getNumeroIndividu() != 0) {
					List<RapportView> rapportsFiliationView = getRapportsFiliation(pp);
					rapportsView.addAll(rapportsFiliationView);
				}
			}
			tiersVisuView.setDossiersApparentes(rapportsView);
			Map<String, Boolean> allowedOnglet = initAllowedModif();
			setDroitEdition(tiers, allowedOnglet);
			tiersVisuView.setAllowedOnglet(allowedOnglet);
		}

		return tiersVisuView;
	}

	/**
	 * initialise les droits d'édition des onglets du tiers
	 * @return la map de droit d'édition des onglets
	 */
	private Map<String, Boolean> initAllowedModif(){
		Map<String, Boolean> allowedModif = new HashMap<String, Boolean>();
		allowedModif.put(TiersVisuView.MODIF_FISCAL, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_ADRESSE, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_CIVIL, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_COMPLEMENT, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_DEBITEUR, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_DOSSIER, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_RAPPORT, Boolean.FALSE);
		allowedModif.put(TiersVisuView.MODIF_MOUVEMENT, Boolean.FALSE);

		return allowedModif;
	}


	/**
	 * Recuperation des adresses historiques
	 *
	 * @param tiers
	 * @param adresseActive
	 * @return List<AdresseView>
	 * @throws AdressesResolutionException
	 */
	private List<AdresseView> getAdressesHistoriques(Tiers tiers, boolean adresseHisto) throws AdresseException{

		List<AdresseView> adresses = new ArrayList<AdresseView>();

		if (adresseHisto) {
			final AdressesFiscalesHisto adressesFiscalHisto = adresseService.getAdressesFiscalHisto(tiers, false);
			if (adressesFiscalHisto != null) {
				// rempli tous les types d'adresse
				for (TypeAdresseTiers type : TypeAdresseTiers.values()) {
					fillAdressesView(adresses, adressesFiscalHisto, type, tiers);
				}
			}
		}
		else {
			final AdressesFiscales adressesFiscales = adresseService.getAdressesFiscales(tiers, null, false);
			if (adressesFiscales != null) {
				// rempli tous les types d'adresse
				for (TypeAdresseTiers type : TypeAdresseTiers.values()) {
					fillAdressesView(adresses, adressesFiscales, type, tiers);
				}
			}
		}

		Collections.sort(adresses, new AdresseViewComparator());

		return adresses;
	}

	/**
	 * Rempli la collection des adressesView avec les adresses fiscales historiques du type spécifié.
	 */
	private void fillAdressesView(List<AdresseView> adressesView, final AdressesFiscalesHisto adressesFiscalHisto, TypeAdresseTiers type,
			Tiers tiers) {

		final Collection<AdresseGenerique> adresses = adressesFiscalHisto.ofType(type);
		if (adresses == null) {
			// rien à faire
			return;
		}

		for (AdresseGenerique adresse : adresses) {
			AdresseView adresseView = createVisuAdresseView(adresse, type, tiers);
			adressesView.add(adresseView);
		}
	}

	/**
	 * Methode annexe de creation d'adresse view pour un type donne
	 *
	 * @param addProf
	 * @param type
	 * @return
	 * @throws InfrastructureException
	 */
	private AdresseView createVisuAdresseView(	AdresseGenerique adr, TypeAdresseTiers type,
											Tiers tiers) {
		AdresseView adresseView = createAdresseView(adr, type, tiers);

		RegDate dateJour = RegDate.get();
		if (		((adr.getDateDebut() == null) || (adr.getDateDebut().isBeforeOrEqual(dateJour)))
				&& 	((adr.getDateFin() == null) || (adr.getDateFin().isAfterOrEqual(dateJour))) ) {
			adresseView.setActive(true);
		} else {
			adresseView.setActive(false);
		}


		return adresseView;
	}

	/**
	 * Mise à jour de la vue Declaration Impot Ordinaire
	 *
	 * @param contribuable
	 * @return
	 */
	private List<DeclarationImpotDetailView> getDeclarationsImpotOrdinaire(Contribuable contribable) {

		List<DeclarationImpotDetailView> disView = new ArrayList<DeclarationImpotDetailView>();
		Set<Declaration> declarations = contribable.getDeclarations();
		for (Declaration declaration : declarations) {
			if (declaration instanceof DeclarationImpotOrdinaire) {
				DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) declaration;
				DeclarationImpotDetailView diView = new DeclarationImpotDetailView();
				diView.setId(di.getId());
				diView.setDateDebutPeriodeImposition(di.getDateDebut());
				diView.setDateFinPeriodeImposition(di.getDateFin());
				diView.setPeriodeFiscale(di.getPeriode() != null ? di.getPeriode().getAnnee() : null);
				diView.setAnnule(di.isAnnule());
				final EtatDeclaration dernierEtat = di.getDernierEtat();
				diView.setEtat(dernierEtat == null ? null : dernierEtat.getEtat());
				diView.setDelaiAccorde(di.getDelaiAccordeAu());
				diView.setDateRetour(di.getDateRetour());
				disView.add(diView);
			}
		}
		Collections.sort(disView, new DeclarationImpotDetailComparator());
		return disView;
	}

	/**
	 * Mise à jour de la vue MouvementDetailView
	 *
	 * @param contribuable
	 * @return
	 */
	private List<MouvementDetailView> getMouvements(Contribuable contribuable) throws InfrastructureException{

		final List<MouvementDetailView> mvtsView = new ArrayList<MouvementDetailView>();
		final Set<MouvementDossier> mvts = contribuable.getMouvementsDossier();
		for (MouvementDossier mvt : mvts) {
			if (mvt.getEtat().isTraite()) {
				final MouvementDetailView mvtView = mouvementVisuManager.getView(mvt);
				mvtsView.add(mvtView);
			}
		}
		Collections.sort(mvtsView);
		return mvtsView;
	}
}
