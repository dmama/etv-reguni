package ch.vd.uniregctb.fiscal.logging;

public class EnumTypeLogging extends org.apache.commons.lang.enums.Enum {

	public final static EnumTypeLogging All = new EnumTypeLogging("All");
	public final static EnumTypeLogging Host = new EnumTypeLogging("Host");
	
	protected EnumTypeLogging(String name) {
		super(name);
	}

}
