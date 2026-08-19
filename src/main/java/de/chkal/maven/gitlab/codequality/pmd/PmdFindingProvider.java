package de.chkal.maven.gitlab.codequality.pmd;

import de.chkal.maven.gitlab.codequality.Finding;
import de.chkal.maven.gitlab.codequality.Finding.Severity;
import de.chkal.maven.gitlab.codequality.FindingProvider;
import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PmdFindingProvider implements FindingProvider {

  private final File repositoryRoot;

  public PmdFindingProvider(File repositoryRoot) {
    this.repositoryRoot = repositoryRoot;
  }

  @Override
  public String getName() {
    return "PMD";
  }

  @Override
  public List<Finding> getFindings(InputStream stream) {

    try {

      JAXBContext jaxbContext = JAXBContext.newInstance(Pmd.class);
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      Pmd pmdWarnings = (Pmd) unmarshaller.unmarshal(stream);

      return pmdWarnings.getFile().stream()
          .flatMap(this::transformFile)
          .collect(Collectors.toList());

    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  private Stream<Finding> transformFile(de.chkal.maven.gitlab.codequality.pmd.File file) {
	return file.getViolation().stream().map(violation -> transformViolation(file, violation));
  }

  private Finding transformViolation(de.chkal.maven.gitlab.codequality.pmd.File file, Violation violation) {

    Finding finding = new Finding();
    finding.setDescription(String.format("%s: %s", getName(), violation.getValue().replace("\n", "").replace("\r", "")));
    finding.setFingerprint(createFingerprint(file, violation));
    finding.setSeverity(getSeverity(violation.getPriority()));
    finding.setPath(getRepositoryRelativePath(file));
    finding.setLine(getLineNumber(violation));
    return finding;

  }

  private String getRepositoryRelativePath(de.chkal.maven.gitlab.codequality.pmd.File file) {
    Path absolutePath = Path.of(file.getName());
    return repositoryRoot.toPath().relativize(absolutePath).toString();
  }

  private Severity getSeverity(String severity) {
    switch (severity) {
      case "1":
      case "2":
        return Severity.MAJOR;
      case "3":
      case "4":
        return Severity.MINOR;
      default:
        return Severity.INFO;
    }
  }

  private String createFingerprint(de.chkal.maven.gitlab.codequality.pmd.File file, Violation violation) {

    try {

      /*
       * The fingerprint is created from:
       *   - file path
       *   - priority
       *   - rule
       *   - message
       *   - class/method/variable if available
       *   - column index (which will most likely not change for a finding)
       *
       * We do NOT use:
       *   - line number (will change if code is added/removed above or below the finding)
       */
      String key = String.format("%s:%s:%s:%s:%s:%s:%s:%s",
          getRepositoryRelativePath(file),
          violation.getPriority(),
          violation.getRule(),
          violation.getValue(),
          violation.getClass(),
          violation.getMethod(),
          violation.getVariable(),
          violation.getBegincolumn()
      );

      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(key.getBytes(StandardCharsets.UTF_8));
      byte[] digest = messageDigest.digest();

      return DatatypeConverter.printHexBinary(digest).toLowerCase(Locale.ROOT);

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

  }

  private static Integer getLineNumber(Violation violation) {
	return violation.getBeginline().intValue();
  }

}
