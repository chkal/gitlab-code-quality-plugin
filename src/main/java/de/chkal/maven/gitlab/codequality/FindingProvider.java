package de.chkal.maven.gitlab.codequality;

import java.io.File;
import java.util.List;

public interface FindingProvider {

  List<Finding> getFindings(File inputFile);

}
