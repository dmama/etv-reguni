package ch.vd.uniregctb.rattrapage.ech99;


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

import ch.vd.uniregctb.common.CsvHelper;

public class MaJNonHabitantsSqlGenerator {

	private static final String USER = "[RattrapageEch99]";
	private static final String SEP = Character.toString(CsvHelper.COMMA);

	public static void main(String[] args) throws Exception {
		final Configuration cfg = new Configuration();
		cfg.setClassForTemplateLoading(MaJNonHabitantsSqlGenerator.class, "/");
		cfg.setObjectWrapper(new DefaultObjectWrapper());

		try (FileInputStream fis = new FileInputStream(args[0]);
		     InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
		     BufferedReader input = new BufferedReader(isr);
		     FileOutputStream fos = new FileOutputStream(args[1]);
		     Writer out = new OutputStreamWriter(fos, CsvHelper.CHARSET)) {

			final String[] headers = input.readLine().split(SEP);
			final Map<String, Object> root = new HashMap<>(2);
			final List<Map<String, String>> list = new ArrayList<>(700);
			int total = 0;
			root.put("LIST", list);
			root.put("USER", USER);
			root.put("DATE", new Date());
			String dataLine = input.readLine();
			while (dataLine != null) {
				final String[] datas = dataLine.split(SEP, headers.length);
				final Map<String, String> dataMap = new HashMap<>(headers.length);
				for (int i = 0; i < headers.length; i++) {
					dataMap.put(headers[i], datas[i]);
				}
				list.add(dataMap);
				total++;
				dataLine = input.readLine();
			}
			root.put("TOTAL", total);
			final Template temp = cfg.getTemplate("ch/vd/uniregctb/rattrapage/ech99/maj-non-habitants.sql.ftl", "UTF-8");
			temp.process(root, out);
			out.flush();
		}
	}
}
