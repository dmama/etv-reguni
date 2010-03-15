package ch.vd.uniregctb.security;

import java.util.List;
import java.util.Properties;

import ch.vd.ati.security.BasicRolesGranter;

public class UniregBasicRolesGranter extends BasicRolesGranter {

	//private static final Logger LOGGER = Logger.getLogger(UniregBasicRolesGranter.class);

	@Override
	public Properties getRoleMappings() {
		return super.getRoleMappings();
	}

	@Override
	public String getRolePrefix() {
		return super.getRolePrefix();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List grant(List list) {
		/*String str = "";
		for (Object o : list) {
			str += " "+o.toString();
		}*/
		return super.grant(list);
	}

	@Override
	public String grant(String str) {
		return super.grant(str);
	}

	@Override
	public String[] grant(String[] list) {
		/*String str = "";
		for (String s : list) {
			str += " "+s;
		}*/
		return super.grant(list);
	}

}
