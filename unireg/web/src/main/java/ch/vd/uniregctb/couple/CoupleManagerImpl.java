package ch.vd.uniregctb.couple;

import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.DroitAccesService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class CoupleManagerImpl implements CoupleManager {

	private final Logger LOGGER = Logger.getLogger(CoupleManagerImpl.class);

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private MetierService metierService;
	private DroitAccesService droitAccesService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public void setDroitAccesService(DroitAccesService droitAccesService) {
		this.droitAccesService = droitAccesService;
	}

	@Override
	public Couple getCoupleForReconstitution(PersonnePhysique pp1, PersonnePhysique pp2, RegDate date) {
		EnsembleTiersCouple couplePP1 = tiersService.getEnsembleTiersCouple(pp1, date);
		EnsembleTiersCouple couplePP2 = tiersService.getEnsembleTiersCouple(pp2, date);
		MenageCommun menageAReconstituer = null;
		PersonnePhysique tiersAAjouter = null;
		if (couplePP1 != null) {
			menageAReconstituer = couplePP1.getMenage();
			tiersAAjouter = pp2;
		}
		else {
			menageAReconstituer = couplePP2.getMenage();
			tiersAAjouter = pp1;
		}

		return new Couple(TypeUnion.RECONSTITUTION_MENAGE, menageAReconstituer, tiersAAjouter);
	}

	@Override
	public Couple getCoupleForFusion(PersonnePhysique pp1, PersonnePhysique pp2, @Nullable RegDate date) {
		EnsembleTiersCouple couplePP1 = tiersService.getEnsembleTiersCouple(pp1, date);
		EnsembleTiersCouple couplePP2 = tiersService.getEnsembleTiersCouple(pp2, date);
		MenageCommun menagePP1 = null;
		if (couplePP1 != null) {
			menagePP1 = couplePP1.getMenage();
		}
		MenageCommun menagePP2 = null;
		if (couplePP2 != null) {
			menagePP2 = couplePP2.getMenage();
		}

		return new Couple(TypeUnion.FUSION_MENAGES, menagePP1, menagePP2);
	}

	@NotNull
	@Override
	public CoupleInfo determineInfoFuturCouple(@Nullable Long pp1Id, @Nullable Long pp2Id, @Nullable Long mcId) {
		final PersonnePhysique pp1 = pp1Id == null ? null : (PersonnePhysique) tiersDAO.get(pp1Id);
		final PersonnePhysique pp2 = pp2Id == null ? null : (PersonnePhysique) tiersDAO.get(pp2Id);
		final Contribuable couple = mcId == null ? null : (Contribuable) tiersDAO.get(mcId);

		if (pp1 == null) {
			return new CoupleInfo(null, null, null, null);
		}

		// type d'union et date de début
		TypeUnion type = TypeUnion.COUPLE;
		EtatCivil etatCivil = EtatCivil.MARIE;
		RegDate dateDebut = (couple == null ? null : determineDateDebutCouple(couple));
		Long reconcMcId = null;

		if (pp2 == null) {
			// cas d'un(e) marié(e) seul(e)
			type = TypeUnion.SEUL;
		}
		else {

			// détermination de l'état civil du couple (MARIE, PACS)
			final boolean memeSexe = tiersService.isMemeSexe(pp1, pp2);
			etatCivil = (memeSexe ? EtatCivil.LIE_PARTENARIAT_ENREGISTRE : EtatCivil.MARIE);

			final EnsembleTiersCouple ensemble1 = tiersService.getEnsembleTiersCouple(pp1, null);
			final EnsembleTiersCouple ensemble2 = tiersService.getEnsembleTiersCouple(pp2, null);

			if (ensemble1 == null && ensemble2 == null) {
				// les contribuables ne font pas partie d’un ménage commun ouvert...
				final RapportEntreTiers app1 = pp1.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				final RapportEntreTiers app2 = pp2.getDernierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				if (app1 != null && app2 != null && app1.getObjetId() != null && app1.getObjetId().equals(app2.getObjetId())) {
					// mais ils ont fait partie de même ménage commun
					type = TypeUnion.RECONCILIATION;
					reconcMcId = app1.getObjetId();
				}
			}
			else if (ensemble1 != null && ensemble2 != null) {
				// les contribuable font encore partie d'un ménage commun ouvet
				if (ensemble1.getMenage() != ensemble2.getMenage()) {
					// les deux ménages ne sont pas les mêmes
					type = TypeUnion.FUSION_MENAGES;
					final MenageCommun menagePrincipal = metierService.getMenageForFusion(ensemble1.getMenage(), ensemble2.getMenage());
					final PersonnePhysique principal = (menagePrincipal == ensemble1.getMenage() ? pp1 : pp2);
					final RapportEntreTiers rapport = menagePrincipal.getPremierRapportObjet(TypeRapportEntreTiers.APPARTENANCE_MENAGE, principal);
					dateDebut = rapport.getDateDebut();
					reconcMcId = menagePrincipal.getId();
				}
			}
			else {
				// il y a un seul ménage commun ouvert
				type = TypeUnion.RECONSTITUTION_MENAGE;

				// récuperation de la date de mariage
				final PersonnePhysique pp = ensemble1 != null ? pp1 : pp2;
				final MenageCommun menage = ensemble1 != null ? ensemble1.getMenage() : ensemble2.getMenage();
				dateDebut = pp.getPremierRapportSujet(TypeRapportEntreTiers.APPARTENANCE_MENAGE, menage).getDateDebut();
				reconcMcId = menage.getId();
			}
		}

		return new CoupleInfo(type, etatCivil, dateDebut, reconcMcId);
	}

	private RegDate determineDateDebutCouple(Contribuable mc) {
		if (mc instanceof PersonnePhysique && !((PersonnePhysique) mc).isHabitantVD()) {
			// [UNIREG-3297] on renseigne automatiquement la date de debut pour les non-habitants
			final ForFiscalPrincipal ffp = mc.getPremierForFiscalPrincipal();
			return ffp == null ? null : ffp.getDateDebut();
		}
		return null;
	}

	@Override
	public MenageCommun sauverCouple(@NotNull Long pp1Id, @Nullable Long pp2Id, @Nullable Long mcId, @NotNull RegDate dateDebut, @NotNull TypeUnion typeUnion, @NotNull EtatCivil etatCivil,
	                                 @Nullable String remarque) throws MetierServiceException {

		final PersonnePhysique premierPP = (PersonnePhysique) tiersService.getTiers(pp1Id);
		final PersonnePhysique secondPP = (pp2Id == null ? null : (PersonnePhysique) tiersService.getTiers(pp2Id));
		final Tiers futurMc = (mcId == null ? null : tiersService.getTiers(mcId));

		switch (typeUnion) {
		case SEUL:
		case COUPLE:
			if (mcId == null) {
				return metierService.marie(dateDebut, premierPP, secondPP, remarque, etatCivil, false, null);
			}
			else {
				if (futurMc instanceof PersonnePhysique) {
					// changement de type de NonHabitant à MenageCommun
					final PersonnePhysique pp = (PersonnePhysique) futurMc;
					if (pp.isHabitantVD()) {
						throw new ActionException("Le contribuable sélectionné comme couple est un habitant");
					}
					if (pp.isConnuAuCivil()) {
						throw new ActionException("Le contribuable sélectionné comme couple est un ancien habitant");
					}

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
					tiersDAO.evict(futurMc);
				}
				else {
					// pas besoin de changer le type de tiers, le tiers doit déjà correspondre à un ménage commun annulé
					Assert.isInstanceOf(MenageCommun.class, futurMc);
				}

				// rattachement des tiers au ménage
				final MenageCommun menage = (MenageCommun) tiersService.getTiers(mcId);
				return metierService.rattachToMenage(menage, premierPP, secondPP, dateDebut, remarque, etatCivil, false, null);
			}

		case RECONCILIATION:
			return metierService.reconcilie(premierPP, secondPP, dateDebut, remarque, false, null);

		case RECONSTITUTION_MENAGE: {
			final CoupleManager.Couple couple = getCoupleForReconstitution(premierPP, secondPP, dateDebut);
			return metierService.reconstitueMenage((MenageCommun) couple.getPremierTiers(), (PersonnePhysique) couple.getSecondTiers(), dateDebut, remarque, etatCivil);
		}

		case FUSION_MENAGES: {
			final CoupleManager.Couple couple = getCoupleForFusion(premierPP, secondPP, null);
			return metierService.fusionneMenages((MenageCommun) couple.getPremierTiers(), (MenageCommun) couple.getSecondTiers(), remarque, etatCivil);
		}
		default:
			throw new IllegalArgumentException("Type d'union inconnu = [" + typeUnion + "]");
		}
	}

}
