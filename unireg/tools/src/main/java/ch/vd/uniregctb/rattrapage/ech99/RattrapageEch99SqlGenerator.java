package ch.vd.uniregctb.rattrapage.ech99;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

import ch.vd.uniregctb.common.CsvHelper;

public class RattrapageEch99SqlGenerator {

	private static final String USER = "[RattrapageEch99]";
	private static final int DEFAULT_BATCH_SIZE = 100;

	public static void main(String[] args) throws Exception {
		final Configuration cfg = new Configuration();
		cfg.setClassForTemplateLoading(RattrapageEch99SqlGenerator.class, "/");
		cfg.setObjectWrapper(new DefaultObjectWrapper());

		int batchSize;
		try {
			batchSize = Integer.parseInt(args[2]);
		} catch (ArrayIndexOutOfBoundsException e) {
			batchSize = DEFAULT_BATCH_SIZE;
		}

		BufferedReader input = null;
		Writer out = null;
		try {
			input = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), CsvHelper.CHARSET));
			final Map<String, Object> root = new HashMap<String, Object>(2);
			final List<String> evtIds = new ArrayList<String>();
			root.put("EVT_IDS", evtIds);
			root.put("USER", USER);
			String dataLine = input.readLine();
			while (dataLine != null) {
				StringBuilder sb = new StringBuilder(batchSize * 20);
				for (int i = 0; i < batchSize && dataLine != null; i++) {
					sb.append(dataLine);
					dataLine = input.readLine();
					if (i < batchSize - 1 && dataLine != null) {
						sb.append(", ");
					}
				}
				evtIds.add(sb.toString());
			}
			final Template temp = cfg.getTemplate("ch/vd/uniregctb/rattrapage/ech99/rattrapage.sql.ftl", "UTF-8");
			out = new OutputStreamWriter(new FileOutputStream(args[1]),CsvHelper.CHARSET);
			temp.process(root, out);
			out.flush();
		} finally {
			if (input != null) {
				input.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}
}
