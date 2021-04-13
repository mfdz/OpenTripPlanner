package org.opentripplanner.updater.car_park;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;

public class CarParkUpdaterParameters implements PollingGraphUpdaterParameters {
  private final String configRef;
  private final String url;
  private final int frequencySec;
  private final CarParkDataSource source;

  public CarParkUpdaterParameters(
      String configRef,
      String url,
      int frequencySec,
      CarParkDataSource source
  ) {
    this.configRef = configRef;
    this.url = url;
    this.frequencySec = frequencySec;
    this.source = source;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public int getFrequencySec() {
    return frequencySec;
  }

  /**
   * The config name/type for the updater. Used to reference the configuration element.
   */
  @Override
  public String getConfigRef() {
    return configRef;
  }

  public CarParkDataSource sourceParameters() {
    return source;
  }
}
