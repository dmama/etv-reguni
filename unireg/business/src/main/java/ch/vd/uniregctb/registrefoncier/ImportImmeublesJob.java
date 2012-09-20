package ch.vd.uniregctb.registrefoncier;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.ImportImmeublesRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamFile;
import ch.vd.uniregctb.scheduler.JobParamString;

public class ImportImmeublesJob extends JobDefinition {

	private static final String NAME = "ImportImmeublesJob";
	private static final String CATEGORIE = "RF";

	private static final String FILE = "File";
	private static final String ENCODING = "Encoding";
	private static final String DEFAULT_ENCODING = "ISO-8859-15";

	private RegistreFoncierService registreFoncierService;
	private RapportService rapportService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public ImportImmeublesJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param1 = new JobParam();
		param1.setDescription("Fichier d'import (*.zip)");
		param1.setName(FILE);
		param1.setMandatory(true);
		param1.setType(new JobParamFile());
		addParameterDefinition(param1, null);

		final JobParam param2 = new JobParam();
		param2.setDescription("Encoding du fichier");
		param2.setName(ENCODING);
		param2.setMandatory(false);
		param2.setType(new JobParamString());
		addParameterDefinition(param2, DEFAULT_ENCODING);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final StatusManager status = getStatusManager();
		final byte[] zippedContent = getFileContent(params, FILE);
		final String encoding = getEncoding(params);

		final ImportImmeublesResults results;

		final ZipInputStream zipstream = new ZipInputStream(new ByteArrayInputStream(zippedContent));
		try {
			zipstream.getNextEntry();
			results = registreFoncierService.importImmeubles(zipstream, encoding, status);
		}
		finally {
			zipstream.close();
		}

		final ImportImmeublesRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("L'import des immeubles du registre foncier est terminée.", rapport);
	}

	private String getEncoding(Map<String, Object> params) {
		String encoding = getStringValue(params, ENCODING);
		if (StringUtils.isBlank(encoding)) {
			encoding = DEFAULT_ENCODING;
		}
		return encoding;
	}
}
