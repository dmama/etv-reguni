package ch.vd.uniregctb.couple.manager;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.couple.CoupleHelper;
import ch.vd.uniregctb.couple.CoupleHelper.Couple;
import ch.vd.uniregctb.couple.view.CoupleRecapView;
import ch.vd.uniregctb.couple.view.TypeUnion;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.manager.TiersManager;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 *  Methodes pour gerer CoupleRecapController
 *
 * @author xcifde
 *
 */
public class CoupleRecapManagerImpl extends TiersManager implements CoupleRecapManager {

	private MetierService metierService;

	private CoupleHelper coupleHelper;

	public MetierService getMetierService() {
		return metierService;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	/**
	 * Alimente la vue CoupleRecapView avec un seul membre
	 *
	 * @param numeroPremier
	 * @return
	 */
	public CoupleRecapView get(Long numeroPremierPP)  {
		CoupleRecapView coupleRecapView = new CoupleRecapView();
		initializeCouple(getPP(numeroPremierPP), null, null, coupleRecapView);
		return coupleRecapView;
	}

	/**
	 * Alimente la vue CoupleRecapView
	 *
	 * @param numeroPremier
	 * @param numeroSecond
	 * @return
	 */
	public CoupleRecapView get(Long numeroPremierPP, Long numeroSecondPP)  {
		CoupleRecapView coupleRecapView = new CoupleRecapView();
		initializeCouple(getPP(numeroPremierPP), getPP(numeroSecondPP), null, coupleRecapView);
		return coupleRecapView;
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.couple.manager.CoupleRecapManager#get(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public CoupleRecapView get(Long numeroPremierPP, Long numeroSecondPP, Long numeroCTB) {
		CoupleRecapView coupleRecapView = new CoupleRecapView();
		PersonnePhysique pp1 = getPP(numeroPremierPP);
		PersonnePhysique pp2 = numeroSecondPP != null ? getPP(numeroSecondPP) : null;
		Contribuable nh = getCTB(numeroCTB);
		initializeCouple(pp1, pp2, nh, coupleRecapView);
		return coupleRecapView;
	}

	private Contribuable getCTB(Long numeroCTB) {
		Contribuable contribuable = (Contribuable) tiersService.getTiers(numeroCTB);
		
		if (contribuable == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.contribuable.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		if (!checkDroitEdit(contribuable)) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.contribuable.droit.modification" , null,  WebContextUtils.getDefaultLocale()));
		}
		return contribuable;
	}

	private PersonnePhysique getPP(Long numero) {
		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(numero);
		if (pp == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.pp.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}

		if (!checkDroitEdit(pp)) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.droit.modification" , null,  WebContextUtils.getDefaultLocale()));
		}
		return pp;
	}

