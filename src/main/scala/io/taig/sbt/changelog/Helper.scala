package io.taig.sbt.changelog

import cats.data.Xor
import cats.syntax.xor._
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.lib.{ AnyObjectId, Constants }
import org.eclipse.jgit.revwalk.RevCommit

import scala.collection.JavaConversions._
import scala.language.reflectiveCalls

object Helper {
    /**
     * A Range is either a SINCE..UNTIL combination, or only a start value
     * such as HEAD
     */
    type Range = ( AnyObjectId, AnyObjectId ) Xor AnyObjectId

    /**
     * Gets the most recent tags (one commit may have several tags) ignoring
     * the current HEAD
     */
    def recentTags( implicit g: Git ): List[String] = {
        try {
            val commits = g.log().call().iterator().toList.drop( 1 )
            val tags = g.tagList().call().toList

            commits
                .map { commit ⇒
                    tags
                        .filter( _.getObjectId == commit.getId )
                        .map( _.getName )
                        .filter( _.startsWith( "refs/tags/" ) )
                        .map( _.replace( "refs/tags/", "" ) )
                }
                .headOption
                .getOrElse( List.empty )
        } catch {
            case _: NoHeadException ⇒ List.empty
        }
    }

    /**
     * Gets a commit Iterator for all commits from the given range
     */
    def commits( range: Option[Range] )( implicit g: Git ): Iterator[RevCommit] = {
        val iterator = for {
            ref ← Option( g.getRepository.exactRef( Constants.HEAD ) )
            id ← Option( ref.getObjectId )
        } yield {
            val log = g.log()

            range.foreach {
                case Xor.Left( ( since, until ) ) ⇒ log.addRange( since, until )
                case Xor.Right( start )           ⇒ log.add( start )
            }

            log.call().iterator().to[Iterator]
        }

        iterator.getOrElse( Iterator.empty )
    }

    def parseRange( range: String )( implicit g: Git ): Option[( String, String ) Xor String] = {
        range.split( "\\.\\." ) match {
            case Array( since, until ) ⇒ Some( ( since, until ).left )
            case Array( start )        ⇒ Some( start.right )
            case _                     ⇒ None
        }
    }

    def resolveDefaultRange( tags: List[String] )( implicit g: Git ): Range = {
        recentTags match {
            case head :: _ ⇒ ( resolve( head ), HEAD ).left
            case _         ⇒ HEAD.right
        }
    }

    def HEAD( implicit g: Git ) = resolve( "HEAD" )

    def resolve( id: String )( implicit g: Git ) = {
        g.getRepository.resolve( id )
    }

    def using[T <: { def close() }, U]( resource: T )( block: T ⇒ U ): U = {
        try {
            block( resource )
        } finally {
            if ( resource != null ) resource.close()
        }
    }
}