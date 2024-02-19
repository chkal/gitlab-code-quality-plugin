package de.chkal.maven.gitlab.codequality;

import de.chkal.maven.gitlab.codequality.checkstyle.CheckstyleFindingProvider;
import de.chkal.maven.gitlab.codequality.spotbugs.SpotbugsFindingProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

  @Parameter(property = "glcqp.spotbugsEnabled", defaultValue = "true")
  public boolean spotbugsEnabled;

  @Parameter(property = "glcqp.spotbugsInputFile", defaultValue = "${project.build.directory}/spotbugsXml.xml")
  public File spotbugsInputFile;

  @Parameter(property = "glcqp.checkstyleEnabled", defaultValue = "true")
  public boolean checkstyleEnabled;

  @Parameter(property = "glcqp.checkstyeInputFile", defaultValue = "${project.build.directory}/checkstyle-result.xml")
  public File checkstyleInputFile;

  @Parameter(property = "glcqp.outputFile", defaultValue = "${project.build.directory}/gl-code-quality-report.json")
  public File outputFile;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  public void execute() throws MojoFailureException {

    Logger log = new Logger(getLog());

    // Lookup repository root so we can build repository-relative paths
    File repositoryRoot = getRepositoryRootDir(project.getBasedir(), log);

    List<Finding> findings = new ArrayList<>();

    // Run SpotBugs provider
    findings.addAll(executeProvider(
        new SpotbugsFindingProvider(project, repositoryRoot, log),
        spotbugsEnabled,
        spotbugsInputFile,
        log
    ));

    // Run Checkstyle provider
    findings.addAll(executeProvider(
        new CheckstyleFindingProvider(repositoryRoot),
        checkstyleEnabled,
        checkstyleInputFile,
        log
    ));

    // Create GitLab report
    if (findings.size() > 0) {
      try (FileOutputStream stream = new FileOutputStream(outputFile)) {
        new ReportSerializer().write(findings, stream);
        log.info("GitLab code quality report for {} issue created: {}",
            findings.size(), outputFile);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

  }

  private static List<Finding> executeProvider(FindingProvider provider,
      boolean active, File file, Logger log) throws MojoFailureException {

    // Checkstyle
    if (active) {

      if (file.canRead()) {

        try (InputStream stream = new FileInputStream(file)) {

          List<Finding> findings = provider.getFindings(stream);

          log.info("{} report with {} issues found: {}", provider.getName(),
              findings.size(), file);

          return findings;

        } catch (IOException e) {
          throw new MojoFailureException("IO error", e);
        }

      } else {
        log.info("{} report not found: {}", provider.getName(), file);
      }

    } else {
      log.info("{} support disabled.", provider.getName());
    }

    return Collections.emptyList();


  }

  private static File getRepositoryRootDir(File initial, Logger log) {

    File current = initial;

    do {

      if (new File(current, ".git").exists()) {
        log.debug("Detected git root directory: {}", current);
        return current;
      }

      current = current.getParentFile();

    } while (current != null);

    log.warn("Failed to locate git root directory. Paths will most likely be incorrect.");
    return initial;

  }

}
