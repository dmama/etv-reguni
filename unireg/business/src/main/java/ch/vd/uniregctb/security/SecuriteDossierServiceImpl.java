package ch.vd.uniregctb.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeDroitAcces;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class SecuriteDossierServiceImpl implements SecuriteDossierService {

	private static final Logger LOGGER = Logger.getLogger(SecuriteDossierServiceImpl.class);

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
			throw new ObjectNotFoundException("Le tiers spécifié n'existe pas");
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
	public Niveau getAcces(String visaOperateur, long tiersId) {

		final Tiers tiers = tiersDAO.get(tiersId, true); // ne pas flusher la session automatiquement
		if (tiers == null) {
			throw new ObjectNotFoundException("Le tiers spécifié n'existe pas");
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
	public List<Niveau> getAcces(String visa, List<Long> ids) {

		final Operateur operateur = serviceSecurite.getOperateur(visa);
		if (operateur == null) {
			final int size = ids.size();
			final ArrayList<Niveau> niveaux = new ArrayList<Niveau>(size);
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
			final MenageCommun mc = (MenageCommun) tiers;
			acces = getAccessForMC(operateur, mc);
		}
		else {
			acces = getAccessForPP(operateur, tiers.getNumero());
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("=> " + acces + " pour l'opérateur [" + operateur.getCode() + "] sur le dossier n° " + tiers.getNumero());
		}

		return acces;
	}

	/**
	 * @return calcul et retourne les niveaux d'accès alloué à un opérateur sur une liste de tiers (qui peuvent être des personnes physiques ou des ménages communs).
	 */
	private List<Niveau> getAcces(Operateur operateur, List<Long> ids) {

		final List<Niveau> niveaux = new ArrayList<Niveau>(ids.size());

		// Récupère la liste des ménages-commun existant dans la liste d'ids spécifiée.
		final List<MenageCommun> menages = tiersDAO.getMenagesCommuns(ids, new HashSet<Parts>(Arrays.asList(Parts.RAPPORTS_ENTRE_TIERS)));
		final Map<Long, MenageCommun> map = new HashMap<Long, MenageCommun>(menages.size());
		for (MenageCommun mc : menages) {
			map.put(mc.getNumero(), mc);
		}

		// Calcul la liste des personnes physiques
		final List<Long> idsPP = new ArrayList<Long>(ids.size() - menages.size());
		for (Long id : ids) {
			if (!map.containsKey(id)) {
				idsPP.add(id);
			}
		}

		// Pré-calcule le niveau d'accès pour toutes les personnes physiques (en une seule requête pour des raisons de perfs)
		final Map<Long, Niveau> niveauxPP = getAccessForPP(operateur, idsPP);

		// Pré-calcule le niveau d'accès pour tous les ménage communs (en une seule requête pour des raisons de perfs)
		final Map<Long, Niveau> niveauxMC = getAccessForMC(operateur, menages);

		// Calcule le niveau d'accès pour chaque id (en respectant l'ordre des ids)
		for (Long id : ids) {

			final Niveau niveau;

			final MenageCommun mc = map.get(id);
			if (mc != null) {
				niveau = niveauxMC.get(id);
			}
			else {
				niveau = niveauxPP.get(id);
			}

			niveaux.add(niveau);
		}

		return niveaux;
	}

	/**
	 * @return le niveau d'accès alloué à un opérateur sur un ménage commun.
	 */
	private Niveau getAccessForMC(Operateur operateur, final MenageCommun mc) {

		// extraction des composants du ménage historique
		Set<Tiers> composants = new HashSet<Tiers>();
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
			Niveau a = getAccessForPP(operateur, t.getNumero());
			if (a == null) {
				accessMinimal = null;
				break;
			}
			else if (accessMinimal != null && a == Niveau.LECTURE) {
				accessMinimal = Niveau.LECTURE;
			}
		}

		return accessMinimal;
	}

	private Map<Long, Niveau> getAccessForMC(Operateur operateur, List<MenageCommun> menages) {

		final Set<Long> idsPP = new HashSet<Long>();
		final Map<Long, List<Long>> idsPPparMC = new HashMap<Long, List<Long>>();

		// extraction de tous les ids des personnes physiques composantes de tous les ménages (vue historique)
		for (MenageCommun mc : menages) {
			List<Long> idsComposants = new ArrayList<Long>();
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
		final Map<Long, Niveau> niveauxPP = getAccessForPP(operateur, new ArrayList<Long>(idsPP));

		Map<Long, Niveau> results = new HashMap<Long, Niveau>();

		// tri des personnes physiques et calcul du niveau d'accès minimal sur chaque ménage
		for (MenageCommun mc : menages) {
			final List<Long> idsComposants = idsPPparMC.get(mc.getNumero());

			Niveau accessMinimal = Niveau.ECRITURE;

			for (Long id : idsComposants) {
				Niveau a = niveauxPP.get(id);
				if (a == null) {
					accessMinimal = null;
					break;
				}
				else if (accessMinimal != null && a == Niveau.LECTURE) {
					accessMinimal = Niveau.LECTURE;
				}
			}

			results.put(mc.getNumero(), accessMinimal);
		}

		return results;
	}

	/**
	 * @return le niveau d'accès alloué à un opérateur sur une personne physique.
	 */
	private Niveau getAccessForPP(Operateur operateur, long ppId) {

		final List<DroitAcces> droits = droitAccesDAO.getDroitsAccessTiers(ppId, RegDate.get());
		return calculateAccesPP(operateur, droits, ppId);
	}

	private Map<Long, Niveau> getAccessForPP(Operateur operateur, List<Long> idsPP) {

		// récupère tous les droits en UNE seule requête (perfs)
		final List<DroitAcces> droitsAll = droitAccesDAO.getDroitsAccessTiers(idsPP, RegDate.get());

		// trie les droits par contribuable
		final Map<Long, List<DroitAcces>> map = new HashMap<Long, List<DroitAcces>>();
		for (DroitAcces d : droitsAll) {
			List<DroitAcces> l = map.get(d.getTiers().getNumero());
			if (l == null) {
				l = new ArrayList<DroitAcces>();
				map.put(d.getTiers().getNumero(), l);
			}
			l.add(d);
		}

		// calcule les droits pour chaque contribuable
		final Map<Long, Niveau> results = new HashMap<Long, Niveau>();
		for (Long id : idsPP) {
			final List<DroitAcces> l = map.get(id);
			if (l == null) {
				// sans indication particulière -> accès en lecture/écriture total
				results.put(id, Niveau.ECRITURE);
			}
			else {
				Niveau niveau = calculateAccesPP(operateur, l, id);
				results.put(id, niveau);
			}
		}

		return results;
	}

	private Niveau calculateAccesPP(Operateur operateur, final List<DroitAcces> droits, long ppId) {

		// on récupère et analyse les droits d'accès au dossiers stockés dans Unireg
		Niveau granted = null;
		Niveau restricted = null;
		boolean otherOperatorsGranted = false;

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("L'opérateur [" + operateur.getCode() + "] possède le numéro technique = " + operateur.getIndividuNoTechnique());
		}

		for (DroitAcces d : droits) {
			if (d.isAnnule()) {
				continue;
			}
			if (d.getNoIndividuOperateur() == operateur.getIndividuNoTechnique()) {
				switch (d.getType()) {
				case AUTORISATION:
					switch (d.getNiveau()) {
					case ECRITURE:
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Trouvé l'autorisation [ECRITURE] de l'opérateur [" + operateur.getCode() + "] sur le dossier n° "
									+ ppId + ".");
						}
						granted = Niveau.ECRITURE;
						break;
					case LECTURE:
						if (granted == null) {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Trouvé l'autorisation [LECTURE] de l'opérateur [" + operateur.getCode()
										+ "] sur le dossier n° " + ppId + ".");
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
										+ "] sur le dossier n° " + ppId + ".");
							}
							restricted = Niveau.ECRITURE;
						}
						break;
					case LECTURE:
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Trouvé l'interdiction [LECTURE] de l'opérateur [" + operateur.getCode() + "] sur le dossier n° "
									+ ppId + ".");
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
								+ ppId + ".");
					}
					otherOperatorsGranted = true;
				}
			}
		}

		// bypass des sécurités pour les membres de la direction de l'ACI (qui sont les seuls à posséder ces rôles)
		if (SecurityProvider.isGranted(Role.ECRITURE_DOSSIER_PROTEGE, operateur.getCode(), ServiceInfrastructureService.noACI)) {
			if (granted == null || granted == Niveau.LECTURE) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Promotion du niveau d'accès de [" + granted + "] à [ECRITURE] pour l'opérateur [" + operateur.getCode()
							+ "] sur le dossier n° " + ppId + " parce qu'il possède le rôle spécial [ECRITURE_DOSSIER_PROTEGE].");
				}
				granted = Niveau.ECRITURE;
			}
		}
		else if (SecurityProvider.isGranted(Role.LECTURE_DOSSIER_PROTEGE, operateur.getCode(), ServiceInfrastructureService.noACI)) {
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

