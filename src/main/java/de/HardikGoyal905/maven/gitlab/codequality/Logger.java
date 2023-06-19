package de.HardikGoyal905.maven.gitlab.codequality;

import org.apache.maven.plugin.logging.Log;

public class Logger {

  private final Log log;

  public Logger(Log log) {
    this.log = log;
  }

  public void debug(String pattern, Object... arguments) {
    log.debug(buildMessage(pattern, arguments));
  }

  public void info(String pattern, Object... arguments) {
    log.info(buildMessage(pattern, arguments));
  }

  public void warn(String pattern, Object... arguments) {
    log.warn(buildMessage(pattern, arguments));
  }

  public void error(String pattern, Object... arguments) {
    log.error(buildMessage(pattern, arguments));
  }

  private String buildMessage(String pattern, Object... arguments) {
    String message = pattern;
    for (Object argument : arguments) {
      message = message.replaceFirst("\\{}", argument != null ? argument.toString() : "null");
    }
    return message;
  }

}
