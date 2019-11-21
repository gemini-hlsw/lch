package edu.gemini.lch.services.impl

import edu.gemini.lch.model.Site
import org.apache.commons.httpclient.UsernamePasswordCredentials

import scala.collection.immutable.HashMap

object JSpOCCredentials {
  val gsUserName = System.getenv("GS_USERNAME")
  val gsPassword = System.getenv("GS_PASSWORD")
  val gnUserName = System.getenv("GN_USERNAME")
  val gnPassword = System.getenv("GN_PASSWORD")
  // The Gemini credentials by site.
  val CredentialsLookup = HashMap[Site, UsernamePasswordCredentials](
    Site.NORTH -> new UsernamePasswordCredentials(gnUserName, gnPassword),
    Site.SOUTH -> new UsernamePasswordCredentials(gsUserName, gsPassword)
  )
}
