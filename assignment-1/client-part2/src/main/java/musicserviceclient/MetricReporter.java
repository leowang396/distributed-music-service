package musicserviceclient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MetricReporter {
  protected static void reportInMemoryList(String outputName, List<String[]> metrics) {
    List<Integer> postResponseTimes = new ArrayList<>();
    List<Integer> getResponseTimes = new ArrayList<>();

    // Iterates over in-memory list to write to output CSV file.
    File metricsCsv = new File(outputName);
    try {
      Files.deleteIfExists(metricsCsv.toPath());
      FileWriter fileWriter = new FileWriter(metricsCsv);

      for (String[] data: metrics) {
        // Extracts raw metrics list into separate POST and GET metrics lists.
        if (data[1].equals("POST")) {
          postResponseTimes.add(Integer.valueOf(data[2]));
        } else {
          getResponseTimes.add(Integer.valueOf(data[2]));
        }

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
          line.append("\"");
          line.append(data[i].replaceAll("\"", "\"\""));
          line.append("\"");
          if (i != data.length - 1) {
            line.append(',');
          }
        }
        line.append("\n");
        fileWriter.write(line.toString());
      }

      fileWriter.close();

    } catch (IOException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }

    // Sorts and computes the mean values for both POST and GET latencies.
    Collections.sort(postResponseTimes);
    int postResponseMean = computeMeanOfList(postResponseTimes);
    Collections.sort(getResponseTimes);
    int getResponseMean = computeMeanOfList(getResponseTimes);

    // Prints required metrics to std out.
    System.out.println(String.format("POST metrics:\n mean: %d millis\n median: %d millis\n"
                    + " p99: %d millis\n min: %d millis\n max: %d millis", postResponseMean,
            postResponseTimes.get(postResponseTimes.size() / 2),
            postResponseTimes.get((int) Math.floor(postResponseTimes.size() * 0.99)),
            postResponseTimes.get(0), postResponseTimes.get(postResponseTimes.size() - 1)));
    System.out.println(String.format("GET metrics:\n mean: %d millis\n median: %d millis\n"
                    + " p99: %d millis\n min: %d millis\n max: %d millis", getResponseMean,
            getResponseTimes.get(getResponseTimes.size() / 2),
            getResponseTimes.get((int) Math.floor(getResponseTimes.size() * 0.99)),
            getResponseTimes.get(0), getResponseTimes.get(getResponseTimes.size() - 1)));
  }

  private static int computeMeanOfList(List<Integer> list) {
    int mean = 0;
    int counter = 1;
    for (int i: list) {
      mean += (i - mean) / counter;
      counter++;
    }

    return mean;
  }
}
