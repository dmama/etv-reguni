package ch.vd.uniregctb.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeDroitAcces;

public class DroitAccesServiceImpl implements DroitAccesService {

	private TiersDAO tiersDAO;
	private DroitAccesDAO droitAccesDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setDroitAccesDAO(DroitAccesDAO droitAccesDAO) {
		this.droitAccesDAO = droitAccesDAO;
	}

	/**
	 * {@inheritDoc}
	 */
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
		if (!(tiers instanceof PersonnePhysique)) {
			throw new DroitAccesException("Le tiers n°" + tiersId + " n'est pas une personne physique.");
		}

		return ajouteDroitAcces(operateurId, (PersonnePhysique) tiers, type, niveau, aujourdhui, null);
	}

	/**
	 * Crée un nouveau droit d'accès sur le dossier de la personne physique donnée, ou adapte un droit existant si possible
	 * @param ppSource dossier source des droits d'accès
	 * @param ppDestination dossier auquel les droits d'accès doivent être ajoutés
	 * @throws DroitAccesException en cas de conflit entre les droits existant sur le dossier de destination et ceux sur le dossier source
	 */
	public void copieDroitsAcces(final PersonnePhysique ppSource, final PersonnePhysique ppDestination) throws DroitAccesException {

		final Set<DroitAcces> droitsSource = ppSource.getDroitsAccesAppliques();
		final Set<DroitAcces> droitsDestination = ppDestination.getDroitsAccesAppliques();
		final RegDate aujourdhui = RegDate.get();

		copieDroitsAcces(droitsSource, droitsDestination, aujourdhui, new CopieDroitCallback() {
			public boolean adapteExistant(DroitAcces src, DroitAcces dst) throws DroitAccesException {
				return adapteExistantPourTransfertDossier(src, dst, aujourdhui);
			}

			public void fillDestinationContext(DroitAcces droit) {
				droit.setTiers(ppDestination);
			}

			public void postTraiteSource(DroitAcces droit) {
				// rien de spécial à faire
			}
		});
	}

	private static interface CopieDroitCallback {
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

		if (droitsSource.size() > 0) {

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
	private DroitAcces ajouteDroitAcces(long operateurId, PersonnePhysique pp, TypeDroitAcces type, Niveau niveau, RegDate dateDebut, RegDate dateFin) {
		final DroitAcces droitAcces = new DroitAcces();
		droitAcces.setDateDebut(dateDebut);
		droitAcces.setDateFin(dateFin);
		droitAcces.setNiveau(niveau);
		droitAcces.setType(type);
		droitAcces.setNoIndividuOperateur(operateurId);
		droitAcces.setTiers(pp);
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
	private boolean adapteExistantPourTransfertOperateur(DroitAcces aAjouter, DroitAcces existant, RegDate aujourdhui) throws DroitAccesException {
		Assert.isFalse(aAjouter.isAnnule());
		return  aAjouter.getTiers().getNumero().equals(existant.getTiers().getNumero()) &&
				adapteExistant(aAjouter.getType(), aAjouter.getNiveau(), aAjouter, existant, aujourdhui);
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
		Assert.isFalse(aAjouter.isAnnule());
		return  aAjouter.getNoIndividuOperateur() == existant.getNoIndividuOperateur() &&
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
	 * @throws DroitAccesException en cas de conflit (la spécification dit qu'un conflit est un droit qui serait à la fois en écriture et en lecture, ou à la fois une autorisation et une interdiction)
	 */
	private boolean adapteExistant(TypeDroitAcces type, Niveau niveau, DateRange periode, DroitAcces existant, RegDate aujourdhui) throws DroitAccesException {

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
					final String msg = String.format("Impossible d'ajouter le droit d'accès %s/%s sur le dossier %s à l'opérateur %d car celui-ci entrerait en conflit avec un droit %s/%s existant",
													type, niveau, FormatNumeroHelper.numeroCTBToDisplay(existant.getTiers().getNumero()),
													existant.getNoIndividuOperateur(), existant.getType(), existant.getNiveau());
					throw new DroitAccesException(msg);
				}
			}
		}

		// rien n'a été modifié
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void annuleDroitAcces(long id) throws DroitAccesException {

		final DroitAcces da = droitAccesDAO.get(id);
		if (da == null) {
			throw new DroitAccesException("Le droit d'accès n°" + id + " n'existe pas");
		}
		da.setAnnule(true);
		da.setDateFin(RegDate.get());
	}

	/**
	 * {@inheritDoc}
	 */
	public void copieDroitsAcces(long operateurSourceId, long operateurTargetId) throws DroitAccesException {
		copie(operateurSourceId, operateurTargetId, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public void transfereDroitsAcces(long operateurSourceId, long operateurTargetId) throws DroitAccesException {
		copie(operateurSourceId, operateurTargetId, true);
	}

	private void copie(final long operateurSourceId, final long operateurTargetId, final boolean fermeSource) throws DroitAccesException {

		final List<DroitAcces> source = droitAccesDAO.getDroitsAcces(operateurSourceId);
		final List<DroitAcces> target = droitAccesDAO.getDroitsAcces(operateurTargetId);
		final RegDate dateReference = RegDate.get();

		copieDroitsAcces(source, target, dateReference, new CopieDroitCallback() {
			public boolean adapteExistant(DroitAcces src, DroitAcces dst) throws DroitAccesException {
				return adapteExistantPourTransfertOperateur(src, dst, dateReference);
			}

			public void fillDestinationContext(DroitAcces droit) {
				droit.setNoIndividuOperateur(operateurTargetId);
			}

			public void postTraiteSource(DroitAcces droit) {
				if (fermeSource) {
					droit.setDateFin(dateReference);
				}
			}
		});
	}
}
