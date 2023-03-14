package de.chkal.maven.gitlab.codequality;

import de.chkal.maven.gitlab.codequality.spotbugs.SpotbugsFindingProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.VERIFY)
public class GenerateMojo extends AbstractMojo {

  @Parameter(defaultValue = "true")
  public boolean spotbugsEnabled;

  @Parameter(defaultValue = "${project.build.directory}/spotbugsXml.xml")
  public File spotbugsInputFile;

  @Parameter(defaultValue = "${project.build.directory}/gl-code-quality-report.json")
  public File outputFile;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  public void execute() throws MojoExecutionException, MojoFailureException {

    Logger log = new Logger(getLog());

    // Lookup repository root so we can build repository-relative paths
    File repositoryRoot = getRepositoryRootDir(project.getBasedir(), log);

    List<Finding> findings = new ArrayList<>();

    // SpotBugs
    if (spotbugsEnabled) {

      if (spotbugsInputFile.canRead()) {

        List<Finding> spotbugsFindings = new SpotbugsFindingProvider(project, repositoryRoot, log)
            .getFindings(spotbugsInputFile);

        log.info("SpotBugs XML report with {} issues found: {}",
            spotbugsFindings.size(), spotbugsInputFile);

        findings.addAll(spotbugsFindings);

      } else {
        log.info(String.format("SpotBugs XML report not found: %s", spotbugsInputFile));
      }

    } else {
      log.info("SpotBugs support disabled.");
    }

    // Create GitLab report
    try (FileOutputStream stream = new FileOutputStream(outputFile)) {
      new ReportSerializer().write(findings, stream);
      log.info("GitLab code quality report containing {} issue created: {}",
          findings.size(), outputFile);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

  }

  private File getRepositoryRootDir(File initial, Logger log) {

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
