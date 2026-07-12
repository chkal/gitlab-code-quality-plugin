package de.chkal.maven.gitlab.codequality.cpd;

import static org.assertj.core.api.Assertions.assertThat;

import de.chkal.maven.gitlab.codequality.Finding;
import de.chkal.maven.gitlab.codequality.Finding.Severity;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class CpdFindingProviderTest {

  @Test
  void shouldParsePmdFileWithOneViolation() throws IOException {

    try (InputStream fileStream = getFileStream("cpd-with-one-duplication.xml")) {

      File repositoryRoot = new File("/home/user/projects/myapp");
      CpdFindingProvider provider = new CpdFindingProvider(repositoryRoot);
      List<Finding> findings = provider.getFindings(fileStream);

      assertThat(findings).hasSize(2);

      Finding first = findings.get(0);
      assertThat(first.getDescription()).isEqualTo("CPD: Duplication (27 lines)");
      assertThat(first.getFingerprint())
          .isEqualTo("c2bbc64f225dc097435607e0748e35f72e20ec74e35f3df6430a84ea3928d09e");
      assertThat(first.getSeverity()).isEqualTo(Severity.MINOR);
      assertThat(first.getPath()).isEqualTo("src/main/java/my/package/MyClass1.java");
      assertThat(first.getLine()).isEqualTo(157);
      
      Finding second = findings.get(1);
      assertThat(second.getDescription()).isEqualTo("CPD: Duplication (27 lines)");
      assertThat(second.getFingerprint())
          .isEqualTo("c0a6efece288a5496dfe4eae9368e893d94c6e357f956bd8894a7f47fe55b744");
      assertThat(second.getSeverity()).isEqualTo(Severity.MINOR);
      assertThat(second.getPath()).isEqualTo("src/main/java/my/package/MyClass2.java");
      assertThat(second.getLine()).isEqualTo(185);
    }

  }

  private static InputStream getFileStream(String name) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(
        "cpd/" + name
    );
  }

}