package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.Collections;
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
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;

/**
 * Processor qui rapproche les contribuables des propriétaires fonciers
 *
 * @author baba
 *
 */
public class RapprocherCtbProcessor {

	private static final Logger LOGGER = Logger.getLogger(RapprocherCtbProcessor.class);

	private final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final TiersDAO tiersDAO;
	private final AdresseService adresseService;
	private final TiersService tiersService;
	private HashMap<Long, ProprietaireFoncier> mapProprio;

	private final String INDIVIDU_TROUVE_EXACT = "00";
	private final String INDIVIDU_TROUVE_SANS_NAISSANCE = "10";
	private final String INDIVIDU_TROUVE_NON_EXACT = "20";
	private final String INDIVIDUS_TROUVE_NON_EXACT = "21";
	private final String CTB_NON_TROUVE = "30";
	private final String INDIVIDU_NON_TROUVE = "31";
	private final String PLUS_DE_DEUX_INDIV_TROUVE = "32";

	private RapprocherCtbResults rapport;

	public RapprocherCtbProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, TiersDAO tiersDAO,
			AdresseService adresseService, TiersService tiersService) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersDAO = tiersDAO;
		this.adresseService = adresseService;
		this.tiersService = tiersService;

	}

	public RapprocherCtbResults run(List<ProprietaireFoncier> listeProprietaireFoncier, StatusManager s, RegDate dateTraitement) {
		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		status.setMessage("Début du Rapprochement ...");

		final RapprocherCtbResults rapportFinal = new RapprocherCtbResults(dateTraitement);
		mapProprio = getMapRFIds(listeProprietaireFoncier);

		final List<Long> registreFoncierIds = new ArrayList<Long>();
		registreFoncierIds.addAll(mapProprio.keySet());
		Collections.sort(registreFoncierIds);

		final BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(registreFoncierIds, BATCH_SIZE,
				Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		template.setReadonly(true);
		template.execute(new BatchCallback<Long>() {

			private Long idCtb = null;

			@Override
			public void beforeTransaction() {
				rapport = new RapprocherCtbResults();
				idCtb = null;
			}

			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {

				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);

				if (batch.size() == 1) {
					idCtb = batch.get(0);
				}
				traiterBatch(batch);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (willRetry) {
					// le batch va être rejoué -> on peut ignorer le rapport
					rapport = null;
				}
				else {
					// on ajoute l'exception directement dans le rapport final
					rapportFinal.addErrorException(idCtb, e);
					rapport = null;
				}
			}

			@Override
			public void afterTransactionCommit() {
				rapportFinal.add(rapport);
			}
		});

		rapportFinal.interrompu = status.interrupted();
		rapportFinal.end();
		return rapportFinal;

	}

	private HashMap<Long, ProprietaireFoncier> getMapRFIds(List<ProprietaireFoncier> listeProprietaireFoncier) {

		HashMap<Long, ProprietaireFoncier> mapProprio = new HashMap<Long, ProprietaireFoncier>(listeProprietaireFoncier.size());

		for (ProprietaireFoncier proprietaireFoncier : listeProprietaireFoncier) {

			mapProprio.put(proprietaireFoncier.getNumeroRegistreFoncier(), proprietaireFoncier);
		}
		return mapProprio;

	}

	public void traiterBatch(List<Long> batch) throws AdressesResolutionException {

		/*
		 * final Set<Parts> parts = new HashSet<Parts>(); parts.add(Parts.ADRESSES); parts.add(Parts.RAPPORTS_ENTRE_TIERS);
		 *
		 * final List<Tiers> tiers = tiersDAO.getBatch(batch, parts); final Map<Long, Tiers> map = new HashMap<Long, Tiers>(tiers.size());
		 * for (Tiers t : tiers) { map.put(t.getNumero(), t); }
		 */

		for (Long registreFoncierId : batch) {
			ProprietaireFoncier proprietaireFoncier = mapProprio.get(registreFoncierId);
			// Pour Chaque propriétaire, on crée la réponse
			ProprietaireRapproche proprietaireRapproche = new ProprietaireRapproche(proprietaireFoncier);
			Contribuable ctb = tiersDAO.getContribuableByNumero(proprietaireFoncier.getNumeroContribuable());

			// (Contribuable) map.get(ctbId);

			// 2.Si le contribuable est trouvé, remplir les champs FormulePolitesse NomCourrier1,
			// NomCourrier2 avec les valeurs du contribuable et lire le ou les individus par le regroupement.
			// Se limiter aux regroupements ouverts ou, s'ils sont tous fermés,
			// aux regroupements les plus récents (selon date de fin).

			if (ctb != null) {

				AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(ctb, null);

				String salutations = adresseEnvoi.getSalutations();

				List<String> nomCourrier = adresseEnvoi.getNomPrenom();

				proprietaireRapproche.setFormulePolitesse(salutations);

				if (nomCourrier.size() == 1) {
					proprietaireRapproche.setNomCourrier1(nomCourrier.get(0));
				}
				else if (nomCourrier.size() == 2) {
					proprietaireRapproche.setNomCourrier1(nomCourrier.get(0));
					proprietaireRapproche.setNomCourrier2(nomCourrier.get(1));
				}

				if (ctb instanceof PersonnePhysique) {
					PersonnePhysique principal = (PersonnePhysique) ctb;
					traiterIndividu(proprietaireRapproche, principal);

				}
				else if (ctb instanceof MenageCommun) {
					MenageCommun menage = (MenageCommun) ctb;
					traiterMenageCommun(proprietaireRapproche, menage);

				}

			}
			else {
				// 1. Si le contribuable n'est pas trouvé, mettre "Pas de contribuable trouvé" dans Résultat,
				proprietaireRapproche.setResultat(CTB_NON_TROUVE);
				rapport.incrementNbCtbInconnu();
			}

			rapport.addProprietaireRapproche(proprietaireRapproche);
		}

	}

	public void traiterIndividu(ProprietaireRapproche proprietaireRapproche, PersonnePhysique personne) {

		if (personne.isHabitant()) {
			Long numeroCtb = personne.getNumero();
			String nom = tiersService.getNom(personne);
			String prenom = tiersService.getPrenom(personne);
			RegDate dateNaissance = tiersService.getDateNaissance(personne);
			String resultat = determineResultat(proprietaireRapproche, numeroCtb, nom, prenom, dateNaissance);

			setValuesProprietaireRapproche(proprietaireRapproche, numeroCtb, nom, prenom,
					dateNaissance, resultat, true);

			//increment des valeurs de processing
			if (INDIVIDU_TROUVE_EXACT.equals(resultat)) {

				rapport.incrementNbIndividuTrouvesExact();
			}

			else if (INDIVIDU_TROUVE_SANS_NAISSANCE.equals(resultat)) {

				rapport.incrementNbIndividuTrouvesSaufDateNaissance();

			}
			else if (INDIVIDU_TROUVE_NON_EXACT.equals(resultat)) {

				rapport.incrementNbIndividuTrouvesSansCorrespondance();

			}
		}
		else {
			proprietaireRapproche.setResultat(INDIVIDU_NON_TROUVE);
			rapport.incrementNbIndviduInconnu();
		}
	}



	public void traiterMenageCommun(ProprietaireRapproche proprietaireRapproche, MenageCommun menage) {

		PersonnePhysique principal = tiersService.getEnsembleTiersCouple(menage, RegDate.get().year()).getPrincipal();
		PersonnePhysique conjoint = tiersService.getEnsembleTiersCouple(menage, RegDate.get().year()).getConjoint();
		if (principal==null && conjoint==null) {
			proprietaireRapproche.setResultat(INDIVIDU_NON_TROUVE);
			rapport.incrementNbIndviduInconnu();
		}
		else if(principal!=null && conjoint==null) {
			traiterIndividu(proprietaireRapproche, principal);
		}
		else if(principal==null && conjoint!=null){
			traiterIndividu(proprietaireRapproche, conjoint);
		}
		else if (principal!=null && conjoint!=null){
			traiterMembresMenageCommun(proprietaireRapproche, principal, conjoint);
		}


	}

	public void traiterMembresMenageCommun(ProprietaireRapproche proprietaireRapproche, PersonnePhysique principal, PersonnePhysique conjoint){
		if (!principal.isHabitant() && !conjoint.isHabitant()) {
			proprietaireRapproche.setResultat(INDIVIDU_NON_TROUVE);
			rapport.incrementNbIndviduInconnu();
		}
		else if(principal.isHabitant() && !conjoint.isHabitant()) {
			traiterIndividu(proprietaireRapproche, principal);
		}
		else if(!principal.isHabitant() && conjoint.isHabitant()){
			traiterIndividu(proprietaireRapproche, conjoint);
		}
		else if (principal.isHabitant() && conjoint.isHabitant()){

			Long numeroCtbPrincipal = principal.getNumero();
			String nomPrincipal = tiersService.getNom(principal);
			String prenomPrincipal = tiersService.getPrenom(principal);
			RegDate dateNaissancePrincipal = tiersService.getDateNaissance(principal);

			Long numeroCtbConjoint = conjoint.getNumero();
			String nomConjoint = tiersService.getNom(conjoint);
			String prenomConjoint = tiersService.getPrenom(conjoint);
			RegDate dateNaissanceConjoint = tiersService.getDateNaissance(conjoint);

			String resultatPrincipal = determineResultat(proprietaireRapproche, numeroCtbPrincipal, nomPrincipal, prenomPrincipal,
					dateNaissancePrincipal);
			String resultatConjoint = determineResultat(proprietaireRapproche, numeroCtbConjoint, nomConjoint, prenomConjoint,
					dateNaissanceConjoint);

			if (INDIVIDU_TROUVE_EXACT.equals(resultatPrincipal)) {
				setValuesProprietaireRapproche(proprietaireRapproche, numeroCtbPrincipal, nomPrincipal, prenomPrincipal,
						dateNaissancePrincipal, resultatPrincipal, true);
				rapport.incrementNbIndividuTrouvesExact();
			}
			else if (INDIVIDU_TROUVE_EXACT.equals(resultatConjoint)) {

				setValuesProprietaireRapproche(proprietaireRapproche, numeroCtbConjoint, nomConjoint, prenomConjoint,
						dateNaissanceConjoint, resultatConjoint, true);
				rapport.incrementNbIndividuTrouvesExact();

			}
			else if (INDIVIDU_TROUVE_SANS_NAISSANCE.equals(resultatPrincipal)) {

				setValuesProprietaireRapproche(proprietaireRapproche, numeroCtbPrincipal, nomPrincipal, prenomPrincipal,
						dateNaissancePrincipal, resultatPrincipal, true);
				rapport.incrementNbIndividuTrouvesSaufDateNaissance();

			}
			else if (INDIVIDU_TROUVE_SANS_NAISSANCE.equals(resultatConjoint)) {

				setValuesProprietaireRapproche(proprietaireRapproche, numeroCtbConjoint, nomConjoint, prenomConjoint,
						dateNaissanceConjoint, resultatConjoint, true);
				rapport.incrementNbIndividuTrouvesSaufDateNaissance();

			}
			else {

				setValuesProprietaireRapproche(proprietaireRapproche, numeroCtbPrincipal, nomPrincipal, prenomPrincipal,
						dateNaissancePrincipal, null, true);

				setValuesProprietaireRapproche(proprietaireRapproche, numeroCtbConjoint, nomConjoint, prenomConjoint,
						dateNaissanceConjoint, INDIVIDUS_TROUVE_NON_EXACT, false);
				rapport.incrementNbIndividuTrouvesSansCorrespondance();

			}
		}
	}

	public String determineResultat(ProprietaireFoncier proprio, long numeroCtb, String nom, String prenom, RegDate dateNaissance) {

		boolean nomIdentique = false;
		boolean prenomIdentique = false;
		boolean dateNaissaneIdentique = false;

		if (nom.equalsIgnoreCase(proprio.getNom())) {
			nomIdentique = true;
		}
		if (prenom.equalsIgnoreCase(proprio.getPrenom())) {
			prenomIdentique = true;
		}

		RegDate dateNaissanceProprio = proprio.getDateNaissance();

		if (dateNaissance != null && dateNaissanceProprio != null) {
			if (dateNaissance.equals(dateNaissanceProprio)) {
				dateNaissaneIdentique = true;
			}
		}
		else if (dateNaissance == null && dateNaissanceProprio == null) {
			dateNaissaneIdentique = true;
		}

		if (nomIdentique && prenomIdentique && dateNaissaneIdentique) {
			return INDIVIDU_TROUVE_EXACT;
		}

		if (nomIdentique && prenomIdentique && !dateNaissaneIdentique) {
			return INDIVIDU_TROUVE_SANS_NAISSANCE;
		}

		if (!nomIdentique || !prenomIdentique) {
			return INDIVIDU_TROUVE_NON_EXACT;
		}

		return null;

	}

	public Map<Long, Tiers> getListeTiers(List<Long> batch) {
		final Set<Parts> parts = new HashSet<Parts>();
		parts.add(Parts.ADRESSES);
		parts.add(Parts.RAPPORTS_ENTRE_TIERS);

		final List<Tiers> tiers = tiersDAO.getBatch(batch, parts);
		final Map<Long, Tiers> map = new HashMap<Long, Tiers>(tiers.size());
		for (Tiers t : tiers) {
			map.put(t.getNumero(), t);
		}
		return map;
	}

	private void setValuesProprietaireRapproche(ProprietaireRapproche proprietaireRapproche, long numeroCtb, String nom, String prenom,
			RegDate dateNaissance, String resultat, boolean isPrincipal) {

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
