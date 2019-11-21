package edu.gemini.lch.services.impl

import edu.gemini.lch.model.Site
import org.apache.commons.httpclient.UsernamePasswordCredentials

import scala.collection.immutable.HashMap

object JSpOCCredentials {
  val gsUserName = Option(System.getenv("GS_USERNAME")).getOrElse("")
  val gsPassword = Option(System.getenv("GS_PASSWORD")).getOrElse("")
  val gnUserName = Option(System.getenv("GN_USERNAME")).getOrElse("")
  val gnPassword = Option(System.getenv("GN_PASSWORD")).getOrElse("")

  // The Gemini credentials by site.
  val CredentialsLookup = HashMap[Site, UsernamePasswordCredentials](
    Site.NORTH -> new UsernamePasswordCredentials(gnUserName, gnPassword),
    Site.SOUTH -> new UsernamePasswordCredentials(gsUserName, gsPassword)
  )
}
