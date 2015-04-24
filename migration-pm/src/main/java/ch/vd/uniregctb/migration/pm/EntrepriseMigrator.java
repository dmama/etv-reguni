package ch.vd.uniregctb.migration.pm;

import java.util.function.Supplier;

import org.hibernate.SessionFactory;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.migration.pm.adresse.StreetDataMigrator;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.utils.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.utils.IdMapper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public class EntrepriseMigrator extends AbstractEntityMigrator<RegpmEntreprise> {

	public EntrepriseMigrator(SessionFactory uniregSessionFactory, StreetDataMigrator streetDataMigrator, TiersDAO tiersDAO) {
		super(uniregSessionFactory, streetDataMigrator, tiersDAO);
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

		// un supplier qui va renvoyer l'entreprise en cours de migration
		final Supplier<Entreprise> moi = getEntrepriseByRegpmIdSupplier(idMapper, regpm.getId());

		// TODO ajouter un flag sur l'entreprise pour vérifier si elle est déjà migrée ou pas... (problématique de reprise sur incident pendant la migration)
		// TODO migrer les fors, les coordonnées financières, les bouclements, les adresses, les déclarations/documents...

		// migration des mandataires -> liens à créer par la suite
		regpm.getMandataires().forEach(mandat -> {
			final Supplier<? extends Tiers> mandataire;
			if (mandat.getMandataireEntreprise() != null) {
				mandataire = getEntrepriseByRegpmIdSupplier(idMapper, mandat.getMandataireEntreprise().getId());
			}
			else if (mandat.getMandataireEtablissement() != null) {
				mandataire = getEtablissementByRegpmIdSupplier(idMapper, mandat.getMandataireEtablissement().getId());
			}
			else if (mandat.getMandataireIndividu() != null) {
				mandataire = getIndividuByRegpmIdSupplier(idMapper, mandat.getMandataireIndividu().getId());
			}
			else {
				mr.addMessage(MigrationResult.CategorieListe.GENERIQUE, MigrationResult.NiveauMessage.WARN, "Le mandat " + mandat.getId() + " n'a pas de mandataire.");
				return;
			}

			// ajout du lien entre l'entreprise et son mandataire
			// TODO et les autres informations du mandat ?
			linkCollector.addLink(new EntityLinkCollector.MandantMandataireLink<>(moi, mandataire, mandat.getDateAttribution(), mandat.getDateResiliation()));
		});

		// migration des fusions (cette entreprise étant la source)
		regpm.getFusionsApres().forEach(apres -> {
			// TODO et les autres informations de la fusion (forme, date de contrat, date de bilan... ?)
			final Supplier<Entreprise> apresFusion = getEntrepriseByRegpmIdSupplier(idMapper, apres.getEntrepriseApres().getId());
			linkCollector.addLink(new EntityLinkCollector.FusionEntreprisesLink(moi, apresFusion, apres.getDateInscription(), null));
		});
	}

	@Nullable
	@Override
	protected String getMessagePrefix(RegpmEntreprise entity) {
		return String.format("Entreprise %d", entity.getId());
	}
}
