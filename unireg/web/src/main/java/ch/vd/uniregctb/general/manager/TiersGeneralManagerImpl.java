package ch.vd.uniregctb.general.manager;

import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.general.view.RoleView;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.general.view.TiersGeneralView.TypeTiers;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Sexe;

/**
 * Methodes relatives au cartouche contribuable
 * @author xcifde
 *
 */
public class TiersGeneralManagerImpl implements TiersGeneralManager{

	private static final Logger LOGGER = Logger.getLogger(TiersGeneralManagerImpl.class);

	private AdresseService adresseService;

	private TiersService tiersService;

	private ServiceCivilService serviceCivilService;

	private ServiceInfrastructureService infraService;

	private TiersDAO tiersDAO;

	private final Calendar calToday = new GregorianCalendar();

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public Calendar getCalToday() {
		return calToday;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	/**
	 * Alimente TiersGeneralView en fonction du tiers
	 *
	 * @param tiers
	 * @return TiersGeneralView
	 * @throws AdressesResolutionException
	 */
	public TiersGeneralView get(Tiers tiers) {
		final TiersGeneralView tiersGeneralView = new TiersGeneralView();
		setRole(tiersGeneralView, tiers);
		tiersGeneralView.setNumero(tiers.getNumero());
		setTypeTiers(tiers, tiersGeneralView);

		try {
			final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(tiers, null, false);
			tiersGeneralView.setAdresseEnvoi(adresseEnvoi);
		} catch (Exception e) {
			tiersGeneralView.setAdresseEnvoi(null);
			tiersGeneralView.setAdresseEnvoiException(e);
		}
		tiersGeneralView.setNatureTiers(tiers.getNatureTiers());
		tiersGeneralView.setAnnule(tiers.isAnnule());

		final ValidationResults validationResults = tiers.validate();
		setErreursFiliation(tiers, validationResults);
		setErreursAdresses(tiers, validationResults);

		tiersGeneralView.setValidationResults(validationResults);
		setCommuneGestion(tiers, tiersGeneralView);

		return tiersGeneralView;

	}

	private void setCommuneGestion(Tiers tiers, TiersGeneralView view) {
		final ForGestion forGestion = tiersService.getDernierForGestionConnu(tiers, null);
		if (forGestion != null) {
			final int ofsCommune = forGestion.getNoOfsCommune();
			try {
				final Commune commune = infraService.getCommuneByNumeroOfsEtendu(ofsCommune);
				view.setNomCommuneGestion(commune.getNomMinuscule());
			}
			catch (InfrastructureException e) {
				LOGGER.error("Erreur lors de la récupération de la commune de gestion", e);
				view.setNomCommuneGestion(null);
			}
		}
	}

	private void setTypeTiers(Tiers tiers, TiersGeneralView tiersGeneralView) {

		final TypeTiers type;

		if (tiers instanceof PersonnePhysique) {
			final Sexe sexe = tiersService.getSexe((PersonnePhysique)tiers);
			if (sexe == null) {
				type = TypeTiers.SEXE_INCONNU;
			}
			else if (sexe.equals(Sexe.MASCULIN)) {
				type = TypeTiers.HOMME;
			}
			else {
				type = TypeTiers.FEMME;
			}
		}
		else if (tiers instanceof Entreprise) {
			type = TypeTiers.ENTREPRISE;
		}
		else if (tiers instanceof AutreCommunaute) {
			type = TypeTiers.AUTRE_COMM;
		}
		else if (tiers instanceof MenageCommun) {
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun)tiers, null);
			final PersonnePhysique principal = ensemble.getPrincipal();
			final PersonnePhysique conjoint = ensemble.getConjoint();

			Sexe sexePrincipal = tiersService.getSexe(principal);
			Sexe sexeConjoint = tiersService.getSexe(conjoint);
			if (sexePrincipal == null && sexeConjoint != null) {
				// Le conjoint passe principal si son sexe est connu mais que celui du principal ne l'est pas
				sexePrincipal = sexeConjoint;
				sexeConjoint = null;
			}

			if (sexePrincipal == null && sexeConjoint == null) {
				type = TypeTiers.MC_SEXE_INCONNU;
			}
			else if (sexeConjoint == null) {
				if (sexePrincipal.equals(Sexe.MASCULIN)) {
					type = TypeTiers.MC_HOMME_SEUL;
				}
				else {
					type = TypeTiers.MC_FEMME_SEULE;
				}
			}
			else {
				if (sexePrincipal.equals(sexeConjoint)) {
					if (sexePrincipal.equals(Sexe.MASCULIN)) {
						type = TypeTiers.MC_HOMME_HOMME;
					}
					else {
						type = TypeTiers.MC_FEMME_FEMME;
					}
				}
				else {
					type = TypeTiers.MC_MIXTE;
				}
			}
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			type = TypeTiers.COLLECT_ADMIN;
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			type = TypeTiers.DEBITEUR;
		}
		else {
			type = null;
		}

