package ch.vd.uniregctb.migration.pm;

import java.util.function.Supplier;

import org.hibernate.SessionFactory;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.migration.pm.adresse.StreetDataMigrator;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.utils.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.utils.IdMapper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.TiersDAO;

public class EtablissementMigrator extends AbstractEntityMigrator<RegpmEtablissement> {

	public EtablissementMigrator(SessionFactory uniregSessionFactory, StreetDataMigrator streetDataMigrator, TiersDAO tiersDAO) {
		super(uniregSessionFactory, streetDataMigrator, tiersDAO);
	}

	@Override
	protected void doMigrate(RegpmEtablissement regpm, MigrationResult mr, EntityLinkCollector linkCollector, IdMapper idMapper) {
		// TODO à un moment, il faudra quand-même se demander comment cela se passe avec RCEnt, non ?

		// TODO migrer l'établissement

		// on crée forcément un nouvel établissement
		final Etablissement unireg = saveEntityToDb(new Etablissement());
		idMapper.addEtablissement(regpm, unireg);

		// on crée le lien vers l'entreprise (les indépendants ne viendront pas de RegPM) TODO revoir les dates du lien...
		final Supplier<Entreprise> entreprise = getEntrepriseByRegpmIdSupplier(idMapper, regpm.getEntreprise().getId());
		final Supplier<Etablissement> moi = getEtablissementByRegpmIdSupplier(idMapper, regpm.getId());
		linkCollector.addLink(new EntityLinkCollector.EtablissementEntrepriseLink(moi, entreprise, regpm.getDateInscriptionRC(), regpm.getDateRadiationRC()));

		// TODO migrer l'enseigne, les coordonnées financières...
		// TODO comment fait-on pour que la création de ces établissements engendre des fors secondaires ?
	}

	@Nullable
	@Override
	protected String getMessagePrefix(RegpmEtablissement entity) {
		return String.format("Etablissement %d de l'entreprise %d", entity.getId(), entity.getEntreprise().getId());
	}
}
