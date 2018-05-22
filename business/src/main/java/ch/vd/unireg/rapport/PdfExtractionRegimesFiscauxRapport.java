package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.regimefiscal.extraction.ExtractionRegimesFiscauxResults;
import ch.vd.unireg.tiers.RegimeFiscal;

/**
 * Générateur du rapport PDF d'exécution du batch d'extraction des régimes fiscaux utilisés
 */
public class PdfExtractionRegimesFiscauxRapport extends PdfRapport {

	public void write(final ExtractionRegimesFiscauxResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {
		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'extraction des régimes fiscaux utilisés");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Avec historique :", String.valueOf(results.avecHistorique));
				table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
				table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (status.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{.6f, .4f}, table -> {
				table.addLigne("Nombre d'entreprises sans régime fiscal VD :", String.valueOf(results.getSansRegimeFiscal().stream().filter(srf -> srf.portee == RegimeFiscal.Portee.VD).count()));
				table.addLigne("Nombre d'entreprises sans régime fiscal CH :", String.valueOf(results.getSansRegimeFiscal().stream().filter(srf -> srf.portee == RegimeFiscal.Portee.CH).count()));
				table.addLigne("Entreprises en erreur :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Répartition par code de régime fiscal
		addEntete1("Répartition des régimes trouvés");
		{
			addTableSimple(new float[]{.7f, .15f, .15f}, table -> {
				table.addLigne(StringUtils.EMPTY, RegimeFiscal.Portee.VD.name(), RegimeFiscal.Portee.CH.name());

				final Map<String, String> libelles = new TreeMap<>();
				final Map<RegimeFiscal.Portee, Map<String, MutableLong>> counts = new EnumMap<>(RegimeFiscal.Portee.class);
				for (ExtractionRegimesFiscauxResults.PlageRegimeFiscal plage : results.getPlagesRegimeFiscal()) {
					libelles.put(plage.code, plage.libelle);
					final Map<String, MutableLong> map = counts.computeIfAbsent(plage.portee, key -> new HashMap<>());
					map.computeIfAbsent(plage.code, key -> new MutableLong(0L)).increment();
				}

				for (Map.Entry<String, String> entry : libelles.entrySet()) {
					final String code = entry.getKey();
					final String libelle = entry.getValue();
					final long vd = Optional.of(counts.getOrDefault(RegimeFiscal.Portee.VD, Collections.emptyMap()))
							.map(map -> map.get(code))
							.map(MutableLong::longValue)
							.orElse(0L);
					final long ch = Optional.of(counts.getOrDefault(RegimeFiscal.Portee.CH, Collections.emptyMap()))
							.map(map -> map.get(code))
							.map(MutableLong::longValue)
							.orElse(0L);
					table.addLigne(String.format("%s - %s", code, libelle), Long.toString(vd), Long.toString(ch));
				}
			});
		}

		// Régimes manquants
		{
			final String filename = "sans_regime.csv";
			final String titre = "Liste des entreprises sans régime VD ou CH";
			final String listeVide = "(aucune)";
			try (TemporaryFile contenu = genererSansRegime(results.getSansRegimeFiscal(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = genererListeErreurs(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Régimes fiscaux extraits
		{
			final String filename = "regimes_fiscaux.csv";
			final String titre = " Liste des régimes fiscaux";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = genererListeRegimes(results.getPlagesRegimeFiscal(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile genererListeErreurs(List<ExtractionRegimesFiscauxResults.Erreur> erreurs, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(erreurs, filename, status, new CsvHelper.FileFiller<ExtractionRegimesFiscauxResults.Erreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("ERREUR");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ExtractionRegimesFiscauxResults.Erreur erreur) {
				b.append(erreur.idEntreprise).append(COMMA);
				b.append(escapeChars(erreur.message));
				return true;
			}
		});
	}

	@NotNull
	private static String toString(@Nullable FormeLegale formeLegale) {
		return Optional.ofNullable(formeLegale)
				.map(fl -> String.format("%s - %s", fl.getCode(), fl.getLibelle()))
				.orElse(StringUtils.EMPTY);
	}

	private TemporaryFile genererSansRegime(List<ExtractionRegimesFiscauxResults.SansRegimeFiscal> liste, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<ExtractionRegimesFiscauxResults.SansRegimeFiscal>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("NO_IDE").append(COMMA);
				b.append("RAISON_SOCIALE").append(COMMA);
				b.append("FORME_JURIDIQUE").append(COMMA);
				b.append("PORTEE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ExtractionRegimesFiscauxResults.SansRegimeFiscal elt) {
				b.append(elt.idEntreprise).append(COMMA);
				b.append(FormatNumeroHelper.formatNumIDE(elt.ide)).append(COMMA);
				b.append(CsvHelper.escapeChars(elt.raisonSociale)).append(COMMA);
				b.append(PdfExtractionRegimesFiscauxRapport.toString(elt.formeLegale)).append(COMMA);
				b.append(elt.portee);
				return true;
			}
		});
	}

	private TemporaryFile genererListeRegimes(List<ExtractionRegimesFiscauxResults.PlageRegimeFiscal> liste, String fileName, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(liste, fileName, status, new CsvHelper.FileFiller<ExtractionRegimesFiscauxResults.PlageRegimeFiscal>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("NO_IDE").append(COMMA);
				b.append("RAISON_SOCIALE").append(COMMA);
				b.append("FORME_JURIDIQUE").append(COMMA);
				b.append("PORTEE").append(COMMA);
				b.append("DATE_DEBUT").append(COMMA);
				b.append("DATE_FIN").append(COMMA);
				b.append("CODE_REGIME").append(COMMA);
				b.append("LIBELLE_REGIME");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ExtractionRegimesFiscauxResults.PlageRegimeFiscal elt) {
				b.append(elt.idEntreprise).append(COMMA);
				b.append(FormatNumeroHelper.formatNumIDE(elt.ide)).append(COMMA);
				b.append(CsvHelper.escapeChars(elt.raisonSociale)).append(COMMA);
				b.append(PdfExtractionRegimesFiscauxRapport.toString(elt.formeLegale)).append(COMMA);
				b.append(elt.portee).append(COMMA);
				b.append(elt.dateDebut).append(COMMA);
				b.append(elt.dateFin).append(COMMA);
				b.append(elt.code).append(COMMA);
				b.append(escapeChars(elt.libelle));
				return true;
			}
		});
	}
}
