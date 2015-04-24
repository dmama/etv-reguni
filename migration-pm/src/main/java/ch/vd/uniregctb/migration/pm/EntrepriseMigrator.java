package ch.vd.uniregctb.migration.pm;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hibernate.SessionFactory;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.migration.pm.adresse.StreetDataMigrator;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCanton;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDemandeDelaiSommation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForSecondaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDossierFiscal;
import ch.vd.uniregctb.migration.pm.utils.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.utils.IdMapper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class EntrepriseMigrator extends AbstractEntityMigrator<RegpmEntreprise> {

	public EntrepriseMigrator(SessionFactory uniregSessionFactory, StreetDataMigrator streetDataMigrator, TiersDAO tiersDAO) {
		super(uniregSessionFactory, streetDataMigrator, tiersDAO);
	}

	@Nullable
	@Override
	protected String getMessagePrefix(RegpmEntreprise entity) {
		return String.format("Entreprise %d", entity.getId());
	}

	@Override
	protected void doMigrate(RegpmEntreprise regpm, MigrationResult mr, EntityLinkCollector linkCollector, IdMapper idMapper) {
		// TODO à un moment, il faudra quand-même se demander comment cela se passe avec RCEnt, non ?

		// TODO migrer l'entreprise (ou la retrouver déjà migrée en base)

		// Les entreprises conservent leur numéro comme numéro de contribuable
		Entreprise unireg = getEntityFromDb(Entreprise.class, regpm.getId());
		if (unireg == null) {
			mr.addMessage(MigrationResult.CategorieListe.PM_MIGREE, MigrationResult.NiveauMessage.WARN, "L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.");
			unireg = saveEntityToDb(new Entreprise(regpm.getId()));
		}
		idMapper.addEntreprise(regpm, unireg);

		// TODO ajouter un flag sur l'entreprise pour vérifier si elle est déjà migrée ou pas... (problématique de reprise sur incident pendant la migration)
		// TODO migrer les coordonnées financières, les bouclements, les adresses, les déclarations/documents...

		migrateDeclarations(regpm, unireg, mr);
		migrateForsPrincipaux(regpm, unireg, mr);
		migrateForsSecondaires(regpm, unireg, mr);

		migrateMandataires(regpm, mr, linkCollector, idMapper);
		migrateFusionsApres(regpm, linkCollector, idMapper);
	}

	/**
	 * Le côté polymorphique d'une relation est en général exprimé dans RegPM par plusieurs liens distincts, dont un seul est non-vide. Dans Unireg,
	 * il n'y a en général qu'un seul lien vers une entité à caractère polymorphique... Cette méthode permet donc de transcrire une façon de faire
	 * dans l'autre.
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 * @param entrepriseSupplier accès au lien vers une entreprise
	 * @param etablissementSupplier accès au lien vers un établissement
	 * @param individuSupplier accès au lien vers un individu
	 * @return un accès vers l'entité Unireg correspondant à l'entité (entreprise, établissement ou individu) de RegPM liée&nbsp;; cet accès n'est pas résolvable immédiatement
	 * mais est plutôt destiné à être résolu une fois toutes les entités d'un graphe migrées&nbsp;; la valeur retournée est nulle dans le cas où aucun des accès en entrée n'a
	 * fourni une entité.
	 */
	@Nullable
	private Supplier<? extends Tiers> getPolymorphicSupplier(IdMapper idMapper,
	                                                         @Nullable Supplier<RegpmEntreprise> entrepriseSupplier,
	                                                         @Nullable Supplier<RegpmEtablissement> etablissementSupplier,
	                                                         @Nullable Supplier<RegpmIndividu> individuSupplier) {

		final RegpmEntreprise entreprise = entrepriseSupplier != null ? entrepriseSupplier.get() : null;
		if (entreprise != null) {
			return getEntrepriseByRegpmIdSupplier(idMapper, entreprise.getId());
		}

		final RegpmEtablissement etablissement = etablissementSupplier != null ? etablissementSupplier.get() : null;
		if (etablissement != null) {
			return getEtablissementByRegpmIdSupplier(idMapper, etablissement.getId());
		}

		final RegpmIndividu individu = individuSupplier != null ? individuSupplier.get() : null;
		if (individu != null) {
			return getIndividuByRegpmIdSupplier(idMapper, individu.getId());
		}

		return null;
	}

	/**
	 * Migration des liens de fusions (ceux qui clôturent l'entreprise en question, les autres étant traités au moment de la migration des entreprises précédentes)
	 * @param regpm l'entreprise qui va disparaître dans la fusion
	 * @param linkCollector collecteur de liens à créer
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 */
	private void migrateFusionsApres(RegpmEntreprise regpm, EntityLinkCollector linkCollector, IdMapper idMapper) {
		// un supplier qui va renvoyer l'entreprise en cours de migration
		final Supplier<Entreprise> moi = getEntrepriseByRegpmIdSupplier(idMapper, regpm.getId());

		// migration des fusions (cette entreprise étant la source)
		regpm.getFusionsApres().forEach(apres -> {
			// TODO et les autres informations de la fusion (forme, date de contrat, date de bilan... ?)
			final Supplier<Entreprise> apresFusion = getEntrepriseByRegpmIdSupplier(idMapper, apres.getEntrepriseApres().getId());
			linkCollector.addLink(new EntityLinkCollector.FusionEntreprisesLink(moi, apresFusion, apres.getDateInscription(), null));
		});
	}

	/**
	 * Migration des mandataires d'une entreprise
	 * @param regpm entreprise à migrer
	 * @param mr collecteur de messages, de données à logguer...
	 * @param linkCollector collecteur de liens à créer
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 */
	private void migrateMandataires(RegpmEntreprise regpm, MigrationResult mr, EntityLinkCollector linkCollector, IdMapper idMapper) {
		// un supplier qui va renvoyer l'entreprise en cours de migration
		final Supplier<Entreprise> moi = getEntrepriseByRegpmIdSupplier(idMapper, regpm.getId());

		// migration des mandataires -> liens à créer par la suite
		regpm.getMandataires().forEach(mandat -> {

			// récupération du mandataire qui peut être une autre entreprise, un établissement ou un individu
			final Supplier<? extends Tiers> mandataire = getPolymorphicSupplier(idMapper, mandat::getMandataireEntreprise, mandat::getMandataireEtablissement, mandat::getMandataireIndividu);
			if (mandataire == null) {
				mr.addMessage(MigrationResult.CategorieListe.GENERIQUE, MigrationResult.NiveauMessage.WARN, "Le mandat " + mandat.getId() + " n'a pas de mandataire.");
				return;
			}

			// ajout du lien entre l'entreprise et son mandataire
			// TODO et les autres informations du mandat ?
			linkCollector.addLink(new EntityLinkCollector.MandantMandataireLink<>(moi, mandataire, mandat.getDateAttribution(), mandat.getDateResiliation()));
		});
	}

	/**
	 * Migration des déclarations d'impôts, de leurs états, délais...
	 */
	private void migrateDeclarations(RegpmEntreprise regpm, Entreprise unireg, MigrationResult mr) {

		// boucle sur chacune des déclarations
		regpm.getDossiersFiscaux().forEach(dossier -> {

			final PeriodeFiscale pf = getEntityFromDb(PeriodeFiscale.class, dossier.getPf());

			DeclarationImpotOrdinaire di = new DeclarationImpotOrdinaire();
			di.setTiers(unireg);
			di = saveEntityToDb(di);
			unireg.getDeclarations().add(di);

			di.setDateDebut(dossier.getAssujettissement().getDateDebut());
			di.setDateFin(dossier.getAssujettissement().getDateFin());
			di.setDelais(migrateDelaisDeclaration(dossier, di));
			di.setEtats(migrateEtatsDeclaration(dossier, di));
			di.setNumero(dossier.getNoParAnnee());
			di.setPeriode(pf);

			if (dossier.getEtat() == RegpmTypeEtatDossierFiscal.ANNULE) {
				di.setAnnulationUser(Optional.ofNullable(dossier.getLastMutationOperator()).orElse(AuthenticationHelper.getCurrentPrincipal()));
				di.setAnnulationDate(Optional.ofNullable((Date) dossier.getLastMutationTimestamp()).orElseGet(DateHelper::getCurrentDate));
			}

			// TODO dates/visas de création/modification
		});
	}

	/**
	 * Génération des délais de dépôt
	 */
	private Set<DelaiDeclaration> migrateDelaisDeclaration(RegpmDossierFiscal dossier, Declaration di) {

		final Set<DelaiDeclaration> delais = new LinkedHashSet<>();

		// délai initial
		if (dossier.getDelaiRetour() != null) {
			final DelaiDeclaration delai = new DelaiDeclaration();
			delai.setConfirmationEcrite(false);
			delai.setDateDemande(dossier.getDateEnvoi());           // TODO le délai initial est "demandé" à la date d'envoi, non ?
			delai.setDateTraitement(dossier.getDateEnvoi());
			delai.setDeclaration(di);
			delai.setDelaiAccordeAu(dossier.getDelaiRetour());
			delais.add(delai);
		}

		// fonction de conversion
		final Function<RegpmDemandeDelaiSommation, DelaiDeclaration> mapper = regpm -> {
			final DelaiDeclaration delai = new DelaiDeclaration();
			delai.setConfirmationEcrite(regpm.isImpressionLettre());        // TODO cela suppose une clé d'archivage, non ?
			delai.setDateDemande(regpm.getDateDemande());
			delai.setDateTraitement(regpm.getDateReception());              // TODO on est sûr ce de mapping ?
			delai.setDeclaration(di);
			delai.setDelaiAccordeAu(regpm.getDelaiAccorde());
			return delai;
		};

		// TODO que fait-on avec les demandes de délai en cours d'analyse (etat = DEMANDEE) ?
		// TODO que fait-on avec les demandes de délai refusées (etat = REFUSEE) ?
		// TODO que fait-on avec les demandes de délai après sommation (type = APRES_SOMMATION) ?

		// demandes ultérieures
		dossier.getDemandesDelai().stream()
				.filter(demande -> demande.getType() == RegpmTypeDemandeDelai.AVANT_SOMMATION)
				.filter(demande -> demande.getEtat() == RegpmTypeEtatDemandeDelai.ACCORDEE)
				.map(mapper)
				.forEach(delais::add);
		return delais;
	}

	/**
	 * Génération des états d'une déclaration
	 */
	private Set<EtatDeclaration> migrateEtatsDeclaration(RegpmDossierFiscal dossier, Declaration di) {

		final Set<EtatDeclaration> etats = new LinkedHashSet<>();

		// envoi
		if (dossier.getDateEnvoi() != null) {
			etats.add(new EtatDeclarationEmise(dossier.getDateEnvoi()));
		}

		// sommation
		if (dossier.getDateEnvoiSommation() != null) {
			etats.add(new EtatDeclarationSommee(dossier.getDateEnvoiSommation(), dossier.getDateEnvoiSommation()));
		}

		// retour
		if (dossier.getDateRetour() != null) {
			etats.add(new EtatDeclarationRetournee(dossier.getDateRetour(), "CEDI"));       // TODO source = CEDI ?
		}

		// TODO la taxation d'office (= échéance, au sens Unireg) existait-elle ?

		return etats;
	}

	private void migrateForsPrincipaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResult mr) {

		// TODO sont-ce vraiment des fors de classe ForFiscalPrincipal qu'il faut instancier ? (problème avec le mode d'imposition qui n'a rien à faire là pour les PM...)

		final Function<RegpmForPrincipal, Optional<ForFiscalPrincipal>> mapper = f -> {
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal();
			ffp.setDateDebut(f.getDateValidite());
			ffp.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
			ffp.setMotifRattachement(MotifRattachement.DOMICILE);
			if (f.getCommune() != null) {
				final RegpmCommune commune = f.getCommune();
				ffp.setNumeroOfsAutoriteFiscale(commune.getNoOfs());
				ffp.setTypeAutoriteFiscale(commune.getCanton() == RegpmCanton.VD ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
			}
			else if (f.getOfsPays() != null) {
				if (f.getOfsPays() == ServiceInfrastructureService.noOfsSuisse) {
					mr.addMessage(MigrationResult.CategorieListe.FORS, MigrationResult.NiveauMessage.ERROR, String.format("For principal %s sans commune mais sur Suisse", f.getId()));
					return Optional.empty();
				}
				ffp.setNumeroOfsAutoriteFiscale(f.getOfsPays());
				ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			}
			else {
				mr.addMessage(MigrationResult.CategorieListe.FORS, MigrationResult.NiveauMessage.ERROR, String.format("For principal %s sans autorité fiscale", f.getId()));
				return Optional.empty();
			}
			ffp.setTiers(unireg);
			return Optional.of(ffp);
		};

		final List<ForFiscalPrincipal> liste = regpm.getForsPrincipaux().stream()
				.map(mapper)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.sorted(Comparator.comparing(ForFiscalPrincipal::getDateDebut))
				.collect(Collectors.toList());

		// assignation des dates de fin
		RegDate dateFinCourante = regpm.getDateFinFiscale();
		for (ForFiscalPrincipal ffp : CollectionsUtils.revertedOrder(liste)) {
			ffp.setDateFin(dateFinCourante);
			dateFinCourante = ffp.getDateDebut().getOneDayBefore();
		}

		// assignation des motifs
		final MovingWindow<ForFiscalPrincipal> wnd = new MovingWindow<>(liste);
		while (wnd.hasNext()) {
			final MovingWindow.Snapshot<ForFiscalPrincipal> snap = wnd.next();
			final ForFiscalPrincipal current = snap.getCurrent();
			final ForFiscalPrincipal previous = snap.getPrevious();
			final ForFiscalPrincipal next = snap.getNext();

			// le tout premier for a un motif d'ouverture indéterminé
			if (previous == null) {
				current.setMotifOuverture(MotifFor.INDETERMINE);
			}

			// le tout dernier for a un motif de fermeture indéterminé si la date de fermeture est non-nulle
			if (next == null && current.getDateFin() != null) {
				current.setMotifFermeture(MotifFor.INDETERMINE);
			}

			// comparaison des types d'autorité fiscales pour les mutations
			if (next != null) {
				final TypeAutoriteFiscale currentTAF = current.getTypeAutoriteFiscale();
				final TypeAutoriteFiscale nextTAF = next.getTypeAutoriteFiscale();
				final MotifFor motif;
				if (currentTAF == nextTAF) {
					// TODO il y a sans doute d'autres possibilités, comme une fusion de communes...
					motif = MotifFor.DEMENAGEMENT_VD;
				}
				else if (nextTAF == TypeAutoriteFiscale.PAYS_HS) {
					motif = MotifFor.DEPART_HS;
				}
				else if (currentTAF == TypeAutoriteFiscale.PAYS_HS) {
					motif = MotifFor.ARRIVEE_HS;
				}
				else if (nextTAF == TypeAutoriteFiscale.COMMUNE_HC) {
					motif = MotifFor.DEPART_HC;
				}
				else {
					motif = MotifFor.ARRIVEE_HC;
				}

				current.setMotifFermeture(motif);
				next.setMotifOuverture(motif);
			}
		}

		// on les ajoute au tiers
		liste.forEach(unireg::addForFiscal);
	}

	private void migrateForsSecondaires(RegpmEntreprise regpm, Entreprise unireg, MigrationResult mr) {

		final Function<RegpmForSecondaire, Optional<ForFiscalSecondaire>> mapper = f -> {
			final ForFiscalSecondaire ffs = new ForFiscalSecondaire();
			ffs.setDateDebut(f.getDateDebut());
			ffs.setDateFin(f.getDateFin());
			ffs.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
			ffs.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
			ffs.setMotifFermeture(f.getDateFin() != null ? MotifFor.FIN_EXPLOITATION : null);
			ffs.setMotifRattachement(MotifRattachement.ETABLISSEMENT_STABLE);

			final RegpmCommune commune = f.getCommune();
			if (commune != null) {
				ffs.setNumeroOfsAutoriteFiscale(commune.getNoOfs());
				ffs.setTypeAutoriteFiscale(commune.getCanton() == RegpmCanton.VD ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
			}
			else {
				mr.addMessage(MigrationResult.CategorieListe.FORS, MigrationResult.NiveauMessage.ERROR, String.format("For secondaire %d sans autorité fiscale", f.getId()));
				return Optional.empty();
			}

			if (ffs.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				mr.addMessage(MigrationResult.CategorieListe.FORS, MigrationResult.NiveauMessage.WARN, String.format("For secondaire %d hors Vaud", f.getId()));
			}

			ffs.setTiers(unireg);
			return Optional.of(ffs);
		};

		// TODO comment différencier les fors fiscaux "immeuble" des fors fiscaux "établissement stable" ?

		// construction de la liste des fors secondaires
		regpm.getForsSecondaires().stream()
				.map(mapper)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.forEach(unireg::addForFiscal);
	}
}
