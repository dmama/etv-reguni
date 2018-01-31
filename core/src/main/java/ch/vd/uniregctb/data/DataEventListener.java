package ch.vd.uniregctb.data;

/**
 * Interface de notification de changements sur les données Unireg, que le changement
 * soit dans les sources de données (civiles) ou dans les données fiscales.
 */
public interface DataEventListener extends CivilDataEventListener, FiscalDataEventListener {
}
