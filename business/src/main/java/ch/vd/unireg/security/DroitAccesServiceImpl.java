package ch.vd.unireg.security;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.service.ServiceSecuriteException;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeDroitAcces;

public class DroitAccesServiceImpl implements DroitAccesService {

	private TiersDAO tiersDAO;
	private DroitAccesDAO droitAccesDAO;
	private ServiceSecuriteService serviceSecuriteService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setDroitAccesDAO(DroitAccesDAO droitAccesDAO) {
		this.droitAccesDAO = droitAccesDAO;
	}

	public void setServiceSecuriteService(ServiceSecuriteService serviceSecuriteService) {
		this.serviceSecuriteService = serviceSecuriteService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DroitAcces ajouteDroitAcces(long operateurId, long tiersId, TypeDroitAcces type, Niveau niveau) throws DroitAccesException {

		final RegDate aujourdhui = RegDate.get();

		final DroitAcces da = droitAccesDAO.getDroitAcces(operateurId, tiersId, aujourdhui);
		if (da != null && !da.isAnnule()) {
			throw new DroitAccesException("Un droit d'accès existe déjà entre l'opérateur n°" + operateurId + " et le tiers n°" + tiersId);
		}

		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null) {
			throw new DroitAccesException("Le tiers n°" + tiersId + " n'existe pas.");
		}
		checkTiers(tiers);
		return ajouteDroitAcces(operateurId, (Contribuable) tiers, type, niveau, aujourdhui, null);
	}

	/**
	 * Vérifie que le tiers est soit une personne physique soit une entreprise
	 * @param tiers tiers à tester
	 * @throws DroitAccesException si le tiers n'est ni une personne physique ni une entreprise
	 */
	private static void checkTiers(Tiers tiers) throws DroitAccesException {
		if (!(tiers instanceof PersonnePhysique) && !(tiers instanceof Entreprise)) {
			throw new DroitAccesException("Le tiers n°" + tiers.getNumero() + " n'est ni une personne physique ni une entreprise.");
		}
	}

	/**
	 * Crée un nouveau droit d'accès sur le dossier du contribuable donné, ou adapte un droit existant si possible
	 * @param ctbSource dossier source des droits d'accès
	 * @param ctbDestination dossier auquel les droits d'accès doivent être ajoutés
	 * @throws DroitAccesException en cas de conflit entre les droits existant sur le dossier de destination et ceux sur le dossier source
	 */
	@Override
	public void copieDroitsAcces(final Contribuable ctbSource, final Contribuable ctbDestination) throws DroitAccesException {
		checkTiers(ctbDestination);
		final Set<DroitAcces> droitsSource = ctbSource.getDroitsAccesAppliques();
		final Set<DroitAcces> droitsDestination = ctbDestination.getDroitsAccesAppliques();
		final RegDate aujourdhui = RegDate.get();

		copieDroitsAcces(droitsSource, droitsDestination, aujourdhui, new CopieDroitCallback() {
			@Override
			public boolean adapteExistant(DroitAcces src, DroitAcces dst) throws DroitAccesException {
				return adapteExistantPourTransfertDossier(src, dst, aujourdhui);
			}

			@Override
			public void fillDestinationContext(DroitAcces droit) {
				droit.setTiers(ctbDestination);
			}

			@Override
			public void postTraiteSource(DroitAcces droit) {
				// rien de spécial à faire
			}
		});
	}

	private interface CopieDroitCallback {
		/**
		 * Appelé pour déterminer si une simple adaptation du droit de destination
		 * est suffisante (ou conflictuelle) pour intégrer un nouveau droit d'accès
		 * @param src le droit d'accès à intégrer
		 * @param dst le droit d'accès potentiellement adaptable pour tenir compte du nouveau droit
		 * @return <code>true</code> si le droit de destination est adapté et que cela suffit, <code>false</code> si une adaptation n'est pas possible ou suffisante
		 * @throws DroitAccesException en cas de conflit entre le droit existant (la destination) et le droit candidat (la source)
		 */
		boolean adapteExistant(DroitAcces src, DroitAcces dst) throws DroitAccesException;

		/**
		 * Appelé pour remplir les données du contexte de destination sur le droit nouvellement dupliqué depuis un droit de l'environnement source
		 * @param droit droit d'accès à compléter
		 */
		void fillDestinationContext(DroitAcces droit);

		/**
		 * Appelé avec le droit source une fois que sa copie est terminée
		 * @param droit droit source
		 */
		void postTraiteSource(DroitAcces droit);
	}

