package de.chkal.maven.gitlab.codequality.pmd;

import static org.assertj.core.api.Assertions.assertThat;

import de.chkal.maven.gitlab.codequality.Finding;
import de.chkal.maven.gitlab.codequality.Finding.Severity;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class PmdFindingProviderTest {

  @Test
  void shouldParsePmdFileWithOneViolation() throws IOException {

    try (InputStream fileStream = getFileStream("pmd-with-one-violation.xml")) {

      File repositoryRoot = new File("/home/user/projects/myapp");
      PmdFindingProvider provider = new PmdFindingProvider(repositoryRoot);
      List<Finding> findings = provider.getFindings(fileStream);

      assertThat(findings).hasSize(1);

      Finding first = findings.get(0);
      assertThat(first.getDescription()).isEqualTo("PMD: Empty if statement");
      assertThat(first.getFingerprint())
          .isEqualTo("73c15d693ccc60b3b851bcd200819758ee970fb4aa7e62a06f1c270eb8cc3c99");
      assertThat(first.getSeverity()).isEqualTo(Severity.MINOR);
      assertThat(first.getPath()).isEqualTo("src/main/java/my/package/MyClass.java");
      assertThat(first.getLine()).isEqualTo(258);
    }

  }

  private static InputStream getFileStream(String name) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(
        "pmd/" + name
    );
  }

}