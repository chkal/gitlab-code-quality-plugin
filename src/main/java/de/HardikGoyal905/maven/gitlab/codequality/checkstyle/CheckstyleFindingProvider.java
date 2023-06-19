package de.HardikGoyal905.maven.gitlab.codequality.checkstyle;

import de.HardikGoyal905.maven.gitlab.codequality.FindingProvider;
import de.HardikGoyal905.maven.gitlab.codequality.Finding;
import de.HardikGoyal905.maven.gitlab.codequality.Finding.Severity;
import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
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
import javax.lang.model.type.ErrorType;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class CheckstyleFindingProvider implements FindingProvider {

  private final File repositoryRoot;

  public CheckstyleFindingProvider(File repositoryRoot) {
    this.repositoryRoot = repositoryRoot;
  }

  @Override
  public String getName() {
    return "Checkstyle";
  }

  @Override
  public List<Finding> getFindings(InputStream stream) {

    try {

      XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
      XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(stream);

      JAXBContext jaxbContext = JAXBContext.newInstance(CheckstyleType.class);
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      JAXBElement<CheckstyleType> checkstyleType =
          unmarshaller.unmarshal(xmlStreamReader, CheckstyleType.class);

      return checkstyleType.getValue().getFile().stream()
          .flatMap(this::transformFileType)
          .collect(Collectors.toList());

    } catch (JAXBException | XMLStreamException e) {
      throw new IllegalStateException(e);
    }
  }

  private Stream<Finding> transformFileType(FileType fileType) {
    return fileType.getError().stream().map(errorType -> transformErrorType(fileType, errorType));
  }

  private Finding transformErrorType(FileType fileType, ErrorType errorType) {

    Finding finding = new Finding();
    finding.setDescription(String.format("%s: %s", getName(), errorType.getMessage()));
    finding.setFingerprint(createFingerprint(fileType, errorType));
    finding.setSeverity(getSeverity(errorType.getSeverity()));
    finding.setPath(getRepositoryRelativePath(fileType));
    finding.setLine(getLineNumber(errorType));
    return finding;

  }

  private String getRepositoryRelativePath(FileType fileType) {
    Path absolutePath = Path.of(fileType.getName());
    return repositoryRoot.toPath().relativize(absolutePath).toString();
  }

  private Severity getSeverity(String severity) {
    switch (severity) {
      case "error":
        return Severity.MAJOR;
      case "warning":
        return Severity.MINOR;
      case "info":
      case "ignore":
      default:
        return Severity.INFO;
    }
  }

  private String createFingerprint(FileType fileType, ErrorType errorType) {

    try {

      /*
       * The fingerprint is created from:
       *   - file path
       *   - severity
       *   - message text
       *   - column index (which will most likely not change for a finding)
       *
       * We do NOT use:
       *   - line number (will change if code is added/removed above or below the finding)
       */
      String key = String.format("%s:%s:%s:%s",
          getRepositoryRelativePath(fileType),
          errorType.getSeverity(),
          errorType.getMessage(),
          errorType.getColumn()
      );

      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(key.getBytes(StandardCharsets.UTF_8));
      byte[] digest = messageDigest.digest();

      return DatatypeConverter.printHexBinary(digest).toLowerCase(Locale.ROOT);

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

  }

  private static Integer getLineNumber(ErrorType errorType) {
    return errorType.getLine() != null && errorType.getLine().matches("\\d+")
        ? Integer.parseInt(errorType.getLine())
        : 1;
  }

}
