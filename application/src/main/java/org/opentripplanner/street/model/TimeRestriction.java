package org.opentripplanner.street.model;

/**
 * Represents a time restriction, used for opening hours, time dependant turn restrictions etc.
 *
 * @author hbruch
 */
public interface TimeRestriction {
  boolean active(long time);
}
