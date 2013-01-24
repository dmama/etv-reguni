package ch.vd.uniregctb;

import com.sun.codemodel.JClass;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;

@SuppressWarnings({"UnusedDeclaration"})
public class UniregCustomWsPlugin extends Plugin {

	//private static final Logger LOGGER = Logger.getLogger(UniregCustomWsPlugin.class.getName());

	private static final String DATE_EQUALS = "\n    @Override\n" +
			"    public boolean equals(Object obj) {\n" +
			"        if (this == obj)\n" +
			"            return true;\n" +
			"        if (obj == null)\n" +
			"            return false;\n" +
			"        if (getClass() != obj.getClass())\n" +
			"            return false;\n" +
			"        Date other = (Date) obj;\n" +
			"        return day == other.day && month == other.month && year == other.year;\n" +
			"    }\n";
	
	private static final String DATE_HASHCODE = "\n    @Override\n" +
			"    public int hashCode() {\n" +
			"        final int prime = 31;\n" +
			"        int result = 1;\n" +
			"        result = prime * result + day;\n" +
			"        result = prime * result + month;\n" +
			"        result = prime * result + year;\n" +
			"        return result;\n" +
			"    }\n";
	
	private static final String DATE_COMPARETO = "\n    @Override\n" +
			"    public int compareTo(Date o) {\n" +
			"        if (this.year == o.year) {\n" +
			"            if (this.month == o.month) {\n" +
			"                return this.day - o.day;\n" +
			"            }\n" +
			"            else {\n" +
			"                return this.month - o.month;\n" +
			"            }\n" +
			"        }\n" +
			"        else {\n" +
			"            return this.year - o.year;\n" +
			"        }\n" +
			"    }\n";

	@Override
	public String getOptionName() {
		return "Xunireg-ws";
	}

	@Override
	public String getUsage() {
		return "  -Xunireg-ws        :  special customizations for Unireg";
	}

	@Override
	public boolean run(final Outline outline, final Options options, final ErrorHandler errorHandler) {
		// For each defined class

		for (final ClassOutline classOutline : outline.getClasses()) {
			if (classOutline.implClass.name().equals("Date")) {
				final JClass comparableClass = classOutline.implClass.owner().ref("java.lang.Comparable<Date>");
				classOutline.implClass._implements(comparableClass);
				classOutline.implClass.direct(DATE_HASHCODE);
				classOutline.implClass.direct(DATE_EQUALS);
				classOutline.implClass.direct(DATE_COMPARETO);
			}
		}
		return true;
	}
}
