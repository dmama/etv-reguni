package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.registrefoncier.dataimport.MutationsRFDetectorResults;

/**
 * Rapport PDF contenant les résultats du job de détection des mutations sur les immeubles du RF.
 */
@SuppressWarnings("Duplicates")
public class PdfMutationsRFDetectorRapport extends PdfRapport {

	public void write(final MutationsRFDetectorResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status, ApplicationContext applicationContext) throws Exception {

		final EvenementRFMutationDAO evenementRFMutationDAO = applicationContext.getBean("evenementRFMutationDAO", EvenementRFMutationDAO.class);
		if (evenementRFMutationDAO == null) {
			throw new IllegalArgumentException("Le bean 'evenementRFMutationDAO' n'existe pas.");
		}

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de détection des mutations du RF.");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Id de l'import :", String.valueOf(results.getImportId()));
				table.addLigne("Import initial :", String.valueOf(results.isImportInitial()));
				table.addLigne("Type :", String.valueOf(results.getType()));
				table.addLigne("Date de valeur :", RegDateHelper.dateToDisplayString(results.getDateValeur()));
				table.addLigne("Nombre de threads :", String.valueOf(results.getNbThreads()));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{.6f, .4f}, table -> {
				for (TypeEntiteRF typeEntity : TypeEntiteRF.values()) {
					final String entityName = getName(typeEntity);
					table.addLigne("Mutations sur les " + entityName + " :", "");
					for (TypeMutationRF typeMutation : TypeMutationRF.values()) {
						final String typeMutationName = getName(typeMutation);
						final long count = evenementRFMutationDAO.count(results.getImportId(), typeEntity, typeMutation);
						table.addLigne("  - " + typeMutationName + " :", String.valueOf(count));
					}
				}
				table.addLigne("Nombre d'avertissements :", String.valueOf(results.getAvertissements().size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getNbErreurs()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results.endTime - results.startTime));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// tous les fichiers CSV
		for (TypeEntiteRF typeEntity : TypeEntiteRF.values()) {
			final String entityName = getName(typeEntity);
			for (TypeMutationRF typeMutation : TypeMutationRF.values()) {
				final String typeMutationName = getName(typeMutation);
				final String filename = typeEntity.name().toLowerCase() + "_" + typeMutation.name().toLowerCase() + ".csv";
				final String titre = "Mutations de " + typeMutationName + " sur les " + entityName + " ";
				final String listVide = "(aucun)";
				final Iterator<String> iter = evenementRFMutationDAO.findRfIds(results.getImportId(), typeEntity, typeMutation);
				try (TemporaryFile contenu = idRFAsCsvFile(iter, filename, status)) {
					if (contenu != null) {
						addListeDetaillee(writer, titre, listVide, filename, contenu);
					}
				}
			}
		}

		// Avertissement
		{
			final String filename = "avertissements.csv";
			final String titre = " Liste des avertissements";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = avertissementsAsCsvFile(results.getAvertissements(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// Cas en erreur
		{
			final String filename = "erreurs.csv";
			final String titre = " Liste des cas en erreur";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = erreursAsCsvFile(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	@NotNull
	private static String getName(TypeMutationRF typeMutation) {
		final String typeMutationName;
		switch (typeMutation) {
		case CREATION:
			typeMutationName = "création";
			break;
		case MODIFICATION:
			typeMutationName = "modification";
			break;
		case SUPPRESSION:
			typeMutationName = "suppression";
			break;
		default:
			throw new IllegalArgumentException("Type de mutation inconnu = [" + typeMutation + "]");
		}
		return typeMutationName;
	}

	@NotNull
	private static String getName(TypeEntiteRF typeEntity) {
		switch (typeEntity) {
		case AYANT_DROIT:
			return "ayant-droits";
		case BATIMENT:
			return "bâtiments";
		case COMMUNE:
			return "communes";
		case DROIT:
			return "droits";
		case IMMEUBLE:
			return "immeubles";
		case SURFACE_AU_SOL:
			return "surfaces au sol";
		case SERVITUDE:
			return "servitudes";
		default:
			throw new IllegalArgumentException("Type d'entité inconnue = [" + typeEntity + "]");
		}
	}

	private TemporaryFile idRFAsCsvFile(Iterator<String> iter, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(iter, filename, status, new CsvHelper.FileFiller<String>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("ID_RF").append(COMMA);
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, String elt) {
				b.append(elt).append(COMMA);
				return true;
			}
		});
	}

	private TemporaryFile avertissementsAsCsvFile(List<MutationsRFDetectorResults.Avertissement> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<MutationsRFDetectorResults.Avertissement>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("ID_RF").append(COMMA);
					b.append("EGRID").append(COMMA);
					b.append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, MutationsRFDetectorResults.Avertissement elt) {
					b.append(elt.idRF).append(COMMA);
					b.append(elt.egrid).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.message));
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile erreursAsCsvFile(List<MutationsRFDetectorResults.Erreur> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<MutationsRFDetectorResults.Erreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("ID_RF").append(COMMA);
					b.append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, MutationsRFDetectorResults.Erreur elt) {
					b.append(elt.idRF).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.message));
					return true;
				}
			});
		}
		return contenu;
	}
}