	private void copieDroitsAcces(Collection<DroitAcces> droitsSource, Collection<DroitAcces> droitsDestination, RegDate dateReference, CopieDroitCallback callback) throws DroitAccesException {

		if (!droitsSource.isEmpty()) {

			for (DroitAcces src : droitsSource) {

				if (!src.isValidAt(dateReference)) {
					// inutile de copier les droits antérieurs
					continue;
				}

				// copie nécessaire ou adaptation suffisante ?
				boolean copieNecessaire = true;
				for (DroitAcces dest : droitsDestination) {
					if (callback.adapteExistant(src, dest)) {
						copieNecessaire = false;
						// on continue la boucle des fois qu'on tomberait sur un conflit...
					}
				}

				// s'il faut faire une copie, c'est le moment
				if (copieNecessaire) {
					final DroitAcces nouveau = src.duplicate();
					nouveau.setDateDebut(dateReference);
					nouveau.setDateFin(null);
					callback.fillDestinationContext(nouveau);
					droitAccesDAO.save(nouveau);
				}

				callback.postTraiteSource(src);
			}
		}
	}

	/**
	 * Crée une nouvelle instance de DroitAcces non-annulée
	 * @return la nouvelle instance déjà sauvegardée
	 */
	private DroitAcces ajouteDroitAcces(long operateurId, Contribuable ctb, TypeDroitAcces type, Niveau niveau, RegDate dateDebut, RegDate dateFin) {
		final DroitAcces droitAcces = new DroitAcces();
		droitAcces.setDateDebut(dateDebut);
		droitAcces.setDateFin(dateFin);
		droitAcces.setNiveau(niveau);
		droitAcces.setType(type);
		droitAcces.setNoIndividuOperateur(operateurId);
		droitAcces.setTiers(ctb);
		return droitAccesDAO.save(droitAcces);
	}

	/**
	 * Détecte un éventuel conflit autour des deux droits donnés (peuvent être considérés comme en conflit ici deux droits qui sont assignés à deux opérateurs différents, puisque le but est justement de faire
	 * ce test avant de faire une copie/un transfert de droit d'un opérateur à un autre)
	 * @param aAjouter candidat à l'ajout
	 * @param existant droit existant avec lequel il faut vérifier la compatibilité
	 * @param aujourdhui date du jour (= date d'ouverture au plus tard du droit existant en cas d'adaptation)
	 * @return <code>true</code> si le droit déjà existant a juste été adapté à l'ajout, <code>false</code> si rien n'a été modifié
	 * @throws DroitAccesException en cas de conflit (la spécification dit qu'un conflit est un droit qui serait à la fois en écriture et en lecture, ou à la fois une autorisation et une interdiction)
	 */
	private boolean adapteExistantPourTransfertDossier(DroitAcces aAjouter, DroitAcces existant, RegDate aujourdhui) throws DroitAccesException {
		if (aAjouter.isAnnule()) {
			throw new IllegalArgumentException();
		}
		return aAjouter.getNoIndividuOperateur() == existant.getNoIndividuOperateur() &&
				adapteExistant(aAjouter.getType(), aAjouter.getNiveau(), aAjouter, existant, aujourdhui);
	}

