package ch.vd.unireg.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersDAO.Parts;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeDroitAcces;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class SecuriteDossierServiceImpl implements SecuriteDossierService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecuriteDossierServiceImpl.class);

	private SecurityProviderInterface securityProvider;
	private ServiceSecuriteService serviceSecurite;
	private TiersDAO tiersDAO;
	private DroitAccesDAO droitAccesDAO;

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public Niveau getAcces(long tiersId) throws ObjectNotFoundException {

		final Tiers tiers = tiersDAO.get(tiersId, true); // ne pas flusher la session automatiquement
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}
		return getAcces(tiers);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public Niveau getAcces(Tiers tiers) {

		final String visaOperateur = AuthenticationHelper.getCurrentPrincipal();
		if (visaOperateur == null) {
			return null;
		}

		final Operateur operateur = serviceSecurite.getOperateur(visaOperateur);
		if (operateur == null) {
			// pas d'opérateur défini -> pas de droit
			LOGGER.warn("L'opérateur [" + visaOperateur + "] n'existe pas.");
			return null;
		}

		return getAcces(operateur, tiers);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public Niveau getAcces(@NotNull String visaOperateur, long tiersId) {

		final Tiers tiers = tiersDAO.get(tiersId, true); // ne pas flusher la session automatiquement
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		final Operateur operateur = serviceSecurite.getOperateur(visaOperateur);
		if (operateur == null) {
			// pas d'opérateur défini -> pas de droit
			LOGGER.warn("L'opérateur [" + visaOperateur + "] n'existe pas.");
			return null;
		}

		return getAcces(operateur, tiers);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public List<Niveau> getAcces(@NotNull String visa, List<Long> ids) {

		final Operateur operateur = serviceSecurite.getOperateur(visa);
		if (operateur == null) {
			final int size = ids.size();
			final ArrayList<Niveau> niveaux = new ArrayList<>(size);
			for (int i = 0; i < size; ++i) {
				// pas d'opérateur défini -> pas de droit
				niveaux.add(null);
			}
			return niveaux;
		}

		return getAcces(operateur, ids);
	}

	/**
	 * @return calcul et retourne le niveau d'accès alloué à un opérateur sur un tiers (qui peut être une personne physique ou un ménage commun).
	 */
	private Niveau getAcces(Operateur operateur, Tiers tiers) {

		final Niveau acces;
		if (tiers instanceof MenageCommun) {
			acces = getAccessForMenage(operateur, (MenageCommun) tiers);
		}
		else if (tiers instanceof Etablissement) {
			acces = getAccessForEtablissement(operateur, (Etablissement) tiers);
		}
		else {
			acces = getAccessDirect(operateur, tiers.getNumero());
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("=> " + acces + " pour l'opérateur [" + operateur.getCode() + "] sur le dossier n° " + tiers.getNumero());
		}

		return acces;
	}

	private static <T extends Tiers> List<T> extract(Class<T> clazz, List<Tiers> source) {
		final List<T> resultat = new ArrayList<>(source.size());
		for (Tiers t : source) {
			if (clazz.isAssignableFrom(t.getClass())) {
				//noinspection unchecked
				resultat.add((T) t);
			}
		}
		return resultat;
	}

	private static <T extends Tiers> Map<Long, T> buildMapById(Collection<T> source) {
		final Map<Long, T> map = new HashMap<>(source.size());
		for (T tiers : source) {
			map.put(tiers.getNumero(), tiers);
		}
		return map;
	}

	/**
	 * @return calcul et retourne les niveaux d'accès alloués à un opérateur sur une liste de tiers (qui peuvent être des personnes physiques, des ménages communs, des entreprises ou des établissements).
	 */
	private List<Niveau> getAcces(Operateur operateur, List<Long> ids) {

		final List<Tiers> dossiers = tiersDAO.getBatch(ids, EnumSet.of(Parts.RAPPORTS_ENTRE_TIERS));
		final Map<Long, MenageCommun> menagesCommuns = buildMapById(extract(MenageCommun.class, dossiers));
		final Map<Long, Etablissement> etablissements = buildMapById(extract(Etablissement.class, dossiers));
		final Map<Long, PersonnePhysique> personnesPhysiques = buildMapById(extract(PersonnePhysique.class, dossiers));
		final Map<Long, Entreprise> entreprises = buildMapById(extract(Entreprise.class, dossiers));
		final Map<Long, Tiers> remaining = buildMapById(dossiers);
		remaining.keySet().removeAll(menagesCommuns.keySet());
		remaining.keySet().removeAll(etablissements.keySet());
		remaining.keySet().removeAll(personnesPhysiques.keySet());
		remaining.keySet().removeAll(entreprises.keySet());

		// constitution d'une map des accès globaux
		final Map<Long, Niveau> mapNiveaux = new HashMap<>(ids.size());
		mapNiveaux.putAll(getAccessDirect(operateur, personnesPhysiques.keySet()));
		mapNiveaux.putAll(getAccessDirect(operateur, entreprises.keySet()));
		mapNiveaux.putAll(getAccessForMenages(operateur, menagesCommuns.values()));
		mapNiveaux.putAll(getAccessForEtablissements(operateur, etablissements.values()));
		mapNiveaux.putAll(getAccessDirect(operateur, remaining.keySet()));

		// Calcule le niveau d'accès pour chaque id (en respectant l'ordre des ids)
		final List<Niveau> niveaux = new ArrayList<>(ids.size());
		for (Long id : ids) {
			niveaux.add(mapNiveaux.get(id));
		}
		return niveaux;
	}

	/**
	 * @return le niveau d'accès alloué à un opérateur sur un ménage commun.
	 */
	private Niveau getAccessForMenage(Operateur operateur, final MenageCommun mc) {

		// extraction des composants du ménage historique
		Set<Tiers> composants = new HashSet<>();
		for (RapportEntreTiers r : mc.getRapportsObjet()) {
			if (!r.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == r.getType()) {
				final Tiers sujet = tiersDAO.get(r.getSujetId());
				composants.add(sujet);
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Le dossier n° " + mc.getNumero() + " est un ménage-commun composé des tiers = "
					+ ArrayUtils.toString(composants.toArray()));
		}

		// calcul du droit d'accès minimal (= le plus restrictif)
		Niveau accessMinimal = Niveau.ECRITURE;
		for (Tiers t : composants) {
			if (t.isAnnule()) {
				continue;
			}
			final Niveau a = getAccessDirect(operateur, t.getNumero());
			if (a == null) {
				accessMinimal = null;
				break;
			}
			else if (a == Niveau.LECTURE) {
				accessMinimal = Niveau.LECTURE;
			}
		}

		return accessMinimal;
	}

	private Map<Long, Niveau> getAccessForMenages(Operateur operateur, Collection<MenageCommun> menages) {

		final Set<Long> idsPP = new HashSet<>();
		final Map<Long, List<Long>> idsPPparMC = new HashMap<>();

		// extraction de tous les ids des personnes physiques composantes de tous les ménages (vue historique)
		for (MenageCommun mc : menages) {
			final List<Long> idsComposants = new ArrayList<>();
			idsPPparMC.put(mc.getNumero(), idsComposants);
			for (RapportEntreTiers r : mc.getRapportsObjet()) {
				if (!r.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == r.getType()) {
					final Long id = r.getSujetId();
					idsPP.add(id);
					idsComposants.add(id);
				}
			}
		}

		// récupération des niveaux des personnes physiques d'une seule requête
		final Map<Long, Niveau> niveauxPP = getAccessDirect(operateur, idsPP);

		// tri des personnes physiques et calcul du niveau d'accès minimal sur chaque ménage
		final Map<Long, Niveau> results = new HashMap<>(menages.size());
		for (MenageCommun mc : menages) {
			final List<Long> idsComposants = idsPPparMC.get(mc.getNumero());

			Niveau accessMinimal = Niveau.ECRITURE;
			for (Long id : idsComposants) {
				final Niveau a = niveauxPP.get(id);
				if (a == null) {
					accessMinimal = null;
					break;
				}
				else if (a == Niveau.LECTURE) {
					accessMinimal = Niveau.LECTURE;
				}
			}

			results.put(mc.getNumero(), accessMinimal);
		}

		return results;
	}

	private Map<Long, Niveau> getAccessForEtablissements(Operateur operateur, Collection<Etablissement> etablissements) {

		final Set<Long> idsEntites = new HashSet<>();
		final Map<Long, Set<Long>> idsEntiteParEtablissement = new HashMap<>();

		// récupérons les identifiants des contribuables (entreprises et personnes physiques, peut-être...) concernées
		for (Etablissement etablissement : etablissements) {
			final Set<Long> idsPourEtablissement = new HashSet<>();
			idsEntiteParEtablissement.put(etablissement.getNumero(), idsPourEtablissement);
			for (RapportEntreTiers ret : etablissement.getRapportsObjet()) {
				if (!ret.isAnnule() && ret.getType() == TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE) {
					final Long idEntite = ret.getSujetId();
					idsEntites.add(idEntite);
					idsPourEtablissement.add(idEntite);
				}
			}
		}

		// on va chercher les accès directs sur les entités en question
		final Map<Long, Niveau> accesDirects = getAccessDirect(operateur, idsEntites);

		// reconstitution de la map par établissement
		final Map<Long, Niveau> parEtablissement = new HashMap<>(etablissements.size());
		for (Etablissement etablissement : etablissements) {
			final Set<Long> idsEntitesDeLEtablissement = idsEntiteParEtablissement.get(etablissement.getNumero());

			// on commence par l'accès le plus large et on réduit au fur et à mesure que l'on trouve des contraintes supplémentaires
			Niveau accesMinimal = Niveau.ECRITURE;
			for (Long idEntite : idsEntitesDeLEtablissement) {
				final Niveau direct = accesDirects.get(idEntite);
				if (direct == null) {
					accesMinimal = null;        // fini ! aucun accès autorisé !
					break;
				}
				else if (direct == Niveau.LECTURE) {
					accesMinimal = Niveau.LECTURE;
				}
			}
			parEtablissement.put(etablissement.getNumero(), accesMinimal);
		}

		return parEtablissement;
	}

	private Niveau getAccessForEtablissement(Operateur operateur, Etablissement etablissement) {

		// récupération des entités parentes de l'établissement
		final Set<Tiers> entites = new HashSet<>();
		for (RapportEntreTiers ret : etablissement.getRapportsObjet()) {
			if (!ret.isAnnule() && ret.getType() == TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE) {
				final Tiers entite = tiersDAO.get(ret.getSujetId());
				entites.add(entite);
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Le dossier n° "  + etablissement.getNumero() + " est un établissement lié aux tiers " + CollectionsUtils.toString(entites, StringRenderer.DEFAULT, ", ", "(aucun)"));
		}

		// on commence par le droit le plus large et on restreint en fonction des contraintes trouvées
		Niveau accesMininal = Niveau.ECRITURE;
		for (Tiers entite : entites) {
			if (entite.isAnnule()) {
				continue;
			}
			final Niveau direct = getAccessDirect(operateur, entite.getNumero());
			if (direct == null) {
				accesMininal = null;        // fini, aucun droit !
				break;
			}
			else if (direct == Niveau.LECTURE) {
				accesMininal = Niveau.LECTURE;
			}
		}
		return accesMininal;
	}

	/**
	 * @return le niveau d'accès alloué à un opérateur sur une personne physique.
	 */
	private Niveau getAccessDirect(Operateur operateur, long ppId) {
		final List<DroitAcces> droits = droitAccesDAO.getDroitsAccessTiers(ppId, RegDate.get());
		return calculateAccesDirect(operateur, droits, ppId);
	}

	private Map<Long, Niveau> getAccessDirect(Operateur operateur, Set<Long> idsPP) {

		// récupère tous les droits en UNE seule requête (perfs)
		final List<DroitAcces> droitsAll = droitAccesDAO.getDroitsAccessTiers(idsPP, RegDate.get());

		// trie les droits par contribuable
		final Map<Long, List<DroitAcces>> map = new HashMap<>(idsPP.size());
		for (DroitAcces d : droitsAll) {
			final List<DroitAcces> l = map.computeIfAbsent(d.getTiers().getNumero(), k -> new ArrayList<>());
			l.add(d);
		}

		// calcule les droits pour chaque contribuable
		final Map<Long, Niveau> results = new HashMap<>();
		for (Long id : idsPP) {
			final List<DroitAcces> l = map.get(id);
			if (l == null) {
				// sans indication particulière -> accès en lecture/écriture total
				results.put(id, Niveau.ECRITURE);
			}
			else {
				final Niveau niveau = calculateAccesDirect(operateur, l, id);
				results.put(id, niveau);
			}
		}

		return results;
	}

	private Niveau calculateAccesDirect(Operateur operateur, final List<DroitAcces> droits, long ppId) {

		// on récupère et analyse les droits d'accès au dossiers stockés dans Unireg
		Niveau granted = null;
		Niveau restricted = null;
		boolean otherOperatorsGranted = false;

		for (DroitAcces d : droits) {
			if (d.isAnnule()) {
				continue;
			}
			if (StringUtils.equalsIgnoreCase(d.getVisaOperateur(), operateur.getCode())) {
				switch (d.getType()) {
				case AUTORISATION:
					switch (d.getNiveau()) {
					case ECRITURE:
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Trouvé l'autorisation [ECRITURE] de l'opérateur [" + operateur.getCode() + "] sur le dossier n° "
									+ ppId + '.');
						}
						granted = Niveau.ECRITURE;
						break;
					case LECTURE:
						if (granted == null) {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Trouvé l'autorisation [LECTURE] de l'opérateur [" + operateur.getCode()
										+ "] sur le dossier n° " + ppId + '.');
							}
							granted = Niveau.LECTURE;
						}
						break;
					}
					break;

				case INTERDICTION:
					switch (d.getNiveau()) {
					case ECRITURE:
						if (restricted == null) {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Trouvé l'interdiction [ECRITURE] de l'opérateur [" + operateur.getCode()
										+ "] sur le dossier n° " + ppId + '.');
							}
							restricted = Niveau.ECRITURE;
						}
						break;
					case LECTURE:
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Trouvé l'interdiction [LECTURE] de l'opérateur [" + operateur.getCode() + "] sur le dossier n° "
									+ ppId + '.');
						}
						restricted = Niveau.LECTURE;
						break;
					}
				}
			}
			else {
				if (d.getType() == TypeDroitAcces.AUTORISATION) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Trouvé une autorisation d'un autre opérateur que [" + operateur.getCode() + "] sur le dossier n° "
								+ ppId + '.');
					}
					otherOperatorsGranted = true;
				}
			}
		}

		// bypass des sécurités pour les membres de la direction de l'ACI (qui sont les seuls à posséder ces rôles)
		if (securityProvider.isGranted(Role.ECRITURE_DOSSIER_PROTEGE, operateur.getCode(), ServiceInfrastructureService.noACI)) {
			if (granted == null || granted == Niveau.LECTURE) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Promotion du niveau d'accès de [" + granted + "] à [ECRITURE] pour l'opérateur [" + operateur.getCode()
							+ "] sur le dossier n° " + ppId + " parce qu'il possède le rôle spécial [ECRITURE_DOSSIER_PROTEGE].");
				}
				granted = Niveau.ECRITURE;
			}
		}
		else if (securityProvider.isGranted(Role.LECTURE_DOSSIER_PROTEGE, operateur.getCode(), ServiceInfrastructureService.noACI)) {
			if (granted == null) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Promotion du niveau d'accès de [null] à [LECTURE] pour l'opérateur [" + operateur.getCode()
							+ "] sur le dossier n° " + ppId + " parce qu'il possède le rôle spécial [LECTURE_DOSSIER_PROTEGE].");
				}
				granted = Niveau.LECTURE;
			}
		}

		if (granted != null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("L'opérateur [" + operateur.getCode() + "] possède le droit " + granted + " sur le dossier n° " + ppId);
			}
			return granted;
		}

		if (otherOperatorsGranted) {
			// d'autres opérateurs possèdent des autorisations d'accès -> restriction implicite des droits d'accès pour ceux qui n'ont en pas.
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("L'opérateur [" + operateur.getCode() + "] ne possède aucun droit d'accès au dossier n° " + ppId
						+ " car d'autres opérateurs possèdent des autorisations.");
			}
			return null;
		}

		if (restricted != null) {
			if (restricted == Niveau.ECRITURE) {
				// restriction de l'accès en écriture -> droit d'accès en lecture seulement
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("L'opérateur [" + operateur.getCode() + "] possède le droit LECTURE au dossier n° " + ppId
							+ " car il y a une restriction explicite en écriture.");
				}
				return Niveau.LECTURE;
			}
			else {
				// restriction de l'accès en lecture -> plus aucun droit d'accès
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("L'opérateur [" + operateur.getCode() + "] ne possède aucun droit d'accès au dossier n° " + ppId
							+ " car il y a une restriction explicite en lecture.");
				}
				return null;
			}
		}

		// sans indication particulière -> accès en lecture/écriture total
		return Niveau.ECRITURE;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDroitAccesDAO(DroitAccesDAO droitAccesDAO) {
		this.droitAccesDAO = droitAccesDAO;
	}
}

