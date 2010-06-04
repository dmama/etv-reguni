
package ch.vd.uniregctb.webservices.batch;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum JobStatut {

    JOB_EXCEPTION,
	JOB_INTERRUPTING,
    JOB_INTERRUPTED,
    JOB_OK,
    JOB_READY,
    JOB_RUNNING;

    public String value() {
        return name();
    }

    public static JobStatut fromValue(String v) {
        return valueOf(v);
    }

}
