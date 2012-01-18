package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Processor qui rapproche les contribuables des propriétaires fonciers
 *
 * @author baba
 */
public class RapprocherCtbProcessor {

	private static final Logger LOGGER = Logger.getLogger(RapprocherCtbProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final TiersDAO tiersDAO;
	private final AdresseService adresseService;
	private final TiersService tiersService;
	private final ServiceCivilService serviceCivil;

	public RapprocherCtbProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, TiersDAO tiersDAO, AdresseService adresseService, TiersService tiersService,
	                              ServiceCivilService serviceCivil) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersDAO = tiersDAO;
		this.adresseService = adresseService;
		this.tiersService = tiersService;
		this.serviceCivil = serviceCivil;
	}

	public RapprocherCtbResults run(List<ProprietaireFoncier> listeProprietairesFonciers, StatusManager s, final RegDate dateTraitement, int nbThreads) {
		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		status.setMessage("Début du Rapprochement ...");

		final RapprocherCtbResults rapportFinal = new RapprocherCtbResults(dateTraitement);
		final ParallelBatchTransactionTemplate<ProprietaireFoncier, RapprocherCtbResults> template =
				new ParallelBatchTransactionTemplate<ProprietaireFoncier, RapprocherCtbResults>(listeProprietairesFonciers, BATCH_SIZE,
						nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager,
						status, hibernateTemplate);
		template.setReadonly(true);
		template.execute(rapportFinal, new BatchCallback<ProprietaireFoncier, RapprocherCtbResults>() {

			@Override
			public RapprocherCtbResults createSubRapport() {
				return new RapprocherCtbResults(dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<ProprietaireFoncier> batch, RapprocherCtbResults r) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0).getNumeroRegistreFoncier() + "; " + batch.get(batch.size() - 1).getNumeroRegistreFoncier() + "] ...", percent);
				traiterBatch(batch, r);
				return !status.interrupted();
			}
		});

		rapportFinal.interrompu = status.interrupted();
		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterBatch(List<ProprietaireFoncier> batch, RapprocherCtbResults rapport) throws AdresseException {

		// pré-chargement des tiers et individus concernés par ce lot
		final List<Long> idCtbs = new ArrayList<Long>(batch.size());
		for (ProprietaireFoncier p : batch) {
			if (p.getNumeroContribuable() != null) {
				idCtbs.add(p.getNumeroContribuable());
			}
		}

		final Set<TiersDAO.Parts> parts = new HashSet<TiersDAO.Parts>();
		parts.add(TiersDAO.Parts.ADRESSES);
		parts.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);

		final List<Tiers> tierz = tiersDAO.getBatch(idCtbs, parts);
		preloadIndividus(tierz, RegDate.get().year());

		// maintenant, le boulot d'extraction proprement dit...
		for (ProprietaireFoncier proprietaireFoncier : batch) {

			final ProprietaireRapproche proprietaireRapproche = new ProprietaireRapproche(proprietaireFoncier);
			final Contribuable ctb = proprietaireFoncier.getNumeroContribuable() != null ? tiersDAO.getContribuableByNumero(proprietaireFoncier.getNumeroContribuable()) : null;

			// 2.Si le contribuable est trouvé, remplir les champs FormulePolitesse NomCourrier1,
			// NomCourrier2 avec les valeurs du contribuable et lire le ou les individus par le regroupement.
			// Se limiter aux regroupements ouverts ou, s'ils sont tous fermés,
			// aux regroupements les plus récents (selon date de fin).

			if (ctb != null) {

				final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.COURRIER, false);
				final String formuleAppel = adresseEnvoi.getFormuleAppel();
				final List<String> nomCourrier = adresseEnvoi.getNomsPrenomsOuRaisonsSociales();

				proprietaireRapproche.setFormulePolitesse(formuleAppel);

				if (nomCourrier.size() == 1) {
					proprietaireRapproche.setNomCourrier1(nomCourrier.get(0));
				}
				else if (nomCourrier.size() == 2) {
					proprietaireRapproche.setNomCourrier1(nomCourrier.get(0));
					proprietaireRapproche.setNomCourrier2(nomCourrier.get(1));
				}

				if (ctb instanceof PersonnePhysique) {
					final PersonnePhysique principal = (PersonnePhysique) ctb;
					traiterPersonnePhysique(proprietaireRapproche, principal);
				}
				else if (ctb instanceof MenageCommun) {
					final MenageCommun menage = (MenageCommun) ctb;
					traiterMenageCommun(proprietaireRapproche, menage);
				}
				else {
					// ni une personne physique, ni un ménage commun ?
					// je ne crois pas que nous soyons supposés accepter ce cas, ou bien ?
					rapport.addError(proprietaireFoncier, RapprocherCtbResults.ErreurType.CTB_NON_PP_NI_MC, null);
					proprietaireRapproche.setResultat(ProprietaireRapproche.CodeRetour.CTB_NON_TROUVE);
				}
			}
			else {
				// Si le contribuable n'est pas trouvé, mettre "Pas de contribuable trouvé" dans Résultat,
				proprietaireRapproche.setResultat(ProprietaireRapproche.CodeRetour.CTB_NON_TROUVE);
			}

			rapport.addProprietaireRapproche(proprietaireRapproche);
		}
	}

	private void preloadIndividus(List<Tiers> tierz, int anneePeriode) {
		final Map<Long, PersonnePhysique> ppByNoIndividu = new HashMap<Long, PersonnePhysique>(tierz.size() * 2);
		final RegDate date = RegDate.get(anneePeriode, 12, 31);
		for (Tiers tiers : tierz) {
			if (tiers instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) tiers;
				if (pp.isConnuAuCivil()) {
					final Long noIndividu = pp.getNumeroIndividu();
					ppByNoIndividu.put(noIndividu, pp);
				}
			}
			else if (tiers instanceof MenageCommun) {
				final Set<PersonnePhysique> membres = tiersService.getComposantsMenage((MenageCommun) tiers, date);
				if (membres != null) {
					for (PersonnePhysique pp : membres) {
						if (pp.isConnuAuCivil()) {
							ppByNoIndividu.put(pp.getNumeroIndividu(), pp);
						}
					}
				}
			}
		}

		if (!ppByNoIndividu.isEmpty()) {
			// remplit le cache des individus...
			final List<Individu> individus = serviceCivil.getIndividus(ppByNoIndividu.keySet(), null, AttributeIndividu.ADRESSES);

			// et on remplit aussi le cache individu sur les personnes physiques... (utilisé pour l'accès à la date de décès et au sexe)
			for (Individu individu : individus) {
				final PersonnePhysique pp = ppByNoIndividu.get(individu.getNoTechnique());
				pp.setIndividuCache(individu);
			}
		}
	}

	private void traiterPersonnePhysique(ProprietaireRapproche proprietaireRapproche, PersonnePhysique personne) {
		if (personne.isConnuAuCivil()) {
			final Long numeroCtb = personne.getNumero();
			final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(personne);
			final String nom = nomPrenom.getNom();
			final String prenom = nomPrenom.getPrenom();
			final RegDate dateNaissance = tiersService.getDateNaissance(personne);
			final ProprietaireRapproche.CodeRetour resultat = determineResultat(proprietaireRapproche, nom, prenom, dateNaissance);
			setValuesProprietaireRapproche(proprietaireRapproche, numeroCtb, nom, prenom, dateNaissance, resultat, true);
		}
		else {
			proprietaireRapproche.setResultat(ProprietaireRapproche.CodeRetour.INDIVIDU_NON_TROUVE);
		}
	}

	private void traiterMenageCommun(ProprietaireRapproche proprietaireRapproche, MenageCommun menage) {

		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(menage, RegDate.get().year());
		final PersonnePhysique principal = couple.getPrincipal();
		final PersonnePhysique conjoint = couple.getConjoint();
		if (principal == null && conjoint == null) {
			proprietaireRapproche.setResultat(ProprietaireRapproche.CodeRetour.INDIVIDU_NON_TROUVE);
		}
		else if (principal != null && conjoint == null) {
			traiterPersonnePhysique(proprietaireRapproche, principal);
		}
		else if (principal == null) {
			traiterPersonnePhysique(proprietaireRapproche, conjoint);
		}
		else {
			traiterMembresMenageCommun(proprietaireRapproche, principal, conjoint);
		}
	}

	private void traiterMembresMenageCommun(ProprietaireRapproche proprietaireRapproche, PersonnePhysique principal, PersonnePhysique conjoint) {
		final boolean principalConnuAuCivil = principal.isConnuAuCivil();
		final boolean conjointConnuAuCivil = conjoint.isConnuAuCivil();
		if (!principalConnuAuCivil && !conjointConnuAuCivil) {
			proprietaireRapproche.setResultat(ProprietaireRapproche.CodeRetour.INDIVIDU_NON_TROUVE);
		}
		else if (principalConnuAuCivil && !conjointConnuAuCivil) {
			traiterPersonnePhysique(proprietaireRapproche, principal);
		}
		else if (!principalConnuAuCivil) {
			traiterPersonnePhysique(proprietaireRapproche, conjoint);
		}
		else {

			final Long numeroCtbPrincipal = principal.getNumero();
			final NomPrenom nomPrenomPrincipal = tiersService.getDecompositionNomPrenom(principal);
			final String nomPrincipal = nomPrenomPrincipal.getNom();
			final String prenomPrincipal = nomPrenomPrincipal.getPrenom();
			final RegDate dateNaissancePrincipal = tiersService.getDateNaissance(principal);

			final Long numeroCtbConjoint = conjoint.getNumero();

			final NomPrenom nomPrenomConjoint = tiersService.getDecompositionNomPrenom(conjoint);
			final String nomConjoint = nomPrenomConjoint.getNom();
			final String prenomConjoint = nomPrenomConjoint.getPrenom();
			final RegDate dateNaissanceConjoint = tiersService.getDateNaissance(conjoint);

			final ProprietaireRapproche.CodeRetour resultatPrincipal = determineResultat(proprietaireRapproche, nomPrincipal, prenomPrincipal, dateNaissancePrincipal);
			final ProprietaireRapproche.CodeRetour resultatConjoint = determineResultat(proprietaireRapproche, nomConjoint, prenomConjoint, dateNaissanceConjoint);

			if (resultatPrincipal == ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT) {
				setValuesProprietaireRapproche(proprietaireRapproche, numeroCtbPrincipal, nomPrincipal, prenomPrincipal, dateNaissancePrincipal, resultatPrincipal, true);
			}
			else if (resultatConjoint == ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT) {
				setValuesProprietaireRapproche(proprietaireRapproche, numeroCtbConjoint, nomConjoint, prenomConjoint, dateNaissanceConjoint, resultatConjoint, true);
			}
			else if (resultatPrincipal == ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT_SAUF_NAISSANCE) {
				setValuesProprietaireRapproche(proprietaireRapproche, numeroCtbPrincipal, nomPrincipal, prenomPrincipal, dateNaissancePrincipal, resultatPrincipal, true);
			}
			else if (resultatConjoint == ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT_SAUF_NAISSANCE) {
				setValuesProprietaireRapproche(proprietaireRapproche, numeroCtbConjoint, nomConjoint, prenomConjoint, dateNaissanceConjoint, resultatConjoint, true);
			}
			else {
				setValuesProprietaireRapproche(proprietaireRapproche, numeroCtbPrincipal, nomPrincipal, prenomPrincipal, dateNaissancePrincipal,
						ProprietaireRapproche.CodeRetour.INDIVIDUS_TROUVE_NON_EXACT, true);
				setValuesProprietaireRapproche(proprietaireRapproche, numeroCtbConjoint, nomConjoint, prenomConjoint, dateNaissanceConjoint,
						ProprietaireRapproche.CodeRetour.INDIVIDUS_TROUVE_NON_EXACT, false);
			}
		}
	}

	private ProprietaireRapproche.CodeRetour determineResultat(ProprietaireFoncier proprio, String nom, String prenom, RegDate dateNaissance) {

		boolean nomIdentique = false;
		boolean prenomIdentique = false;
		boolean dateNaissanceIdentique = false;

		if (nom.equalsIgnoreCase(proprio.getNom())) {
			nomIdentique = true;
		}
		//SIFISC-3296 Si le contribuable n'a pas de prénom dans unireg ou d ans le fichier RF on renvoie un code 20. permet d'éviter un NPE
		if (prenom == null || proprio.getPrenom() == null) {
			return ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_NON_EXACT;
		}
		else if (prenom.equalsIgnoreCase(proprio.getPrenom())) {
			prenomIdentique = true;
		}

		final RegDate dateNaissanceProprio = proprio.getDateNaissance();
		if (dateNaissance != null && dateNaissanceProprio != null) {
			if (dateNaissance.equals(dateNaissanceProprio)) {
				dateNaissanceIdentique = true;
			}
		}
		else if (dateNaissance == null && dateNaissanceProprio == null) {
			dateNaissanceIdentique = true;
		}

		if (nomIdentique && prenomIdentique) {
			if (dateNaissanceIdentique) {
				return ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT;
			}
			else {
				return ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_EXACT_SAUF_NAISSANCE;
			}
		}
		else {
			return ProprietaireRapproche.CodeRetour.INDIVIDU_TROUVE_NON_EXACT;
		}
	}

	private void setValuesProprietaireRapproche(ProprietaireRapproche proprietaireRapproche, Long numeroCtb, String nom, String prenom, RegDate dateNaissance,
	                                            ProprietaireRapproche.CodeRetour resultat, boolean isPrincipal) {
		if (isPrincipal) {
			proprietaireRapproche.setNumeroContribuable1(numeroCtb);
			proprietaireRapproche.setNom1(nom);
			proprietaireRapproche.setPrenom1(prenom);
			proprietaireRapproche.setDateNaissance1(dateNaissance);
		}
		else {
			proprietaireRapproche.setNumeroContribuable2(numeroCtb);
			proprietaireRapproche.setNom2(nom);
			proprietaireRapproche.setPrenom2(prenom);
			proprietaireRapproche.setDateNaissance2(dateNaissance);
		}
		proprietaireRapproche.setResultat(resultat);
	}
}
