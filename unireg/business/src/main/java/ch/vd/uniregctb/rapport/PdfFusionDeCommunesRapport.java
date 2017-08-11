package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.FusionDeCommunesResults;

/**
 * Rapport PDF contenant les résultats de l'exécution d'un job d'ouverture des fors des habitants majeurs
 */
public class PdfFusionDeCommunesRapport extends PdfRapport {

	private final ServiceInfrastructureService infraService;

	public PdfFusionDeCommunesRapport(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void write(final FusionDeCommunesResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		PdfMajoriteRapport document = new PdfMajoriteRapport();
		PdfWriter writer = PdfWriter.getInstance(document, os);
		document.open();
		document.addMetaInfo(nom, description);
		document.addEnteteUnireg();

		// Titre
		document.addTitrePrincipal("Rapport d'exécution du job de fusion de communes");

		// Paramètres
		document.addEntete1("Paramètres");
		{
			document.addTableSimple(2, table -> {
				table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
				table.addLigne("Date de fusion:", RegDateHelper.dateToDisplayString(results.dateFusion));
				table.addLigne("Anciennes Communes:", displayCommunes(results.anciensNoOfs, results.dateFusion.getOneDayBefore(), infraService));
				table.addLigne("Commune résultante:", displayCommune(results.nouveauNoOfs, results.dateFusion, infraService));
			});
		}

		// Résultats
		document.addEntete1("Résultats");
		{
			if (results.interrompu) {
				document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			document.addTableSimple(new float[]{70, 30}, table -> {
				table.addLigne("Nombre total de tiers examinés:", String.valueOf(results.nbTiersExamines));
				table.addLigne("Nombre de tiers en erreur:", String.valueOf(results.tiersEnErreur.size()));
				table.addLigne("Nombre de tiers traités pour un for:", String.valueOf(results.tiersTraitesPourFors.size()));
				table.addLigne("Nombre de tiers ignorés pour les fors:", String.valueOf(results.tiersIgnoresPourFors.size()));
				table.addLigne("Nombre de tiers traités pour une décision ACI:", String.valueOf(results.tiersTraitesPourDecisions.size()));
				table.addLigne("Nombre de tiers ignorés pour les décisions ACI:", String.valueOf(results.tiersIgnoresPourDecisions.size()));
				table.addLigne("Nombre de tiers traités pour un domicile d'établissement:", String.valueOf(results.tiersTraitesPourDomicilesEtablissement.size()));
				table.addLigne("Nombre de tiers ignorés pour les domiciles d'établissement:", String.valueOf(results.tiersIgnoresPourDomicilesEtablissement.size()));
				table.addLigne("Nombre de tiers traités pour un allègement fiscal:", String.valueOf(results.tiersTraitesPourAllegementsFiscaux.size()));
				table.addLigne("Nombre de tiers ignorés pour les allègements fiscaux:", String.valueOf(results.tiersIgnoresPourAllegementsFiscaux.size()));

				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			});
		}

		// Tiers en erreur
		{
			final String filename = "tiers_en_erreur.csv";
			final String titre = "Liste des tiers en erreur";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = asCsvFileErreurs(results.tiersEnErreur, filename, status)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Tiers traités pour leurs fors
		{
			final String filename = "tiers_traites_fors.csv";
			final String titre = "Liste des tiers traités pour leurs fors";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = ctbIdsAsCsvFile(results.tiersTraitesPourFors, filename, status)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Tiers ignorés dans leurs fors (déjà au bon endroit...)
		{
			final String filename = "tiers_ignores_fors.csv";
			final String titre = "Liste des tiers ignorés dans leurs fors";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = asCsvFileIgnores(results.tiersIgnoresPourFors, filename, status)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Tiers traités pour leurs décisions ACI
		{
			final String filename = "tiers_traites_decisions.csv";
			final String titre = "Liste des tiers traités pour leurs décisions ACI";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = ctbIdsAsCsvFile(results.tiersTraitesPourDecisions, filename, status)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Tiers ignorés dans leurs décisions ACI (déjà au bon endroit...)
		{
			final String filename = "tiers_ignores_decisions.csv";
			final String titre = "Liste des tiers ignorés dans leurs décisions ACI";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = asCsvFileIgnores(results.tiersIgnoresPourDecisions, filename, status)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Etablissements traités pour leurs domiciles
		{
			final String filename = "etablissements_traites_domiciles.csv";
			final String titre = "Liste des établissements traités pour leurs domiciles";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = ctbIdsAsCsvFile(results.tiersTraitesPourDomicilesEtablissement, filename, status)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Etablissements ignorés dans leurs domiciles (déjà au bon endroit...)
		{
			final String filename = "etablissements_ignores_domiciles.csv";
			final String titre = "Liste des établissements ignorés dans leurs domiciles";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = asCsvFileIgnores(results.tiersIgnoresPourDomicilesEtablissement, filename, status)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Entreprises traitées pour leurs allègements fiscaux
		{
			final String filename = "entreprises_traitees_allegements.csv";
			final String titre = "Liste des entreprises traitées pour leurs allègements fiscaux";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = ctbIdsAsCsvFile(results.tiersTraitesPourAllegementsFiscaux, filename, status)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Entreprises ignorées dans leurs allègements fiscaux (déjà au bon endroit...)
		{
			final String filename = "entreprises_ignorees_allegements.csv";
			final String titre = "Liste des entreprises ignorées dans leurs allègements fiscaux";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = asCsvFileIgnores(results.tiersIgnoresPourAllegementsFiscaux, filename, status)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		document.close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile asCsvFileErreurs(Collection<FusionDeCommunesResults.Erreur> erreurs, String filename, StatusManager statusManager) {
		return CsvHelper.asCsvTemporaryFile(erreurs, filename, statusManager, new CsvHelper.FileFiller<FusionDeCommunesResults.Erreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_TIERS").append(COMMA).append("DESCRIPTION").append(COMMA).append("DETAILS");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, FusionDeCommunesResults.Erreur elt) {
				b.append(elt.noTiers).append(COMMA);
				b.append(escapeChars(elt.getDescriptionRaison())).append(COMMA);
				b.append(escapeChars(elt.details));
				return true;
			}
		});
	}

	private TemporaryFile asCsvFileIgnores(Collection<FusionDeCommunesResults.Ignore> ignores, String filename, StatusManager statusManager) {
		return CsvHelper.asCsvTemporaryFile(ignores, filename, statusManager, new CsvHelper.FileFiller<FusionDeCommunesResults.Ignore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_TIERS").append(COMMA).append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, FusionDeCommunesResults.Ignore elt) {
				b.append(elt.noTiers).append(COMMA);
				b.append(escapeChars(elt.getDescriptionRaison()));
				return true;
			}
		});
	}

	private String displayCommune(int noOfs, RegDate dateReference, ServiceInfrastructureService infraService) {

		final StringBuilder s = new StringBuilder();

		Commune commune;
		try {
			commune = infraService.getCommuneByNumeroOfs(noOfs, dateReference);
		}
		catch (ServiceInfrastructureException e) {
			commune = null;
		}

		if (commune == null) {
			s.append("<unknown>");
		}
		else {
			s.append(commune.getNomOfficiel());
		}

		s.append(" (").append(noOfs).append(')');
		return s.toString();
	}

	private String displayCommunes(Set<Integer> noOfs, RegDate dateReference, ServiceInfrastructureService infraService) {

		StringBuilder s = new StringBuilder();
		for (Integer no : noOfs) {
			s.append(displayCommune(no, dateReference, infraService)).append(", ");
		}

		final String string = s.toString();
		return string.substring(0, string.length() - 2); // supprime le dernier ", "
	}
}