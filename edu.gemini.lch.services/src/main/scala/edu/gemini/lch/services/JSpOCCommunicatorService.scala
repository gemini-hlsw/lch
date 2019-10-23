package edu.gemini.lch.services

import java.io.File
import edu.gemini.lch.model.Site

import scala.collection.immutable.Map
import scala.util.{Failure, Success, Try}


trait JSpOCCommunicatorService {
  import JSpOCCommunicatorService._

  /**
   * Connect to the JSpOC service.
   * This (or the other connect method) must be called to authenticate prior to calling any of the other methods.
   * @param site the site (Site.NORTH or Site.SOUTH) for which to authenticate: credentials are stored in the impl
   * @return Success, or Failure[Throwable]
   */
  def connect(site: Site): Try[Unit]

  /**
   * Connect to the JSpOC service using the supplied credentials.
   * This (or the other connect method) must be called to authenticate prior to calling any of the other methods.
   * @param username the username / identity
   * @param password the password for the username
   * @return Success, or Failure[Throwable]
   */
  def connect(username: String, password: String): Try[Unit]

  /**
   * Download a file from JSpOC to a local file.
   * @param remoteFilename the name of the file on the JSpOC server
   * @param destination a File object representing the local file to which to store the remote file
   * @return Success, or Failure[Throwable]
   */
  def downloadFile(remoteFilename: String, destination: File): Try[Unit]

  /**
   * Download a file using its unique FILE_ID from JSpOC to a local file.
   * @param fileId the unique FILE_ID corresponding to the file as per JSpOC
   * @param destination a File object representing the local file to which to store the remote file
   * @return Success, or Failure[Throwable]
   */
  def downloadFile(fileId: Int, destination: File): Try[Unit]

  /**
   * Provide access to a file on JSpOC as a byte array.
   * @param fileId the unique FILE_ID corresponding to the file as per JSpOC
   * @return Success(byte array) on success, Failure[Throwable] on failure
   */
  def downloadFileAsByteArray(fileId: Int): Try[Array[Byte]]

  /**
   * Given a folder ID, find all the new files contained in that JSpOC folder and download the requested attributes and
   * return them in a map. The maps of file info are sorted by the first FileAttribute in the attribute list.
   * @param folderId the ID of the JSpOC folder
   * @param attributeList the list of desired attributes to return, with defaults FileId and Name
   * @return Success(list of FileAttribute -> String pairs) representing the file info on success, Failure[Throwable] on failure
   */
  def newFileInfo(folderId: Int, currentMaxId: Int)(implicit attributeList: List[FileAttribute] = List(FileId, Name)): Try[List[Map[FileAttribute,String]]]

  /**
   * Given a folder ID, determine a list of the file IDs and names corresponding to newer event files in the folder
   * than what we currently have stored.
   * @param folderId the ID of the JSpOC folder
   * @return Success(list containing the new IDs) on success, Failure[Throwable] on failure
   */
  def newFileIDs(folderId: Int, currentMaxId: Int): Try[List[(Int,String)]]

  /**
   * Given a site, poll the upload folder for new PAM files and if there are any, download the files and return
   * a list of pairs (filename, contents) where contents consist of a byte array.
   *
   * @note An empty list is returned in the case of failure: this is ugly, but useful since this is used in Java.
   * @param site the site, used to select the PAM directory
   * @return Success(list of pairs (filename, contents)) where contents consist of a byte array on success,
   *         Failure[Throwable] on failure.
   */
  def downloadNewPAMFiles(site: Site, currentMaxId: Int): Try[List[PamFile]]

  /**
   * Gets the result of downloadNewPamFiles as a java list.
   * @param site the site, used to select the PAM directory and determine the latest file
   * @return A Java list of pairs (filename, contents) where contents consists of a byte array
   */
  def downloadNewPAMFilesAsJava(site: Site, currentMaxId: Int): java.util.List[PamFile]

  /**
   * Upload a local file to the PRM directory for the given site.
   * @param site the site, used to select the PRM directory
   * @param name the name of the file
   * @param bytes the content of the file
   * @return true on success, false otherwise
   */
  def uploadPRMFile(site: Site, name: String, bytes: Array[Byte]): Try[Unit]

}

case class PamFile(folderId: Int, fileId: Int, name: String, content: Array[Byte])

object JSpOCCommunicatorService {
  sealed class FileAttribute(n: String) {
    val name = n
  }
  case object FileId extends FileAttribute("FILE_ID")
  case object Name extends FileAttribute("FILE_NAME")
  case object FolderId extends FileAttribute("FOLDER_ID")
  case object FullPath extends FileAttribute("FILE_FULLPATH")
  case object UploadTimeStamp extends FileAttribute("FILE_UPLOADED")
  case object SizeInBytes extends FileAttribute("FILE_BYTES")
  case object ContentType extends FileAttribute("FILE_TYPE")

  // To run things in Java to convert Try[A] to A / thrown exception.
  def runAsJava[A](result: Try[A]): A = result match {
    case Success(vl) => vl
    case Failure(ex) => throw ex
  }
}