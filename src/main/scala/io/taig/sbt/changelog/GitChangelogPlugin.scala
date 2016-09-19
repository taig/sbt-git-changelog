package io.taig.sbt.changelog

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import cats.data.NonEmptyList
import cats.instances.list._
import org.eclipse.jgit.api.Git
import sbt.Keys._
import sbt._

import scala.util.{ Success, Try }

object GitChangelogPlugin extends AutoPlugin {
    object autoImport extends Keys

    import autoImport._

    override def trigger = allRequirements

    override def projectSettings: Seq[Def.Setting[_]] = Seq(
        changelogGit := {
            val locations = Set(
                baseDirectory.value,
                ( baseDirectory in LocalRootProject ).value
            )

            locations map { directory ⇒
                Try( Git.open( directory ) )
            } collectFirst {
                case Success( git ) ⇒ git
            } getOrElse {
                sys.error {
                    s"""
                       |No git configuration found at:
                       |${locations mkString "\n"}
                       |Did you git init?
                    """.stripMargin.trim
                }
            }
        },
        changelogEntryName := version.value,
        changelogEntryDate := Some( new Date() ),
        changelogEntryDatePattern := "yyyy-MM-dd",
        changelogFile := baseDirectory.value / "CHANGELOG.md",
        changelogRecentTags := {
            Helper.using( changelogGit.value ) { implicit git ⇒
                Helper.recentTags
            }
        },
        changelogCommits := { range ⇒
            Helper.using( changelogGit.value ) { implicit git ⇒
                Helper.commits( range )
            }
        },
        changelogFormatEntry := { ( title, date, changes ) ⇒
            s"""
               |$title${date.map( "\n\n" + _ ).getOrElse( "" )}
               |
               |$changes
             """.stripMargin.trim
        },
        changelogEntryNameFormat := { name ⇒
            s"## $name"
        },
        changelogEntryDateFormat := { date ⇒
            s"_${date}_"
        },
        changelogEntryCommitFormat := { commit ⇒
            s" * ${commit.getShortMessage}"
        },
        changelogEntryCheck := { line ⇒
            line.startsWith {
                changelogEntryNameFormat.value( "" )
            }
        },
        changelogEntryFile := File.createTempFile( "changelog", ".md" ),
        changelogTemplate := {
            """
              |# Changelog
            """.stripMargin.trim
        },
        changelogEntryCommits := {
            import collection.JavaConversions._

            val range = Helper.using( changelogGit.value ) { implicit git ⇒
                Helper.resolveDefaultRange( changelogRecentTags.value )
            }

            val commits = changelogCommits.value( Some( range ) ).toList

            NonEmptyList
                .fromList( commits )
                .getOrElse {
                    sys.error( "No recent commits found" )
                }
        },
        changelogGeneratePromptEdit := true,
        changelogGenerate := {
            implicit val log = streams.value.log

            val name = changelogEntryName.value
            val title = changelogEntryNameFormat.value( name )
            val pattern = changelogEntryDatePattern.value
            val date = changelogEntryDate.value map {
                new SimpleDateFormat( pattern ).format( _ )
            } map {
                changelogEntryDateFormat.value
            }
            val commits = changelogEntryCommits.value
            val content = commits
                .map( changelogEntryCommitFormat.value )
                .toList
                .mkString( "\n" )
            val entry = changelogFormatEntry.value( title, date, content )

            val edited = if ( changelogGeneratePromptEdit.value ) {
                val input = changelogEntryFile.value
                IO.write( input, entry )
                Interaction.editFile(
                    "Edit this file to apply changes",
                    entry,
                    input,
                    false
                )
            } else {
                entry
            }

            val output = changelogFile.value

            if ( !output.exists() ) {
                output.getParentFile.mkdirs()
                output.createNewFile()
            }

            val current = IO.read( output ) match {
                case ""      ⇒ changelogTemplate.value
                case content ⇒ content
            }

            val lines = current.split( "\n" )

            if ( current.contains( title ) ) {
                sys.error( s"Release entry $name does already exist" )
            }

            val ( header, footer ) = lines
                .span( line ⇒ !changelogEntryCheck.value( line ) )

            val changelog = s"""
                  |${header.mkString( "\n" )}
                  |$edited
                  |
                  |${footer.mkString( "\n" )}
                """.stripMargin.trim

            IO.write( output, changelog )
            changelog
        }
    )
}