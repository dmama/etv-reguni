package ch.vd.uniregctb.couple.manager;

import java.util.Set;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.couple.CoupleHelper;
import ch.vd.uniregctb.couple.CoupleHelper.Couple;
import ch.vd.uniregctb.couple.view.CoupleRecapView;
import ch.vd.uniregctb.couple.view.TypeUnion;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.DroitAccesService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.NatureTiers;
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

	private DroitAccesService droitAccesService;

	private MetierService metierService;

	private CoupleHelper coupleHelper;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDroitAccesService(DroitAccesService droitAccesService) {
		this.droitAccesService = droitAccesService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCoupleHelper(CoupleHelper coupleHelper) {
		this.coupleHelper = coupleHelper;
	}
	/**
	 * Alimente la vue CoupleRecapView avec un seul membre
	 *
	 * @param numeroPremier
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
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
	@Override
	@Transactional(readOnly = true)
	public CoupleRecapView get(Long numeroPremierPP, Long numeroSecondPP)  {
		CoupleRecapView coupleRecapView = new CoupleRecapView();
		initializeCouple(getPP(numeroPremierPP), getPP(numeroSecondPP), null, coupleRecapView);
		return coupleRecapView;
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.couple.manager.CoupleRecapManager#get(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	@Override
	@Transactional(readOnly = true)
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
		coupleRecapView.setPremierePersonne(tiersGeneralManager.getPersonnePhysique(premierPP, true));
		if(secondPP != null) {
			coupleRecapView.setSecondePersonne(tiersGeneralManager.getPersonnePhysique(secondPP, true));
		}
		coupleRecapView.setNouveauCtb(contribuableCouple == null);
		if (contribuableCouple != null) {
			coupleRecapView.setNumeroTroisiemeTiers(contribuableCouple.getNumero());
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

			// Si le ou les contribuables ne font pas partie d’un ménage commun ouvert...
			if (couplePP1 == null && couplePP2 == null) {
				final RapportEntreTiers dernierRapportPP1 = premierPP.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				final RapportEntreTiers dernierRapportPP2 = secondPP.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				if (dernierRapportPP1 != null && dernierRapportPP2 != null) {
					final Long idMenagePP1 = dernierRapportPP1.getObjetId();
					final Long idMenagePP2 = dernierRapportPP2.getObjetId();
					// ...et ont fait partie en dernier lieu d’un même ménage commun fermé pour cause de séparation...
					if (idMenagePP1 != null && idMenagePP1.equals(idMenagePP2)) {
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
						final PersonnePhysique principal = tiersService.getPersonnesPhysiques(menagePrincipal).toArray(new PersonnePhysique[0])[0];
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
	@Override
	@Transactional(readOnly = true)
	public boolean estDejaEnMenage(Long numero) {
		PersonnePhysique personne = (PersonnePhysique) tiersService.getTiers(numero);
		MenageCommun menageCommun = getTiersService().findMenageCommun(personne, null);
		return menageCommun != null;
	}

	/**
	 * Persiste le rapport de travail
	 * @param rapportView
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public MenageCommun save(CoupleRecapView coupleRecapView) throws MetierServiceException {

		final PersonnePhysique premierPP = (PersonnePhysique) tiersService.getTiers(coupleRecapView.getPremierePersonne().getNumero());
		final PersonnePhysique secondPP;
		if (coupleRecapView.getSecondePersonne() != null) {
			secondPP = (PersonnePhysique) tiersService.getTiers(coupleRecapView.getSecondePersonne().getNumero());
		}
		else {
			secondPP = null;
		}

		final Long numeroTroisiemeTiers = coupleRecapView.getNumeroTroisiemeTiers();
		final Tiers tiers = (numeroTroisiemeTiers == null ? null : tiersService.getTiers(numeroTroisiemeTiers));

		final TypeUnion typeUnion = coupleRecapView.getTypeUnion();
		final RegDate date;
		if ((typeUnion == TypeUnion.COUPLE || typeUnion == TypeUnion.SEUL) && !coupleRecapView.isNouveauCtb()) {
			if (tiers.getNatureTiers() == NatureTiers.NonHabitant) {
				date = tiers.getDateDebutActivite(); // [UNIREG-3297] on ne tient pas compte de la date affichée à l'écran
			}
			else {
				date = coupleRecapView.getDateCoupleExistant();
			}
		}
		else {
			date = RegDate.get(coupleRecapView.getDateDebut());
		}

		switch (coupleRecapView.getTypeUnion()) {
			case SEUL:
			case COUPLE:
				if (numeroTroisiemeTiers != null) {
					{
						if (tiers instanceof PersonnePhysique) {
							// changement de type de NonHabitant à MenageCommun
							final PersonnePhysique pp = (PersonnePhysique) tiers;
							Assert.isTrue(!pp.isHabitantVD(), "Le contribuable sélectionné comme couple est un habitant");
							Assert.isNull(pp.getNumeroIndividu(), "Le contribuable sélectionné comme couple est un ancien habitant");

							// [UNIREG-2893] recopie des droits d'accès éventuels sur le non-habitant vers les nouveaux membres du couple
							final Set<DroitAcces> droits = pp.getDroitsAccesAppliques();
							if (droits != null && droits.size() > 0) {
								try {
									if (premierPP != null) {
										droitAccesService.copieDroitsAcces(pp, premierPP);
									}
									if (secondPP != null) {
										droitAccesService.copieDroitsAcces(pp, secondPP);
									}
								}
								catch (DroitAccesException e) {
									LOGGER.error("Erreur lors de la copie des droits d'accès au dossier", e);
									throw new ActionException(e.getMessage());
								}
							}

							tiersService.changeNHenMenage(pp.getNumero());
							tiersService.getTiersDAO().evict(tiers);
						}
						else {
							// pas besoin de changer le type de tiers, le tiers doit déjà correspondre à un ménage commun annulé
							Assert.isInstanceOf(MenageCommun.class, tiers);
						}
					}

					// rattachement des tiers au ménage
					final MenageCommun menage = (MenageCommun) tiersService.getTiers(numeroTroisiemeTiers);
					return metierService.rattachToMenage(menage, premierPP, secondPP, date, coupleRecapView.getRemarque(), coupleRecapView.getEtatCivil(), false, null);
				}
				else {
					return metierService.marie(date, premierPP, secondPP, coupleRecapView.getRemarque(), coupleRecapView.getEtatCivil(), false, null);
				}

			case RECONCILIATION:
				return metierService.reconcilie(premierPP, secondPP, date, coupleRecapView.getRemarque(), false, null);

			case RECONSTITUTION_MENAGE:
			{
				final Couple couple = coupleHelper.getCoupleForReconstitution(premierPP, secondPP, date);
				return metierService.reconstitueMenage((MenageCommun) couple.getPremierTiers(), (PersonnePhysique) couple.getSecondTiers(), date, coupleRecapView.getRemarque(), coupleRecapView.getEtatCivil());
			}

			case FUSION_MENAGES:
			{
				final Couple couple = coupleHelper.getCoupleForFusion(premierPP, secondPP, null);
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
	@Override
	@Transactional(readOnly = true)
	public boolean isMajeurAt(TiersGeneralView tiersGeneralView, RegDate dateDebut) {
		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(tiersGeneralView.getNumero());
		return metierService.isMajeurAt(pp, dateDebut);
	}
}
