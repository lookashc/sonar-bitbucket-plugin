package ch.mibex.bitbucket.sonar

import ch.mibex.bitbucket.sonar.utils.SonarUtils
import org.sonar.api.batch.rule.Severity
import org.sonar.api.batch.{BatchSide, InstantiationStrategy}
import org.sonar.api.config.Settings
import org.sonar.api.platform.Server

@BatchSide
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
class SonarBBPluginConfig(settings: Settings, server: Server) {

  def isEnabled: Boolean = settings.hasKey(SonarBBPlugin.BitbucketAccountName)

  def accountName(): String = settings.getString(SonarBBPlugin.BitbucketAccountName)

  def repoSlug(): String = settings.getString(SonarBBPlugin.BitbucketRepoSlug)

  def teamName(): String = settings.getString(SonarBBPlugin.BitbucketTeamName)

  def oauthTokenClientKey(): String = settings.getString(SonarBBPlugin.BitbucketOAuthClientKey)

  def oauthTokenClientSecret(): String = settings.getString(SonarBBPlugin.BitbucketOAuthClientSecret)

  def apiKey(): String = settings.getString(SonarBBPlugin.BitbucketApiKey)

  def minSeverity(): String = settings.getString(SonarBBPlugin.SonarQubeMinSeverity)

  def approveUnApproveEnabled(): Boolean = settings.getBoolean(SonarBBPlugin.BitbucketApproveUnapprove)

  def buildStatusEnabled(): Boolean = settings.getBoolean(SonarBBPlugin.BitbucketBuildStatus)

  def sonarApprovalSeverityLevel(): String = settings.getString(SonarBBPlugin.SonarUnapprovalSeverityLevel)

  def sonarAddNewIssuesFromNonChangedLines(): Boolean = settings.getBoolean(SonarBBPlugin.SonarAddNewIssuesFromNonChangedLines)

  def branchName(): String = {
    var branchName = settings.getString(SonarBBPlugin.BitbucketBranchName)
    Option(branchName) foreach { _ =>
      branchName = branchName.replaceFirst("^origin/", "") // Jenkins GIT_BRANCH variable contains a origin/ prefix
      if (branchIllegalCharReplacement().nonEmpty) {
        branchName = branchName.replaceAll("/", branchIllegalCharReplacement())
      }
    }
    branchName
  }

  def pullRequestId(): Int = settings.getInt(SonarBBPlugin.BitbucketPullRequestId)

  private def branchIllegalCharReplacement() = // we cannot use "" as defaultValue with SonarQube settings
    Option(settings.getString(SonarBBPlugin.SonarQubeIllegalBranchCharReplacement)).getOrElse("")

  def validateOrThrow(): Unit = {
    require(
      Option(server.getVersion).isEmpty || server.getVersion != "5.1",
      s"${SonarBBPlugin.PluginLogPrefix} SonarQube v5.1 is not supported because of issue SONAR-6398"
    )
    require(
      SonarUtils.isValidBranchNameReplacement(branchIllegalCharReplacement()),
      s"""${SonarBBPlugin.PluginLogPrefix} Only the following characters
         |are allowed as replacement: ${SonarUtils.LegalBranchNameReplacementChars}""".stripMargin.replaceAll("\n", " ")
    )
    require(
      Option(branchName()).nonEmpty || pullRequestId() != 0,
      s"${SonarBBPlugin.PluginLogPrefix} The branch to analyze or the pull request ID must be given"
    )
    require(
      isValidAuthenticationGiven,
      s"""${SonarBBPlugin.PluginLogPrefix} Either the name and API key for the Bitbucket team account
        |or an OAuth client key and its secret must be given""".stripMargin.replaceAll("\n", " ")
    )
    Option(minSeverity()) foreach { severity =>
      try {
        Severity.valueOf(severity)
      } catch {
        case _: IllegalArgumentException =>
          throw new IllegalArgumentException(s"${SonarBBPlugin.PluginLogPrefix} Invalid severity $severity")
      }
    }
  }

  private def isValidAuthenticationGiven =
    (Option(teamName()).nonEmpty && Option(apiKey()).nonEmpty) ||
      (Option(oauthTokenClientKey()).nonEmpty && Option(oauthTokenClientSecret()).nonEmpty)

  override def toString: String =
    s"""[PluginConfiguration](
        |accountName=${accountName()},
        |repoSlug=${repoSlug()},
        |teamName=${teamName()},
        |apiKey=${Option(apiKey()).flatMap(s => Option("***")).orNull},
        |oauthTokenClientKey=${Option(oauthTokenClientKey()).flatMap(s => Option("***")).orNull},
        |oauthTokenClientSecret=${Option(oauthTokenClientSecret()).flatMap(s => Option("***")).orNull},
        |branchName=${branchName()},
        |pullRequestId=${pullRequestId()}
        |approveUnApproveEnabled=${approveUnApproveEnabled()},
        |minSeverity=${minSeverity()})""".stripMargin.replaceAll("\n", "")

}