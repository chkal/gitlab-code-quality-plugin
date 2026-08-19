package de.chkal.maven.gitlab.codequality.cpd;

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

public class CpdFindingProvider implements FindingProvider {

  private final File repositoryRoot;

  public CpdFindingProvider(File repositoryRoot) {
    this.repositoryRoot = repositoryRoot;
  }

  @Override
  public String getName() {
    return "CPD";
  }

  @Override
  public List<Finding> getFindings(InputStream stream) {

    try {

      JAXBContext jaxbContext = JAXBContext.newInstance(PmdCpd.class);
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      PmdCpd cpdWarnings = (PmdCpd) unmarshaller.unmarshal(stream);

      return cpdWarnings.getDuplication().stream()
          .flatMap(this::transformDuplication)
          .collect(Collectors.toList());

    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  private Stream<Finding> transformDuplication(Duplication duplication) {
	return duplication.getFile().stream().map(fileLocation -> transformFileLocation(duplication, fileLocation));
  }

  private Finding transformFileLocation(Duplication duplication, FileLocation fileLocation) {

    Finding finding = new Finding();
    finding.setDescription(String.format("%s: Duplication (%d lines)", getName(), duplication.getLines().intValue()));
    finding.setFingerprint(createFingerprint(duplication, fileLocation));
    finding.setSeverity(getSeverity(duplication));
    finding.setPath(getRepositoryRelativePath(fileLocation));
    finding.setLine(getLineNumber(fileLocation));
    return finding;

  }

  private String getRepositoryRelativePath(FileLocation file) {
    Path absolutePath = Path.of(file.getPath());
    return repositoryRoot.toPath().relativize(absolutePath).toString();
  }

  private Severity getSeverity(Duplication duplication) {
    if (duplication.getLines().intValue() > 30) {
        return Severity.MAJOR;
    }
    if (duplication.getLines().intValue() > 10) {
        return Severity.MINOR;
    }
    return Severity.INFO;
  }

  private String createFingerprint(Duplication duplication, FileLocation fileLocation) {

    try {

      /*
       * The fingerprint is created from:
       *   - file path
       *   - number of lines
       *   - number of tokens
       *   - code fragment
       *   - column index (which will most likely not change for a finding)
       *   - end column index (which will most likely not change for a finding)
       *
       * We do NOT use:
       *   - line number (will change if code is added/removed above or below the finding)
       */
      String key = String.format("%s:%s:%s:%s:%s:%s",
          getRepositoryRelativePath(fileLocation),
          duplication.getLines().intValue(),
          duplication.getTokens().intValue(),
          duplication.getCodefragment().value,
          fileLocation.getColumn(),
          fileLocation.getEndcolumn()
      );

      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(key.getBytes(StandardCharsets.UTF_8));
      byte[] digest = messageDigest.digest();

      return DatatypeConverter.printHexBinary(digest).toLowerCase(Locale.ROOT);

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

  }

  private static Integer getLineNumber(FileLocation fileLocation) {
	return fileLocation.getLine().intValue();
  }

}
