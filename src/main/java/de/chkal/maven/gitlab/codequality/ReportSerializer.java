package de.chkal.maven.gitlab.codequality;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class ReportSerializer {

  public void write(List<Finding> findings, OutputStream outputStream) throws IOException {

    try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      Gson gson = new Gson().newBuilder().setPrettyPrinting().create();

      JsonWriter jsonWriter = gson.newJsonWriter(writer);

      jsonWriter.beginArray();
      for (Finding finding : findings) {

        jsonWriter.beginObject();

        jsonWriter.name("description").value(finding.getDescription());

        jsonWriter.name("fingerprint").value(finding.getFingerprint());

        if (finding.getSeverity() != null) {
          jsonWriter.name("severity").value(finding.getSeverity().name().toLowerCase(Locale.ROOT));
        }

        if (finding.getPath() != null) {
          jsonWriter.name("location").beginObject();
          jsonWriter.name("path").value(finding.getPath());
          if (finding.getLine() != null) {
            jsonWriter.name("lines").beginObject();
            jsonWriter.name("begin").value(finding.getLine());
            jsonWriter.endObject();
          }
          jsonWriter.endObject();
        }

        jsonWriter.endObject();

      }
      jsonWriter.endArray();

    }

  }
}
