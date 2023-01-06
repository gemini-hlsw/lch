package edu.gemini.lch.services.impl

import java.io._
import java.net.URLEncoder
import javax.annotation.Resource

import edu.gemini.lch.model.Site
import edu.gemini.lch.services.{JSpOCCommunicatorService, LaserNightService, PamFile}
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.cookie.CookiePolicy
import org.apache.commons.httpclient.methods._
import org.apache.commons.httpclient.methods.multipart.{ByteArrayPartSource, FilePart, MultipartRequestEntity, Part}
import org.apache.commons.httpclient.{HttpClient, HttpMethod, HttpStatus, UsernamePasswordCredentials}
import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils

import scala.collection.JavaConverters._
import scala.collection.immutable.{HashMap, Map}
import scala.io.Source
import scala.util.Try
import scala.util.parsing.json.JSON


@Service
class JSpOCCommunicatorServiceImpl extends JSpOCCommunicatorService {

  private val LOGGER: Logger = Logger.getLogger(classOf[JSpOCCommunicatorServiceImpl])

  import JSpOCCommunicatorService._
  import JSpOCCommunicatorServiceImpl._

  @Value("${lch.jspoc.pam.id}")
  private var pamFolderId: Int = 339  // 339 is zLCH/Gemini North Test/PAM; value will be replaced with value from configuration

  @Value("${lch.jspoc.prm.id}")
  private var prmFolderId: Int = 339  // 339 is zLCH/Gemini North Test/PAM; value will be replaced with value from configuration

  @Resource
  private var laserNightService: LaserNightService = null

  private val client = new HttpClient()

  /**
   * @inheritdoc
   */
  override def connect(site: Site): Try[Unit] =
    connect(JSpOCCredentials.CredentialsLookup(site))


  /**
   * @inheritdoc
   */
  override def connect(username: String, password: String): Try[Unit] =
    connect(new UsernamePasswordCredentials(username, password))


  /**
   * Internal connection method that establishes and authenticates a connection to JSpOC using the supplied credentials.
   * @param credentials identity / password combination
   * @return true on successful connection and authentication, false otherwise
   */
  private def connect(credentials: UsernamePasswordCredentials): Try[Unit] = {
    client.getState.setCredentials(JSpOCAuthScope, credentials)
    client.getParams.setCookiePolicy(CookiePolicy.RFC_2109)

    // Login explicitly, which is needed.
    val loginURL = "https://" + Host + "/ajaxauth/login/"
    val loginPost = new PostMethod(loginURL)
    loginPost.addParameter("identity", credentials.getUserName)
    loginPost.addParameter("password", credentials.getPassword)

    processMethod(loginPost)
  }


  /**
   * Encodes a filename for use by JSpOC. Note that URLEncode.encode always converts ' ' to '+' as per the
   * specification, but JSpOC expects "%20" instead, which is why this must be done with some intervention.
   * @param filename the filename to encode
   * @return the JSpOC-encoded filename
   */
  private def encodeFilename(filename: String): String =
    URLEncoder.encode(filename, "UTF-8").replace("+", "%20")


  /**
   * Parse the simple JSpOC JSON response into a map of key - value pairs.
   * @param response the response to parse
   * @return Some(Map[String,String]) if the parsing was successful and there is useful data, and None otherwise
   */
  private def parseSimpleJSON(response: String): Option[List[Map[String, String]]] =
    JSON.parseFull(response).map(_.asInstanceOf[List[Map[String, String]]])


  /**
   * Given a filename on the remote JSpOC server, retrieve its ID.
   * @param filename the filename on the server
   * @return the ID, or -1 if no such ID, wrapped in a Try for exception handling
   */
  private def getFileID(filename: String): Try[Int] = {
    val fileIDURL = "https://" + Host + "/fileshare/query/class/file/FILE_NAME/" + encodeFilename(filename)
    val method = new GetMethod(fileIDURL)
    method.setFollowRedirects(true)

    processMethodR(method)(postExecute = () =>
      // Note that we use get here to deliberately cause a NoSuchElementException if there is no FileId.
      parseSimpleJSON(method.getResponseBodyAsString).flatMap {
        jsn => jsn.headOption.map(_(FileId.name).toInt)
      }.get
    )
  }


  /**
   * @inheritdoc
   */
  override def downloadFile(remoteFilename: String, destination: File): Try[Unit] =
    getFileID(remoteFilename).flatMap(downloadFile(_, destination))


  /**
   * @inheritdoc
   */
  override def downloadFile(fileId: Int, destination: File): Try[Unit] = {
    val fileContentsURL = "https://" + Host + "/fileshare/query/class/download/FILE_ID/" + fileId + "/format/stream"
    val method = new GetMethod(fileContentsURL)
    method.setFollowRedirects(true)

    processMethod(method)(postExecute = () => {
      val is = method.getResponseBodyAsStream
      val bfw = new BufferedWriter(new FileWriter(destination))
      val s = Source.fromInputStream(is)

      for (line <- s.getLines()) {
        bfw.write(line)
        bfw.newLine()
      }
      bfw.close()
    })
  }

  /**
   * @inheritdoc
   */
  override def downloadFileAsByteArray(fileId: Int): Try[Array[Byte]] = {
    val fileContentsURL = "https://" + Host + "/fileshare/query/class/download/FILE_ID/" + fileId + "/format/stream"
    val method = new GetMethod(fileContentsURL)
    method.setFollowRedirects(true)
    processMethodR(method)(postExecute = () => FileCopyUtils.copyToByteArray(method.getResponseBodyAsStream))
  }


