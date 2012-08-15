package ch.vd.uniregctb.migration.nh;

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

public class MigrationNonHabitantSqlGenerator {

	private static final String SEP = Character.toString(CsvHelper.COMMA);
	private static final String USER = "[MigrationNonHabitantPostRCPERS]";

	public static void main(String[] args) throws Exception {
		final Configuration cfg = new Configuration();
		cfg.setClassForTemplateLoading(MigrationNonHabitantSqlGenerator.class, "/");
		cfg.setObjectWrapper(new DefaultObjectWrapper());

		BufferedReader input = null;
		Writer out = null;
		try {
			input = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), CsvHelper.CHARSET));
			final String[] headers = input.readLine().split(SEP);
			final Map<String, Object> root = new HashMap<String, Object>(2);
			final List<Map<String, String>> tiers = new ArrayList<Map<String, String>>(4000);
			root.put("tiers", tiers);
			root.put("USER", USER);
			String dataLine = input.readLine();
			while (dataLine != null) {
				final String[] datas = dataLine.split(SEP);
				final Map<String, String> dataMap = new HashMap<String, String>(headers.length);
				for (int i = 0; i < headers.length; i++) {
					dataMap.put(headers[i], datas[i].replace("'", "''")); // escape quote
				}
				tiers.add(dataMap);
				dataLine = input.readLine();
			}
			final Template temp = cfg.getTemplate("ch/vd/uniregctb/migration/nh/migration-nh.sql.ftl", "UTF-8");
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
