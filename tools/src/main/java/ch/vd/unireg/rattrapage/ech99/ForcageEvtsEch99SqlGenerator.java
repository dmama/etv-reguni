package ch.vd.unireg.rattrapage.ech99;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

import ch.vd.unireg.common.CsvHelper;

public class ForcageEvtsEch99SqlGenerator {

	private static final String USER = "[RattrapageEch99]";
	private static final int DEFAULT_BATCH_SIZE = 1000;

	public static void main(String[] args) throws Exception {
		final Configuration cfg = new Configuration();
		cfg.setClassForTemplateLoading(ForcageEvtsEch99SqlGenerator.class, "/");
		cfg.setObjectWrapper(new DefaultObjectWrapper());

		int batchSize;
		try {
			batchSize = Integer.parseInt(args[2]);
		} catch (ArrayIndexOutOfBoundsException e) {
			batchSize = DEFAULT_BATCH_SIZE;
		}

		try (FileInputStream fis = new FileInputStream(args[0]);
		     InputStreamReader isr = new InputStreamReader(fis, CsvHelper.CHARSET);
		     BufferedReader input = new BufferedReader(isr);
		     FileOutputStream fos = new FileOutputStream(args[1]);
		     Writer out = new OutputStreamWriter(fos, CsvHelper.CHARSET)) {

			final Map<String, Object> root = new HashMap<>(4);
			final List<String> evtIds = new ArrayList<>();
			root.put("DATE", new Date());
			root.put("LIST_OF_IDS", evtIds);
			root.put("USER", USER);
			String dataLine = input.readLine();
			int evtTotal = 0;
			while (dataLine != null) {
				StringBuilder ids = new StringBuilder(batchSize * 12);
				for (int i = 0; i < batchSize && dataLine != null; i++) {
					ids.append(dataLine);
					evtTotal++;
					dataLine = input.readLine();
					if (i < batchSize - 1 && dataLine != null) {
						ids.append(", ");
					}
				}
				evtIds.add(ids.toString());
			}
			root.put("TOTAL", evtTotal);
			final Template temp = cfg.getTemplate("ch/vd/unireg/rattrapage/ech99/forcage-evts.sql.ftl", "UTF-8");
			temp.process(root, out);
			out.flush();
		}
	}
}
