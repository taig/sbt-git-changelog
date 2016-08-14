package io.taig.sbt.changelog

import java.io.File

import sbt.{ IO, Logger, SimpleReader }

object Interaction {
    /**
     * Ask the user to edit a file
     */
    def editFile( message: String, content: String, file: File, allowEmpty: Boolean = false )(
        implicit
        l: Logger
    ): String = {
        def recursion( content: String, file: File ): String = {
            l.info( "Done?" )
            ask( {
                val current = IO.read( file )

                if ( content != IO.read( file ) ) {
                    l.info( "Updated file:" )
                    l.info( "" )
                    l.info( current )
                    l.info( "" )
                    l.info( "Edit this file to make changes:" )
                    l.info( file.getAbsolutePath )
                    recursion( current, file )
                } else if ( !allowEmpty && current.isEmpty ) {
                    l.error( "File can not be empty. Edit this file:" )
                    l.error( file.getAbsolutePath )
                    recursion( current, file )
                } else {
                    current
                }
            }, abortPrompt() )
        }

        l.info( message )
        l.info( file.getAbsolutePath )

        recursion( content, file )
    }

    def prompt[O]( default: String )( f: String ⇒ O )(
        implicit
        l: Logger
    ): O = prompt( default, f )( PartialFunction.empty )

    def prompt[O]( default: String, f: String ⇒ O )( g: PartialFunction[String, O] )(
        implicit
        l: Logger
    ): O = {
        SimpleReader.readLine( s"[$default]> " ) match {
            case Some( input ) ⇒ g.applyOrElse[String, O]( input, {
                case "" ⇒ f( default )
                case _  ⇒ f( input )
            } )
            case None ⇒
                println( "" )
                sys.error( "Interactive prompt aborted" )
        }
    }

    def ask[O]( yes: ⇒ O ): Option[O] = ask( Some( yes ), None )

    def ask[O]( yes: ⇒ O, no: ⇒ O ): O = {
        SimpleReader.readLine( s"[yes]> " ) match {
            case Some( "" | "y" | "yes" ) ⇒ yes
            case Some( _ )                ⇒ no
            case None                     ⇒ abortPrompt( addLinebreak = true )
        }
    }

    def abortPrompt( addLinebreak: Boolean = false ) = {
        if ( addLinebreak ) {
            println( "" )
        }

        sys.error( "Interactive prompt aborted" )
    }
}