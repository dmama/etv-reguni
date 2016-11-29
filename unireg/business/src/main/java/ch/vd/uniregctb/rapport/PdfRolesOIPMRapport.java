package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.AutoCloseableContainer;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.role.before2016.InfoContribuable;
import ch.vd.uniregctb.role.before2016.InfoContribuablePM;
import ch.vd.uniregctb.role.before2016.ProduireRolesOIPMResults;

/**
 * Rapport PDF contenant les résultats de l'exécution du job de production des rôles pour l'OIPM
 */
public class PdfRolesOIPMRapport extends PdfRolesRapport<ProduireRolesOIPMResults> {

	public PdfRolesOIPMRapport(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	public void write(final ProduireRolesOIPMResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		status.setMessage("Génération du rapport...");

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);

		addEnteteUnireg();
		addTitrePrincipal("Rapport des rôles pour l'OIPM");

		// Paramètres
		addEntete1("Paramètres");
		{
		    addTableSimple(2, table -> {
		        table.addLigne("Année fiscale :", String.valueOf(results.annee));
		        table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
		        table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
		    });
		}

		// Résultats
		addEntete1("Résumé");
		{
			if (status.interrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
		    }

			addTableSimple(2, table -> {
				table.addLigne("Nombre de communes traitées:", String.valueOf(results.getNoOfsCommunesTraitees().size()));
				table.addLigne("Nombre de contribuables traités:", String.valueOf(results.ctbsTraites));
				table.addLigne("Nombre de contribuables ignorés:", String.valueOf(results.ctbsIgnores.size()));
				table.addLigne("Nombre de contribuables en erreur:", String.valueOf(results.ctbsEnErrors.size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			});
		}

		// Détails des contribuables en erreur ou ignorés
		if (!results.ctbsEnErrors.isEmpty()) {
			final String filename = "contribuables_en_erreur.csv";
			final String titre = "Liste des contribuables en erreur";
			final String listVide = "(aucun contribuable en erreur)";
			try (TemporaryFile contenu = asCsvFile(results.ctbsEnErrors, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		if (!results.ctbsIgnores.isEmpty()) {
			final String filename = "contribuables_ignores.csv";
			final String titre = "Liste des contribuables ignorés";
			final String listVide = "(aucun contribuable ignoré)";
			try (TemporaryFile contenu = asCsvFile(results.ctbsIgnores, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// données aggrégées
		final List<InfoContribuablePM> full = results.buildInfoPourRegroupementCommunes(results.getNoOfsCommunesTraitees());

		// Résumé des types de contribuables
		addEntete1("Résumé des types de contribuables trouvés");
		{
			final Map<InfoContribuable.TypeContribuable, Integer> nombreParType = extractNombreParType(full);
			addTableSimple(new float[]{2.0f, 1.0f}, table -> {
				addLignesStatsParTypeCtb(table, nombreParType);
			});
		}

		final List<Commune> communes = getListeCommunes(results.getNoOfsCommunesTraitees(), results.annee, false);
		final Map<Integer, String> nomsCommunes = buildNomsCommunes(communes);

		// Fichier CVS détaillé
		{
			try (AutoCloseableContainer<TemporaryFile> container = new AutoCloseableContainer<>(asCsvFiles(nomsCommunes, full, status))) {
				writeFichierDetail(results, writer, container.getElements(), full.isEmpty(), "OIPM");
			}
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile[] asCsvFiles(Map<Integer, String> nomsCommunes, List<InfoContribuablePM> infos, StatusManager status) {
		status.setMessage("Génération du rapport");
		return traiteListeContribuablesPM(infos, nomsCommunes, InfoContribuable::getNoOfsDerniereCommune);
	}
}