	private void initializeCouple(PersonnePhysique premierPP, PersonnePhysique secondPP, Contribuable contribuableCouple, CoupleRecapView coupleRecapView) {
		coupleRecapView.setPremierePersonne(tiersGeneralManager.get(premierPP, true));
		if(secondPP != null) {
			coupleRecapView.setSecondePersonne(tiersGeneralManager.get(secondPP, true));
		}
		if (contribuableCouple != null) {
			coupleRecapView.setTroisiemeTiers(tiersGeneralManager.get(contribuableCouple));
			coupleRecapView.setNouveauCtb(false);
		}

		if (premierPP != null && secondPP != null) {
			// détermination de l'état civil du couple (MARIE, PACS)
			final boolean memeSexe = tiersService.isMemeSexe(premierPP, secondPP);
			coupleRecapView.setEtatCivil(memeSexe ? EtatCivil.LIE_PARTENARIAT_ENREGISTRE : EtatCivil.MARIE);
		}
		else {
			coupleRecapView.setEtatCivil(EtatCivil.MARIE);
		}

		// type d'union et date de début
		TypeUnion type = TypeUnion.COUPLE;
		if (secondPP == null) {
			// cas d'un(e) marié(e) seul(e)
			type = TypeUnion.SEUL;
			initializeDateCouple(contribuableCouple, coupleRecapView);
		}
		else {
			final EnsembleTiersCouple couplePP1 = tiersService.getEnsembleTiersCouple(premierPP, null);
			final EnsembleTiersCouple couplePP2 = tiersService.getEnsembleTiersCouple(secondPP, null);

			// Si le ou les contribuables ne font partie d’un ménage commun ouvert...
			if (couplePP1 == null && couplePP2 == null) {
				final RapportEntreTiers dernierRapportPP1 = premierPP.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				final RapportEntreTiers dernierRapportPP2 = secondPP.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				if (dernierRapportPP1 != null && dernierRapportPP2 != null) {
					final MenageCommun menagePP1 = (MenageCommun) dernierRapportPP1.getObjet();
					final MenageCommun menagePP2 = (MenageCommun) dernierRapportPP2.getObjet();
					// ...et ont fait partie en dernier lieu d’un même ménage commun fermé pour cause de séparation...
					if (menagePP1 != null && menagePP1 == menagePP2) {
						// ...il s’agit d’une réconciliation
						type = TypeUnion.RECONCILIATION;
					}
				}
				else {
					initializeDateCouple(contribuableCouple, coupleRecapView);
				}
			}
			else if (couplePP1 != null && couplePP2 != null) {
				if (couplePP1.getMenage() != couplePP2.getMenage()) {
					// S'il y a deux ménages communs ouverts...
					type = TypeUnion.FUSION_MENAGES;
					final MenageCommun menagePrincipal = metierService.getMenageForFusion(couplePP1.getMenage(), couplePP2.getMenage());
					if (menagePrincipal != null) {
						final PersonnePhysique principal = menagePrincipal.getPersonnesPhysiques().toArray(new PersonnePhysique[0])[0];
						final RapportEntreTiers rapport = menagePrincipal.getPremierRapportObjet(TypeRapportEntreTiers.APPARTENANCE_MENAGE, principal);
						coupleRecapView.setDateDebut(rapport.getDateDebut().asJavaDate());
					}
				}
			}
			else {
				// S'il y a un seul ménage commun ouvert
				type = TypeUnion.RECONSTITUTION_MENAGE;

				// récuperation de la date de mariage
				final PersonnePhysique pp = couplePP1 != null ? premierPP : secondPP;
				final MenageCommun menage = couplePP1 != null ? couplePP1.getMenage() : couplePP2.getMenage();
				coupleRecapView.setDateDebut(pp.getPremierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE, menage).getDateDebut().asJavaDate());
			}
		}
		coupleRecapView.setTypeUnion(type);
	}

	private void initializeDateCouple(Contribuable contribuableCouple, CoupleRecapView coupleRecapView) {
		if (!coupleRecapView.isNouveauCtb()) {
			if (contribuableCouple instanceof PersonnePhysique) {
				coupleRecapView.setDateCoupleExistant(contribuableCouple.getPremierForFiscalPrincipal().getDateDebut());
			}
		}
	}

	/**
	 * Determine si la personne identifiée par son numéro est en ménage commun
	 *
	 * @param numero
	 * @return
	 */
	public boolean estDejaEnMenage(Long numero) {
		boolean enMenage ;
		PersonnePhysique personne = (PersonnePhysique) tiersService.getTiers(numero);
		MenageCommun menageCommun = getTiersService().findMenageCommun(personne, null);
		if (menageCommun == null) {
			enMenage = false;
		} else {
			enMenage = true;
		}
		return enMenage;
	}

	/**
	 * Persiste le rapport de travail
	 * @param rapportView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public MenageCommun save(CoupleRecapView coupleRecapView) {

		PersonnePhysique premierPP = (PersonnePhysique) tiersService.getTiers(coupleRecapView.getPremierePersonne().getNumero());
		PersonnePhysique secondPP = null;
		if (coupleRecapView.getSecondePersonne() != null) {
			secondPP = (PersonnePhysique) tiersService.getTiers(coupleRecapView.getSecondePersonne().getNumero());
		}

		TypeUnion typeUnion = coupleRecapView.getTypeUnion();
		final RegDate date;
		if ((typeUnion == TypeUnion.COUPLE || typeUnion == TypeUnion.SEUL) && !coupleRecapView.isNouveauCtb()) {
			date = coupleRecapView.getDateCoupleExistant();
		}
		else {
			date = RegDate.get(coupleRecapView.getDateDebut());
		}

		switch (coupleRecapView.getTypeUnion()) {
			case SEUL:
			case COUPLE:
				if (coupleRecapView.getTroisiemeTiers() != null) {
					{
						Tiers tiers = tiersService.getTiers(coupleRecapView.getTroisiemeTiers().getNumero());
						if (tiers instanceof PersonnePhysique) {
							// changement de type de NonHabitant à MenageCommun
							PersonnePhysique pp = (PersonnePhysique) tiers;
							Assert.isTrue(!pp.isHabitant(), "Le contribuable sélectionné comme couple est un habitant");
							Assert.isNull(pp.getNumeroIndividu(), "Le contribuable sélectionné comme couple est un ancien habitant");
							tiersService.changeNHenMenage(pp.getNumero());
							tiersService.getTiersDAO().evict(tiers);
						}
						else {
							// pas besoin de changer le type de tiers, le tiers doit déjà correspondre à un ménage commun annulé
							Assert.isInstanceOf(MenageCommun.class, tiers);
						}
					}
					{
						// rattachement des tiers au ménage
						MenageCommun menage = (MenageCommun) tiersService.getTiers(coupleRecapView.getTroisiemeTiers().getNumero());
						return metierService.rattachToMenage(menage, premierPP, secondPP, date, coupleRecapView.getRemarque(), coupleRecapView.getEtatCivil(), false, null);
					}
				}
				else {
					return metierService.marie(date, premierPP, secondPP, coupleRecapView.getRemarque(), coupleRecapView.getEtatCivil(), false, null);
				}

			case RECONCILIATION:
				return metierService.reconcilie(premierPP, secondPP, date, coupleRecapView.getRemarque(), false, null);

			case RECONSTITUTION_MENAGE:
			{
				Couple couple = coupleHelper.getCoupleForReconstitution(premierPP, secondPP, date);
				return metierService.reconstitueMenage((MenageCommun) couple.getPremierTiers(), (PersonnePhysique) couple.getSecondTiers(), date, coupleRecapView.getRemarque(), coupleRecapView.getEtatCivil());
			}

			case FUSION_MENAGES:
			{
				Couple couple = coupleHelper.getCoupleForFusion(premierPP, secondPP, null);
				return metierService.fusionneMenages((MenageCommun) couple.getPremierTiers(), (MenageCommun) couple.getSecondTiers(), coupleRecapView.getRemarque(), coupleRecapView.getEtatCivil());
			}
		}

		return null;
	}


	/**
	 * Renvoie si la PP est majeur ou non lors de la date de mariage
	 *
	 * @param tiersGeneralView
	 * @param dateDebut
	 * @return
	 */
	public boolean isMajeurAt(TiersGeneralView tiersGeneralView, RegDate dateDebut) {
		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(tiersGeneralView.getNumero());
		return metierService.isMajeurAt(pp, dateDebut);
	}

	public CoupleHelper getCoupleHelper() {
		return coupleHelper;
	}

	public void setCoupleHelper(CoupleHelper coupleHelper) {
		this.coupleHelper = coupleHelper;
	}
}
