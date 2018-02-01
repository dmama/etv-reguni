package ch.vd.uniregctb.hibernate;

import java.util.Map;

public class JsonMapUserType extends JsonUserType<Map> {

	public JsonMapUserType() {
		super(Map.class);
	}
}
