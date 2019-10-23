package edu.gemini.lch.services.simulators

import java.io._

import edu.gemini.lch.model.Site
import org.springframework.stereotype.Service

import scala.collection.immutable.Map
import scala.util.Try
import edu.gemini.lch.services.{PamFile, JSpOCCommunicatorService}


/**
 * Do nothing implementation that can be used for tests.
 */
@Service
class JSpOCCommunicatorServiceSimulator extends JSpOCCommunicatorService {

  import JSpOCCommunicatorService._

  /**
   * @inheritdoc
   */
  override def connect(site: Site): Try[Unit] = Try {}


  /**
   * @inheritdoc
   */
  override def connect(username: String, password: String): Try[Unit] = Try {}


  /**
   * @inheritdoc
   */
  override def downloadFile(remoteFilename: String, destination: File): Try[Unit] = Try {}


  /**
   * @inheritdoc
   */
  override def downloadFile(fileId: Int, destination: File): Try[Unit] = Try {}

  /**
   * @inheritdoc
   */
  override def downloadFileAsByteArray(fileId: Int): Try[Array[Byte]] = Try { new Array[Byte](0) }


  /**
   * @inheritdoc
   */
  override def newFileInfo(folderId: Int, currentMaxId: Int)(implicit attributeList: List[FileAttribute] = List(FileId, Name)): Try[List[Map[FileAttribute, String]]] = Try { List() }


  /**
   * @inheritdoc
   */
  override def newFileIDs(folderId: Int, currentMaxId: Int): Try[List[(Int, String)]] = Try { List() }


  /**
   * @inheritdoc
   */
  override def downloadNewPAMFiles(site: Site, currentMaxId: Int): Try[List[PamFile]] = Try { List() }

  /**
   * @inheritdoc
   */
  override def downloadNewPAMFilesAsJava(site: Site, currentMaxId: Int): java.util.List[PamFile] = new java.util.ArrayList[PamFile]()


  /**
   * @inheritdoc
   */
  override def uploadPRMFile(site: Site, name: String, bytes: Array[Byte]): Try[Unit] =  Try {}

}