		tiersGeneralView.setType(type);
	}

	/**
	 * Met à jour les erreurs de filiation
	 *
	 * @param tiers
	 * @param validationResults
	 */
	private void setErreursFiliation(Tiers tiers, ValidationResults validationResults) {
		if (tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isHabitant()) {
				int year = getCalToday().get(Calendar.YEAR);
				EnumAttributeIndividu[] enumValues = new EnumAttributeIndividu[] {
						EnumAttributeIndividu.ENFANTS, EnumAttributeIndividu.PARENTS
				};
				Individu ind = serviceCivilService.getIndividu(pp.getNumeroIndividu(), year, enumValues);
				Collection<Individu> listFiliations = ind.getEnfants();
				Iterator<Individu> it = listFiliations.iterator();
				for (int i = 0; i < listFiliations.size(); i++) {
					Individu individu = it.next();
					PersonnePhysique habFils  = tiersDAO.getPPByNumeroIndividu(individu.getNoTechnique());
					if (habFils == null) {
						validationResults.addError("Le contribuable enfant correspondant à l'individu '"
													+ individu.getDernierHistoriqueIndividu().getPrenom() + " " + individu.getDernierHistoriqueIndividu().getNom()
													+ "' n'existe pas.");
					}
				}
				Individu mere = ind.getMere();
				if (mere != null) {
					PersonnePhysique habMere  = tiersDAO.getPPByNumeroIndividu(mere.getNoTechnique());
					if (habMere == null) {
						validationResults.addError("Le contribuable mère correspondant à l'individu '"
													+ mere.getDernierHistoriqueIndividu().getPrenom() + " " + mere.getDernierHistoriqueIndividu().getNom()
													+ "' n'existe pas.");
					}
				}
				Individu pere = ind.getPere();
				if (pere != null) {
					PersonnePhysique habPere  = tiersDAO.getPPByNumeroIndividu(pere.getNoTechnique());
					if (habPere == null) {
						validationResults.addError("Le contribuable père correspondant à l'individu '"
													+ pere.getDernierHistoriqueIndividu().getPrenom() + " " + pere.getDernierHistoriqueIndividu().getNom()
													+ "' n'existe pas.");
					}
				}
				for (EtatCivil etatCivil : ind.getEtatsCivils()) {
					if (etatCivil.getDateDebutValidite() == null){
						validationResults.addWarning("Le contribuable possède un état civil (" +
							ch.vd.uniregctb.type.EtatCivil.from(etatCivil.getTypeEtatCivil()) + ") sans date de début. " +
							"Dans la mesure du possible, cette date a été estimée.");
					}
				}
			}
		}
	}

	/**
	 * Calcul les adresses historiques de manière stricte, et reporte toutes les erreurs trouvées.
	 *
	 * @param tiers             le tiers dont on veut vérifier les adresses
	 * @param validationResults le résultat de la validation à compléter avec les éventuelles erreurs trouvées.
	 */
	private void setErreursAdresses(Tiers tiers, ValidationResults validationResults) {
		try {
			adresseService.getAdressesFiscalHisto(tiers, true /* strict */);
		}
		catch (AdresseException e) {
			validationResults.addWarning("Des incohérences ont été détectées dans les adresses du tiers : " + e.getMessage() +
					". Dans la mesure du possible, ces incohérences ont été corrigées à la volée (mais pas sauvées en base).");
		}
	}

	/**
	 * Alimente un cartouche DPI étendu
	 *
	 * @param dpi
	 * @param etendu
	 * @return
	 */
	public TiersGeneralView get(DebiteurPrestationImposable dpi, boolean etendu) {
		TiersGeneralView tiersGeneralView = get(dpi);
		setRole(tiersGeneralView, dpi);
		if (etendu) {
			tiersGeneralView.setCategorie(dpi.getCategorieImpotSource());
			tiersGeneralView.setPeriodicite(dpi.getPeriodiciteDecompte());
			tiersGeneralView.setPeriode(dpi.getPeriodeDecompte());
			tiersGeneralView.setModeCommunication(dpi.getModeCommunication());
			tiersGeneralView.setPersonneContact(dpi.getPersonneContact());
			tiersGeneralView.setNumeroTelephone(dpi.getNumeroTelephonePrive());
		}
		return tiersGeneralView;
	}


	/**
	 * Alimente le carouche d'une personne physique
	 *
	 * @param pp
	 * @param etendu
	 * @return
	 */
	public TiersGeneralView get(PersonnePhysique pp, boolean etendu) {
		TiersGeneralView tiersGeneralView = get(pp);
		setRole(tiersGeneralView, pp);
		if (etendu) {
			 setCaracteristiquesPP(tiersGeneralView, pp);
		}
		return tiersGeneralView;
	}

	/**
	 * Mise à jour des caractéristiques PP
	 *
	 * @param pp
	 * @param tiersGeneralView
	 */
	private void setCaracteristiquesPP( TiersGeneralView tiersGeneralView, PersonnePhysique pp) {
		tiersGeneralView.setDateNaissance(tiersService.getDateNaissance(pp));
		tiersGeneralView.setAncienNumeroAVS(tiersService.getAncienNumeroAssureSocial(pp));
		tiersGeneralView.setNumeroAssureSocial(tiersService.getNumeroAssureSocial(pp));
	}


	/**
	 * Mise à jour du rôle
	 *
	 * @param tiers
	 */
	private void setRole(TiersGeneralView tiersGeneralView, Tiers tiers) {
		RoleView role = new RoleView();
		role.setLigne1(tiers.getRoleLigne1());
		role.setLigne2(tiersService.getRoleAssujettissement(tiers, RegDate.get()));
		tiersGeneralView.setRole(role);
	}

}
