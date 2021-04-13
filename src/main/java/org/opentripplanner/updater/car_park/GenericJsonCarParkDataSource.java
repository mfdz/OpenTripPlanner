package org.opentripplanner.updater.car_park;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.routing.car_park.CarPark;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetch car park JSON feeds and pass each record on to the specific car park subclass
 *
 * @see CarParkDataSource
 */
public abstract class GenericJsonCarParkDataSource implements CarParkDataSource {

    private static final Logger log = LoggerFactory.getLogger(GenericJsonCarParkDataSource.class);

    private final String jsonParsePath;
    private final CarParkUpdaterParameters config;

    ArrayList<CarPark> carParks = new ArrayList<>();

    /**
     * Construct superclass where park list is on the top level of JSON code
     *
     * @param config
     */
    public GenericJsonCarParkDataSource(CarParkUpdaterParameters config, String jsonPath) {
        this.config = config;
        jsonParsePath = jsonPath;
    }

    @Override
    public boolean update() {
        var url = config.getUrl();
        try {
            InputStream data = null;

            URL url2 = new URL(url);

            String proto = url2.getProtocol();
            if (proto.equals("http") || proto.equals("https")) {
                data = HttpUtils.getData(url);
            } else {
                // Local file probably, try standard java
                data = url2.openStream();
            }

            if (data == null) {
                log.warn("Failed to get data from url " + url);
                return false;
            }
            parseJSON(data);
            data.close();
        } catch (IllegalArgumentException e) {
            log.warn("Error parsing car park feed from " + url, e);
            return false;
        } catch (JsonProcessingException e) {
            log.warn("Error parsing car park feed from " + url + "(bad JSON of some sort)", e);
            return false;
        } catch (IOException e) {
            log.warn("Error reading car park feed from " + url, e);
            return false;
        }
        return true;
    }

    private void parseJSON(InputStream dataStream) throws JsonProcessingException,
        IllegalArgumentException, IOException {

        ArrayList<CarPark> out = new ArrayList<>();

        String parkString = convertStreamToString(dataStream);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(parkString);

        if (!jsonParsePath.equals("")) {
            String delimiter = "/";
            String[] parseElement = jsonParsePath.split(delimiter);
            for(int i =0; i < parseElement.length ; i++) {
                rootNode = rootNode.path(parseElement[i]);
            }

            if (rootNode.isMissingNode()) {
                throw new IllegalArgumentException("Could not find jSON elements " + jsonParsePath);
            }
        }

        for (int i = 0; i < rootNode.size(); i++) {
            JsonNode node = rootNode.get(i);
            if (node == null) {
                continue;
            }
            CarPark carPark = makeCarPark(node);
            if (carPark != null)
                out.add(carPark);
        }
        synchronized(this) {
            carParks = out;
        }
    }

    String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner scanner = null;
        String result="";
        try {

            scanner = new java.util.Scanner(is).useDelimiter("\\A");
            result = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
        }
        finally
        {
            if(scanner!=null)
                scanner.close();
        }
        return result;

    }

    @Override
    public synchronized List<CarPark> getCarParks() {
        return carParks;
    }

    public abstract CarPark makeCarPark(JsonNode carParkNode);

    @Override
    public String toString() {
        return getClass().getName() + "(" + url + ")";
    }

}

