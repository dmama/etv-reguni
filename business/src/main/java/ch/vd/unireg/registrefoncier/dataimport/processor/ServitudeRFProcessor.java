package ch.vd.unireg.registrefoncier.dataimport.processor;

import javax.persistence.FlushModeType;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.converter.jaxp.StringSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AddAndSaveHelper;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.BeneficeServitudeRF;
import ch.vd.unireg.registrefoncier.ChargeServitudeRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.ServitudeRF;
import ch.vd.unireg.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dao.ServitudeRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.dataimport.elements.servitude.DienstbarkeitExtendedElement;
import ch.vd.unireg.registrefoncier.dataimport.helper.AyantDroitRFHelper;
import ch.vd.unireg.registrefoncier.dataimport.helper.ImmeubleRFHelper;
import ch.vd.unireg.registrefoncier.dataimport.helper.ServitudesRFHelper;
import ch.vd.unireg.registrefoncier.key.AyantDroitRFKey;
import ch.vd.unireg.registrefoncier.key.DroitRFKey;
import ch.vd.unireg.registrefoncier.key.ImmeubleRFKey;

/**
 * Processeur spécialisé pour traiter les mutations des servitudes.
 */
public class ServitudeRFProcessor implements MutationRFProcessor {

	@NotNull
	private final AyantDroitRFDAO ayantDroitRFDAO;

	@NotNull
	private final ImmeubleRFDAO immeubleRFDAO;

	@NotNull
	private final ServitudeRFDAO servitudeRFDAO;

	@NotNull
	private final ThreadLocal<Unmarshaller> unmarshaller;

	@NotNull
	private final EvenementFiscalService evenementFiscalService;

