package ch.vd.vuta.model;

import java.util.Iterator;

public class SmsDAO extends GenericDAO<SmsModel, Long> {

	public SmsDAO() {

		super(SmsModel.class);
	}
	
	@SuppressWarnings("unchecked")
	public Iterator<SmsModel> iterator() {
		return (Iterator<SmsModel>)super.getHibernateTemplate().iterate("from SmsModel as sms order by sms.dateReception desc");
	}

}
