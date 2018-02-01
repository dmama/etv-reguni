package ch.vd.unireg.migration.nh;

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

import ch.vd.unireg.common.CsvHelper;

public class MigrationNonHabitantSqlGenerator {

	private static final String SEP = Character.toString(CsvHelper.COMMA);
	private static final String USER = "[MigrationNonHabitantPostRCPERS]";

	public static void main(String[] args) throws Exception {
		final Configuration cfg = new Configuration();
		cfg.setClassForTemplateLoading(MigrationNonHabitantSqlGenerator.class, "/");
		cfg.setObjectWrapper(new DefaultObjectWrapper());

		try (FileInputStream fis = new FileInputStream(args[0]);
		     InputStreamReader isr = new InputStreamReader(fis, CsvHelper.CHARSET);
		     BufferedReader input = new BufferedReader(isr);
		     FileOutputStream fos = new FileOutputStream(args[1]);
		     Writer out = new OutputStreamWriter(fos, CsvHelper.CHARSET)) {

			final String[] headers = input.readLine().split(SEP);
			final Map<String, Object> root = new HashMap<>(2);
			final List<Map<String, String>> tiers = new ArrayList<>(4000);
			root.put("tiers", tiers);
			root.put("USER", USER);
			String dataLine = input.readLine();
			while (dataLine != null) {
				final String[] datas = dataLine.split(SEP);
				final Map<String, String> dataMap = new HashMap<>(headers.length);
				for (int i = 0; i < headers.length; i++) {
					dataMap.put(headers[i], datas[i].replace("'", "''")); // escape quote
				}
				tiers.add(dataMap);
				dataLine = input.readLine();
			}
			final Template temp = cfg.getTemplate("ch/vd/unireg/migration/nh/migration-nh.sql.ftl", "UTF-8");
			temp.process(root, out);
			out.flush();
		}
	}
}
