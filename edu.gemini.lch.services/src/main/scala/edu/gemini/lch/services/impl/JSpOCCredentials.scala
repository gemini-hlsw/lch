package edu.gemini.lch.services.impl

import edu.gemini.lch.model.Site
import org.apache.commons.httpclient.UsernamePasswordCredentials

import scala.collection.immutable.HashMap

object JSpOCCredentials {
  def unsafeGetEnv(key: String): String =
    Option(System.getenv(key)).getOrElse {
      System.err.println("Missing environment variables")
      // Absolutely kill the container
      sys.exit(-1)
    }
  val gsUserName = unsafeGetEnv("GS_USERNAME")
  val gsPassword = unsafeGetEnv("GS_PASSWORD")
  val gnUserName = unsafeGetEnv("GN_USERNAME")
  val gnPassword = unsafeGetEnv("GN_PASSWORD")

  // The Gemini credentials by site.
  val CredentialsLookup = HashMap[Site, UsernamePasswordCredentials](
    Site.NORTH -> new UsernamePasswordCredentials(gnUserName, gnPassword),
    Site.SOUTH -> new UsernamePasswordCredentials(gsUserName, gsPassword)
  )
}
