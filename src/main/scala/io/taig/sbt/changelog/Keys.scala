package io.taig.sbt.changelog

import java.util.Date

import cats.data.NonEmptyList
import io.taig.sbt.changelog.Helper.Range
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevCommit
import sbt._

trait Keys {
    val changelogEntryName = taskKey[String] {
        "Name of the new changelog entry, current version by default"
    }

    val changelogEntryDate = taskKey[Option[Date]] {
        "Date of the new changelog entry, today by default"
    }

    val changelogFile = settingKey[File] {
        "Path to the changelog file"
    }

    val changelogGit = taskKey[Git] {
        "Retrieve Git instance for the project"
    }

    val changelogRecentTags = taskKey[List[String]] {
        "Find the most recent git tags"
    }

    val changelogCommits = taskKey[Option[Range] ⇒ RevWalk] {
        "Get every commit in the given range, entire commit history when no " +
            "range is None"
    }

    val changelogEntryCommitFormat = taskKey[RevCommit ⇒ String] {
        "Transform a commit into the changelog representation"
    }

    val changelogEntryCheck = taskKey[String ⇒ Boolean] {
        "Check if the given String is the beginning of a changelog entry"
    }

    val changelogEntryNameFormat = taskKey[String ⇒ String] {
        "Generate the title string to embed in the changelog document"
    }

    val changelogEntryDateFormat = taskKey[String ⇒ String] {
        "Generate the date string to embed in the changelog document"
    }

    val changelogEntryDatePattern = settingKey[String] {
        ""
    }

    val changelogEntryCommits = taskKey[NonEmptyList[RevCommit]] {
        ""
    }

    val changelogFormatEntry = settingKey[( String, Option[String], String ) ⇒ String] {
        "Generate the textual representation from the title, date and changes"
    }

    val changelogTemplate = settingKey[String] {
        "TODO"
    }

    val changelogEntryFile = taskKey[File] {
        """
          |Provide a file for the changelog entry in interactive mode that will
          |be deleted after successful changelog generation
        """.stripMargin.trim
    }

    val changelogGenerate = taskKey[String] {
        "Generate the changelog"
    }

    val changelogGeneratePromptEdit = settingKey[Boolean] {
        "Allow to make changes to the changelog entry before injecting into " +
            "the changelog file"
    }
}