	public ServitudeRFProcessor(@NotNull AyantDroitRFDAO ayantDroitRFDAO, @NotNull ImmeubleRFDAO immeubleRFDAO, @NotNull ServitudeRFDAO servitudeRFDAO, @NotNull XmlHelperRF xmlHelperRF,
	                            @NotNull EvenementFiscalService evenementFiscalService) {
		this.ayantDroitRFDAO = ayantDroitRFDAO;
		this.immeubleRFDAO = immeubleRFDAO;
		this.servitudeRFDAO = servitudeRFDAO;

		unmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getServitudeEtendueContext().createUnmarshaller();
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}
		});
		this.evenementFiscalService = evenementFiscalService;
	}

	@Override
	public void process(@NotNull EvenementRFMutation mutation, boolean importInitial, @Nullable MutationsRFProcessorResults rapport) {

		if (mutation.getEtat() == EtatEvenementRF.TRAITE || mutation.getEtat() == EtatEvenementRF.FORCE) {
			throw new IllegalArgumentException("La mutation n°" + mutation.getId() + " est déjà traitée (état=[" + mutation.getEtat() + "]).");
		}

		final RegDate dateValeur = mutation.getParentImport().getDateEvenement();
		final TypeMutationRF typeMutation = mutation.getTypeMutation();
		final DroitRFKey servitudeKey = new DroitRFKey(mutation.getIdRF(), mutation.getVersionRF());

		if (typeMutation == TypeMutationRF.CREATION || typeMutation == TypeMutationRF.MODIFICATION) {

			// on interpète le XML
			final DienstbarkeitExtendedElement dienstbarkeit;
			try {
				final String content = mutation.getXmlContent();
				final StringSource source = new StringSource(content);
				dienstbarkeit = (DienstbarkeitExtendedElement) unmarshaller.get().unmarshal(source);
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}

			// on crée la servitude en mémoire
			final ServitudeRF servitude = ServitudesRFHelper.newServitudeRF(dienstbarkeit, this::findAyantDroit, this::findImmeuble);

			// on traite la mutation
			if (typeMutation == TypeMutationRF.CREATION) {
				processCreation(importInitial ? null : dateValeur, servitude);
			}
			else {
				processModification(dateValeur, servitude);
			}
		}
		else if (typeMutation == TypeMutationRF.SUPPRESSION) {
			processSuppression(dateValeur, servitudeKey);
		}
		else {
			throw new IllegalArgumentException("Type de mutation inconnu = [" + typeMutation + "]");
		}

		// on renseigne le rapport
		if (rapport != null) {
			rapport.addProcessed(mutation.getId(), TypeEntiteRF.SERVITUDE, mutation.getTypeMutation());
		}
	}

	@NotNull
	private AyantDroitRF findAyantDroit(@NotNull String idRf) {
		final AyantDroitRF ayantDroit = ayantDroitRFDAO.find(new AyantDroitRFKey(idRf), FlushModeType.COMMIT);
		if (ayantDroit == null) {
			throw new IllegalArgumentException("L'ayant-droit idRF=[" + idRf + "] n'existe pas dans la DB.");
		}
		return ayantDroit;
	}

	@NotNull
	private ImmeubleRF findImmeuble(@NotNull String idRf) {
		final ImmeubleRF immeuble = immeubleRFDAO.find(new ImmeubleRFKey(idRf), FlushModeType.COMMIT);
		if (immeuble == null) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + idRf + "] n'existe pas dans la DB.");
		}
		if (immeuble.getDateRadiation() != null) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + idRf + "] est radié, il ne devrait plus changer.");
		}
		return immeuble;
	}

	/**
	 * Traite l'ajout d'une servitude.
	 */
	private void processCreation(@Nullable RegDate dateValeur, @NotNull ServitudeRF servitude) {

		// on sauve la nouvelle servitude
		servitude.setDateDebut(dateValeur);
		servitude = servitudeRFDAO.save(servitude);

		// on publie l'événement fiscal correspondant
		evenementFiscalService.publierOuvertureServitude(servitude.getDateDebutMetier(), servitude);

		// [SIFISC-24553] on met-à-jour à la main de la liste des servitudes pour pouvoir parcourir le graphe des dépendances dans le DatabaseChangeInterceptor
		final Set<ChargeServitudeRF> charges = servitude.getCharges();
		if (charges != null) {
			charges.forEach(charge -> charge.getImmeuble().addChargeServitude(charge));
		}
		final Set<BeneficeServitudeRF> benefices = servitude.getBenefices();
		if (benefices != null) {
			benefices.forEach(benefice -> benefice.getAyantDroit().addBeneficeServitude(benefice));
		}
	}

	/**
	 * Traite la modification d'une servitude sur un ayant-droit
	 */
	private void processModification(@NotNull RegDate dateValeur, @NotNull ServitudeRF servitude) {

		final ServitudeRF persisted = servitudeRFDAO.find(new DroitRFKey(servitude));
		if (persisted == null) {
			throw new IllegalArgumentException("La servitude idRF=[" + servitude.getMasterIdRF() + "] versionRF=[" + servitude.getVersionIdRF() + "] n'existe pas dans la DB.");
		}

		// on met-à-jour la servitude persistée
		persisted.setIdentifiantDroit(servitude.getIdentifiantDroit());
		persisted.setNumeroAffaire(servitude.getNumeroAffaire());
		persisted.setDateDebutMetier(servitude.getDateDebutMetier());
		persisted.setDateFinMetier(servitude.getDateFinMetier());
		persisted.setMotifDebut(servitude.getMotifDebut());
		persisted.setMotifFin(servitude.getMotifFin());

		// changement sur les ayants-droits
		{
			// on détermine les changements
			final List<BeneficeServitudeRF> toAddList = new LinkedList<>(servitude.getBenefices());
			final List<BeneficeServitudeRF> toRemoveList = persisted.getBenefices().stream()
					.filter(AnnulableHelper::nonAnnule)     // on ne tient compte que des bénéfices actifs
					.filter(bene -> bene.isValidAt(dateValeur))
					.collect(Collectors.toCollection(LinkedList::new));

			// on enlève des deux listes tous les ayants-droits identiques
			CollectionsUtils.removeCommonElements(toAddList, toRemoveList, (l, r) -> AyantDroitRFHelper.idRFEquals(l.getAyantDroit(), r.getAyantDroit()));

			// on applique les changements
			toAddList.forEach(lien -> {
				// on ajoute un lien valide à partir de la date valeur
				lien.setDateDebut(dateValeur);
				lien = AddAndSaveHelper.addAndSave(persisted, lien, servitudeRFDAO::save, new ServitudeBeneficeAccessor());
				lien.getAyantDroit().addBeneficeServitude(lien);
			});
			toRemoveList.forEach(lien -> {
				// on ferme le lien existant la veille de la date valeur
				lien.setDateFin(dateValeur.getOneDayBefore());
			});
		}

		// changement sur les immeubles
		{
			// on détermine les changements
			final List<ChargeServitudeRF> toAddList = new LinkedList<>(servitude.getCharges());
			final List<ChargeServitudeRF> toRemoveList = persisted.getCharges().stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(l -> l.isValidAt(dateValeur))
					.collect(Collectors.toCollection(LinkedList::new));

			// on enlève des deux listes tous les immeubles identiques
			CollectionsUtils.removeCommonElements(toAddList, toRemoveList, (l, r) -> ImmeubleRFHelper.idRFEquals(l.getImmeuble(), r.getImmeuble()));

			// on applique les changements
			toAddList.forEach(lien -> {
				// on ajoute un lien valide à partir de la date valeur
				lien.setDateDebut(dateValeur);
				lien = AddAndSaveHelper.addAndSave(persisted, lien, servitudeRFDAO::save, new ServitudeImmeubleAccessor());
				lien.getImmeuble().addChargeServitude(lien);
			});
			toRemoveList.forEach(lien -> {
				// on ferme le lien existant la veille de la date valeur
				lien.setDateFin(dateValeur.getOneDayBefore());
			});
		}

		// on publie l'événement fiscal correspondant
		evenementFiscalService.publierModificationServitude(dateValeur, persisted);
	}

	/**
	 * Traite la suppression (= fermeture) d'une servitude
	 */
	private void processSuppression(@NotNull RegDate dateValeur, @NotNull DroitRFKey servitudeKey) {
		final ServitudeRF persisted = servitudeRFDAO.find(servitudeKey);
		if (persisted == null) {
			throw new IllegalArgumentException("La servitude idRF=[" + servitudeKey.getMasterIdRF() + "] versionRF=[" + servitudeKey.getVersionIdRF() + "] n'existe pas dans la DB.");
		}

		// on ferme la servitude
		final RegDate dateFinTechnique = dateValeur.getOneDayBefore();
		persisted.setDateFin(dateFinTechnique);
		persisted.getCharges().stream()
				.filter(c -> c.getDateFin() == null || c.getDateFin().isAfter(dateFinTechnique))
				.forEach(c -> c.setDateFin(dateFinTechnique));
		persisted.getBenefices().stream()
				.filter(b -> b.getDateFin() == null || b.getDateFin().isAfter(dateFinTechnique))
				.forEach(b -> b.setDateFin(dateFinTechnique));

		if (persisted.getDateFinMetier() == null) {
			// on renseigne la date de fin métier à la même valeur que la date technique, car :
			// - il n'y a pas forcément une nouvelle servitude qui suivra et donc il n'est pas possible de déduire la date de fin métier
			//   de la date de début métier de la servitude suivante (comme pour les droits de propriété)
			// - il n'y a pas d'autre date disponible et il faut bien renseigner quelque chose.
			persisted.setDateFinMetier(dateFinTechnique);

			// on publie l'événement fiscal correspondant
			evenementFiscalService.publierFermetureServitude(persisted.getDateFinMetier(), persisted);
		}
	}

	private static class ServitudeBeneficeAccessor implements AddAndSaveHelper.EntityAccessor<ServitudeRF, BeneficeServitudeRF> {
		@Override
		public Collection<? extends HibernateEntity> getEntities(ServitudeRF container) {
			return container.getBenefices();
		}

		@Override
		public void addEntity(ServitudeRF container, BeneficeServitudeRF entity) {
			container.addBenefice(entity);
		}

		@Override
		public void assertEquals(BeneficeServitudeRF entity1, BeneficeServitudeRF entity2) {
			if (entity1.getDateDebut() != entity2.getDateDebut() || entity1.getDateFin() != entity2.getDateFin()) {
				throw new IllegalArgumentException();
			}
		}
	}

	private static class ServitudeImmeubleAccessor implements AddAndSaveHelper.EntityAccessor<ServitudeRF, ChargeServitudeRF> {
		@Override
		public Collection<? extends HibernateEntity> getEntities(ServitudeRF container) {
			return container.getCharges();
		}

		@Override
		public void addEntity(ServitudeRF container, ChargeServitudeRF entity) {
			container.addCharge(entity);
		}

		@Override
		public void assertEquals(ChargeServitudeRF entity1, ChargeServitudeRF entity2) {
			if (entity1.getDateDebut() != entity2.getDateDebut() || entity1.getDateFin() != entity2.getDateFin()) {
				throw new IllegalArgumentException();
			}
		}
	}
}
