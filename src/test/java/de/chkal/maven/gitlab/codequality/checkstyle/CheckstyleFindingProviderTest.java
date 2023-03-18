package de.chkal.maven.gitlab.codequality.checkstyle;

import static org.assertj.core.api.Assertions.assertThat;

import de.chkal.maven.gitlab.codequality.Finding;
import de.chkal.maven.gitlab.codequality.Finding.Severity;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckstyleFindingProviderTest {

  @Test
  void shouldParseCheckstyleFileWithoutNamespace() throws IOException {

    try (InputStream fileStream = getFileStream("checkstyle-without-namespace.xml")) {

      File repositoryRoot = new File("/home/user/projects/myapp");
      CheckstyleFindingProvider provider = new CheckstyleFindingProvider(repositoryRoot);
      List<Finding> findings = provider.getFindings(fileStream);

      assertThat(findings).hasSize(2);

      Finding first = findings.get(0);
      assertThat(first.getDescription()).startsWith("Checkstyle: 'static' modifier out of order");
      assertThat(first.getFingerprint()).isNotBlank();
      assertThat(first.getSeverity()).isEqualTo(Severity.MAJOR);
      assertThat(first.getPath()).isEqualTo("src/main/java/com/example/myapp/Bar.java");
      assertThat(first.getLine()).isEqualTo(25);

      Finding second = findings.get(1);
      assertThat(second.getDescription()).startsWith("Checkstyle: 'if' child has incorrect");
      assertThat(second.getFingerprint()).isNotBlank();
      assertThat(second.getSeverity()).isEqualTo(Severity.MAJOR);
      assertThat(second.getPath()).isEqualTo("src/main/java/com/example/myapp/Bar.java");
      assertThat(second.getLine()).isEqualTo(90);
    }

  }

  private static InputStream getFileStream(String name) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(
        "checkstyle/" + name
    );
  }

}