  /**
   * @inheritdoc
   */
  override def newFileInfo(folderId: Int, currentMaxId: Int)(implicit attributeList: List[FileAttribute] = List(FileId, Name)): Try[List[Map[FileAttribute, String]]] = {
    val maxId = currentMaxId
    val idFetchURL = "https://" + Host + "/fileshare/query/class/file/FOLDER_ID/" + folderId + "/FILE_ID/%3E" + maxId + attributeList.headOption.fold("")("/orderby/" + _.name)
    val method = new GetMethod(idFetchURL)
    method.setFollowRedirects(true)

    processMethodR(method)(postExecute = () => {
      parseSimpleJSON(method.getResponseBodyAsString).map { json =>
        for {
          fileInfo <- json.filter(map => attributeList.forall(attr => map.keySet.contains(attr.name)))
        } yield attributeList.map(attr => (attr, fileInfo(attr.name))).toMap
      }.getOrElse(Nil)
    })
  }


  /**
   * @inheritdoc
   */
  override def newFileIDs(folderId: Int, currentMaxId: Int): Try[List[(Int, String)]] =
    newFileInfo(folderId, currentMaxId).map { lst =>
      lst.map(x => (x(FileId).toInt, x(Name)))
    }


  /**
   * @inheritdoc
   */
  override def downloadNewPAMFiles(site: Site, currentMaxId: Int): Try[List[PamFile]] = {
    newFileIDs(pamFolderId, currentMaxId).map(_.map { case (id, name) => PamFile(pamFolderId, id, name, downloadFileAsByteArray(id).get)})
  }

  /**
   * @inheritdoc
   */
  override def downloadNewPAMFilesAsJava(site: Site, currentMaxId: Int): java.util.List[PamFile] =
    runAsJava(downloadNewPAMFiles(site, currentMaxId)).asJava


  /**
   * @inheritdoc
   */
  override def uploadPRMFile(site: Site, name: String, bytes: Array[Byte]): Try[Unit] = {
    val uploadURL = "https://" + Host + "/fileshare/query/class/upload/FOLDER_ID/" + prmFolderId
    val uploadPost = new PostMethod(uploadURL)

    processMethod(uploadPost)(
      status = Set(HttpStatus.SC_OK, HttpStatus.SC_CREATED, HttpStatus.SC_ACCEPTED).contains,
      preExecute = () => {
        val source = new ByteArrayPartSource(name, bytes)
        val parts = Array[Part](new FilePart("file", source))
        uploadPost.setRequestEntity(new MultipartRequestEntity(parts, uploadPost.getParams))
      })
  }


  /**
   * Encapsulates the common pattern for executing an HttpMethod of type M and returning boolean indicating success.
   * @param method the method which we want to execute.
   * @param status a function that maps return codes to boolean to indicate success of the execution: failure results
   *               in an HttpFailureException being thrown
   * @param preExecute any pre-processing that must be done on the method before execution
   * @param postExecute any postprocessing that must be done after method execution
   * @tparam M the type of HttpMethod to perform
   * @return Success(Unit) if successful as determined by the status function, and Failure[Exception] if failure
   */
  private def processMethod[M <: HttpMethod](method: M)
                                            (implicit status: Int => Boolean  = _ == HttpStatus.SC_OK,
                                                      preExecute:  EmptyFunction = DoNothing,
                                                      postExecute: EmptyFunction = DoNothing): Try[Unit] =
   processMethodR(method)(() => postExecute())(status, preExecute)


  /**
   * Encapsulates the common pattern for executing an HttpMethod of type M and returning a value processed from the
   * request, if possible.
   * @param method the method which we want to execute.
   * @param postExecute any postprocessing that must be done after method execution to obtain the result
   * @param status a function that maps return codes to boolean to indicate success of the execution: failure results
   *               in an HttpFailureException being thrown
   * @param preExecute any pre-processing that must be done on the method before execution
   * @tparam M the type of HttpMethod to perform
   * @tparam R the type of the result
   * @return Success[R] if a result was processed correctly, and Failure[Exception] if failure
   */
  private def processMethodR[M <: HttpMethod, R](method: M)
                                                (postExecute:         ()   =>  R)
                                                (implicit status:     Int  => Boolean = _ == HttpStatus.SC_OK,
                                                          preExecute: EmptyFunction   = DoNothing): Try[R] = {
    Try {
      preExecute()
      val statusResult = client.executeMethod(method)
      LOGGER.debug("Space-Track: " + method.getResponseBodyAsString)
      if (status(statusResult))
        postExecute()
      else
        throw method
    } Finally {
      method.releaseConnection()
    }
  }
}


object JSpOCCommunicatorServiceImpl {
  // The server information
  private val Host           = "www.space-track.org"
  private val Port           = 443
  private val JSpOCAuthScope = new AuthScope(Host, Port)

  // We need a way to perform the equivalent of a "finally" to release method connections.
  implicit class TryOps[T](val t: Try[T]) extends AnyVal {
    def Finally[Ignore](effect: => Ignore): Try[T] = {
      val ignoring = (_: Any) => { effect; t }
      t transform (ignoring, ignoring)
    }
  }

  // Convert a method directly into an HttpMethod.
  implicit def methodToHttpFailureException(method: HttpMethod): RuntimeException =
    new RuntimeException(s"Could not execute ${method.getURI}: status=${method.getStatusCode}, response=${method.getResponseBodyAsString}")

  type EmptyFunction = () => Unit
  val  DoNothing: EmptyFunction = () => {}
}