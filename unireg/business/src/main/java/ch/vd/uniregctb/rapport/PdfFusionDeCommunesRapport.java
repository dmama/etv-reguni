package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.Set;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
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
			document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
					table.addLigne("Date de fusion:", RegDateHelper.dateToDisplayString(results.dateFusion));
					table.addLigne("Anciennes Communes:", displayCommunes(results.anciensNoOfs, results.dateFusion.getOneDayBefore(), infraService));
					table.addLigne("Commune résultante:", displayCommune(results.nouveauNoOfs, results.dateFusion, infraService));
				}
			});
		}

		// Résultats
		document.addEntete1("Résultats");
		{
			if (results.interrompu) {
				document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre total de tiers:", String.valueOf(results.nbTiersTotal));
					table.addLigne("Nombre de tiers traités:", String.valueOf(results.tiersTraites.size()));
					table.addLigne("Nombre de tiers ignorés:", String.valueOf(results.tiersIgnores.size()));
					table.addLigne("Nombre de tiers en erreur:", String.valueOf(results.tiersEnErrors.size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
				}
			});
		}

		// Habitants traités
		{
			String filename = "tiers_traites.csv";
			String contenu = ctbIdsAsCsvFile(results.tiersTraites, filename, status);
			String titre = "Liste des tiers traités";
			String listVide = "(aucun tiers traité)";
			document.addListeDetaillee(writer, results.tiersTraites.size(), titre, listVide, filename, contenu);
		}

		// Habitants en erreurs
		{
			String filename = "tiers_ignores.csv";
			String contenu = asCsvFile(results.tiersIgnores, filename, status);
			String titre = "Liste des tiers ignorés";
			String listVide = "(aucun tiers ignoré)";
			document.addListeDetaillee(writer, results.tiersIgnores.size(), titre, listVide, filename, contenu);
		}

		// Habitants en erreurs
		{
			String filename = "tiers_en_erreur.csv";
			String contenu = asCsvFile(results.tiersEnErrors, filename, status);
			String titre = "Liste des tiers en erreur";
			String listVide = "(aucun tiers en erreur)";
			document.addListeDetaillee(writer, results.tiersEnErrors.size(), titre, listVide, filename, contenu);
		}

		document.close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String displayCommune(int noOfs, RegDate dateReference, ServiceInfrastructureService infraService) {

		final StringBuilder s = new StringBuilder();

		Commune commune;
		try {
			commune = infraService.getCommuneByNumeroOfsEtendu(noOfs, dateReference);
		}
		catch (ServiceInfrastructureException e) {
			commune = null;
		}

		if (commune == null) {
			s.append("<unknown>");
		}
		else {
			s.append(commune.getNomMinuscule());
		}

		s.append(" (").append(noOfs).append(")");
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