	/**
	 * Détecte un éventuel conflit dans le cas où on veut ajouter un droit au même dossier que le droit existant
	 * @param type type du droit à octroyer
	 * @param niveau niveau du droit à octroyer
	 * @param periode période de temps du nouveau droit
	 * @param existant droit existant avec lequel il faut vérifier la compatibilité
	 * @param aujourdhui date du jour (= date d'ouverture au plus tard du droit existant en cas d'adaptation)
	 * @return <code>true</code> si le droit déjà existant a juste été adapté à l'ajout, <code>false</code> si rien n'a été modifié
	 * @throws DroitAccesConflitException en cas de conflit (la spécification dit qu'un conflit est un droit qui serait à la fois en écriture et en lecture, ou à la fois une autorisation et une interdiction)
	 */
	private boolean adapteExistant(TypeDroitAcces type, Niveau niveau, DateRange periode, DroitAcces existant, RegDate aujourdhui) throws DroitAccesConflitException {

		// si l'un au moins des droits est annulé, il ne peut y avoir de conflit, ni d'adaptation
		if (!existant.isAnnule()) {

			// il ne peut y avoir conflit que si les périodes de validité s'intersectent
			if (DateRangeHelper.intersect(periode, existant)) {

				// mêmes type/niveau de droit -> on doit adapter l'existant (sinon, c'est un conflit)
				if (niveau == existant.getNiveau() && type == existant.getType()) {

					// pas de conflit, mais droit existant -> il est adapté
					existant.setDateFin(null);
					if (!RegDateHelper.isAfterOrEqual(aujourdhui, existant.getDateDebut(), NullDateBehavior.EARLIEST)) {
						existant.setDateDebut(aujourdhui);
					}
					return true;
				}
				else {
					Operateur operateur;
					try {
						operateur = serviceSecuriteService.getOperateur(existant.getNoIndividuOperateur());
					}
					catch (ServiceSecuriteException e) {
						operateur = null;
					}
					final String nomOperateur = operateur != null ? String.format("%s %s", operateur.getPrenom(), operateur.getNom()) : "?";
					final String visaOperateur = operateur != null ? operateur.getCode() : "?";
					final String msg = String.format("Impossible d'ajouter le droit d'accès à l'opérateur '%s' (%s/%d).", nomOperateur, visaOperateur, existant.getNoIndividuOperateur());
					throw new DroitAccesConflitException(msg, new DroitAccesConflit(existant.getTiers().getNumero(),
					                                                                existant.getType(), existant.getNiveau(),
					                                                                type, niveau));
				}
			}
		}

		// rien n'a été modifié
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void annuleDroitAcces(long id) throws DroitAccesException {

		final DroitAcces da = droitAccesDAO.get(id);
		if (da == null) {
			throw new DroitAccesException("Le droit d'accès n°" + id + " n'existe pas");
		}
        annuleDroitAcces(da);

	}

    private void annuleDroitAcces(DroitAcces da) {
        da.setAnnule(true);
		da.setDateFin(RegDate.get());
    }

    @Override
    public void annuleToutLesDroitAcces(long noIndividuOperateur) {
        for (DroitAcces da : droitAccesDAO.getDroitsAcces(noIndividuOperateur)) {
            annuleDroitAcces(da);
        }
    }

	@Override
	public List<DroitAccesConflit> copieDroitsAcces(long operateurSourceId, long operateurTargetId) {
		return copie(operateurSourceId, operateurTargetId, false);
	}

	@Override
	public List<DroitAccesConflit> transfereDroitsAcces(long operateurSourceId, long operateurTargetId) {
		return copie(operateurSourceId, operateurTargetId, true);
	}

	/**
	 * Détecte un éventuel conflit autour des deux droits donnés (peuvent être considérés comme en conflit ici deux droits qui sont assignés à deux opérateurs différents, puisque le but est justement de faire
	 * ce test avant de faire une copie/un transfert de droit d'un opérateur à un autre)
	 * @param aAjouter candidat à l'ajout
	 * @param existant droit existant avec lequel il faut vérifier la compatibilité
	 * @param aujourdhui date du jour (= date d'ouverture au plus tard du droit existant en cas d'adaptation)
	 * @return <code>true</code> si le droit déjà existant a juste été adapté à l'ajout, <code>false</code> s'il faut recopier le droit
	 */
	private boolean adapteExistantPourTransfertOperateur(DroitAcces aAjouter, DroitAcces existant, RegDate aujourdhui, List<DroitAccesConflit> conflits) {
		if (aAjouter.isAnnule()) {
			throw new IllegalArgumentException();
		}
		if (aAjouter.getTiers().getNumero().equals(existant.getTiers().getNumero())) {
			try {
				return adapteExistant(aAjouter.getType(), aAjouter.getNiveau(), aAjouter, existant, aujourdhui);
			}
			catch (DroitAccesConflitException e) {
				conflits.add(e.getConflit());
				return true;
			}
		}
		else {
			return false;
		}
	}

	private List<DroitAccesConflit> copie(final long operateurSourceId, final long operateurTargetId, final boolean fermeSource) {

		final List<DroitAcces> source = droitAccesDAO.getDroitsAcces(operateurSourceId);
		final List<DroitAcces> target = droitAccesDAO.getDroitsAcces(operateurTargetId);
		final RegDate dateReference = RegDate.get();

		final List<DroitAccesConflit> conflits = new LinkedList<>();
		try {
			copieDroitsAcces(source, target, dateReference, new CopieDroitCallback() {
				@Override
				public boolean adapteExistant(DroitAcces src, DroitAcces dst) {
					return adapteExistantPourTransfertOperateur(src, dst, dateReference, conflits);
				}

				@Override
				public void fillDestinationContext(DroitAcces droit) {
					droit.setNoIndividuOperateur(operateurTargetId);
				}

				@Override
				public void postTraiteSource(DroitAcces droit) {
					if (fermeSource && droit.isValidAt(dateReference)) {
						droit.setDateFin(dateReference);
					}
				}
			});
		}
		catch (DroitAccesException e) {
			// cela ne devrait pas être possible maintenant que l'on gère les conflits à ce niveau
			throw new RuntimeException(e);
		}
		return conflits.isEmpty() ? Collections.emptyList() : conflits;
	}
}
