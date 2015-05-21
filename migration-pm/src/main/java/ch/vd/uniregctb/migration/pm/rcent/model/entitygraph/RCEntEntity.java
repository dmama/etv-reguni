package ch.vd.uniregctb.migration.pm.rcent.model.entitygraph;

/**
 * Represent a node in the RCEnt data graph.
 *
 * The node has a list of ranged element representing its data as snapshots of its own sub-graph.
 *
 * The node also has fields, that can in turn be either RCEnt entities, either discrete, non-historical
 * data.
 *
 * This allows several levels of historical granularity to cohabit in the same representation of RCEnt data.
 */
public abstract class RCEntEntity {

}
